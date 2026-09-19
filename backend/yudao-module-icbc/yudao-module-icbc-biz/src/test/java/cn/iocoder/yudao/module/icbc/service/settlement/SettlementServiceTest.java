package cn.iocoder.yudao.module.icbc.service.settlement;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo.*;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementConfirmReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementDisputeReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementVersionMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import cn.iocoder.yudao.module.icbc.service.settlement.impl.SettlementServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
 * {@link SettlementServiceImpl} 的单元测试。
 *
 * <p>覆盖 #33 的验收：按到场批次聚合、生成后不可加单、确认门禁、异议→改版→重新确认、
 * 超时不自动确认、线下签字等价确认、作废对自然人可见。
 */
@Import({SettlementServiceImpl.class, NaturalPersonServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class SettlementServiceTest extends BaseDbUnitTest {

    private static final Long MEMBER_USER_ID = 9001L;

    @Resource
    private SettlementService settlementService;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private IcbcSettlementVersionMapper versionMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @MockBean
    private cn.iocoder.yudao.module.icbc.service.station.StationService stationService;

    @AfterEach
    public void tearDown() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // ==================== 聚合与生成 ====================

    @Test
    public void testGenerate_aggregatesUngroupedAcquisitionsAndSnapshots() {
        PayeeInfoDO payee = insertPayee();
        insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        insertAcquisition(payee.getId(), "ACQ_B", new BigDecimal("2000.00"));

        Long settlementId = settlementService.generate(generateReq(payee.getId()));

        SettlementRespVO detail = settlementService.getDetail(settlementId);
        assertEquals(1, detail.getCurrentVersionNo());
        assertEquals(SettlementConfirmStatusEnum.PENDING.getStatus(), detail.getConfirmStatus());
        assertEquals(2, detail.getAcquisitionCount());
        assertEquals(0, new BigDecimal("3000.00").compareTo(detail.getTotalAmount()));
        assertEquals(2, detail.getLines().size());
        assertFalse(detail.getSettled());
        // 版本快照落哈希，确认时引用它
        assertEquals(1, detail.getVersions().size());
        assertNotNull(detail.getVersions().get(0).getSnapshotHash());
        // 收购单挂上了结算单
        assertEquals(settlementId, acquisitionMapper.selectByAcquisitionNo("ACQ_A").getSettlementId());
    }

    @Test
    public void testGenerate_noAcquisitionRejected() {
        PayeeInfoDO payee = insertPayee();
        assertServiceException(() -> settlementService.generate(generateReq(payee.getId())),
                SETTLEMENT_NO_ACQUISITION);
    }

    @Test
    public void testGenerate_afterGenerateCannotAddToSameSettlement() {
        PayeeInfoDO payee = insertPayee();
        insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        Long first = settlementService.generate(generateReq(payee.getId()));

        // 生成后再进来一张，不会进入老结算单，只能新建
        insertAcquisition(payee.getId(), "ACQ_B", new BigDecimal("2000.00"));
        Long second = settlementService.generate(generateReq(payee.getId()));

        assertNotEquals(first, second);
        assertEquals(1, settlementService.getDetail(first).getAcquisitionCount());
        assertEquals(1, settlementService.getDetail(second).getAcquisitionCount());
        assertNotEquals(first, acquisitionMapper.selectByAcquisitionNo("ACQ_B").getSettlementId());
    }

    @Test
    public void testGenerate_aggregatesOnlySameStation() {
        PayeeInfoDO payee = insertPayee();
        when(stationService.getStation(101L)).thenReturn(station(101L, "城东"));
        when(stationService.getStation(102L)).thenReturn(station(102L, "城西"));
        insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"), 101L);
        insertAcquisition(payee.getId(), "ACQ_B", new BigDecimal("2000.00"), 102L);

        SettlementGenerateReqVO reqVO = generateReq(payee.getId());
        reqVO.setStationId(101L);
        Long settlementId = settlementService.generate(reqVO);

        // 一次到场批次 = 同出售者 + 同场站：城东那张归组，城西那张不动
        SettlementRespVO detail = settlementService.getDetail(settlementId);
        assertEquals(1, detail.getAcquisitionCount());
        assertEquals(101L, detail.getStationId());
        assertEquals("城东", detail.getStationName());
        assertEquals(settlementId, acquisitionMapper.selectByAcquisitionNo("ACQ_A").getSettlementId());
        assertNull(acquisitionMapper.selectByAcquisitionNo("ACQ_B").getSettlementId());
    }

    @Test
    public void testBatchSuggestion_onlySameStationWithinShiftWindow() {
        PayeeInfoDO payee = insertPayee();
        when(stationService.getStation(101L)).thenReturn(station(101L, "城东"));
        insertAcquisitionAt(payee.getId(), "ACQ_NEW", new BigDecimal("1000.00"), 101L,
                LocalDateTime.now().minusHours(1));
        insertAcquisitionAt(payee.getId(), "ACQ_OLD", new BigDecimal("2000.00"), 101L,
                LocalDateTime.now().minusHours(5));
        insertAcquisition(payee.getId(), "ACQ_OTHER_STATION", new BigDecimal("3000.00"), 102L);

        // 默认 4 小时窗口、同场站；且只建议不自动合并（这里不落任何结算单）
        SettlementBatchSuggestionVO suggestion = settlementService.getBatchSuggestion(payee.getId(), 101L);
        assertEquals(4, suggestion.getShiftHours());
        assertEquals(1, suggestion.getCount());
        assertEquals("ACQ_NEW", suggestion.getAcquisitions().get(0).getAcquisitionNo());
        assertNotNull(suggestion.getSuggestionNote());
        assertNull(acquisitionMapper.selectByAcquisitionNo("ACQ_NEW").getSettlementId());
    }

    // ==================== 确认门禁 ====================

    @Test
    public void testGate_blocksUntilConfirmed() {
        PayeeInfoDO payee = insertPayee();
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        Long settlementId = settlementService.generate(generateReq(payee.getId()));

        // 未确认：门禁拦住
        assertFalse(settlementService.isSettlementConfirmed(acquisition.getId()));
        assertServiceException(() -> settlementService.assertSettlementConfirmed(acquisition.getId()),
                SETTLEMENT_NOT_CONFIRMED);

        bindLogin(payee);
        settlementService.confirm(confirmReq(settlementId), "10.0.0.1", "UA-1");

        assertTrue(settlementService.isSettlementConfirmed(acquisition.getId()));
        SettlementRespVO detail = settlementService.getDetail(settlementId);
        assertEquals(SettlementConfirmStatusEnum.CONFIRMED.getStatus(), detail.getConfirmStatus());
        assertNotNull(detail.getConfirmTime());
        assertNotNull(detail.getConfirmHash());
    }

    @Test
    public void testGate_noSettlementBlocks() {
        PayeeInfoDO payee = insertPayee();
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        assertServiceException(() -> settlementService.assertSettlementConfirmed(acquisition.getId()),
                SETTLEMENT_NOT_CONFIRMED);
    }

    // ==================== 异议与版本 ====================

    @Test
    public void testDisputeThenEnterpriseChangeCreatesNewVersionAndNeedsReconfirm() {
        PayeeInfoDO payee = insertPayee();
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        Long settlementId = settlementService.generate(generateReq(payee.getId()));
        bindLogin(payee);

        SellerSettlementDisputeReqVO dispute = new SellerSettlementDisputeReqVO();
        dispute.setNaturalPersonId(payee.getNaturalPersonId());
        dispute.setSettlementId(settlementId);
        dispute.setReason("02");
        dispute.setNote("扣杂按 3% 收，现场说好是 1%");
        settlementService.raiseDispute(dispute);

        SettlementRespVO disputed = settlementService.getDetail(settlementId);
        assertEquals(SettlementConfirmStatusEnum.DISPUTED.getStatus(), disputed.getConfirmStatus());
        assertEquals(1, disputed.getDisputeCount());
        assertEquals("扣杂不符", disputed.getDisputeReasonName());

        // 企业改：产生新版本，回到待确认
        SettlementChangeReqVO change = new SettlementChangeReqVO();
        change.setSettlementId(settlementId);
        change.setChangeReason("扣杂按 1% 更正");
        SettlementChangeReqVO.Line line = new SettlementChangeReqVO.Line();
        line.setAcquisitionId(acquisition.getId());
        line.setDeduction(new BigDecimal("1"));
        line.setDeductionMethod("RATIO");
        line.setUnitPrice(new BigDecimal("100"));
        change.setLines(List.of(line));
        settlementService.changeByEnterprise(change);

        SettlementRespVO changed = settlementService.getDetail(settlementId);
        assertEquals(SettlementConfirmStatusEnum.PENDING.getStatus(), changed.getConfirmStatus());
        assertEquals(2, changed.getCurrentVersionNo());
        assertNull(changed.getConfirmHash());
        assertEquals(2, changed.getVersions().size());
        // 议异后企业改过，未重新确认前门禁仍拦住
        assertFalse(settlementService.isSettlementConfirmed(acquisition.getId()));

        // 自然人对新版再次确认
        settlementService.confirm(confirmReq(settlementId), "10.0.0.2", "UA-2");
        assertTrue(settlementService.isSettlementConfirmed(acquisition.getId()));
    }

    @Test
    public void testDispute_otherReasonRequiresNote() {
        PayeeInfoDO payee = insertPayee();
        insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        Long settlementId = settlementService.generate(generateReq(payee.getId()));
        bindLogin(payee);

        SellerSettlementDisputeReqVO dispute = new SellerSettlementDisputeReqVO();
        dispute.setNaturalPersonId(payee.getNaturalPersonId());
        dispute.setSettlementId(settlementId);
        dispute.setReason("99");
        assertServiceException(() -> settlementService.raiseDispute(dispute), SETTLEMENT_DISPUTE_NOTE_REQUIRED);
    }

    // ==================== 超时 ====================

    @Test
    public void testTimeout_doesNotAutoConfirm() {
        PayeeInfoDO payee = insertPayee();
        insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        Long settlementId = settlementService.generate(generateReq(payee.getId()));
        // 人为把截止时间拨到过去
        IcbcSettlementDO update = new IcbcSettlementDO();
        update.setId(settlementId);
        update.setDeadlineTime(LocalDateTime.now().minusDays(1));
        settlementMapper.updateById(update);

        int escalated = settlementService.handleTimeout();

        assertEquals(1, escalated);
        SettlementRespVO detail = settlementService.getDetail(settlementId);
        // 到期不自动确认，而是升级为需线下签字确认
        assertEquals(SettlementConfirmStatusEnum.OFFLINE_REQUIRED.getStatus(), detail.getConfirmStatus());
        assertNotEquals(SettlementConfirmStatusEnum.CONFIRMED.getStatus(), detail.getConfirmStatus());
    }

    // ==================== 线下签字与作废 ====================

    @Test
    public void testOfflineSign_equalsConfirmation() {
        PayeeInfoDO payee = insertPayee();
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        Long settlementId = settlementService.generate(generateReq(payee.getId()));

        SettlementOfflineSignReqVO sign = new SettlementOfflineSignReqVO();
        sign.setSettlementId(settlementId);
        sign.setFileUrl("https://cdn/sign.jpg");
        sign.setHandler("李四");
        settlementService.offlineSign(sign);

        SettlementRespVO detail = settlementService.getDetail(settlementId);
        assertEquals(SettlementConfirmStatusEnum.OFFLINE_CONFIRMED.getStatus(), detail.getConfirmStatus());
        assertTrue(settlementService.isSettlementConfirmed(acquisition.getId()));
    }

    @Test
    public void testCancelAcquisition_leavesReasonVisible() {
        PayeeInfoDO payee = insertPayee();
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));
        Long settlementId = settlementService.generate(generateReq(payee.getId()));

        SettlementAcquisitionCancelReqVO cancel = new SettlementAcquisitionCancelReqVO();
        cancel.setAcquisitionId(acquisition.getId());
        cancel.setReason("磅单重复录入，已合并到另一笔");
        settlementService.cancelAcquisition(cancel);

        IcbcAcquisitionDO cancelled = acquisitionMapper.selectById(acquisition.getId());
        assertEquals(AcquisitionStatusEnum.CANCELLED.getStatus(), cancelled.getStatus());
        assertEquals("磅单重复录入，已合并到另一笔", cancelled.getCancelReason());
        // 作废对自然人可见
        SettlementRespVO detail = settlementService.getDetail(settlementId);
        assertTrue(detail.getLines().stream()
                .anyMatch(line -> "磅单重复录入，已合并到另一笔".equals(line.getCancelReason())));
    }

    @Test
    public void testChangeByEnterprise_rejectsAcquisitionOfAnotherSettlement() {
        PayeeInfoDO payeeA = insertPayee("110101199001011234", "13800138000");
        PayeeInfoDO payeeB = insertPayee("110101199001011235", "13800138001");
        IcbcAcquisitionDO acqA = insertAcquisition(payeeA.getId(), "ACQ_A", new BigDecimal("1000.00"));
        IcbcAcquisitionDO acqB = insertAcquisition(payeeB.getId(), "ACQ_B", new BigDecimal("2000.00"));
        Long settlementA = settlementService.generate(generateReq(payeeA.getId()));
        settlementService.generate(generateReq(payeeB.getId()));

        // 改 A 的结算单，却传了 B 的收购单：必须拦住，否则会改到账外而版本快照无法解释
        SettlementChangeReqVO change = new SettlementChangeReqVO();
        change.setSettlementId(settlementA);
        change.setChangeReason("试图改别的结算单的收购单");
        SettlementChangeReqVO.Line line = new SettlementChangeReqVO.Line();
        line.setAcquisitionId(acqB.getId());
        line.setUnitPrice(new BigDecimal("999"));
        change.setLines(List.of(line));

        assertServiceException(() -> settlementService.changeByEnterprise(change),
                SETTLEMENT_ACQUISITION_NOT_IN_SETTLEMENT, acqB.getId());
        // B 的收购单没被改
        assertEquals(0, new BigDecimal("2000.00").compareTo(
                acquisitionMapper.selectById(acqB.getId()).getAmount()));
        // A 的收购单也没被误改
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                acquisitionMapper.selectById(acqA.getId()).getAmount()));
    }

    @Test
    public void testCancelAcquisition_unGroupedRejected() {
        PayeeInfoDO payee = insertPayee();
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_A", new BigDecimal("1000.00"));

        SettlementAcquisitionCancelReqVO cancel = new SettlementAcquisitionCancelReqVO();
        cancel.setAcquisitionId(acquisition.getId());
        cancel.setReason("尚未归入结算单");
        assertServiceException(() -> settlementService.cancelAcquisition(cancel),
                SETTLEMENT_ACQUISITION_NOT_GROUPED);
        // 未被作废
        assertEquals(AcquisitionStatusEnum.REGISTERED.getStatus(),
                acquisitionMapper.selectById(acquisition.getId()).getStatus());
    }

    // ==================== 造数 ====================

    private SettlementGenerateReqVO generateReq(Long payeeId) {
        SettlementGenerateReqVO reqVO = new SettlementGenerateReqVO();
        reqVO.setPayeeId(payeeId);
        return reqVO;
    }

    private SellerSettlementConfirmReqVO confirmReq(Long settlementId) {
        IcbcSettlementDO settlement = settlementMapper.selectById(settlementId);
        SellerSettlementConfirmReqVO reqVO = new SellerSettlementConfirmReqVO();
        reqVO.setNaturalPersonId(settlement.getNaturalPersonId());
        reqVO.setSettlementId(settlementId);
        reqVO.setVersionId(settlement.getCurrentVersionId());
        return reqVO;
    }

    private void bindLogin(PayeeInfoDO payee) {
        naturalPersonService.bindLogin(payee.getNaturalPersonId(), MEMBER_USER_ID, "REGISTER", null);
        setLoginUser(MEMBER_USER_ID);
    }

    private void setLoginUser(Long memberUserId) {
        cn.iocoder.yudao.framework.security.core.LoginUser loginUser =
                new cn.iocoder.yudao.framework.security.core.LoginUser();
        loginUser.setId(memberUserId);
        loginUser.setUserType(cn.iocoder.yudao.framework.common.enums.UserTypeEnum.MEMBER.getValue());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        loginUser, null, null));
    }

    private PayeeInfoDO insertPayee() {
        return insertPayee("110101199001011234", "13800138000");
    }

    private PayeeInfoDO insertPayee(String idCardNo, String mobile) {
        IcbcNaturalPersonDO person = naturalPersonService.register(
                registerReq("张三", idCardNo, mobile));
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId("PARTNER_" + mobile)
                .naturalPersonId(person.getId())
                .name("张三")
                .mobile(mobile)
                .idCardNo(idCardNo)
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO registerReq(
            String name, String idCardNo, String mobile) {
        cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO reqVO =
                new cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO();
        reqVO.setName(name);
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        return reqVO;
    }

    private IcbcAcquisitionDO insertAcquisition(Long payeeId, String no, BigDecimal amount) {
        return insertAcquisition(payeeId, no, amount, null);
    }

    private IcbcAcquisitionDO insertAcquisition(Long payeeId, String no, BigDecimal amount, Long stationId) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(no)
                .payeeId(payeeId)
                .partnerPayeeId("PARTNER_1")
                .sellerName("张三")
                .sellerMobile("13800138000")
                .categoryName("废钢")
                .unit("吨")
                .taxRate(new BigDecimal("0.01"))
                .taxMethod("GENERAL")
                .mergedCode("1090101010000000000")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("100"))
                .settlementWeight(new BigDecimal("10"))
                .amount(amount)
                .grossWeight(new BigDecimal("12"))
                .tareWeight(new BigDecimal("2"))
                .deduction(BigDecimal.ZERO)
                .weightTicketNo("WT_" + no)
                .stationId(stationId)
                .status(AcquisitionStatusEnum.REGISTERED.getStatus())
                .build();
        acquisitionMapper.insert(acquisition);
        return acquisition;
    }

    private IcbcAcquisitionDO insertAcquisitionAt(Long payeeId, String no, BigDecimal amount, Long stationId,
                                                 LocalDateTime createTime) {
        IcbcAcquisitionDO acquisition = insertAcquisition(payeeId, no, amount, stationId);
        acquisition.setCreateTime(createTime);
        acquisitionMapper.updateById(acquisition);
        return acquisition;
    }

    private IcbcStationDO station(Long id, String name) {
        return IcbcStationDO.builder()
                .id(id)
                .stationCode("STATION_" + id)
                .name(name)
                .openStatus(1)
                .build();
    }

}
