package cn.iocoder.yudao.module.icbc.service.appointment;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentArriveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentNoShowReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentCreateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.appointment.IcbcAppointmentMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.AppointmentStatusEnum;
import cn.iocoder.yudao.module.icbc.service.appointment.impl.AppointmentServiceImpl;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link AppointmentServiceImpl} 的单元测试（#35，ADR 0020）。
 *
 * <p>守住这条功能的边界：预约不是订单（不接单、不拒单）、预计数量以「约」标注、
 * 只有到场 / 未到场两个动作、取消只限待到站、到站登记时能按出售者带出。
 */
@Import({AppointmentServiceImpl.class, NaturalPersonServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class AppointmentServiceTest extends BaseDbUnitTest {

    private static final Long MEMBER_USER_ID = 9001L;

    @Resource
    private AppointmentService appointmentService;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private IcbcAppointmentMapper appointmentMapper;
    @Resource
    private IcbcStationMapper stationMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @MockBean
    private TenantApi tenantApi;
    @MockBean
    private PayeeInfoService payeeInfoService;
    @MockBean
    private IcbcGoodsConfigService goodsConfigService;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testCreate_notBound_rejected() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        setLoginUser(MEMBER_USER_ID);
        assertServiceException(() -> appointmentService.create(createReq(person.getId(), "ST_A", 1L)),
                NATURAL_PERSON_NOT_BOUND_TO_LOGIN);
    }

    @Test
    public void testCreate_unknownStationCode_rejected() {
        IcbcNaturalPersonDO person = boundPerson();
        // 没有场站
        assertServiceException(() -> appointmentService.create(createReq(person.getId(), "NOPE", 1L)),
                STATION_NOT_EXISTS);
    }

    @Test
    public void testCreate_snapshotsStationAndGoods_andLabelsQuantityAsApprox() {
        IcbcNaturalPersonDO person = boundPerson();
        insertStation("ST_A", "城东收货点", 1L);
        IcbcGoodsConfigDO goods = insertGoods("废钢", "吨");

        AppointmentCreateReqVO req = createReq(person.getId(), "ST_A", goods.getId());
        req.setExpectedQuantity(new BigDecimal("12.50"));
        req.setPlateNo("京A12345");
        req.setExpectedArrivalTime(LocalDateTime.now().plusHours(2));
        Long id = appointmentService.create(req);

        IcbcAppointmentDO saved = appointmentMapper.selectById(id);
        assertNotNull(saved);
        assertTrue(saved.getAppointmentNo().startsWith("APT"));
        assertEquals(AppointmentStatusEnum.PENDING.getStatus(), saved.getStatus());
        assertEquals("城东收货点", saved.getStationName());
        assertEquals("ST_A", saved.getStationCode());
        assertEquals("废钢", saved.getCategoryName());
        assertEquals("吨", saved.getUnit());
        assertEquals("京A12345", saved.getPlateNo());
        assertEquals(person.getId(), saved.getNaturalPersonId());

        AppointmentRespVO resp = appointmentService.getListForSeller(person.getId()).get(0);
        // 预计数量一律以「约」标注
        assertEquals("约 12.5 吨", resp.getExpectedQuantityText());
        assertEquals("待到站", resp.getStatusName());
        // 界面上必须说清它不是订单
        assertTrue(resp.getScopeNote().contains("不是订单"));
        assertTrue(resp.getScopeNote().contains("不占额度"));
    }

    @Test
    public void testCreate_negativeQuantity_rejected() {
        IcbcNaturalPersonDO person = boundPerson();
        insertStation("ST_A", "城东收货点", 1L);
        IcbcGoodsConfigDO goods = insertGoods("废钢", "吨");
        AppointmentCreateReqVO req = createReq(person.getId(), "ST_A", goods.getId());
        req.setExpectedQuantity(new BigDecimal("-1"));
        assertServiceException(() -> appointmentService.create(req), APPOINTMENT_EXPECTED_QUANTITY_INVALID);
    }

    @Test
    public void testCreate_missingArrivalTime_rejected() {
        IcbcNaturalPersonDO person = boundPerson();
        insertStation("ST_A", "城东收货点", 1L);
        IcbcGoodsConfigDO goods = insertGoods("废钢", "吨");
        AppointmentCreateReqVO req = createReq(person.getId(), "ST_A", goods.getId());
        req.setExpectedArrivalTime(null);
        assertServiceException(() -> appointmentService.create(req), APPOINTMENT_ARRIVAL_TIME_REQUIRED);
    }

    @Test
    public void testCancel_onlyPending_andOnlyOwnAppointment() {
        IcbcNaturalPersonDO person = boundPerson();
        Long id = createAppointment(person.getId(), "ST_A", "废钢");

        // 别人的预约：不存在
        AppointmentCancelReqVO foreign = new AppointmentCancelReqVO();
        foreign.setNaturalPersonId(person.getId() + 1);
        foreign.setId(id);
        assertServiceException(() -> appointmentService.cancel(foreign), NATURAL_PERSON_NOT_BOUND_TO_LOGIN);

        AppointmentCancelReqVO cancel = new AppointmentCancelReqVO();
        cancel.setNaturalPersonId(person.getId());
        cancel.setId(id);
        appointmentService.cancel(cancel);
        IcbcAppointmentDO cancelled = appointmentMapper.selectById(id);
        assertEquals(AppointmentStatusEnum.CANCELLED.getStatus(), cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());

        // 已取消不能再取消
        assertServiceException(() -> appointmentService.cancel(cancel), APPOINTMENT_NOT_CANCELLABLE);
    }

    @Test
    public void testMarkArrived_idempotent_andNoShowAfterTerminalRejected() {
        IcbcNaturalPersonDO person = boundPerson();
        Long id = createAppointment(person.getId(), "ST_A", "废钢");

        AppointmentArriveReqVO arrive = new AppointmentArriveReqVO();
        arrive.setId(id);
        arrive.setAcquisitionId(888L);
        appointmentService.markArrived(arrive);
        IcbcAppointmentDO arrived = appointmentMapper.selectById(id);
        assertEquals(AppointmentStatusEnum.ARRIVED.getStatus(), arrived.getStatus());
        assertEquals(888L, arrived.getAcquisitionId());
        LocalDateTime arrivedAt = arrived.getArrivedAt();
        assertNotNull(arrivedAt);

        // 重复标记幂等：不覆盖到场时间
        appointmentService.markArrived(arrive);
        assertEquals(arrivedAt, appointmentMapper.selectById(id).getArrivedAt());

        // 已到场不能再标未到场
        AppointmentNoShowReqVO noShow = new AppointmentNoShowReqVO();
        noShow.setId(id);
        noShow.setReason("等不及走了");
        cn.iocoder.yudao.framework.common.exception.ServiceException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                        () -> appointmentService.markNoShow(noShow));
        assertEquals(APPOINTMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    public void testMarkNoShow() {
        IcbcNaturalPersonDO person = boundPerson();
        Long id = createAppointment(person.getId(), "ST_A", "废钢");
        AppointmentNoShowReqVO noShow = new AppointmentNoShowReqVO();
        noShow.setId(id);
        noShow.setReason("过了时间没来");
        appointmentService.markNoShow(noShow);
        IcbcAppointmentDO saved = appointmentMapper.selectById(id);
        assertEquals(AppointmentStatusEnum.NO_SHOW.getStatus(), saved.getStatus());
        assertEquals("过了时间没来", saved.getNoShowReason());
    }

    @Test
    public void testListPendingForPayee_onlyPending_orderedByArrivalTime() {
        IcbcNaturalPersonDO person = boundPerson();
        insertStation("ST_A", "城东收货点", 1L);
        IcbcGoodsConfigDO goods = insertGoods("废钢", "吨");
        PayeeInfoDO payee = insertPayee(person.getId(), 1L, "PARTNER_A");
        when(payeeInfoService.ensureNaturalPerson(org.mockito.ArgumentMatchers.any()))
                .thenReturn(person);

        // 晚到的先插入，验证按预计到站时间升序
        insertAppointment(person.getId(), payee.getId(), goods.getId(), "晚约",
                LocalDateTime.now().plusHours(5), AppointmentStatusEnum.PENDING);
        insertAppointment(person.getId(), payee.getId(), goods.getId(), "早约",
                LocalDateTime.now().plusHours(1), AppointmentStatusEnum.PENDING);
        insertAppointment(person.getId(), payee.getId(), goods.getId(), "已到场",
                LocalDateTime.now().plusHours(2), AppointmentStatusEnum.ARRIVED);
        insertAppointment(person.getId(), payee.getId(), goods.getId(), "已取消",
                LocalDateTime.now().plusHours(3), AppointmentStatusEnum.CANCELLED);

        List<AppointmentRespVO> pending = appointmentService.listPendingForPayee(payee.getId());
        assertEquals(2, pending.size());
        assertEquals("早约", pending.get(0).getCategoryName());
        assertEquals("晚约", pending.get(1).getCategoryName());
        assertTrue(pending.stream().allMatch(item -> "待到站".equals(item.getStatusName())));
    }

    @Test
    public void testListForSeller_acrossEnterprises() {
        IcbcNaturalPersonDO person = boundPerson();
        insertStation("ST_A", "城东收货点", 1L);
        IcbcGoodsConfigDO goods = insertGoods("废钢", "吨");
        insertAppointment(person.getId(), null, goods.getId(), "甲企业约",
                LocalDateTime.now().plusHours(1), AppointmentStatusEnum.PENDING);
        insertAppointment(person.getId(), null, goods.getId(), "乙企业约",
                LocalDateTime.now().plusHours(2), AppointmentStatusEnum.PENDING);

        when(tenantApi.getTenantName(1L)).thenReturn("甲回收");
        List<AppointmentRespVO> list = appointmentService.getListForSeller(person.getId());
        assertEquals(2, list.size());
        // 跨企业读取只对本人开放；到站登记时带出仍是本企业口径
        assertTrue(list.stream().allMatch(item -> item.getStationName() != null));
    }

    @Test
    public void testGetPage_filterByStatus() {
        IcbcNaturalPersonDO person = boundPerson();
        insertStation("ST_A", "城东收货点", 1L);
        IcbcGoodsConfigDO goods = insertGoods("废钢", "吨");
        insertAppointment(person.getId(), null, goods.getId(), "待到站",
                LocalDateTime.now().plusHours(1), AppointmentStatusEnum.PENDING);
        insertAppointment(person.getId(), null, goods.getId(), "未到场",
                LocalDateTime.now().minusHours(1), AppointmentStatusEnum.NO_SHOW);

        AppointmentPageReqVO req = new AppointmentPageReqVO();
        req.setStatus(AppointmentStatusEnum.NO_SHOW.getStatus());
        PageResult<AppointmentRespVO> page = appointmentService.getPage(req);
        assertEquals(1, page.getTotal());
        assertEquals("未到场", page.getList().get(0).getStatusName());
    }

    // ==================== 辅助方法 ====================

    private IcbcNaturalPersonDO boundPerson() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        naturalPersonService.bindLogin(person.getId(), MEMBER_USER_ID, "REGISTER", null);
        setLoginUser(MEMBER_USER_ID);
        return person;
    }

    private Long createAppointment(Long naturalPersonId, String stationCode, String categoryName) {
        insertStation(stationCode, "城东收货点", 1L);
        IcbcGoodsConfigDO goods = insertGoods(categoryName, "吨");
        return appointmentService.create(createReq(naturalPersonId, stationCode, goods.getId()));
    }

    private AppointmentCreateReqVO createReq(Long naturalPersonId, String stationCode, Long goodsConfigId) {
        AppointmentCreateReqVO req = new AppointmentCreateReqVO();
        req.setNaturalPersonId(naturalPersonId);
        req.setStationCode(stationCode);
        req.setGoodsConfigId(goodsConfigId);
        req.setExpectedArrivalTime(LocalDateTime.now().plusHours(2));
        return req;
    }

    private IcbcNaturalPersonDO register(String idCardNo, String mobile) {
        NaturalPersonRegisterReqVO req = new NaturalPersonRegisterReqVO();
        req.setName("张三");
        req.setIdCardNo(idCardNo);
        req.setMobile(mobile);
        return naturalPersonService.register(req);
    }

    private void setLoginUser(Long memberUserId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(memberUserId);
        loginUser.setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, null));
    }

    private IcbcStationDO insertStation(String code, String name, Long tenantId) {
        if (stationMapper.selectByStationCode(code) != null) {
            return stationMapper.selectByStationCode(code);
        }
        IcbcStationDO station = IcbcStationDO.builder().stationCode(code).name(name).openStatus(1).build();
        station.setTenantId(tenantId);
        stationMapper.insert(station);
        return station;
    }

    private IcbcGoodsConfigDO insertGoods(String name, String unit) {
        IcbcGoodsConfigDO goods = new IcbcGoodsConfigDO();
        goods.setName(name);
        goods.setUnit(unit);
        goods.setStatus(0);
        goods.setTaxRate(new BigDecimal("0.01"));
        goodsConfigMapper.insert(goods);
        return goods;
    }

    private PayeeInfoDO insertPayee(Long naturalPersonId, Long tenantId, String partnerPayeeId) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId(partnerPayeeId)
                .naturalPersonId(naturalPersonId)
                .name("张三")
                .mobile("13800138000")
                .idCardNo("110101199001011234")
                .build();
        payee.setTenantId(tenantId);
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private void insertAppointment(Long naturalPersonId, Long payeeId, Long goodsConfigId,
                                   String categoryName, LocalDateTime arrivalTime,
                                   AppointmentStatusEnum status) {
        IcbcAppointmentDO appointment = IcbcAppointmentDO.builder()
                .appointmentNo("APT_TEST_" + System.nanoTime())
                .naturalPersonId(naturalPersonId)
                .payeeId(payeeId)
                .stationId(1L)
                .stationCode("ST_A")
                .stationName("城东收货点")
                .goodsConfigId(goodsConfigId)
                .categoryName(categoryName)
                .unit("吨")
                .expectedQuantity(new BigDecimal("3"))
                .expectedArrivalTime(arrivalTime)
                .status(status.getStatus())
                .build();
        appointmentMapper.insert(appointment);
    }

}
