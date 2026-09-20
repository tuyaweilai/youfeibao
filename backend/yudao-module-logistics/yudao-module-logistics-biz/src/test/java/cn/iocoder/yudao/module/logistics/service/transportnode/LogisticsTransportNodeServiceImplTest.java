package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalResolveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportAbnormalTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transportnode.impl.LogisticsTransportNodeServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.transporttask.impl.LogisticsTransportTaskServiceImpl;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import cn.iocoder.yudao.module.logistics.service.vehicle.impl.LogisticsVehicleServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsTransportNodeServiceImpl} 的单元测试（V2b #78；V4 #71 开全五类并加异常）。
 *
 * <p>钉住四件事：
 * <ol>
 *   <li><b>写入幂等</b>——弱网补传会重复提交同一条事实；</li>
 *   <li><b>两个时间分开</b>——发生时间 ≠ 上报时间，时间线按发生时间排，补录晚到不倒序业务；</li>
 *   <li><b>照片必填策略</b>——交接完成与卸货完成必须有照片，其余三类允许没有；</li>
 *   <li><b>异常是独立标记</b>——它只落事实、带解决留痕，**不改变任务状态机**。</li>
 * </ol>
 */
@Import({LogisticsTransportNodeServiceImpl.class, LogisticsTransportTaskServiceImpl.class,
        LogisticsVehicleServiceImpl.class, LogisticsDriverServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsTransportNodeServiceImplTest extends BaseDbUnitTest {

    private static final Long OPERATOR_USER_ID = 1024L;

    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeMapper logisticsTransportNodeMapper;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;

    @AfterEach
    public void clearLoginUser() {
        SecurityContextHolder.clearContext();
    }

    // ==================== 五类节点与双时间 ====================

    @Test
    public void testReportNode_allFiveTypes_supportedAndStored() {
        Long taskId = createAssignedTask();
        logisticsTransportTaskService.acceptTask(taskId);
        LocalDateTime base = LocalDateTime.now().minusHours(6);

        // 五类全部可上报（V4 #71 删掉了 V2b 的「只支持起运」白名单）
        report(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, base, "n-1");
        report(taskId, LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED, base.plusMinutes(10), "n-2", "photo-1");
        report(taskId, LogisticsTransportNodeTypeEnum.DEPARTED, base.plusMinutes(20), "n-3");
        report(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_STATION, base.plusMinutes(50), "n-4");
        report(taskId, LogisticsTransportNodeTypeEnum.UNLOADED, base.plusMinutes(70), "n-5", "photo-2");

        List<LogisticsTransportNodeDO> nodes = logisticsTransportNodeService.getNodeListByTaskId(taskId);
        assertEquals(5, nodes.size());
        assertEquals(Arrays.asList(
                        LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP.getType(),
                        LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED.getType(),
                        LogisticsTransportNodeTypeEnum.DEPARTED.getType(),
                        LogisticsTransportNodeTypeEnum.ARRIVED_STATION.getType(),
                        LogisticsTransportNodeTypeEnum.UNLOADED.getType()),
                nodes.stream().map(LogisticsTransportNodeDO::getNodeType).collect(Collectors.toList()));
    }

    @Test
    public void testReportNode_departed_advancesTaskToInTransit() {
        Long taskId = createAssignedTask();
        LocalDateTime departedAt = LocalDateTime.now().minusHours(2); // 事情是两小时前发生的

        Long nodeId = report(taskId, LogisticsTransportNodeTypeEnum.DEPARTED, departedAt, "req-1");

        LogisticsTransportNodeDO node = logisticsTransportNodeMapper.selectById(nodeId);
        assertEquals(departedAt, node.getNodeTime());
        assertNotNull(node.getReportTime());
        assertTrue(node.getReportTime().isAfter(node.getNodeTime()),
                "发生时间与上报时间要分开：补录晚到不代表业务倒序");
        assertEquals("城东场站门口", node.getLocation());

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.IN_TRANSIT.getStatus(), task.getStatus());
        // 起运时间取**发生时间**，不是上报时间
        assertEquals(departedAt, task.getStartTime());
    }

    @Test
    public void testReportNode_backfillLate_doesNotReorderBusiness() {
        Long taskId = createAssignedTask();

        // 先报「到达场站」（发生时间晚），再补报「到达提货点」（发生时间早三小时）
        LocalDateTime stationAt = LocalDateTime.now().minusMinutes(10);
        LocalDateTime pickupAt = LocalDateTime.now().minusHours(3);
        report(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_STATION, stationAt, "req-station");
        report(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, pickupAt, "req-pickup");

        List<LogisticsTransportNodeDO> nodes = logisticsTransportNodeService.getNodeListByTaskId(taskId);
        // 时间线按**发生时间**排，不按上报顺序——补录晚到不等于业务倒序
        assertEquals(Arrays.asList(
                        LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP.getType(),
                        LogisticsTransportNodeTypeEnum.ARRIVED_STATION.getType()),
                nodes.stream().map(LogisticsTransportNodeDO::getNodeType).collect(Collectors.toList()));
        // 这些节点都不推进状态：任务还停在「已分配」
        assertEquals(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus(),
                logisticsTransportTaskMapper.selectById(taskId).getStatus());
        assertTrue(nodes.stream().allMatch(node -> node.getReportTime().isAfter(node.getNodeTime())));
    }

    @Test
    public void testReportNode_sameClientRequestId_isIdempotent() {
        Long taskId = createAssignedTask();
        LocalDateTime departedAt = LocalDateTime.now().minusMinutes(30);

        Long first = report(taskId, LogisticsTransportNodeTypeEnum.DEPARTED, departedAt, "req-same");
        Long second = report(taskId, LogisticsTransportNodeTypeEnum.DEPARTED, departedAt, "req-same");

        assertEquals(first, second, "重复补传要返回既有节点，而不是再落一条");
        assertEquals(1, logisticsTransportNodeMapper.selectListByTaskId(taskId).size());
    }

    // ==================== 照片必填策略 ====================

    @Test
    public void testReportNode_handoverAndUnloaded_requirePhotos() {
        Long taskId = createAssignedTask();

        // 交接完成、卸货完成：没有照片直接拦
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED.getType(), LocalDateTime.now(),
                                "photo-req-1")),
                TRANSPORT_NODE_PHOTO_REQUIRED);
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, LogisticsTransportNodeTypeEnum.UNLOADED.getType(), LocalDateTime.now(),
                                "photo-req-2")),
                TRANSPORT_NODE_PHOTO_REQUIRED);
        assertEquals(0, logisticsTransportNodeMapper.selectListByTaskId(taskId).size(),
                "被拦的上报不该留下半条记录");

        // 带上照片就放行
        Long handoverId = report(taskId, LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED,
                LocalDateTime.now(), "photo-ok-1", "photo-1");
        assertNotNull(handoverId);
    }

    @Test
    public void testReportNode_otherTypes_doNotRequirePhotos() {
        Long taskId = createAssignedTask();

        // 到达提货点 / 起运 / 到达场站：允许没有照片
        assertNotNull(report(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, LocalDateTime.now(), "np-1"));
        assertNotNull(report(taskId, LogisticsTransportNodeTypeEnum.DEPARTED, LocalDateTime.now(), "np-2"));
        assertNotNull(report(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_STATION, LocalDateTime.now(), "np-3"));
    }

    // ==================== 校验 ====================

    @Test
    public void testReportNode_unknownType_isRejected() {
        Long taskId = createAssignedTask();

        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, 99, LocalDateTime.now(), "req-unknown")),
                TRANSPORT_NODE_TYPE_UNKNOWN);
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, null, LocalDateTime.now(), "req-null-type")),
                TRANSPORT_NODE_TYPE_REQUIRED);
    }

    @Test
    public void testReportNode_taskNotExists_isRejected() {
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(-1L, LogisticsTransportNodeTypeEnum.DEPARTED.getType(), LocalDateTime.now(), "req-4")),
                TRANSPORT_TASK_NOT_EXISTS);
    }

    @Test
    public void testReportNode_missingNodeTime_isRejected() {
        Long taskId = createAssignedTask();

        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(), null, "req-5")),
                TRANSPORT_NODE_TIME_REQUIRED);
    }

    @Test
    public void testReportNode_pendingOrCancelledTask_isRejected() {
        // 待分配：还没有车与人，谈不上运输过程
        Long pendingTaskId = logisticsTransportTaskService.createTask(newTask(null, null));
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(pendingTaskId, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP.getType(), LocalDateTime.now(),
                                "req-pending")),
                TRANSPORT_NODE_TASK_NOT_REPORTABLE);

        // 已取消：这趟活已经了结
        Long cancelledTaskId = createAssignedTask();
        LogisticsTransportTaskCancelReqVO cancelReqVO = new LogisticsTransportTaskCancelReqVO();
        cancelReqVO.setId(cancelledTaskId);
        cancelReqVO.setCancelReason("客户改期");
        logisticsTransportTaskService.cancelTask(cancelReqVO);
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(cancelledTaskId, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP.getType(), LocalDateTime.now(),
                                "req-cancelled")),
                TRANSPORT_NODE_TASK_NOT_REPORTABLE);
    }

    @Test
    public void testReportNode_completedTask_allowsLateBackfill() {
        // 「补录晚到不导致业务倒序」：事后补报事实是常态，已完成仍允许补录，只是不倒序业务
        Long taskId = createAssignedTask();
        logisticsTransportTaskService.completeTask(taskId);

        Long nodeId = report(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_STATION,
                LocalDateTime.now().minusHours(1), "req-completed-backfill");

        assertNotNull(nodeId);
        assertEquals(LogisticsTransportTaskStatusEnum.COMPLETED.getStatus(),
                logisticsTransportTaskMapper.selectById(taskId).getStatus());
    }

    // ==================== 异常：独立标记，不改状态机 ====================

    @Test
    public void testReportAbnormal_doesNotChangeTaskStatus() {
        Long taskId = createAssignedTask();
        logisticsTransportTaskService.acceptTask(taskId);

        Long abnormalId = reportAbnormal(taskId, LogisticsTransportAbnormalTypeEnum.VEHICLE_BREAKDOWN,
                LocalDateTime.now().minusMinutes(5), "abn-1", "aa-remark");

        LogisticsTransportNodeDO node = logisticsTransportNodeMapper.selectById(abnormalId);
        assertNull(node.getNodeType(), "异常事实没有节点类型——它不是流程里的某一步");
        assertEquals(LogisticsTransportAbnormalTypeEnum.VEHICLE_BREAKDOWN.getType(), node.getAbnormalType());
        assertEquals("发动机水温过高，已靠边停车", node.getAbnormalReason());
        assertFalse(node.getAbnormalResolved());
        assertNotNull(node.getReportTime());
        // 异常是独立标记：任务状态**不动**（还是「已接单」）
        assertEquals(LogisticsTransportTaskStatusEnum.ACCEPTED.getStatus(),
                logisticsTransportTaskMapper.selectById(taskId).getStatus());

        List<LogisticsTransportNodeRespVO> resps = logisticsTransportNodeService.toRespList(
                logisticsTransportNodeService.getNodeListByTaskId(taskId));
        assertEquals(1, resps.size());
        assertEquals("车辆故障", resps.get(0).getAbnormalTypeName());
        assertNull(resps.get(0).getNodeTypeName());
    }

    @Test
    public void testReportAbnormal_sameClientRequestId_isIdempotent() {
        Long taskId = createAssignedTask();

        Long first = reportAbnormal(taskId, LogisticsTransportAbnormalTypeEnum.ROAD_CLOSED,
                LocalDateTime.now(), "abn-same", null);
        Long second = reportAbnormal(taskId, LogisticsTransportAbnormalTypeEnum.ROAD_CLOSED,
                LocalDateTime.now(), "abn-same", null);

        assertEquals(first, second);
        assertEquals(1, logisticsTransportNodeMapper.selectListByTaskId(taskId).size());
    }

    @Test
    public void testReportAbnormal_requiresReasonAndKnownType() {
        Long taskId = createAssignedTask();

        LogisticsTransportAbnormalReportReqVO noReason = newAbnormal(taskId,
                LogisticsTransportAbnormalTypeEnum.OTHER.getType(), LocalDateTime.now(), "abn-no-reason");
        noReason.setAbnormalReason("  ");
        assertServiceException(() -> logisticsTransportNodeService.reportAbnormal(noReason),
                TRANSPORT_ABNORMAL_REASON_REQUIRED);

        LogisticsTransportAbnormalReportReqVO unknownType = newAbnormal(taskId, 99, LocalDateTime.now(), "abn-x");
        assertServiceException(() -> logisticsTransportNodeService.reportAbnormal(unknownType),
                TRANSPORT_ABNORMAL_TYPE_UNKNOWN);
    }

    @Test
    public void testResolveAbnormal_recordsResolution_andRejectsDoubleResolve() {
        loginAs(OPERATOR_USER_ID);
        Long taskId = createAssignedTask();
        LogisticsTransportTaskDO before = logisticsTransportTaskMapper.selectById(taskId);
        Long abnormalId = reportAbnormal(taskId, LogisticsTransportAbnormalTypeEnum.COUNTERPARTY_ABSENT,
                LocalDateTime.now().minusMinutes(20), "abn-resolve", null);

        LogisticsTransportAbnormalResolveReqVO resolveReqVO = new LogisticsTransportAbnormalResolveReqVO();
        resolveReqVO.setId(abnormalId);
        resolveReqVO.setResolveRemark("对方回来了，已装车");
        logisticsTransportNodeService.resolveAbnormal(resolveReqVO);

        LogisticsTransportNodeDO resolved = logisticsTransportNodeMapper.selectById(abnormalId);
        assertTrue(resolved.getAbnormalResolved());
        assertNotNull(resolved.getAbnormalResolvedAt());
        assertEquals(OPERATOR_USER_ID, resolved.getAbnormalResolvedBy());
        assertEquals("对方回来了，已装车", resolved.getAbnormalResolvedRemark());
        // 解决异常同样不改任务状态
        assertEquals(before.getStatus(), logisticsTransportTaskMapper.selectById(taskId).getStatus());

        // 已解决的不再覆盖留痕
        assertServiceException(() -> logisticsTransportNodeService.resolveAbnormal(resolveReqVO),
                TRANSPORT_ABNORMAL_ALREADY_RESOLVED);
    }

    @Test
    public void testResolveAbnormal_notAnAbnormalNode_isRejected() {
        Long taskId = createAssignedTask();
        Long normalNodeId = report(taskId, LogisticsTransportNodeTypeEnum.DEPARTED, LocalDateTime.now(), "normal-1");

        LogisticsTransportAbnormalResolveReqVO resolveReqVO = new LogisticsTransportAbnormalResolveReqVO();
        resolveReqVO.setId(normalNodeId);
        assertServiceException(() -> logisticsTransportNodeService.resolveAbnormal(resolveReqVO),
                TRANSPORT_ABNORMAL_NOT_EXISTS);
    }

    // ==================== 读取面 ====================

    @Test
    public void testGetNodeListByTaskNo_timelineOrderedByNodeTime() {
        Long taskId = createAssignedTask();
        LocalDateTime later = LocalDateTime.now().minusMinutes(10);
        report(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_STATION, later, "req-late");

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        List<LogisticsTransportNodeDO> nodes = logisticsTransportNodeService.getNodeListByTaskNo(task.getTaskNo());
        assertEquals(1, nodes.size());
        assertEquals(later, nodes.get(0).getNodeTime());
    }

    @Test
    public void testGetNodeListByTaskNo_blankOrUnknown_returnsEmpty() {
        assertTrue(logisticsTransportNodeService.getNodeListByTaskNo(null).isEmpty());
        assertTrue(logisticsTransportNodeService.getNodeListByTaskNo(" ").isEmpty());
        assertTrue(logisticsTransportNodeService.getNodeListByTaskNo("TT-NOT-EXISTS").isEmpty());
        assertTrue(logisticsTransportNodeService.getNodeListByTaskId(null).isEmpty());
    }

    @Test
    public void testToRespList_carriesPhotosAndTypeName() {
        Long taskId = createAssignedTask();
        LogisticsTransportNodeReportReqVO reqVO = newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(),
                LocalDateTime.now(), "req-photos");
        reqVO.setPhotos(Arrays.asList("https://file/1.jpg", "https://file/2.jpg"));
        logisticsTransportNodeService.reportNode(reqVO);

        List<LogisticsTransportNodeRespVO> resps = logisticsTransportNodeService.toRespList(
                logisticsTransportNodeService.getNodeListByTaskId(taskId));

        assertEquals(1, resps.size());
        assertEquals("起运", resps.get(0).getNodeTypeName());
        assertEquals(Arrays.asList("https://file/1.jpg", "https://file/2.jpg"), resps.get(0).getPhotos());
    }

    @Test
    public void testToRespList_emptyPhotos_returnsEmptyListNotNull() {
        Long taskId = createAssignedTask();
        report(taskId, LogisticsTransportNodeTypeEnum.DEPARTED, LocalDateTime.now(), "req-no-photo");

        List<LogisticsTransportNodeRespVO> resps = logisticsTransportNodeService.toRespList(
                logisticsTransportNodeService.getNodeListByTaskId(taskId));

        assertNotNull(resps.get(0).getPhotos());
        assertTrue(resps.get(0).getPhotos().isEmpty(), "没传照片要给空列表，不是 null（前端直接遍历）");
    }

    // ==================== 辅助 ====================

    private void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(1);
        loginUser.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, null));
    }

    private Long createAssignedTask() {
        Long vehicleId = createVehicle();
        Long driverId = createDriver();
        Long taskId = logisticsTransportTaskService.createTask(newTask(vehicleId, driverId));
        return taskId;
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

    private Long report(Long taskId, LogisticsTransportNodeTypeEnum type, LocalDateTime nodeTime,
                        String clientRequestId, String... photos) {
        LogisticsTransportNodeReportReqVO reqVO = newReport(taskId, type.getType(), nodeTime, clientRequestId);
        if (photos.length > 0) {
            reqVO.setPhotos(Arrays.asList(photos));
        }
        return logisticsTransportNodeService.reportNode(reqVO);
    }

    private Long reportAbnormal(Long taskId, LogisticsTransportAbnormalTypeEnum type, LocalDateTime nodeTime,
                                String clientRequestId, String photo) {
        LogisticsTransportAbnormalReportReqVO reqVO = newAbnormal(taskId, type.getType(), nodeTime, clientRequestId);
        if (photo != null) {
            reqVO.setPhotos(Arrays.asList(photo));
        }
        return logisticsTransportNodeService.reportAbnormal(reqVO);
    }

    private static LogisticsTransportAbnormalReportReqVO newAbnormal(Long taskId, Integer abnormalType,
                                                                    LocalDateTime nodeTime,
                                                                    String clientRequestId) {
        LogisticsTransportAbnormalReportReqVO reqVO = new LogisticsTransportAbnormalReportReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setAbnormalType(abnormalType);
        reqVO.setAbnormalReason("发动机水温过高，已靠边停车");
        reqVO.setNodeTime(nodeTime);
        reqVO.setLocation("绕城高速北段");
        reqVO.setLatitude(new BigDecimal("30.1234567"));
        reqVO.setLongitude(new BigDecimal("120.1234567"));
        reqVO.setClientRequestId(clientRequestId);
        return reqVO;
    }

    private static LogisticsTransportNodeReportReqVO newReport(Long taskId, Integer nodeType,
                                                               LocalDateTime nodeTime, String clientRequestId) {
        LogisticsTransportNodeReportReqVO reqVO = new LogisticsTransportNodeReportReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setNodeType(nodeType);
        reqVO.setNodeTime(nodeTime);
        reqVO.setLocation("城东场站门口");
        reqVO.setLatitude(new BigDecimal("30.1234567"));
        reqVO.setLongitude(new BigDecimal("120.1234567"));
        reqVO.setClientRequestId(clientRequestId);
        return reqVO;
    }

    private static LogisticsTransportTaskCreateReqVO newTask(Long vehicleId, Long driverId) {
        LogisticsTransportTaskCreateReqVO task = new LogisticsTransportTaskCreateReqVO();
        task.setVehicleId(vehicleId);
        task.setDriverId(driverId);
        task.setDepartureAddress("城东场站");
        task.setPickupAddress("某某路 1 号");
        return task;
    }

}
