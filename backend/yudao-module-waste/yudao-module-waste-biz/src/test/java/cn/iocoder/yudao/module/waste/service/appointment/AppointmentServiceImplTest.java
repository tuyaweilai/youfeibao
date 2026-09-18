package cn.iocoder.yudao.module.waste.service.appointment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.dal.mysql.appointment.AppointmentMapper;
import cn.iocoder.yudao.module.waste.enums.AppointmentStatusEnum;
import cn.iocoder.yudao.module.waste.enums.AssignmentTypeEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link AppointmentServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(AppointmentServiceImpl.class)
public class AppointmentServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AppointmentServiceImpl appointmentService;

    @Resource
    private AppointmentMapper appointmentMapper;

    @MockBean
    private EnterpriseInfoService enterpriseInfoService;

    @Test
    public void testCreateAppointment_success() {
        // Mock 企业信息
        EnterpriseInfoDO enterpriseInfo = new EnterpriseInfoDO();
        enterpriseInfo.setId(1L);
        enterpriseInfo.setName("测试企业");
        when(enterpriseInfoService.getEnterpriseInfo(1L)).thenReturn(enterpriseInfo);

        // 准备参数
        AppointmentCreateReqVO createReqVO = randomPojo(AppointmentCreateReqVO.class, o -> {
            o.setProducerEnterpriseId(1L);
            o.setRecyclerEnterpriseId(null); // 不指定回收企业，避免验证问题
            o.setProducerContactName("张三");
            o.setProducerContactPhone("13800138000");
            o.setWasteCode("HW01");
            o.setWasteName("医疗废物");
            o.setWasteCategory("医疗");
            o.setEstimatedQuantity(new BigDecimal("100.50"));
            o.setQuantityUnit("吨");
            o.setPickupAddress("北京市朝阳区xxx街道xxx号");
            o.setExpectedPickupTime(LocalDateTime.now().plusDays(1));
            o.setBusinessMode(0);
            o.setPriorityLevel(1); // 设置在有效范围内的值
        });

        // 调用
        Long appointmentId = appointmentService.createAppointment(createReqVO);
        
        // 校验结果
        assertNotNull(appointmentId);
        AppointmentDO appointment = appointmentMapper.selectById(appointmentId);
        assertNotNull(appointment);
        assertEquals("张三", appointment.getProducerContactName());
        assertEquals("医疗废物", appointment.getWasteName());
        assertEquals("测试企业", appointment.getProducerEnterpriseName());
    }

    @Test
    public void testUpdateAppointment_success() {
        // mock 数据
        AppointmentDO dbAppointment = randomPojo(AppointmentDO.class, o -> {
            o.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
            o.setBusinessMode(0);
            o.setPriorityLevel(20);
            o.setProducerEnterpriseName("测试企业");
        });
        appointmentMapper.insert(dbAppointment);

        // 准备参数
        AppointmentUpdateReqVO updateReqVO = randomPojo(AppointmentUpdateReqVO.class, o -> {
            o.setId(dbAppointment.getId());
            o.setBusinessMode(0);
            o.setPriorityLevel(30);
            o.setProducerContactName("李四");
            o.setWasteName("更新后的废物名称");
        });

        // 调用
        appointmentService.updateAppointment(updateReqVO);
        // 校验是否更新正确
        AppointmentDO appointment = appointmentMapper.selectById(dbAppointment.getId());
        assertNotNull(appointment);
        assertEquals("李四", appointment.getProducerContactName());
        assertEquals("更新后的废物名称", appointment.getWasteName());
    }

    @Test
    public void testDeleteAppointment_success() {
        // mock 数据
        AppointmentDO dbAppointment = randomPojo(AppointmentDO.class, o -> {
            o.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
            o.setBusinessMode(0);
            o.setPriorityLevel(40);
            o.setProducerEnterpriseName("测试企业");
        });
        appointmentMapper.insert(dbAppointment);
        // 调用
        appointmentService.deleteAppointment(dbAppointment.getId());
        // 校验数据不存在了
        assertNull(appointmentMapper.selectById(dbAppointment.getId()));
    }

    @Test
    public void testConfirmAppointment_success() {
        // mock 数据
        AppointmentDO dbAppointment = randomPojo(AppointmentDO.class, o -> {
            o.setAppointmentStatus(AppointmentStatusEnum.WAITING_RECYCLER_CONFIRM.getStatus());
            o.setRecyclerEnterpriseId(1L);
            o.setBusinessMode(0);
            o.setPriorityLevel(30);
            o.setProducerEnterpriseName("测试企业");
        });
        appointmentMapper.insert(dbAppointment);

        // 调用
        appointmentService.confirmAppointment(dbAppointment.getId(), "确认收货");
        // 校验状态更新
        AppointmentDO appointment = appointmentMapper.selectById(dbAppointment.getId());
        assertEquals(appointment.getAppointmentStatus(), AppointmentStatusEnum.CONFIRMED.getStatus());
        assertNotNull(appointment.getConfirmTime());
    }

    @Test
    public void testConfirmAppointment_cannotConfirm() {
        // mock 数据
        AppointmentDO dbAppointment = randomPojo(AppointmentDO.class, o -> {
            o.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
            o.setBusinessMode(0);
            o.setPriorityLevel(40);
            o.setProducerEnterpriseName("测试企业");
        });
        appointmentMapper.insert(dbAppointment);

        // 调用，并断言异常
        assertThrows(Exception.class, () -> appointmentService.confirmAppointment(dbAppointment.getId(), "确认收货"));
    }

    @Test
    public void testRejectAppointment_success() {
        // mock 数据
        AppointmentDO dbAppointment = randomPojo(AppointmentDO.class, o -> {
            o.setAppointmentStatus(AppointmentStatusEnum.WAITING_RECYCLER_CONFIRM.getStatus());
            o.setBusinessMode(0);
            o.setPriorityLevel(50);
            o.setProducerEnterpriseName("测试企业");
        });
        appointmentMapper.insert(dbAppointment);

        // 调用
        appointmentService.rejectAppointment(dbAppointment.getId(), "不符合接收条件");
        // 校验状态更新
        AppointmentDO appointment = appointmentMapper.selectById(dbAppointment.getId());
        assertEquals(appointment.getAppointmentStatus(), AppointmentStatusEnum.REJECTED.getStatus());
        assertNotNull(appointment.getRejectTime());
        assertEquals("不符合接收条件", appointment.getRejectReason());
    }

    @Test
    public void testCancelAppointment_success() {
        // mock 数据
        AppointmentDO dbAppointment = randomPojo(AppointmentDO.class, o -> {
            o.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
            o.setBusinessMode(0);
            o.setPriorityLevel(60);
            o.setProducerEnterpriseName("测试企业");
        });
        appointmentMapper.insert(dbAppointment);

        // 调用
        appointmentService.cancelAppointment(dbAppointment.getId(), "临时变更计划");
        // 校验状态更新
        AppointmentDO appointment = appointmentMapper.selectById(dbAppointment.getId());
        assertEquals(appointment.getAppointmentStatus(), AppointmentStatusEnum.CANCELLED.getStatus());
        assertNotNull(appointment.getCancelTime());
        assertEquals("临时变更计划", appointment.getCancelReason());
    }

    @Test
    public void testAssignRecyclerEnterprise_success() {
        // mock 企业信息
        EnterpriseInfoDO mockRecyclerEnterprise = new EnterpriseInfoDO();
        mockRecyclerEnterprise.setId(1L);
        mockRecyclerEnterprise.setName("测试回收企业");
        when(enterpriseInfoService.getEnterpriseInfo(1L)).thenReturn(mockRecyclerEnterprise);
        
        // mock 数据
        AppointmentDO dbAppointment = randomPojo(AppointmentDO.class, o -> {
            o.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
            o.setBusinessMode(0);
            o.setPriorityLevel(70);
            o.setProducerEnterpriseName("测试企业");
        });
        appointmentMapper.insert(dbAppointment);

        // 调用
        appointmentService.assignRecyclerEnterprise(dbAppointment.getId(), 1L, 
                AssignmentTypeEnum.MANUAL.getType(), "admin");
        // 校验回收企业分配
        AppointmentDO appointment = appointmentMapper.selectById(dbAppointment.getId());
        assertEquals(1L, appointment.getRecyclerEnterpriseId());
        assertEquals("测试回收企业", appointment.getRecyclerEnterpriseName());
        assertEquals(AssignmentTypeEnum.MANUAL.getType(), appointment.getAssignmentType());
        assertEquals("admin", appointment.getAssignmentOperator());
        assertNotNull(appointment.getAssignmentTime());
        assertEquals(AppointmentStatusEnum.WAITING_RECYCLER_CONFIRM.getStatus(), appointment.getAppointmentStatus());
    }

    @Test
    public void testValidateAppointmentExists_success() {
        // mock 数据
        AppointmentDO dbAppointment = randomPojo(AppointmentDO.class, o -> {
            o.setEstimatedQuantity(new BigDecimal("0.75")); // 固定精确值
            o.setBusinessMode(0);
            o.setPriorityLevel(20);
            o.setProducerEnterpriseName("测试企业");
            // 设置固定的浮点值，避免精度问题
            o.setPickupLatitude(new BigDecimal("0.7096194"));
            o.setPickupLongitude(new BigDecimal("0.9312763"));
            o.setDeliveryLatitude(new BigDecimal("0.8263379"));
            o.setDeliveryLongitude(new BigDecimal("0.1435948"));
        });
        appointmentMapper.insert(dbAppointment);
        
        // 调用
        AppointmentDO result = appointmentService.validateAppointmentExists(dbAppointment.getId());
        
        // 断言 - 使用自定义比较器处理BigDecimal精度问题
        assertPojoEquals(dbAppointment, result, "pickupLatitude", "pickupLongitude", "estimatedQuantity", 
                "deliveryLatitude", "deliveryLongitude");
        // 单独断言BigDecimal字段，使用compareTo进行比较而不是equals
        assertEquals(0, dbAppointment.getEstimatedQuantity().compareTo(result.getEstimatedQuantity()));
        assertEquals(0, dbAppointment.getPickupLatitude().compareTo(result.getPickupLatitude()));
        assertEquals(0, dbAppointment.getPickupLongitude().compareTo(result.getPickupLongitude()));
        assertEquals(0, dbAppointment.getDeliveryLatitude().compareTo(result.getDeliveryLatitude()));
        assertEquals(0, dbAppointment.getDeliveryLongitude().compareTo(result.getDeliveryLongitude()));
    }

    @Test
    public void testValidateAppointmentExists_notExists() {
        assertThrows(Exception.class, () -> appointmentService.validateAppointmentExists(randomLongId()));
    }
} 