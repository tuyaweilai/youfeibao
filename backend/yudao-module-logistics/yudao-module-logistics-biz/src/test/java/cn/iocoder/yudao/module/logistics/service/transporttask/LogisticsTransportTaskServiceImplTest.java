package cn.iocoder.yudao.module.logistics.service.transporttask;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskAssignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.vehicle.LogisticsVehicleMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporttask.impl.LogisticsTransportTaskServiceImpl;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import cn.iocoder.yudao.module.logistics.service.vehicle.impl.LogisticsVehicleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsTransportTaskServiceImpl} 的单元测试（V2b #78）。
 *
 * <p>断言的是外部可观察行为：状态怎么推进、非法推进是否被拒、取消为什么要填原因、
 * 以及**派车与释放车辆**这两个副作用（「运输中」这个状态的唯一出入口）。
 */
@Import({LogisticsTransportTaskServiceImpl.class, LogisticsVehicleServiceImpl.class,
        LogisticsDriverServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsTransportTaskServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;
    @Resource
    private LogisticsVehicleMapper logisticsVehicleMapper;

    @Test
    public void testCreateTask_withoutVehicle_isPending() {
        Long taskId = logisticsTransportTaskService.createTask(newTask(null, null));

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.PENDING.getStatus(), task.getStatus());
        assertNotNull(task.getTaskNo());
        assertTrue(task.getTaskNo().startsWith("TT"));
        assertNull(task.getPlateNo());
        assertNull(task.getDriverName());
    }

    @Test
    public void testCreateTask_withVehicleAndDriver_isAssignedAndOccupiesVehicle() {
        Long vehicleId = createVehicle("浙A12345");
        Long driverId = createDriver(1024L, "张三");

        Long taskId = logisticsTransportTaskService.createTask(newTask(vehicleId, driverId));

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus(), task.getStatus());
        assertNotNull(task.getAssignTime());
        // 引用 + 快照都在
        assertEquals(vehicleId, task.getVehicleId());
        assertEquals("浙A12345", task.getPlateNo());
        assertEquals(driverId, task.getDriverId());
        assertEquals("张三", task.getDriverName());
        assertEquals("13800138000", task.getDriverMobile());
        // 车辆被占用：「运输中」只由任务驱动
        assertEquals(LogisticsVehicleStatusEnum.IN_TRANSIT.getStatus(),
                logisticsVehicleMapper.selectById(vehicleId).getStatus());
    }

    @Test
    public void testCreateTask_onlyOneOfVehicleAndDriver_isRejected() {
        Long vehicleId = createVehicle("浙A12345");
        Long driverId = createDriver(1024L, "张三");

        assertServiceException(() -> logisticsTransportTaskService.createTask(newTask(vehicleId, null)),
                TRANSPORT_TASK_ASSIGN_REQUIRED);
        assertServiceException(() -> logisticsTransportTaskService.createTask(newTask(null, driverId)),
                TRANSPORT_TASK_ASSIGN_REQUIRED);
    }

    @Test
    public void testAssignTask_pendingToAssigned() {
        Long taskId = logisticsTransportTaskService.createTask(newTask(null, null));
        Long vehicleId = createVehicle("浙A12345");
        Long driverId = createDriver(1024L, "张三");

        LogisticsTransportTaskAssignReqVO assignReqVO = new LogisticsTransportTaskAssignReqVO();
        assignReqVO.setId(taskId);
        assignReqVO.setVehicleId(vehicleId);
        assignReqVO.setDriverId(driverId);
        logisticsTransportTaskService.assignTask(assignReqVO);

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus(), task.getStatus());
        assertEquals("浙A12345", task.getPlateNo());
    }

    @Test
    public void testAssignTask_alreadyAssigned_isRejected() {
        Long vehicleId = createVehicle("浙A12345");
        Long driverId = createDriver(1024L, "张三");
        Long taskId = logisticsTransportTaskService.createTask(newTask(vehicleId, driverId));

        LogisticsTransportTaskAssignReqVO assignReqVO = new LogisticsTransportTaskAssignReqVO();
        assignReqVO.setId(taskId);
        assignReqVO.setVehicleId(vehicleId);
        assignReqVO.setDriverId(driverId);

        // 已派车不再接受改派（改派承接关系归 V4；一期要换车就取消重派）
        assertServiceException(() -> logisticsTransportTaskService.assignTask(assignReqVO),
                TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE);
    }

    @Test
    public void testAssignTask_vehicleInMaintenance_isRejected() {
        Long vehicleId = createVehicle("浙A12345");
        LogisticVehicleToMaintenance(vehicleId);
        Long driverId = createDriver(1024L, "张三");
        Long taskId = logisticsTransportTaskService.createTask(newTask(null, null));

        LogisticsTransportTaskAssignReqVO assignReqVO = new LogisticsTransportTaskAssignReqVO();
        assignReqVO.setId(taskId);
        assignReqVO.setVehicleId(vehicleId);
        assignReqVO.setDriverId(driverId);

        assertServiceException(() -> logisticsTransportTaskService.assignTask(assignReqVO),
                TRANSPORT_TASK_VEHICLE_NOT_AVAILABLE);
    }

    @Test
    public void testAssignTask_driverNotActive_isRejected() {
        Long vehicleId = createVehicle("浙A12345");
        Long driverId = createDriver(1024L, "张三");
        LogisticsDriverSaveReqVO driver = newDriver(1024L, "张三");
        driver.setId(driverId);
        driver.setStatus(LogisticsDriverStatusEnum.INACTIVE.getStatus());
        logisticsDriverService.updateDriver(driver);
        Long taskId = logisticsTransportTaskService.createTask(newTask(null, null));

        LogisticsTransportTaskAssignReqVO assignReqVO = new LogisticsTransportTaskAssignReqVO();
        assignReqVO.setId(taskId);
        assignReqVO.setVehicleId(vehicleId);
        assignReqVO.setDriverId(driverId);

        assertServiceException(() -> logisticsTransportTaskService.assignTask(assignReqVO),
                TRANSPORT_TASK_DRIVER_NOT_ACTIVE);
    }

    @Test
    public void testAcceptThenTransit_advancesThroughStates() {
        Long taskId = createAssignedTask();

        logisticsTransportTaskService.acceptTask(taskId);
        LogisticsTransportTaskDO accepted = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.ACCEPTED.getStatus(), accepted.getStatus());
        assertNotNull(accepted.getAcceptTime());

        // 重复接单是幂等的（离线补传/重复点击不报错，也不再覆盖接单时间）
        logisticsTransportTaskService.acceptTask(taskId);
        assertEquals(accepted.getAcceptTime(),
                logisticsTransportTaskMapper.selectById(taskId).getAcceptTime());

        logisticsTransportTaskService.transitStatus(accepted, LogisticsTransportTaskStatusEnum.IN_TRANSIT);
        assertEquals(LogisticsTransportTaskStatusEnum.IN_TRANSIT.getStatus(),
                logisticsTransportTaskMapper.selectById(taskId).getStatus());
    }

    @Test
    public void testCompleteTask_releasesVehicle() {
        Long vehicleId = createVehicle("浙A12345");
        Long driverId = createDriver(1024L, "张三");
        Long taskId = logisticsTransportTaskService.createTask(newTask(vehicleId, driverId));

        logisticsTransportTaskService.acceptTask(taskId);
        logisticsTransportTaskService.completeTask(taskId);

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.COMPLETED.getStatus(), task.getStatus());
        assertNotNull(task.getCompleteTime());
        assertEquals(LogisticsVehicleStatusEnum.AVAILABLE.getStatus(),
                logisticsVehicleMapper.selectById(vehicleId).getStatus());
    }

    @Test
    public void testCancelTask_requiresReasonAndReleasesVehicle() {
        Long vehicleId = createVehicle("浙A12345");
        Long driverId = createDriver(1024L, "张三");
        Long taskId = logisticsTransportTaskService.createTask(newTask(vehicleId, driverId));

        LogisticsTransportTaskCancelReqVO cancelReqVO = new LogisticsTransportTaskCancelReqVO();
        cancelReqVO.setId(taskId);
        cancelReqVO.setCancelReason("客户临时改期");
        logisticsTransportTaskService.cancelTask(cancelReqVO);

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.CANCELLED.getStatus(), task.getStatus());
        assertEquals("客户临时改期", task.getCancelReason());
        assertNotNull(task.getCancelTime());
        assertEquals(LogisticsVehicleStatusEnum.AVAILABLE.getStatus(),
                logisticsVehicleMapper.selectById(vehicleId).getStatus());
    }

    @Test
    public void testCancelTask_withoutReason_isRejected() {
        Long taskId = logisticsTransportTaskService.createTask(newTask(null, null));

        LogisticsTransportTaskCancelReqVO cancelReqVO = new LogisticsTransportTaskCancelReqVO();
        cancelReqVO.setId(taskId);
        // 服务层也要拦：VO 的 @NotEmpty 只挡 HTTP 入口，离线补传/内部调用会绕过它
        assertServiceException(() -> logisticsTransportTaskService.cancelTask(cancelReqVO),
                TRANSPORT_TASK_CANCEL_REASON_REQUIRED);
    }

    @Test
    public void testCancelTask_afterCompleted_isRejected() {
        Long taskId = createAssignedTask();
        logisticsTransportTaskService.completeTask(taskId);

        LogisticsTransportTaskCancelReqVO cancelReqVO = new LogisticsTransportTaskCancelReqVO();
        cancelReqVO.setId(taskId);
        cancelReqVO.setCancelReason("不跑了");
        assertServiceException(() -> logisticsTransportTaskService.cancelTask(cancelReqVO),
                TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE);
    }

    @Test
    public void testUpdateTask_allowedBeforeDeparture_rejectedAfter() {
        Long taskId = createAssignedTask();

        LogisticsTransportTaskSaveReqVO updateReqVO = newTask(null, null);
        updateReqVO.setId(taskId);
        updateReqVO.setPickupAddress("新地址");
        logisticsTransportTaskService.updateTask(updateReqVO);
        assertEquals("新地址", logisticsTransportTaskMapper.selectById(taskId).getPickupAddress());

        // 起运之后不再改面单
        logisticsTransportTaskService.acceptTask(taskId);
        logisticsTransportTaskService.transitStatus(
                logisticsTransportTaskMapper.selectById(taskId),
                LogisticsTransportTaskStatusEnum.IN_TRANSIT);
        LogisticsTransportTaskSaveReqVO afterStart = newTask(null, null);
        afterStart.setId(taskId);
        afterStart.setPickupAddress("又改地址");
        assertServiceException(() -> logisticsTransportTaskService.updateTask(afterStart),
                TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE);

        // 更新不会动手上的车与人
        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertNotNull(task.getPlateNo());
        assertNotNull(task.getDriverName());
    }

    @Test
    public void testUpdateTask_doesNotChangeStatusOrAssignment() {
        Long vehicleId = createVehicle("浙A12345");
        Long driverId = createDriver(1024L, "张三");
        Long taskId = logisticsTransportTaskService.createTask(newTask(vehicleId, driverId));

        LogisticsTransportTaskSaveReqVO updateReqVO = newTask(null, null);
        updateReqVO.setId(taskId);
        updateReqVO.setPickupContactPhone("13900139000");
        logisticsTransportTaskService.updateTask(updateReqVO);

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus(), task.getStatus());
        assertEquals(vehicleId, task.getVehicleId());
        assertEquals(driverId, task.getDriverId());
        assertEquals("13900139000", task.getPickupContactPhone());
    }

    @Test
    public void testGetTask_notExists() {
        assertServiceException(() -> logisticsTransportTaskService.getTask(-1L), TRANSPORT_TASK_NOT_EXISTS);
    }

    @Test
    public void testGetTaskByTaskNo_blankOrUnknown_returnsNull() {
        assertNull(logisticsTransportTaskService.getTaskByTaskNo(null));
        assertNull(logisticsTransportTaskService.getTaskByTaskNo(" "));
        assertNull(logisticsTransportTaskService.getTaskByTaskNo("TT-NOT-EXISTS"));
    }

    // ==================== 辅助 ====================

    private Long createAssignedTask() {
        Long vehicleId = createVehicle("浙A" + System.nanoTime() % 100000);
        Long driverId = createDriver(System.nanoTime() % 100000 + 1, "张三");
        return logisticsTransportTaskService.createTask(newTask(vehicleId, driverId));
    }

    private Long createVehicle(String plateNo) {
        LogisticsVehicleSaveReqVO vehicle = new LogisticsVehicleSaveReqVO();
        vehicle.setPlateNo(plateNo);
        vehicle.setVehicleType("厢式货车");
        vehicle.setCapacityTon(new BigDecimal("10.5"));
        vehicle.setStatus(LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        return logisticsVehicleService.createVehicle(vehicle);
    }

    private void LogisticVehicleToMaintenance(Long vehicleId) {
        LogisticsVehicleDO vehicle = logisticsVehicleMapper.selectById(vehicleId);
        vehicle.setStatus(LogisticsVehicleStatusEnum.MAINTENANCE.getStatus());
        logisticsVehicleMapper.updateById(vehicle);
    }

    private Long createDriver(Long userId, String name) {
        return logisticsDriverService.createDriver(newDriver(userId, name));
    }

    private static LogisticsDriverSaveReqVO newDriver(Long userId, String name) {
        LogisticsDriverSaveReqVO driver = new LogisticsDriverSaveReqVO();
        driver.setUserId(userId);
        driver.setName(name);
        driver.setMobile("13800138000");
        driver.setSource(LogisticsDriverSourceEnum.SELF.getSource());
        driver.setStatus(LogisticsDriverStatusEnum.ACTIVE.getStatus());
        return driver;
    }

    private static LogisticsTransportTaskSaveReqVO newTask(Long vehicleId, Long driverId) {
        LogisticsTransportTaskSaveReqVO task = new LogisticsTransportTaskSaveReqVO();
        task.setVehicleId(vehicleId);
        task.setDriverId(driverId);
        task.setDepartureAddress("城东场站");
        task.setPickupAddress("某某路 1 号");
        task.setPickupContactName("李四");
        task.setPickupContactPhone("13700137000");
        return task;
    }

}
