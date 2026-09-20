package cn.iocoder.yudao.module.logistics.service.vehicle;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.VehicleCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.VehicleUpdateReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.VehicleDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.vehicle.VehicleMapper;
import cn.iocoder.yudao.module.logistics.enums.VehicleStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link VehicleServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(VehicleServiceImpl.class)
@ActiveProfiles("unit-test")
public class VehicleServiceImplTest extends BaseDbUnitTest {

    @Resource
    private VehicleServiceImpl vehicleService;

    @Resource
    private VehicleMapper vehicleMapper;

    @Test
    public void testCreateVehicle_success() {
        // 准备参数
        VehicleCreateReqVO createReqVO = randomPojo(VehicleCreateReqVO.class, o -> {
            o.setPlateNumber("京A12345");
            o.setStatus(VehicleStatusEnum.AVAILABLE.getStatus());
            o.setCapacityKg(new BigDecimal("5000.00"));
            o.setVehiclePhotos(Arrays.asList("http://example.com/photo1.jpg"));
            o.setLicensePhotos(Arrays.asList("http://example.com/license1.jpg"));
        });

        // 调用
        Long vehicleId = vehicleService.createVehicle(createReqVO);
        // 断言
        assertNotNull(vehicleId);
        // 校验记录的属性是否正确
        VehicleDO vehicle = vehicleMapper.selectById(vehicleId);
        assertPojoEquals(createReqVO, vehicle, "vehiclePhotos", "licensePhotos");
        assertNotNull(vehicle.getVehiclePhotos());
        assertNotNull(vehicle.getLicensePhotos());
    }

    @Test
    public void testCreateVehicle_plateNumberExists() {
        // mock 数据
        VehicleDO dbVehicle = randomPojo(VehicleDO.class, o -> o.setPlateNumber("京A12345"));
        vehicleMapper.insert(dbVehicle);// @Sql: 先插入出一条存在的数据
        // 准备参数
        VehicleCreateReqVO createReqVO = randomPojo(VehicleCreateReqVO.class, o -> {
            o.setPlateNumber("京A12345"); // 设置相同的车牌号
            o.setStatus(VehicleStatusEnum.AVAILABLE.getStatus());
        });

        // 调用，并断言异常
        assertServiceException(() -> vehicleService.createVehicle(createReqVO), VEHICLE_PLATE_NUMBER_EXISTS);
    }

    @Test
    public void testUpdateVehicle_success() {
        // mock 数据
        VehicleDO dbVehicle = randomPojo(VehicleDO.class, o -> {
            o.setPlateNumber("京A12345");
            o.setStatus(VehicleStatusEnum.AVAILABLE.getStatus());
        });
        vehicleMapper.insert(dbVehicle);// @Sql: 先插入出一条存在的数据
        // 准备参数
        VehicleUpdateReqVO updateReqVO = randomPojo(VehicleUpdateReqVO.class, o -> {
            o.setId(dbVehicle.getId()); // 设置更新的 ID
            o.setPlateNumber("京A54321");
            o.setStatus(VehicleStatusEnum.MAINTENANCE.getStatus());
            o.setVehiclePhotos(Arrays.asList("http://example.com/photo2.jpg"));
            o.setLicensePhotos(Arrays.asList("http://example.com/license2.jpg"));
        });

        // 调用
        vehicleService.updateVehicle(updateReqVO);
        // 校验是否更新正确
        VehicleDO vehicle = vehicleMapper.selectById(updateReqVO.getId()); // 获取最新的
        assertPojoEquals(updateReqVO, vehicle, "vehiclePhotos", "licensePhotos");
    }

    @Test
    public void testUpdateVehicle_notExists() {
        // 准备参数
        VehicleUpdateReqVO updateReqVO = randomPojo(VehicleUpdateReqVO.class);

        // 调用，并断言异常
        assertServiceException(() -> vehicleService.updateVehicle(updateReqVO), VEHICLE_NOT_EXISTS);
    }

    @Test
    public void testDeleteVehicle_success() {
        // mock 数据
        VehicleDO dbVehicle = randomPojo(VehicleDO.class, o -> {
            o.setStatus(VehicleStatusEnum.AVAILABLE.getStatus());
        });
        vehicleMapper.insert(dbVehicle);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbVehicle.getId();

        // 调用
        vehicleService.deleteVehicle(id);
        // 校验数据不存在了
        assertNull(vehicleMapper.selectById(id));
    }

    @Test
    public void testDeleteVehicle_notExists() {
        // 准备参数
        Long id = randomLongId();

        // 调用，并断言异常
        assertServiceException(() -> vehicleService.deleteVehicle(id), VEHICLE_NOT_EXISTS);
    }

    @Test
    public void testDeleteVehicle_inUse() {
        // mock 数据
        VehicleDO dbVehicle = randomPojo(VehicleDO.class, o -> {
            o.setStatus(VehicleStatusEnum.IN_TRANSIT.getStatus()); // 设置为运输中状态
        });
        vehicleMapper.insert(dbVehicle);
        // 准备参数
        Long id = dbVehicle.getId();

        // 调用，并断言异常
        assertServiceException(() -> vehicleService.deleteVehicle(id), VEHICLE_IN_USE);
    }

} 