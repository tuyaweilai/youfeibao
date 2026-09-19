package cn.iocoder.yudao.module.icbc.service.payee;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeBankCardChangeSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.IcbcPayeeBankCardChangeDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeBankCardChangeStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import cn.iocoder.yudao.module.icbc.service.payee.impl.PayeeBankCardChangeServiceImpl;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PayeeBankCardChangeServiceImpl} 的单元测试（#37）。
 *
 * <p>覆盖三条硬规则：不允许多张卡、审核期间付款挂起、换卡失败原卡仍有效。
 */
@Import({PayeeBankCardChangeServiceImpl.class, PayeeInfoServiceImpl.class, NaturalPersonServiceImpl.class,
        UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class PayeeBankCardChangeServiceTest extends BaseDbUnitTest {

    private static final String OLD_CARD = "6222021234567890";
    private static final String NEW_CARD = "6222029999888877";

    @Resource
    private PayeeBankCardChangeService bankCardChangeService;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    // ==================== 发起变更 ====================

    @Test
    public void testRequestChange_snapshotsOldCardAndLeavesActiveCardUntouched() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC1", "110101199001020001", "13800010001", OLD_CARD);

        IcbcPayeeBankCardChangeDO change = bankCardChangeService.requestChange(req(payee.getId(), NEW_CARD, "中国工商银行"));

        assertEquals(PayeeBankCardChangeStatusEnum.PENDING_REVIEW.getStatus(), change.getStatus());
        assertEquals("7890", change.getOldCardTail());
        assertEquals(NEW_CARD, change.getNewBankCardNo());
        assertNotNull(change.getChangeNo());
        assertNotNull(change.getRequestedAt());
        // 不允许多张卡：生效中的那张卡在审核通过前不动
        assertEquals(OLD_CARD, payeeInfoMapper.selectById(payee.getId()).getBankCardNo());
    }

    @Test
    public void testRequestChange_rejectedWhenNotOnboarded() {
        PayeeInfoDO payee = insertPayee("USER_BC2", "110101199001020002", "13800010002", OLD_CARD,
                IcbcStatusEnum.AuditStatus.PENDING.getStatus(), null);

        assertServiceException(() -> bankCardChangeService.requestChange(req(payee.getId(), NEW_CARD, null)),
                PAYEE_BANK_CARD_CHANGE_NOT_ONBOARDED);
    }

    @Test
    public void testRequestChange_rejectedWhenAnotherChangePending() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC3", "110101199001020003", "13800010003", OLD_CARD);
        bankCardChangeService.requestChange(req(payee.getId(), NEW_CARD, null));

        assertServiceException(() -> bankCardChangeService.requestChange(req(payee.getId(), "6222020000111122", null)),
                PAYEE_BANK_CARD_CHANGE_ALREADY_PENDING);
    }

    @Test
    public void testRequestChange_rejectedWithoutCard() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC4", "110101199001020004", "13800010004", null);

        assertServiceException(() -> bankCardChangeService.requestChange(req(payee.getId(), "  ", null)),
                SELLER_BANK_CARD_REQUIRED);
    }

    // ==================== 结果收敛 ====================

    @Test
    public void testApplyOnboardingResult_passPromotesNewCard() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC5", "110101199001020005", "13800010005", OLD_CARD);
        bankCardChangeService.requestChange(req(payee.getId(), NEW_CARD, "中国工商银行"));

        IcbcPayeeBankCardChangeDO resolved =
                bankCardChangeService.applyOnboardingResult(payee.getId(), "02", "pass", "M-NEW", null);

        assertEquals(PayeeBankCardChangeStatusEnum.EFFECTIVE.getStatus(), resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
        assertEquals("pass", resolved.getAuditResult());
        // 新卡生效：收方档案换成新卡，工行账户事实跟着走
        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals(NEW_CARD, updated.getBankCardNo());
        assertEquals("中国工商银行", updated.getBankName());
        assertEquals("M-NEW", updated.getIcbcMediumId());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
        // 在途已清，付款不再挂起
        assertFalse(bankCardChangeService.hasPending(payee.getId()));
        assertDoesNotThrow(() -> bankCardChangeService.assertPaymentNotSuspended(payee.getId()));
    }

    @Test
    public void testApplyOnboardingResult_rejectKeepsOldCardEffective() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC6", "110101199001020006", "13800010006", OLD_CARD);
        bankCardChangeService.requestChange(req(payee.getId(), NEW_CARD, null));

        IcbcPayeeBankCardChangeDO resolved =
                bankCardChangeService.applyOnboardingResult(payee.getId(), "02", "reject", null, "卡号与姓名不符");

        assertEquals(PayeeBankCardChangeStatusEnum.REJECTED.getStatus(), resolved.getStatus());
        assertEquals("卡号与姓名不符", resolved.getRejectReason());
        // 换卡失败不等于收款能力被打掉：原卡继续有效、建档仍是 READY
        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals(OLD_CARD, updated.getBankCardNo());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
        assertFalse(bankCardChangeService.hasPending(payee.getId()));
    }

    @Test
    public void testApplyOnboardingResult_rejectWithoutOpenacctStatusStillResolves() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC7", "110101199001020007", "13800010007", OLD_CARD);
        bankCardChangeService.requestChange(req(payee.getId(), NEW_CARD, null));

        // 数据接口回调可能只带 result=reject：拒绝是权威结论，照样收敛
        IcbcPayeeBankCardChangeDO resolved =
                bankCardChangeService.applyOnboardingResult(payee.getId(), null, "reject", null, null);

        assertEquals(PayeeBankCardChangeStatusEnum.REJECTED.getStatus(), resolved.getStatus());
    }

    @Test
    public void testApplyOnboardingResult_noPendingChangeIsNoop() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC8", "110101199001020008", "13800010008", OLD_CARD);

        // 没有在途变更时属于首次建档的结果，本服务不该碰收方档案
        assertNull(bankCardChangeService.applyOnboardingResult(payee.getId(), "02", "pass", "M1", null));
        assertEquals(OLD_CARD, payeeInfoMapper.selectById(payee.getId()).getBankCardNo());
    }

    // ==================== 付款门禁与取消 ====================

    @Test
    public void testAssertPaymentNotSuspended_blockedWhilePending_thenRecoversAfterCancel() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC9", "110101199001020009", "13800010009", OLD_CARD);
        IcbcPayeeBankCardChangeDO change = bankCardChangeService.requestChange(req(payee.getId(), NEW_CARD, null));

        assertServiceException(() -> bankCardChangeService.assertPaymentNotSuspended(payee.getId()),
                PAYEE_BANK_CARD_CHANGE_IN_PROGRESS);

        bankCardChangeService.cancelChange(change.getId(), "出售者不再办理");

        assertDoesNotThrow(() -> bankCardChangeService.assertPaymentNotSuspended(payee.getId()));
        assertEquals(OLD_CARD, payeeInfoMapper.selectById(payee.getId()).getBankCardNo());
    }

    @Test
    public void testCancelChange_onlyPendingCancellable() {
        PayeeInfoDO payee = insertReadyPayee("USER_BC10", "110101199001020010", "13800010010", OLD_CARD);
        IcbcPayeeBankCardChangeDO change = bankCardChangeService.requestChange(req(payee.getId(), NEW_CARD, null));
        bankCardChangeService.cancelChange(change.getId(), null);

        assertServiceException(() -> bankCardChangeService.cancelChange(change.getId(), null),
                PAYEE_BANK_CARD_CHANGE_NOT_CANCELLABLE, "变更已取消");
        assertServiceException(() -> bankCardChangeService.cancelChange(999999L, null),
                PAYEE_BANK_CARD_CHANGE_NOT_EXISTS);
    }

    // ==================== 批量与历史 ====================

    @Test
    public void testPendingMapAndHistory() {
        PayeeInfoDO payeeA = insertReadyPayee("USER_BC11", "110101199001020011", "13800010011", OLD_CARD);
        PayeeInfoDO payeeB = insertReadyPayee("USER_BC12", "110101199001020012", "13800010012", OLD_CARD);
        IcbcPayeeBankCardChangeDO first = bankCardChangeService.requestChange(req(payeeA.getId(), NEW_CARD, null));
        bankCardChangeService.requestChange(req(payeeB.getId(), NEW_CARD, null));
        // A 的第一笔被拒后再发起第二笔，历史里有两条
        bankCardChangeService.applyOnboardingResult(payeeA.getId(), "02", "reject", null, "重来");
        bankCardChangeService.requestChange(req(payeeA.getId(), "6222020000333344", null));

        Map<Long, IcbcPayeeBankCardChangeDO> pending =
                bankCardChangeService.pendingMap(List.of(payeeA.getId(), payeeB.getId()));
        assertEquals(2, pending.size());
        assertEquals("6222020000333344", pending.get(payeeA.getId()).getNewBankCardNo());

        List<IcbcPayeeBankCardChangeDO> history = bankCardChangeService.listByPayeeId(payeeA.getId());
        assertEquals(2, history.size());
        assertEquals(first.getId(), history.get(1).getId());
    }

    // ==================== 助手 ====================

    private PayeeBankCardChangeSaveReqVO req(Long payeeId, String cardNo, String bankName) {
        PayeeBankCardChangeSaveReqVO reqVO = new PayeeBankCardChangeSaveReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setNewBankCardNo(cardNo);
        reqVO.setNewBankName(bankName);
        reqVO.setRequestSource("SELLER_PORTAL");
        return reqVO;
    }

    private PayeeInfoDO insertReadyPayee(String partnerPayeeId, String idCardNo, String mobile, String bankCardNo) {
        return insertPayee(partnerPayeeId, idCardNo, mobile, bankCardNo,
                IcbcStatusEnum.AuditStatus.APPROVED.getStatus(), PayeeOnboardingOutcomeEnum.READY.getCode());
    }

    private PayeeInfoDO insertPayee(String partnerPayeeId, String idCardNo, String mobile, String bankCardNo,
                                    Integer status, String onboardingState) {
        IcbcNaturalPersonDO person = registerPerson(idCardNo, mobile);
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId(partnerPayeeId)
                .naturalPersonId(person.getId())
                .name(person.getName())
                .idCardNo(idCardNo)
                .mobile(mobile)
                .bankCardNo(bankCardNo)
                .bankName("中国建设银行")
                .idSignDate("2020-01-01")
                .idValidityPeriod("2030-01-01")
                .businessType("RECYCLE")
                .status(status)
                .onboardingState(onboardingState)
                .icbcReceiverStatus(PayeeOnboardingOutcomeEnum.READY.getCode().equals(onboardingState) ? "1" : "0")
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private IcbcNaturalPersonDO registerPerson(String idCardNo, String mobile) {
        NaturalPersonRegisterReqVO reqVO = new NaturalPersonRegisterReqVO();
        reqVO.setName("张三");
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        return naturalPersonService.register(reqVO);
    }

}
