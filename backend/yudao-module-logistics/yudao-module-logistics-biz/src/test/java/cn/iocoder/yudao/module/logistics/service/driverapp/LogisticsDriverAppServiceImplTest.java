package cn.iocoder.yudao.module.logistics.service.driverapp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.driverapp.impl.LogisticsDriverAppServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
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
import java.time.LocalDateTime;
import java.util.Arrays;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsDriverAppServiceImpl} 的单元测试（V2c #79）。
 *
 * <p>这一票最该被钉住的就是**归属**：司机有个「司机端权限」不等于他能看别人的活。
 * 列表强制按登录账号过滤、详情 / 接单 / 上报都要校验任务是不是派给他的。
 */
@Import({LogisticsDriverAppServiceImpl.class, LogisticsTransportTaskServiceImpl.class,
        LogisticsTransportNodeServiceImpl.class, LogisticsVehicleServiceImpl.class,
        LogisticsDriverServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsDriverAppServiceImplTest extends BaseDbUnitTest {

    private static final Long DRIVER_USER_ID = 1024L;
    private static final Long OTHER_USER_ID = 2048L;

    @Resource
    private LogisticsDriverAppService logisticsDriverAppService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;

    @AfterEach
    public void clearLoginUser() {
        // 单测里手动塞的登录态要清掉，避免污染后续用例
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetCurrentDriver_notADriver_isRejected() {
        loginAs(OTHER_USER_ID);

        assertServiceException(() -> logisticsDriverAppService.getCurrentDriver(), DRIVER_PROFILE_NOT_FOUND);
    }

    @Test
    public void testGetMyTaskPage_onlyMyTasks() {
        Long myDriverId = createDriver(DRIVER_USER_ID, "张三");
        Long otherDriverId = createDriver(OTHER_USER_ID, "李四");
        Long myTaskId = createAssignedTask("浙A11111", myDriverId);
        createAssignedTask("浙A22222", otherDriverId);

        loginAs(DRIVER_USER_ID);
        PageResult<LogisticsTransportTaskDO> page = logisticsDriverAppService.getMyTaskPage(
                new LogisticsTransportTaskPageReqVO());

        assertEquals(1, page.getTotal(), "只能看到派给自己的任务");
        assertEquals(myTaskId, page.getList().get(0).getId());
    }

    @Test
    public void testGetMyTaskPage_ignoresDriverIdParam() {
        Long myDriverId = createDriver(DRIVER_USER_ID, "张三");
        Long otherDriverId = createDriver(OTHER_USER_ID, "李四");
        createAssignedTask("浙A11111", myDriverId);
        Long otherTaskId = createAssignedTask("浙A22222", otherDriverId);

        loginAs(DRIVER_USER_ID);
        LogisticsTransportTaskPageReqVO pageReqVO = new LogisticsTransportTaskPageReqVO();
        // 司机在入参里塞别人的 driverId 也没用：归属只看登录账号
        pageReqVO.setDriverId(otherDriverId);
        PageResult<LogisticsTransportTaskDO> page = logisticsDriverAppService.getMyTaskPage(pageReqVO);

        assertEquals(1, page.getTotal());
        assertNotEquals(otherTaskId, page.getList().get(0).getId());
    }

    @Test
    public void testGetMyTask_notMine_isRejected() {
        createDriver(DRIVER_USER_ID, "张三");
        Long otherDriverId = createDriver(OTHER_USER_ID, "李四");
        Long otherTaskId = createAssignedTask("浙A22222", otherDriverId);

        loginAs(DRIVER_USER_ID);

        assertServiceException(() -> logisticsDriverAppService.getMyTask(otherTaskId),
                TRANSPORT_TASK_NOT_BELONG_TO_DRIVER);
    }

    @Test
    public void testAcceptMyTask_advancesOnlyMine() {
        Long myDriverId = createDriver(DRIVER_USER_ID, "张三");
        Long myTaskId = createAssignedTask("浙A11111", myDriverId);

        loginAs(DRIVER_USER_ID);
        logisticsDriverAppService.acceptMyTask(myTaskId);

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(myTaskId);
        assertEquals(LogisticsTransportTaskStatusEnum.ACCEPTED.getStatus(), task.getStatus());
    }

    @Test
    public void testReportMyNode_departed_advancesOnlyMine() {
        Long myDriverId = createDriver(DRIVER_USER_ID, "张三");
        Long myTaskId = createAssignedTask("浙A11111", myDriverId);
        loginAs(DRIVER_USER_ID);
        logisticsDriverAppService.acceptMyTask(myTaskId);

        Long nodeId = logisticsDriverAppService.reportMyNode(
                newReport(myTaskId, LocalDateTime.now().minusMinutes(20), "driver-req-1"));

        assertNotNull(nodeId);
        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(myTaskId);
        assertEquals(LogisticsTransportTaskStatusEnum.IN_TRANSIT.getStatus(), task.getStatus());
        // 上报人记的是登录账号（司机本人）
        assertEquals(DRIVER_USER_ID, SecurityFrameworkUtils.getLoginUserId());
    }

    @Test
    public void testReportMyNode_notMine_isRejected() {
        createDriver(DRIVER_USER_ID, "张三");
        Long otherDriverId = createDriver(OTHER_USER_ID, "李四");
        Long otherTaskId = createAssignedTask("浙A22222", otherDriverId);

        loginAs(DRIVER_USER_ID);

        assertServiceException(
                () -> logisticsDriverAppService.reportMyNode(
                        newReport(otherTaskId, LocalDateTime.now(), "driver-req-2")),
                TRANSPORT_TASK_NOT_BELONG_TO_DRIVER);
    }

    // ==================== 辅助 ====================

    /**
     * 塞一个登录用户：司机端的归属校验全靠它。
     *
     * <p>与 `AppointmentServiceTest` 同一手法直接写 SecurityContext——`SecurityFrameworkUtils.setLoginUser`
     * 需要非空 request（要往里写属性给访问日志用），单测里给不出。
     */
    private void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(1);
        loginUser.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, null));
    }

    private Long createDriver(Long userId, String name) {
        LogisticsDriverSaveReqVO driver = new LogisticsDriverSaveReqVO();
        driver.setUserId(userId);
        driver.setName(name);
        driver.setMobile("13800138000");
        driver.setSource(LogisticsDriverSourceEnum.SELF.getSource());
        driver.setStatus(LogisticsDriverStatusEnum.ACTIVE.getStatus());
        return logisticsDriverService.createDriver(driver);
    }

    private Long createAssignedTask(String plateNo, Long driverId) {
        LogisticsVehicleSaveReqVO vehicle = new LogisticsVehicleSaveReqVO();
        vehicle.setPlateNo(plateNo);
        vehicle.setStatus(LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        Long vehicleId = logisticsVehicleService.createVehicle(vehicle);

        LogisticsTransportTaskSaveReqVO task = new LogisticsTransportTaskSaveReqVO();
        task.setDepartureAddress("城东场站");
        task.setPickupAddress("某某路 1 号");
        task.setCargoName("废钢");
        task.setVehicleId(vehicleId);
        task.setDriverId(driverId);
        return logisticsTransportTaskService.createTask(task);
    }

    private static LogisticsTransportNodeReportReqVO newReport(Long taskId, LocalDateTime nodeTime,
                                                              String clientRequestId) {
        LogisticsTransportNodeReportReqVO reqVO = new LogisticsTransportNodeReportReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setNodeType(LogisticsTransportNodeTypeEnum.DEPARTED.getType());
        reqVO.setNodeTime(nodeTime);
        reqVO.setLocation("客户门口");
        reqVO.setPhotos(Arrays.asList("https://file/driver-1.jpg"));
        reqVO.setClientRequestId(clientRequestId);
        return reqVO;
    }

}
