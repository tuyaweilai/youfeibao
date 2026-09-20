package cn.iocoder.yudao.module.logistics.service.archives;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.expiry.vo.LogisticsExpiryWarningItemVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskAssignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskOverrideAssignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierService;
import cn.iocoder.yudao.module.logistics.service.carrier.impl.LogisticsCarrierServiceImpl;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.expiry.LogisticsExpiryWarningService;
import cn.iocoder.yudao.module.logistics.service.expiry.impl.LogisticsExpiryWarningServiceImpl;
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
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 档案做全与派车门禁的单元测试（V3 #70）。
 *
 * <p>钉住三件事：
 * <ol>
 *   <li><b>软门禁</b>（证件过期）拦得住派车，且**可以**由管理员带着原因授权放行，并留下
 *       原因 / 授权人 / 时间；</li>
 *   <li><b>硬门禁</b>（车辆维修中、司机离职）**不可**授权绕过——那不是流程不便，是无证运营；</li>
 *   <li>到期提醒与门禁**共用同一套「过期」判定**，不会出现「提醒说没事、派车被拦」。</li>
 * </ol>
 */
@Import({LogisticsTransportTaskServiceImpl.class, LogisticsVehicleServiceImpl.class,
        LogisticsDriverServiceImpl.class, LogisticsCarrierServiceImpl.class,
        LogisticsExpiryWarningServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsArchiveGateTest extends BaseDbUnitTest {

    private static final Long ADMIN_USER_ID = 1L;

    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsCarrierService logisticsCarrierService;
    @Resource
    private LogisticsExpiryWarningService logisticsExpiryWarningService;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;

    @AfterEach
    public void clearLoginUser() {
        SecurityContextHolder.clearContext();
    }

    // ==================== 软门禁：证件过期 ====================

    @Test
    public void testAssign_vehicleDocumentExpired_isRejected() {
        Long vehicleId = createVehicle("浙A11111", LocalDate.now().minusDays(1), null);
        Long driverId = createDriver(1024L, "张三", null, null);
        Long taskId = createPendingTask();

        assertServiceException(() -> logisticsTransportTaskService.assignTask(assignReq(taskId, vehicleId, driverId)),
                VEHICLE_DOCUMENT_EXPIRED);
    }

    @Test
    public void testAssign_driverDocumentExpired_isRejected() {
        Long vehicleId = createVehicle("浙A11111", null, null);
        Long driverId = createDriver(1024L, "张三", LocalDate.now().minusDays(1), null);
        Long taskId = createPendingTask();

        assertServiceException(() -> logisticsTransportTaskService.assignTask(assignReq(taskId, vehicleId, driverId)),
                DRIVER_DOCUMENT_EXPIRED);
    }

    @Test
    public void testAssign_documentsExpiringToday_isAllowed() {
        // 今天到期的当天仍然算有效：过期是「早于今天」，否则当天换证的车就出不了门了
        Long vehicleId = createVehicle("浙A11111", LocalDate.now(), null);
        Long driverId = createDriver(1024L, "张三", null, LocalDate.now());
        Long taskId = createPendingTask();

        logisticsTransportTaskService.assignTask(assignReq(taskId, vehicleId, driverId));

        assertEquals(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus(),
                logisticsTransportTaskMapper.selectById(taskId).getStatus());
    }

    // ==================== 授权放行（软门禁的逃生门） ====================

    @Test
    public void testAssignWithOverride_expiredDocuments_recordsReasonAndOperator() {
        loginAs(ADMIN_USER_ID);
        Long vehicleId = createVehicle("浙A11111", LocalDate.now().minusDays(3), null);
        Long driverId = createDriver(1024L, "张三", LocalDate.now().minusDays(3), null);
        Long taskId = createPendingTask();

        LogisticsTransportTaskOverrideAssignReqVO overrideReqVO = new LogisticsTransportTaskOverrideAssignReqVO();
        overrideReqVO.setId(taskId);
        overrideReqVO.setVehicleId(vehicleId);
        overrideReqVO.setDriverId(driverId);
        overrideReqVO.setOverrideReason("行驶证正在换证，客户催货");
        logisticsTransportTaskService.assignTaskWithOverride(overrideReqVO);

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus(), task.getStatus());
        assertEquals("行驶证正在换证，客户催货", task.getOverrideReason());
        assertEquals(ADMIN_USER_ID, task.getOverrideBy());
        assertNotNull(task.getOverrideTime());
        // 正常派车不该有留痕
        assertNull(createAssignedTaskNormally());
    }

    @Test
    public void testAssignWithOverride_withoutReason_isRejected() {
        Long vehicleId = createVehicle("浙A11111", LocalDate.now().minusDays(3), null);
        Long driverId = createDriver(1024L, "张三", null, null);
        Long taskId = createPendingTask();

        LogisticsTransportTaskOverrideAssignReqVO overrideReqVO = new LogisticsTransportTaskOverrideAssignReqVO();
        overrideReqVO.setId(taskId);
        overrideReqVO.setVehicleId(vehicleId);
        overrideReqVO.setDriverId(driverId);
        // 服务层也要拦：VO 的 @NotEmpty 只挡 HTTP 入口
        assertServiceException(() -> logisticsTransportTaskService.assignTaskWithOverride(overrideReqVO),
                TRANSPORT_TASK_OVERRIDE_REASON_REQUIRED);
    }

    @Test
    public void testAssignWithOverride_cannotBypassHardGate_vehicleInMaintenance() {
        loginAs(ADMIN_USER_ID);
        Long vehicleId = createVehicle("浙A11111", null, null);
        LogisticsVehicleSaveReqVO toMaintenance = new LogisticsVehicleSaveReqVO();
        toMaintenance.setId(vehicleId);
        toMaintenance.setPlateNo("浙A11111");
        toMaintenance.setStatus(LogisticsVehicleStatusEnum.MAINTENANCE.getStatus());
        logisticsVehicleService.updateVehicle(toMaintenance);
        Long driverId = createDriver(1024L, "张三", null, null);
        Long taskId = createPendingTask();

        LogisticsTransportTaskOverrideAssignReqVO overrideReqVO = new LogisticsTransportTaskOverrideAssignReqVO();
        overrideReqVO.setId(taskId);
        overrideReqVO.setVehicleId(vehicleId);
        overrideReqVO.setDriverId(driverId);
        overrideReqVO.setOverrideReason("想绕过维修状态");

        assertServiceException(() -> logisticsTransportTaskService.assignTaskWithOverride(overrideReqVO),
                TRANSPORT_TASK_OVERRIDE_NOT_APPLICABLE);
    }

    @Test
    public void testAssignWithOverride_cannotBypassHardGate_driverInactive() {
        loginAs(ADMIN_USER_ID);
        Long vehicleId = createVehicle("浙A11111", null, null);
        Long driverId = createDriver(1024L, "张三", null, null);
        LogisticsDriverSaveReqVO inactive = newDriver(1024L, "张三", null, null);
        inactive.setId(driverId);
        inactive.setStatus(LogisticsDriverStatusEnum.INACTIVE.getStatus());
        logisticsDriverService.updateDriver(inactive);
        Long taskId = createPendingTask();

        LogisticsTransportTaskOverrideAssignReqVO overrideReqVO = new LogisticsTransportTaskOverrideAssignReqVO();
        overrideReqVO.setId(taskId);
        overrideReqVO.setVehicleId(vehicleId);
        overrideReqVO.setDriverId(driverId);
        overrideReqVO.setOverrideReason("想绕过离职状态");

        assertServiceException(() -> logisticsTransportTaskService.assignTaskWithOverride(overrideReqVO),
                TRANSPORT_TASK_OVERRIDE_NOT_APPLICABLE);
    }

    // ==================== 承运商 ====================

    @Test
    public void testCreateDriver_carrierSource_requiresCarrier() {
        LogisticsDriverSaveReqVO driver = newDriver(1024L, "第三方司机", null, null);
        driver.setSource(LogisticsDriverSourceEnum.CARRIER.getSource());

        assertServiceException(() -> logisticsDriverService.createDriver(driver), DRIVER_CARRIER_REQUIRED);
    }

    @Test
    public void testCreateDriver_carrierSource_withInactiveCarrier_isRejected() {
        Long carrierId = createCarrier("某某物流", 1);
        LogisticsDriverSaveReqVO driver = newDriver(1024L, "第三方司机", null, null);
        driver.setSource(LogisticsDriverSourceEnum.CARRIER.getSource());
        driver.setCarrierId(carrierId);

        assertServiceException(() -> logisticsDriverService.createDriver(driver), CARRIER_NOT_ACTIVE);
    }

    @Test
    public void testCreateDriver_carrierSource_withActiveCarrier_isAllowed() {
        Long carrierId = createCarrier("某某物流", 0);
        LogisticsDriverSaveReqVO driver = newDriver(1024L, "第三方司机", null, null);
        driver.setSource(LogisticsDriverSourceEnum.CARRIER.getSource());
        driver.setCarrierId(carrierId);

        Long driverId = logisticsDriverService.createDriver(driver);

        assertEquals(carrierId, logisticsDriverService.getDriver(driverId).getCarrierId());
    }

    @Test
    public void testCreateCarrier_duplicateName_isRejected() {
        createCarrier("某某物流", 0);

        LogisticsCarrierSaveReqVO carrier = newCarrier("某某物流", 0);
        assertServiceException(() -> logisticsCarrierService.createCarrier(carrier), CARRIER_NAME_DUPLICATE);
    }

    // ==================== 到期提醒与门禁同口径 ====================

    @Test
    public void testExpiryWarning_listsExpiredAndDueSoon_sortedByDaysLeft() {
        // 一辆已过期、一辆 10 天后到期、一辆没登记到期日
        createVehicle("浙A11111", LocalDate.now().minusDays(5), LocalDate.now().plusDays(200));
        createVehicle("浙B22222", LocalDate.now().plusDays(10), null);
        createVehicle("浙C33333", null, null);
        createDriver(1024L, "张三", LocalDate.now().minusDays(1), LocalDate.now().plusDays(400));

        List<LogisticsExpiryWarningItemVO> warnings = logisticsExpiryWarningService.getWarningList(30);

        assertEquals(3, warnings.size(), "没登记到期日的不该提醒");
        // 最急的排最前：已过期的行驶证 → 已过期的驾驶证 → 10 天后到期的行驶证
        assertEquals("车辆-行驶证", warnings.get(0).getCategory());
        assertTrue(warnings.get(0).getExpired());
        assertEquals(-5, warnings.get(0).getDaysLeft());
        assertEquals("司机-驾驶证", warnings.get(1).getCategory());
        assertTrue(warnings.get(1).getExpired());
        assertEquals("车辆-行驶证", warnings.get(2).getCategory());
        assertFalse(warnings.get(2).getExpired());
        assertEquals(10, warnings.get(2).getDaysLeft());
    }

    @Test
    public void testExpiryWarningAndGate_shareTheSameVerdict() {
        // 提醒里判为已过期的那辆车，派车必须也被拦——两处各写一套判定就会出现这条裂缝
        Long vehicleId = createVehicle("浙A11111", LocalDate.now().minusDays(1), null);
        Long driverId = createDriver(1024L, "张三", null, null);
        Long taskId = createPendingTask();

        List<LogisticsExpiryWarningItemVO> warnings = logisticsExpiryWarningService.getWarningList(30);
        assertTrue(warnings.stream().anyMatch(w -> w.getSubjectId().equals(vehicleId) && w.getExpired()));

        assertServiceException(() -> logisticsTransportTaskService.assignTask(assignReq(taskId, vehicleId, driverId)),
                VEHICLE_DOCUMENT_EXPIRED);
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

    /**
     * 正常派车（证件齐全）后返回任务的授权放行原因——应为 null。
     */
    private String createAssignedTaskNormally() {
        Long vehicleId = createVehicle("浙D44444", LocalDate.now().plusDays(365), LocalDate.now().plusDays(365));
        Long driverId = createDriver(2048L, "李四", LocalDate.now().plusDays(365), LocalDate.now().plusDays(365));
        Long taskId = createPendingTask();
        logisticsTransportTaskService.assignTask(assignReq(taskId, vehicleId, driverId));
        return logisticsTransportTaskMapper.selectById(taskId).getOverrideReason();
    }

    private Long createVehicle(String plateNo, LocalDate licenseExpiry, LocalDate insuranceExpiry) {
        LogisticsVehicleSaveReqVO vehicle = new LogisticsVehicleSaveReqVO();
        vehicle.setPlateNo(plateNo);
        vehicle.setStatus(LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        vehicle.setDrivingLicenseExpiryDate(licenseExpiry);
        vehicle.setInsuranceExpiryDate(insuranceExpiry);
        return logisticsVehicleService.createVehicle(vehicle);
    }

    private Long createDriver(Long userId, String name, LocalDate licenseExpiry, LocalDate certExpiry) {
        return logisticsDriverService.createDriver(newDriver(userId, name, licenseExpiry, certExpiry));
    }

    private static LogisticsDriverSaveReqVO newDriver(Long userId, String name, LocalDate licenseExpiry,
                                                     LocalDate certExpiry) {
        LogisticsDriverSaveReqVO driver = new LogisticsDriverSaveReqVO();
        driver.setUserId(userId);
        driver.setName(name);
        driver.setMobile("13800138000");
        driver.setSource(LogisticsDriverSourceEnum.SELF.getSource());
        driver.setStatus(LogisticsDriverStatusEnum.ACTIVE.getStatus());
        driver.setDrivingLicenseExpiryDate(licenseExpiry);
        driver.setQualificationCertExpiryDate(certExpiry);
        return driver;
    }

    private Long createCarrier(String name, int status) {
        return logisticsCarrierService.createCarrier(newCarrier(name, status));
    }

    private static LogisticsCarrierSaveReqVO newCarrier(String name, int status) {
        LogisticsCarrierSaveReqVO carrier = new LogisticsCarrierSaveReqVO();
        carrier.setName(name);
        carrier.setContactName("王经理");
        carrier.setContactMobile("13900139000");
        carrier.setStatus(status);
        return carrier;
    }

    private Long createPendingTask() {
        LogisticsTransportTaskSaveReqVO task = new LogisticsTransportTaskSaveReqVO();
        task.setDepartureAddress("城东场站");
        task.setPickupAddress("某某路 1 号");
        return logisticsTransportTaskService.createTask(task);
    }

    private static LogisticsTransportTaskAssignReqVO assignReq(Long taskId, Long vehicleId, Long driverId) {
        LogisticsTransportTaskAssignReqVO reqVO = new LogisticsTransportTaskAssignReqVO();
        reqVO.setId(taskId);
        reqVO.setVehicleId(vehicleId);
        reqVO.setDriverId(driverId);
        return reqVO;
    }

}
