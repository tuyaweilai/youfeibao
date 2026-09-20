package cn.iocoder.yudao.module.waste.service.appointment;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.waste.controller.app.appointment.vo.AppAppointmentCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.app.appointment.vo.AppAppointmentRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.dal.mysql.appointment.AppointmentMapper;
import cn.iocoder.yudao.module.waste.enums.AppointmentStatusEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * APP端预约服务测试
 */
@Import(AppointmentServiceImpl.class)
public class AppAppointmentServiceTest extends BaseDbUnitTest {

    @Resource
    private AppointmentService appointmentService;

    @Resource
    private AppointmentMapper appointmentMapper;

    @MockBean
    private EnterpriseInfoService enterpriseInfoService;

    @Test
    public void testCreateAppointmentByApp_success() {
        // Mock企业信息
        EnterpriseInfoDO enterprise = new EnterpriseInfoDO();
        enterprise.setId(1L);
        enterprise.setName("测试产废企业");
        when(enterpriseInfoService.getEnterpriseInfo(1L)).thenReturn(enterprise);

        // 准备参数
        AppAppointmentCreateReqVO createReqVO = new AppAppointmentCreateReqVO();
        createReqVO.setProducerContactName("张三");
        createReqVO.setProducerContactPhone("13800138000");
        createReqVO.setWasteCode("HW01");
        createReqVO.setWasteName("医疗废物");
        createReqVO.setWasteCategory("医疗");
        createReqVO.setEstimatedQuantity(new BigDecimal("100.50"));
        createReqVO.setQuantityUnit("吨");
        createReqVO.setPickupAddress("北京市朝阳区xxx街道xxx号");
        createReqVO.setExpectedPickupTime(LocalDateTime.now().plusDays(1));
        createReqVO.setIsUrgent(false);
        createReqVO.setPriorityLevel(1);

        // 调用
        Long appointmentId = appointmentService.createAppointmentByApp(createReqVO, 1L);

        // 校验
        assertNotNull(appointmentId);
        AppointmentDO appointment = appointmentMapper.selectById(appointmentId);
        assertNotNull(appointment);
        assertEquals(1L, appointment.getProducerEnterpriseId());
        assertEquals("测试产废企业", appointment.getProducerEnterpriseName());
        assertEquals("张三", appointment.getProducerContactName());
        assertEquals("13800138000", appointment.getProducerContactPhone());
        assertEquals("HW01", appointment.getWasteCode());
        assertEquals("医疗废物", appointment.getWasteName());
        // APP端创建预约会自动分配回收企业，状态会变为"待回收方确认"
        assertEquals(AppointmentStatusEnum.WAITING_RECYCLER_CONFIRM.getStatus(), appointment.getAppointmentStatus());
    }

    @Test
    public void testGetMyAppointmentDetail_success() {
        // Mock企业信息
        EnterpriseInfoDO enterprise = new EnterpriseInfoDO();
        enterprise.setId(1L);
        enterprise.setName("测试产废企业");
        when(enterpriseInfoService.getEnterpriseInfo(1L)).thenReturn(enterprise);

        // 创建测试数据
        AppointmentDO appointment = new AppointmentDO();
        appointment.setAppointmentNo("AP20231201001234");
        appointment.setProducerEnterpriseId(1L);
        appointment.setProducerEnterpriseName("测试产废企业");
        appointment.setProducerContactName("张三");
        appointment.setProducerContactPhone("13800138000");
        appointment.setWasteCode("HW01");
        appointment.setWasteName("医疗废物");
        appointment.setWasteCategory("医疗");
        appointment.setEstimatedQuantity(new BigDecimal("100.50"));
        appointment.setQuantityUnit("吨");
        appointment.setPickupAddress("北京市朝阳区xxx街道xxx号");
        appointment.setExpectedPickupTime(LocalDateTime.now().plusDays(1));
        appointment.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
        appointment.setBusinessMode(0);
        appointment.setIsUrgent(false);
        appointment.setPriorityLevel(1);
        appointmentMapper.insert(appointment);

        // 调用
        AppAppointmentRespVO result = appointmentService.getMyAppointmentDetail(appointment.getId(), 1L);

        // 校验
        assertNotNull(result);
        assertEquals(appointment.getId(), result.getId());
        assertEquals("AP20231201001234", result.getAppointmentNo());
        assertEquals(1L, result.getProducerEnterpriseId());
        assertEquals("测试产废企业", result.getProducerEnterpriseName());
        assertEquals("待处理", result.getAppointmentStatusName());
    }

    @Test
    public void testCancelMyAppointment_success() {
        // 创建测试数据
        AppointmentDO appointment = new AppointmentDO();
        appointment.setAppointmentNo("AP20231201001234");
        appointment.setProducerEnterpriseId(1L);
        appointment.setProducerEnterpriseName("测试产废企业");
        appointment.setProducerContactName("张三");
        appointment.setProducerContactPhone("13800138000");
        appointment.setWasteCode("HW01");
        appointment.setWasteName("医疗废物");
        appointment.setWasteCategory("医疗");
        appointment.setEstimatedQuantity(new BigDecimal("100.50"));
        appointment.setQuantityUnit("吨");
        appointment.setPickupAddress("北京市朝阳区xxx街道xxx号");
        appointment.setExpectedPickupTime(LocalDateTime.now().plusDays(1));
        appointment.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
        appointment.setBusinessMode(0);
        appointment.setIsUrgent(false);
        appointment.setPriorityLevel(1);
        appointmentMapper.insert(appointment);

        // 调用
        appointmentService.cancelMyAppointment(appointment.getId(), "临时变更计划", 1L);

        // 校验
        AppointmentDO updatedAppointment = appointmentMapper.selectById(appointment.getId());
        assertEquals(AppointmentStatusEnum.CANCELLED.getStatus(), updatedAppointment.getAppointmentStatus());
        assertEquals("临时变更计划", updatedAppointment.getCancelReason());
        assertNotNull(updatedAppointment.getCancelTime());
    }

} 