package cn.iocoder.yudao.module.logistics.service.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehiclePageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.vehicle.LogisticsVehicleMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
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
 * {@link LogisticsVehicleServiceImpl} 的单元测试（V2a #77）。
 *
 * <p>断言的是外部可观察行为：车牌能不能重复、哪些状态不允许手工设置、查不到时给什么错。
 * 车牌唯一性只在 Service 层校验（见 SQL 文件头的理由），所以这里就是它的**唯一守卫**——
 * 这条测试比平时更重要。
 */
@Import({LogisticsVehicleServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsVehicleServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsVehicleMapper logisticsVehicleMapper;

    @Test
    public void testCreateVehicle_success() {
        Long id = logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));

        LogisticsVehicleDO vehicle = logisticsVehicleMapper.selectById(id);
        assertNotNull(vehicle);
        assertEquals("浙A12345", vehicle.getPlateNo());
        assertEquals("厢式货车", vehicle.getVehicleType());
        assertEquals(0, new BigDecimal("10.500").compareTo(vehicle.getCapacityTon()));
    }

    @Test
    public void testCreateVehicle_duplicatePlateNo_isRejected() {
        logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));

        assertServiceException(
                () -> logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus())),
                VEHICLE_PLATE_NO_DUPLICATE);
    }

    @Test
    public void testUpdateVehicle_sameVehicleKeepsItsOwnPlate() {
        Long id = logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));

        // 改别的字段，车牌不变：不该被自己撞到
        LogisticsVehicleSaveReqVO updateReqVO = newVehicle("浙A12345", LogisticsVehicleStatusEnum.MAINTENANCE.getStatus());
        updateReqVO.setId(id);
        updateReqVO.setVehicleType("平板");
        logisticsVehicleService.updateVehicle(updateReqVO);

        LogisticsVehicleDO vehicle = logisticsVehicleMapper.selectById(id);
        assertEquals("平板", vehicle.getVehicleType());
        assertEquals(LogisticsVehicleStatusEnum.MAINTENANCE.getStatus(), vehicle.getStatus());
    }

    @Test
    public void testUpdateVehicle_plateUsedByAnother_isRejected() {
        logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));
        Long otherId = logisticsVehicleService.createVehicle(newVehicle("浙B54321", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));

        LogisticsVehicleSaveReqVO updateReqVO = newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        updateReqVO.setId(otherId);

        assertServiceException(() -> logisticsVehicleService.updateVehicle(updateReqVO), VEHICLE_PLATE_NO_DUPLICATE);
    }

    @Test
    public void testSaveVehicle_inTransitStatus_isRejected() {
        // 「运输中」由运输任务驱动，不接受在档案上手工设置
        assertServiceException(
                () -> logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.IN_TRANSIT.getStatus())),
                VEHICLE_STATUS_NOT_ALLOW_UPDATE);

        Long id = logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));
        LogisticsVehicleSaveReqVO updateReqVO = newVehicle("浙A12345", LogisticsVehicleStatusEnum.IN_TRANSIT.getStatus());
        updateReqVO.setId(id);
        assertServiceException(() -> logisticsVehicleService.updateVehicle(updateReqVO), VEHICLE_STATUS_NOT_ALLOW_UPDATE);
    }

    @Test
    public void testGetVehicle_notExists() {
        assertServiceException(() -> logisticsVehicleService.getVehicle(-1L), VEHICLE_NOT_EXISTS);
    }

    @Test
    public void testDeleteVehicle_thenPlateCanBeReused() {
        // 逻辑删除后同一个车牌要能重新建档（这正是唯一性不建 DB 唯一键的理由）
        Long id = logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));
        logisticsVehicleService.deleteVehicle(id);

        Long newId = logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));
        assertNotEquals(id, newId);
    }

    @Test
    public void testGetVehiclePage_filterByStatus() {
        logisticsVehicleService.createVehicle(newVehicle("浙A12345", LogisticsVehicleStatusEnum.AVAILABLE.getStatus()));
        logisticsVehicleService.createVehicle(newVehicle("浙B54321", LogisticsVehicleStatusEnum.MAINTENANCE.getStatus()));

        LogisticsVehiclePageReqVO pageReqVO = new LogisticsVehiclePageReqVO();
        pageReqVO.setStatus(LogisticsVehicleStatusEnum.MAINTENANCE.getStatus());
        PageResult<LogisticsVehicleDO> page = logisticsVehicleService.getVehiclePage(pageReqVO);

        assertEquals(1, page.getTotal());
        assertEquals("浙B54321", page.getList().get(0).getPlateNo());
    }

    private static LogisticsVehicleSaveReqVO newVehicle(String plateNo, Integer status) {
        LogisticsVehicleSaveReqVO reqVO = new LogisticsVehicleSaveReqVO();
        reqVO.setPlateNo(plateNo);
        reqVO.setVehicleType("厢式货车");
        reqVO.setCapacityTon(new BigDecimal("10.5"));
        reqVO.setStatus(status);
        return reqVO;
    }

}
