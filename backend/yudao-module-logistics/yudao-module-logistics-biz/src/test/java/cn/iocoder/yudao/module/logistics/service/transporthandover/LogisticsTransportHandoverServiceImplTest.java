package cn.iocoder.yudao.module.logistics.service.transporthandover;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.api.transport.LogisticsTransportApi;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportHandoverRespDTO;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportNodeRespDTO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporthandover.LogisticsTransportHandoverDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporthandover.LogisticsTransportHandoverMapper;
import cn.iocoder.yudao.module.logistics.api.transport.LogisticsTransportApiImpl;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsHandoverDocumentStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transportnode.impl.LogisticsTransportNodeServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transportstop.LogisticsTransportStopService;
import cn.iocoder.yudao.module.logistics.service.transportstop.impl.LogisticsTransportStopServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.transporttask.impl.LogisticsTransportTaskServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporthandover.impl.LogisticsTransportHandoverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import cn.iocoder.yudao.module.logistics.service.vehicle.impl.LogisticsVehicleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 交接登记的单元测试（V6 #73）。
 *
 * <p>钉住三件事：
 * <ol>
 *   <li><b>现场只登记事实</b>——品类 / 参考量 / 参考单价 / 凭证照片原样存下，没有金额，
 *       也没有因为登记就产生任何收购；</li>
 *   <li><b>待补档是一条独立状态</b>——缺要件照记，记了就必须说明缺什么；</li>
 *   <li><b>一家一条</b>——集货时按停靠点各自登记，同一停靠点不重复登记。</li>
 * </ol>
 *
 * <p>「未复磅不得生成收购单」在 icbc 侧钉（收购单在那边生成，收购登记必须引用有效磅次）。
 */
@Import({LogisticsTransportHandoverServiceImpl.class, LogisticsTransportTaskServiceImpl.class,
        LogisticsTransportStopServiceImpl.class, LogisticsTransportNodeServiceImpl.class,
        LogisticsVehicleServiceImpl.class, LogisticsDriverServiceImpl.class,
        LogisticsTransportApiImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsTransportHandoverServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsTransportHandoverService logisticsTransportHandoverService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportStopService logisticsTransportStopService;
    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsTransportHandoverMapper logisticsTransportHandoverMapper;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;
    @Resource
    private LogisticsTransportApi logisticsTransportApi;

    @Test
    public void testCreateHandover_storesFactsAndSnapshots_withoutAnyAmount() {
        Long taskId = createTaskWithStops("A 家地址");
        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();

        Long handoverId = logisticsTransportHandoverService.createHandover(
                newHandover(taskId, stopId, "A 家"));

        LogisticsTransportHandoverDO handover = logisticsTransportHandoverMapper.selectById(handoverId);
        assertNotNull(handover.getHandoverNo());
        assertEquals(task.getTaskNo(), handover.getTaskNo());
        assertEquals(1024L, handover.getPayeeId());
        assertEquals(2048L, handover.getGoodsConfigId());
        assertEquals("废钢铁", handover.getCategoryName());
        // 参考量 / 参考单价是**现场约定值**，原样存下：没有任何「金额 = 量 × 价」的派生
        assertEquals(0, new BigDecimal("12.5").compareTo(handover.getReferenceQuantity()));
        assertEquals(0, new BigDecimal("2600.00").compareTo(handover.getReferenceUnitPrice()));
        // 司机与车辆：引用 + 快照并存（ADR 0032 第 7 条）
        assertEquals(task.getDriverId(), handover.getDriverId());
        assertEquals(task.getVehicleId(), handover.getVehicleId());
        assertEquals(task.getPlateNo(), handover.getPlateNo());
        assertEquals(task.getDriverName(), handover.getDriverName());
        // 不填即「已齐」
        assertEquals(LogisticsHandoverDocumentStatusEnum.COMPLETE.getStatus(), handover.getDocumentStatus());
        assertEquals(1, logisticsTransportHandoverMapper.selectCountByStopId(stopId));
    }

    @Test
    public void testCreateHandover_idempotentByClientRequestId() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        LogisticsTransportHandoverCreateReqVO reqVO = newHandover(taskId, stopId, "A 家");
        reqVO.setClientRequestId("offline-req-1");

        Long first = logisticsTransportHandoverService.createHandover(reqVO);
        Long second = logisticsTransportHandoverService.createHandover(reqVO);

        assertEquals(first, second, "弱网重复提交必须返回同一条");
        assertEquals(1, logisticsTransportHandoverMapper.selectCountByStopId(stopId));
    }

    @Test
    public void testCreateHandover_sameStopTwice_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        logisticsTransportHandoverService.createHandover(newHandover(taskId, stopId, "A 家"));

        assertServiceException(() -> logisticsTransportHandoverService.createHandover(
                newHandover(taskId, stopId, "A 家")), TRANSPORT_HANDOVER_STOP_ALREADY_REGISTERED);
    }

    @Test
    public void testCreateHandover_taskHasStopsButNoStopId_isRejected() {
        Long taskId = createTaskWithStops("A 家地址", "B 家地址");

        assertServiceException(() -> logisticsTransportHandoverService.createHandover(
                newHandover(taskId, null, "A 家")), TRANSPORT_HANDOVER_STOP_REQUIRED);
    }

    @Test
    public void testCreateHandover_stopNotBelongToTask_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long otherTaskId = createTaskWithStops("D 家地址");
        Long foreignStopId = logisticsTransportStopService.getStopListByTaskId(otherTaskId).get(0).getId();

        assertServiceException(() -> logisticsTransportHandoverService.createHandover(
                newHandover(taskId, foreignStopId, "D 家")), TRANSPORT_HANDOVER_STOP_NOT_BELONG_TO_TASK);
    }

    @Test
    public void testCreateHandover_withoutPayee_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        LogisticsTransportHandoverCreateReqVO reqVO = newHandover(taskId, stopId, "A 家");
        reqVO.setPayeeId(null);

        assertServiceException(() -> logisticsTransportHandoverService.createHandover(reqVO),
                TRANSPORT_HANDOVER_PAYEE_REQUIRED);
    }

    @Test
    public void testCreateHandover_withoutPhotos_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        LogisticsTransportHandoverCreateReqVO reqVO = newHandover(taskId, stopId, "A 家");
        reqVO.setPhotos(List.of());

        assertServiceException(() -> logisticsTransportHandoverService.createHandover(reqVO),
                TRANSPORT_HANDOVER_PHOTO_REQUIRED);
    }

    @Test
    public void testCreateHandover_nonPositiveQuantityOrNegativePrice_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        LogisticsTransportHandoverCreateReqVO zeroQuantity = newHandover(taskId, stopId, "A 家");
        zeroQuantity.setReferenceQuantity(BigDecimal.ZERO);
        assertServiceException(() -> logisticsTransportHandoverService.createHandover(zeroQuantity),
                TRANSPORT_HANDOVER_QUANTITY_INVALID);

        LogisticsTransportHandoverCreateReqVO negativePrice = newHandover(taskId, stopId, "A 家");
        negativePrice.setReferenceUnitPrice(new BigDecimal("-1"));
        assertServiceException(() -> logisticsTransportHandoverService.createHandover(negativePrice),
                TRANSPORT_HANDOVER_QUANTITY_INVALID);
    }

    @Test
    public void testCreateHandover_pendingDocument_requiresGapAndIsStored() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        LogisticsTransportHandoverCreateReqVO withoutGap = newHandover(taskId, stopId, "A 家");
        withoutGap.setDocumentStatus(LogisticsHandoverDocumentStatusEnum.PENDING.getStatus());
        assertServiceException(() -> logisticsTransportHandoverService.createHandover(withoutGap),
                TRANSPORT_HANDOVER_DOCUMENT_GAP_REQUIRED);

        LogisticsTransportHandoverCreateReqVO withGap = newHandover(taskId, stopId, "A 家");
        withGap.setDocumentStatus(LogisticsHandoverDocumentStatusEnum.PENDING.getStatus());
        withGap.setDocumentGap("缺身份证");
        Long handoverId = logisticsTransportHandoverService.createHandover(withGap);

        LogisticsTransportHandoverDO handover = logisticsTransportHandoverMapper.selectById(handoverId);
        assertEquals(LogisticsHandoverDocumentStatusEnum.PENDING.getStatus(), handover.getDocumentStatus());
        assertEquals("缺身份证", handover.getDocumentGap());
        // 待补档是**事实状态**，不是失败：登记照常成功，门禁在 icbc 侧的付款与开票
        LogisticsTransportHandoverRespVO resp = logisticsTransportHandoverService.toResp(handover);
        assertEquals("待补档", resp.getDocumentStatusName());
    }

    @Test
    public void testCreateHandover_unknownDocumentStatus_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        LogisticsTransportHandoverCreateReqVO reqVO = newHandover(taskId, stopId, "A 家");
        reqVO.setDocumentStatus("MAYBE");

        assertServiceException(() -> logisticsTransportHandoverService.createHandover(reqVO),
                TRANSPORT_HANDOVER_DOCUMENT_STATUS_UNKNOWN);
    }

    @Test
    public void testCreateHandover_pendingOrCancelledTask_isRejected() {
        // 待分配：还没派车派人，这一趟根本还没跑
        LogisticsTransportTaskCreateReqVO pending = new LogisticsTransportTaskCreateReqVO();
        pending.setDepartureAddress("城东场站");
        pending.setPickupAddress("A 家地址");
        Long pendingTaskId = logisticsTransportTaskService.createTask(pending);
        assertServiceException(() -> logisticsTransportHandoverService.createHandover(
                newHandover(pendingTaskId, null, "A 家")), TRANSPORT_HANDOVER_TASK_NOT_REGISTRABLE);

        Long cancelledTaskId = createTaskWithStops("A 家地址");
        LogisticsTransportTaskCancelReqVO cancel = new LogisticsTransportTaskCancelReqVO();
        cancel.setId(cancelledTaskId);
        cancel.setCancelReason("不跑了");
        logisticsTransportTaskService.cancelTask(cancel);
        Long stopId = logisticsTransportStopService.getStopListByTaskId(cancelledTaskId).get(0).getId();
        assertServiceException(() -> logisticsTransportHandoverService.createHandover(
                newHandover(cancelledTaskId, stopId, "A 家")), TRANSPORT_HANDOVER_TASK_NOT_REGISTRABLE);
    }

    @Test
    public void testGetHandoverByStopId_returnsRegistration() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        Long handoverId = logisticsTransportHandoverService.createHandover(newHandover(taskId, stopId, "A 家"));

        assertEquals(handoverId, logisticsTransportHandoverService.getHandoverByStopId(stopId).getId());
        assertNull(logisticsTransportHandoverService.getHandoverByStopId(-1L));
    }

    @Test
    public void testApi_handoverScopedEvidence_excludesOtherStopButKeepsTripLevelNodes() {
        Long taskId = createTaskWithStops("A 家地址", "B 家地址");
        List<cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportStopDO> stops =
                logisticsTransportStopService.getStopListByTaskId(taskId);
        Long stopA = stops.get(0).getId();
        Long stopB = stops.get(1).getId();
        Long handoverA = logisticsTransportHandoverService.createHandover(newHandover(taskId, stopA, "A 家"));

        // A 家的提货节点、B 家的提货节点，以及整趟活的收尾节点
        reportNode(taskId, stopA, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, "a-1");
        reportNode(taskId, stopA, LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED, "a-2", "photo-a");
        reportNode(taskId, stopB, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, "b-1");
        reportNode(taskId, null, LogisticsTransportNodeTypeEnum.UNLOADED, "u-1", "photo-u");

        List<LogisticsTransportNodeRespDTO> evidence = logisticsTransportApi.getEvidenceListByHandoverId(handoverA);

        // A 家的两个提货节点 + 卸货完成（整趟收尾也是这批货的凭证），不含 B 家的
        assertEquals(3, evidence.size());
        assertEquals(1, evidence.stream()
                .filter(node -> LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP.getType().equals(node.getNodeType()))
                .count(), "B 家的到达提货点不能被算进 A 家的凭证");
        assertEquals(1, evidence.stream()
                .filter(node -> LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED.getType().equals(node.getNodeType()))
                .count());
        assertEquals(1, evidence.stream()
                .filter(node -> LogisticsTransportNodeTypeEnum.UNLOADED.getType().equals(node.getNodeType()))
                .count(), "卸货完成是整趟收尾，也要作为这批货的运输凭证给出来");

        LogisticsTransportHandoverRespDTO dto = logisticsTransportApi.getHandover(handoverA);
        assertNotNull(dto);
        assertEquals("已齐", dto.getDocumentStatusName());
        assertEquals(1024L, dto.getPayeeId());
        assertEquals(Arrays.asList("https://file/handover-a.jpg"), dto.getPhotos());
        assertEquals(1, logisticsTransportApi.getRecentHandoverList().size());
    }

    // ==================== 辅助 ====================

    private Long createTaskWithStops(String... addresses) {
        LogisticsTransportTaskCreateReqVO task = new LogisticsTransportTaskCreateReqVO();
        task.setDepartureAddress("城东场站");
        task.setVehicleId(createVehicle());
        task.setDriverId(createDriver());
        task.setStops(Arrays.stream(addresses).map(address -> {
            LogisticsTransportStopSaveReqVO stop = new LogisticsTransportStopSaveReqVO();
            stop.setStopType(1);
            stop.setAddress(address);
            stop.setPayeeName(address.substring(0, 1) + " 家");
            stop.setPayeeMobile("13800138000");
            return stop;
        }).toList());
        return logisticsTransportTaskService.createTask(task);
    }

    private static LogisticsTransportHandoverCreateReqVO newHandover(Long taskId, Long stopId, String payeeName) {
        LogisticsTransportHandoverCreateReqVO reqVO = new LogisticsTransportHandoverCreateReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setStopId(stopId);
        reqVO.setPayeeId(1024L);
        reqVO.setPayeeName(payeeName);
        reqVO.setPayeeMobile("13800138000");
        reqVO.setGoodsConfigId(2048L);
        reqVO.setCategoryName("废钢铁");
        reqVO.setUnit("吨");
        reqVO.setReferenceQuantity(new BigDecimal("12.5"));
        reqVO.setReferenceUnitPrice(new BigDecimal("2600.00"));
        reqVO.setPhotos(Arrays.asList("https://file/handover-a.jpg"));
        reqVO.setOccurTime(LocalDateTime.now());
        return reqVO;
    }

    private Long createVehicle() {
        LogisticsVehicleSaveReqVO vehicle = new LogisticsVehicleSaveReqVO();
        vehicle.setPlateNo("浙A" + System.nanoTime() % 100000);
        vehicle.setStatus(LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        return logisticsVehicleService.createVehicle(vehicle);
    }

    private Long createDriver() {
        LogisticsDriverSaveReqVO driver = new LogisticsDriverSaveReqVO();
        driver.setUserId(System.nanoTime() % 100000 + 1);
        driver.setName("张三");
        driver.setMobile("13800138000");
        driver.setSource(LogisticsDriverSourceEnum.SELF.getSource());
        driver.setStatus(LogisticsDriverStatusEnum.ACTIVE.getStatus());
        return logisticsDriverService.createDriver(driver);
    }

    private Long reportNode(Long taskId, Long stopId, LogisticsTransportNodeTypeEnum type,
                            String clientRequestId, String... photos) {
        LogisticsTransportNodeReportReqVO reqVO = new LogisticsTransportNodeReportReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setStopId(stopId);
        reqVO.setNodeType(type.getType());
        reqVO.setNodeTime(LocalDateTime.now());
        reqVO.setLocation("现场");
        reqVO.setPhotos(photos.length == 0 ? null : Arrays.asList(photos));
        reqVO.setClientRequestId(clientRequestId);
        return logisticsTransportNodeService.reportNode(reqVO);
    }

}
