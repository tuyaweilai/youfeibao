package cn.iocoder.yudao.module.logistics.service.transportstop;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportStopDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportStopStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transportnode.impl.LogisticsTransportNodeServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transportstop.impl.LogisticsTransportStopServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.transporttask.impl.LogisticsTransportTaskServiceImpl;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 多停靠点集货的单元测试（V5 #72）。
 *
 * <p>钉住三件事：
 * <ol>
 *   <li><b>每个停靠点独立推进</b>——各自的节点与进度，互不相串；</li>
 *   <li><b>单点取消不影响其它点</b>——取消一家，另外两家的节点、进度、凭证都还在；</li>
 *   <li><b>模型不混点</b>——提货相关节点必须归到某一停靠点，整趟收尾的两类不许带停靠点。</li>
 * </ol>
 */
@Import({LogisticsTransportStopServiceImpl.class, LogisticsTransportNodeServiceImpl.class,
        LogisticsTransportTaskServiceImpl.class, LogisticsVehicleServiceImpl.class,
        LogisticsDriverServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsTransportStopServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsTransportStopService logisticsTransportStopService;
    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;

    @Test
    public void testCreateStops_assignsSequentialStopNo_andSnapshots() {
        Long taskId = createTaskWithStops("A 家地址", "B 家地址", "C 家地址");
        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);

        List<LogisticsTransportStopDO> stops = logisticsTransportStopService.getStopListByTaskId(taskId);
        assertEquals(3, stops.size());
        assertEquals(Arrays.asList(1, 2, 3), stops.stream().map(LogisticsTransportStopDO::getStopNo).toList());
        assertEquals("A 家地址", stops.get(0).getAddress());
        assertEquals("A 家", stops.get(0).getPayeeName());
        assertEquals(LogisticsTransportStopStatusEnum.PENDING.getStatus(), stops.get(0).getStatus());
        // 任务的提货点地址取**第一个停靠点**快照，便于列表展示；权威在停靠点上
        assertEquals("A 家地址", task.getPickupAddress());
    }

    @Test
    public void testCreateTask_withoutStopsAndWithoutPickupAddress_isRejected() {
        LogisticsTransportTaskCreateReqVO reqVO = new LogisticsTransportTaskCreateReqVO();
        // 既没有停靠点，也没有提货点地址：没有去处
        assertServiceException(() -> logisticsTransportTaskService.createTask(reqVO), TRANSPORT_TASK_STOP_REQUIRED);
    }

    @Test
    public void testStopProgress_isIndependentPerStop() {
        Long taskId = createTaskWithStops("A 家地址", "B 家地址");
        List<LogisticsTransportStopDO> stops = logisticsTransportStopService.getStopListByTaskId(taskId);
        Long stopA = stops.get(0).getId();
        Long stopB = stops.get(1).getId();

        // 只在 A 家上报「到达提货点」与「交接完成」
        reportNode(taskId, stopA, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, "a-1");
        reportNode(taskId, stopA, LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED, "a-2", "photo-a");

        List<LogisticsTransportStopRespVO> resps = logisticsTransportStopService.getStopRespListByTaskId(taskId);
        LogisticsTransportStopRespVO respA = resps.get(0);
        LogisticsTransportStopRespVO respB = resps.get(1);
        // A 家：已交接完成，缺「起运」；B 家：什么都没发生
        assertEquals(LogisticsTransportStopStatusEnum.COMPLETED.getStatus(), respA.getStatus());
        assertEquals(2, respA.getNodes().size());
        assertEquals(Collections.singletonList("起运"), respA.getMissingNodeNames());
        assertEquals(LogisticsTransportStopStatusEnum.PENDING.getStatus(), respB.getStatus());
        assertTrue(respB.getNodes().isEmpty(), "B 家的进度不能被 A 家的节点带动");
        assertEquals(Arrays.asList("到达提货点", "交接完成", "起运"), respB.getMissingNodeNames());
        // 还剩 1 家没提
        assertEquals(1, logisticsTransportStopService.getPendingStopCounts(Collections.singletonList(taskId))
                .getOrDefault(taskId, 0));
    }

    @Test
    public void testTaskScopedNodes_areNotCountedIntoAnyStopProgress() {
        Long taskId = createTaskWithStops("A 家地址");
        // 到达场站 / 卸货完成是整趟活的收尾，不带停靠点，也不该推进任何停靠点
        reportNode(taskId, null, LogisticsTransportNodeTypeEnum.ARRIVED_STATION, "station-1");
        reportNode(taskId, null, LogisticsTransportNodeTypeEnum.UNLOADED, "unload-1", "photo-u");

        List<LogisticsTransportStopRespVO> resps = logisticsTransportStopService.getStopRespListByTaskId(taskId);
        assertEquals(1, resps.size());
        assertEquals(LogisticsTransportStopStatusEnum.PENDING.getStatus(), resps.get(0).getStatus());
        assertTrue(resps.get(0).getNodes().isEmpty());
    }

    @Test
    public void testReportStopNode_withoutStopId_isRejected_whenTaskHasStops() {
        Long taskId = createTaskWithStops("A 家地址", "B 家地址");

        // 有停靠点却不说是哪一家：集货时不能把 A 家的「到达提货点」算到 B 家头上
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, null, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, "no-stop")),
                TRANSPORT_STOP_REQUIRED_FOR_NODE);
    }

    @Test
    public void testReportTaskScopedNode_withStopId_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();

        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, stopId, LogisticsTransportNodeTypeEnum.ARRIVED_STATION, "bad-scope")),
                TRANSPORT_STOP_NOT_ALLOWED_FOR_NODE);
    }

    @Test
    public void testReportNode_stopNotBelongToTask_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long otherTaskId = createTaskWithStops("D 家地址");
        Long foreignStopId = logisticsTransportStopService.getStopListByTaskId(otherTaskId).get(0).getId();

        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, foreignStopId, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, "foreign")),
                TRANSPORT_STOP_NOT_BELONG_TO_TASK);
    }

    @Test
    public void testCancelStop_onlyAffectsThatStop() {
        Long taskId = createTaskWithStops("A 家地址", "B 家地址", "C 家地址");
        List<LogisticsTransportStopDO> stops = logisticsTransportStopService.getStopListByTaskId(taskId);
        Long stopA = stops.get(0).getId();
        Long stopB = stops.get(1).getId();
        Long stopC = stops.get(2).getId();

        // A 家先交接完成
        reportNode(taskId, stopA, LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED, "a-1", "photo-a");

        // 取消 B 家（对方临时不卖了）
        LogisticsTransportStopCancelReqVO cancelReqVO = new LogisticsTransportStopCancelReqVO();
        cancelReqVO.setId(stopB);
        cancelReqVO.setCancelReason("对方临时不卖了");
        logisticsTransportStopService.cancelStop(cancelReqVO);

        // B 家已取消，不能再上报；A / C 家不受影响
        assertEquals(LogisticsTransportStopStatusEnum.CANCELLED.getStatus(),
                logisticsTransportStopService.getStop(stopB).getStatus());
        assertEquals(LogisticsTransportStopStatusEnum.COMPLETED.getStatus(),
                logisticsTransportStopService.getStop(stopA).getStatus());
        assertEquals(LogisticsTransportStopStatusEnum.PENDING.getStatus(),
                logisticsTransportStopService.getStop(stopC).getStatus());
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, stopB, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, "b-cancelled")),
                TRANSPORT_STOP_CANCELLED_NOT_REPORTABLE);
        // A 家继续上报「起运」没问题
        Long departedId = reportNode(taskId, stopA, LogisticsTransportNodeTypeEnum.DEPARTED, "a-2");
        assertNotNull(departedId);
        // 整趟收尾的两类照样可报（与单点取消无关）
        assertNotNull(reportNode(taskId, null, LogisticsTransportNodeTypeEnum.ARRIVED_STATION, "station-1"));

        // 还剩 C 一家没提（A 已交接完成、B 已取消，都不算）
        assertEquals(1, logisticsTransportStopService.getPendingStopCounts(Collections.singletonList(taskId))
                .getOrDefault(taskId, 0));
    }

    @Test
    public void testCancelStop_completedStop_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();
        reportNode(taskId, stopId, LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED, "a-1", "photo-a");

        LogisticsTransportStopCancelReqVO cancelReqVO = new LogisticsTransportStopCancelReqVO();
        cancelReqVO.setId(stopId);
        cancelReqVO.setCancelReason("想取消");
        assertServiceException(() -> logisticsTransportStopService.cancelStop(cancelReqVO),
                TRANSPORT_STOP_STATUS_NOT_ALLOW_CANCEL);
    }

    @Test
    public void testCancelStop_requiresReason() {
        Long taskId = createTaskWithStops("A 家地址");
        Long stopId = logisticsTransportStopService.getStopListByTaskId(taskId).get(0).getId();

        LogisticsTransportStopCancelReqVO cancelReqVO = new LogisticsTransportStopCancelReqVO();
        cancelReqVO.setId(stopId);
        assertServiceException(() -> logisticsTransportStopService.cancelStop(cancelReqVO),
                TRANSPORT_STOP_CANCEL_REASON_REQUIRED);
    }

    @Test
    public void testAddStop_appendsWithNextStopNo_andCanReportNodes() {
        Long taskId = createTaskWithStops("A 家地址");
        LogisticsTransportStopSaveReqVO addReqVO = newStop("B 家地址", "B 家");
        addReqVO.setTaskId(taskId);
        Long newStopId = logisticsTransportTaskService.addStop(addReqVO);

        List<LogisticsTransportStopDO> stops = logisticsTransportStopService.getStopListByTaskId(taskId);
        assertEquals(2, stops.size());
        assertEquals(2, stops.get(1).getStopNo(), "新停靠点接在最后");

        // 新停靠点可正常上报节点
        assertNotNull(reportNode(taskId, newStopId, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, "b-1"));
    }

    @Test
    public void testAddStop_terminalTask_isRejected() {
        Long taskId = createTaskWithStops("A 家地址");
        logisticsTransportTaskService.cancelTask(cancelTask(taskId));

        LogisticsTransportStopSaveReqVO addReqVO = newStop("B 家地址", "B 家");
        addReqVO.setTaskId(taskId);
        assertServiceException(() -> logisticsTransportTaskService.addStop(addReqVO),
                TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE);
    }

    // ==================== 辅助 ====================

    private Long createTaskWithStops(String... addresses) {
        LogisticsTransportTaskCreateReqVO task = new LogisticsTransportTaskCreateReqVO();
        task.setDepartureAddress("城东场站");
        task.setVehicleId(createVehicle());
        task.setDriverId(createDriver());
        task.setStops(Arrays.stream(addresses).map(address ->
                newStop(address, address.substring(0, address.indexOf(' ')) + " 家")).toList());
        return logisticsTransportTaskService.createTask(task);
    }

    private static LogisticsTransportStopSaveReqVO newStop(String address, String payeeName) {
        LogisticsTransportStopSaveReqVO stop = new LogisticsTransportStopSaveReqVO();
        stop.setStopType(1);
        stop.setPayeeId(null);
        stop.setPayeeName(payeeName);
        stop.setPayeeMobile("13800138000");
        stop.setAddress(address);
        stop.setCargoName("废钢");
        stop.setEstimatedQuantity(new BigDecimal("5.5"));
        stop.setQuantityUnit("吨");
        return stop;
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

    private LogisticsTransportTaskCancelReqVO cancelTask(Long taskId) {
        LogisticsTransportTaskCancelReqVO reqVO = new LogisticsTransportTaskCancelReqVO();
        reqVO.setId(taskId);
        reqVO.setCancelReason("不跑了");
        return reqVO;
    }

    private Long reportNode(Long taskId, Long stopId, LogisticsTransportNodeTypeEnum type,
                            String clientRequestId, String... photos) {
        return logisticsTransportNodeService.reportNode(
                newReport(taskId, stopId, type, clientRequestId, photos));
    }

    private static LogisticsTransportNodeReportReqVO newReport(Long taskId, Long stopId,
                                                               LogisticsTransportNodeTypeEnum type,
                                                               String clientRequestId, String... photos) {
        LogisticsTransportNodeReportReqVO reqVO = new LogisticsTransportNodeReportReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setStopId(stopId);
        reqVO.setNodeType(type.getType());
        reqVO.setNodeTime(LocalDateTime.now());
        reqVO.setLocation("现场");
        reqVO.setClientRequestId(clientRequestId);
        if (photos.length > 0) {
            reqVO.setPhotos(Arrays.asList(photos));
        }
        return reqVO;
    }

}
