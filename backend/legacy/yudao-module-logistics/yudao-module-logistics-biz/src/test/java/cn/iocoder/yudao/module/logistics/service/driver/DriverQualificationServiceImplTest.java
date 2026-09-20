package cn.iocoder.yudao.module.logistics.service.driver;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.DriverQualificationCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.DriverQualificationUpdateReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.DriverQualificationDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.driver.DriverQualificationMapper;
import cn.iocoder.yudao.module.logistics.enums.DriverStatusEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Arrays;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link DriverQualificationServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(DriverQualificationServiceImpl.class)
@ActiveProfiles("unit-test")
public class DriverQualificationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DriverQualificationServiceImpl driverQualificationService;

    @Resource
    private DriverQualificationMapper driverQualificationMapper;

    @MockBean
    private AdminUserApi adminUserApi;

    @Test
    public void testCreateDriverQualification_success() {
        // mock 用户存在
        when(adminUserApi.getUser(eq(1L))).thenReturn(randomPojo(AdminUserRespDTO.class, o -> {
            o.setId(1L);
            o.setNickname("张三");
            o.setMobile("13800138000");
        }));

        // 准备参数
        DriverQualificationCreateReqVO createReqVO = randomPojo(DriverQualificationCreateReqVO.class, o -> {
            o.setUserId(1L);
            o.setEnterpriseId(100L);
            o.setDriverCode("D001");
            o.setDrivingLicenseNo("310101199001010001");
            o.setDrivingLicenseType("C1");
            o.setDrivingLicenseExpiryDate(LocalDate.of(2030, 12, 31));
            o.setStatus(DriverStatusEnum.ACTIVE.getStatus());
            o.setDriverLicensePhotos(Arrays.asList("http://example.com/license1.jpg"));
            o.setQualificationCertPhotos(Arrays.asList("http://example.com/cert1.jpg"));
            o.setHazardousCertPhotos(Arrays.asList("http://example.com/hazard1.jpg"));
        });

        // 调用
        Long driverQualificationId = driverQualificationService.createDriverQualification(createReqVO);
        // 断言
        assertNotNull(driverQualificationId);
        // 校验记录的属性是否正确
        DriverQualificationDO driverQualification = driverQualificationMapper.selectById(driverQualificationId);
        assertPojoEquals(createReqVO, driverQualification, "driverLicensePhotos", "qualificationCertPhotos", "hazardousCertPhotos");
        assertNotNull(driverQualification.getDriverLicensePhotos());
        assertNotNull(driverQualification.getQualificationCertPhotos());
        assertNotNull(driverQualification.getHazardousCertPhotos());
    }

    @Test
    public void testCreateDriverQualification_driverCodeExists() {
        // mock 数据
        DriverQualificationDO dbDriverQualification = randomPojo(DriverQualificationDO.class, o -> o.setDriverCode("D001"));
        driverQualificationMapper.insert(dbDriverQualification);// @Sql: 先插入出一条存在的数据
        
        // mock 用户存在
        when(adminUserApi.getUser(eq(1L))).thenReturn(randomPojo(AdminUserRespDTO.class, o -> o.setId(1L)));

        // 准备参数
        DriverQualificationCreateReqVO createReqVO = randomPojo(DriverQualificationCreateReqVO.class, o -> {
            o.setUserId(1L);
            o.setDriverCode("D001"); // 设置相同的司机编号
            o.setDrivingLicenseNo("310101199001010002");
            o.setStatus(DriverStatusEnum.ACTIVE.getStatus());
        });

        // 调用，并断言异常
        assertServiceException(() -> driverQualificationService.createDriverQualification(createReqVO), DRIVER_CODE_EXISTS);
    }

    @Test
    public void testCreateDriverQualification_drivingLicenseNoExists() {
        // mock 数据
        DriverQualificationDO dbDriverQualification = randomPojo(DriverQualificationDO.class, o -> o.setDrivingLicenseNo("310101199001010001"));
        driverQualificationMapper.insert(dbDriverQualification);// @Sql: 先插入出一条存在的数据
        
        // mock 用户存在
        when(adminUserApi.getUser(eq(1L))).thenReturn(randomPojo(AdminUserRespDTO.class, o -> o.setId(1L)));

        // 准备参数
        DriverQualificationCreateReqVO createReqVO = randomPojo(DriverQualificationCreateReqVO.class, o -> {
            o.setUserId(1L);
            o.setDriverCode("D002");
            o.setDrivingLicenseNo("310101199001010001"); // 设置相同的驾驶证号码
            o.setStatus(DriverStatusEnum.ACTIVE.getStatus());
        });

        // 调用，并断言异常
        assertServiceException(() -> driverQualificationService.createDriverQualification(createReqVO), DRIVER_LICENSE_NO_EXISTS);
    }

    @Test
    public void testUpdateDriverQualification_success() {
        // mock 数据
        DriverQualificationDO dbDriverQualification = randomPojo(DriverQualificationDO.class, o -> {
            o.setUserId(1L);
            o.setDriverCode("D001");
            o.setDrivingLicenseNo("310101199001010001");
            o.setStatus(DriverStatusEnum.ACTIVE.getStatus());
        });
        driverQualificationMapper.insert(dbDriverQualification);// @Sql: 先插入出一条存在的数据
        
        // mock 用户存在
        when(adminUserApi.getUser(eq(1L))).thenReturn(randomPojo(AdminUserRespDTO.class, o -> o.setId(1L)));

        // 准备参数
        DriverQualificationUpdateReqVO updateReqVO = randomPojo(DriverQualificationUpdateReqVO.class, o -> {
            o.setId(dbDriverQualification.getId()); // 设置更新的 ID
            o.setUserId(1L);
            o.setDriverCode("D002");
            o.setDrivingLicenseNo("310101199001010002");
            o.setStatus(DriverStatusEnum.ON_LEAVE.getStatus());
            o.setDriverLicensePhotos(Arrays.asList("http://example.com/license2.jpg"));
        });

        // 调用
        driverQualificationService.updateDriverQualification(updateReqVO);
        // 校验是否更新正确
        DriverQualificationDO driverQualification = driverQualificationMapper.selectById(updateReqVO.getId()); // 获取最新的
        assertPojoEquals(updateReqVO, driverQualification, "driverLicensePhotos", "qualificationCertPhotos", "hazardousCertPhotos");
    }

    @Test
    public void testUpdateDriverQualification_notExists() {
        // 准备参数
        DriverQualificationUpdateReqVO updateReqVO = randomPojo(DriverQualificationUpdateReqVO.class);

        // 调用，并断言异常
        assertServiceException(() -> driverQualificationService.updateDriverQualification(updateReqVO), DRIVER_NOT_EXISTS);
    }

    @Test
    public void testDeleteDriverQualification_success() {
        // mock 数据
        DriverQualificationDO dbDriverQualification = randomPojo(DriverQualificationDO.class, o -> {
            o.setStatus(DriverStatusEnum.ACTIVE.getStatus());
        });
        driverQualificationMapper.insert(dbDriverQualification);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbDriverQualification.getId();

        // 调用
        driverQualificationService.deleteDriverQualification(id);
        // 校验数据不存在了
        assertNull(driverQualificationMapper.selectById(id));
    }

    @Test
    public void testDeleteDriverQualification_notExists() {
        // 准备参数
        Long id = randomLongId();

        // 调用，并断言异常
        assertServiceException(() -> driverQualificationService.deleteDriverQualification(id), DRIVER_NOT_EXISTS);
    }

    @Test
    public void testGetDriverQualificationByUserId() {
        // mock 数据
        DriverQualificationDO dbDriverQualification = randomPojo(DriverQualificationDO.class, o -> {
            o.setUserId(1L);
        });
        driverQualificationMapper.insert(dbDriverQualification);

        // 调用
        DriverQualificationDO result = driverQualificationService.getDriverQualificationByUserId(1L);
        // 断言
        assertNotNull(result);
        assertEquals(dbDriverQualification.getId(), result.getId());
        assertEquals(1L, result.getUserId());
    }

    @Test
    public void testGetDriverQualificationByDriverCode() {
        // mock 数据
        DriverQualificationDO dbDriverQualification = randomPojo(DriverQualificationDO.class, o -> {
            o.setDriverCode("D001");
        });
        driverQualificationMapper.insert(dbDriverQualification);

        // 调用
        DriverQualificationDO result = driverQualificationService.getDriverQualificationByDriverCode("D001");
        // 断言
        assertNotNull(result);
        assertEquals(dbDriverQualification.getId(), result.getId());
        assertEquals("D001", result.getDriverCode());
    }

    @Test
    public void testUpdateDriverQualificationStatus() {
        // mock 数据
        DriverQualificationDO dbDriverQualification = randomPojo(DriverQualificationDO.class, o -> {
            o.setStatus(DriverStatusEnum.ACTIVE.getStatus());
        });
        driverQualificationMapper.insert(dbDriverQualification);

        // 调用
        driverQualificationService.updateDriverQualificationStatus(dbDriverQualification.getId(), DriverStatusEnum.INACTIVE.getStatus());
        
        // 校验
        DriverQualificationDO result = driverQualificationMapper.selectById(dbDriverQualification.getId());
        assertEquals(DriverStatusEnum.INACTIVE.getStatus(), result.getStatus());
    }

} 