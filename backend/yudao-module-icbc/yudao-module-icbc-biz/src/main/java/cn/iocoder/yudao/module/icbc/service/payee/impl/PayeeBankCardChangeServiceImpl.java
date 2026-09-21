package cn.iocoder.yudao.module.icbc.service.payee.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeBankCardChangeSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.IcbcPayeeBankCardChangeDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeBankCardChangeMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeBankCardChangeStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeBankCardChangeService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 出售者换银行卡 Service 实现（#37）。
 *
 * <p>归属刻意拆成两处（与 ADR 0017 同源）：<b>生效中的卡</b>挂在收方档案上，<b>在途变更</b>挂在本表上。
 * 这样「不允许多张卡」无需靠约定，而是结构上成立——收方档案永远只有一张卡。
 */
@Slf4j
@Service
@Validated
public class PayeeBankCardChangeServiceImpl implements PayeeBankCardChangeService {

    @Resource
    private PayeeBankCardChangeMapper bankCardChangeMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayeeInfoService payeeInfoService;

    // ==================== 发起变更 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IcbcPayeeBankCardChangeDO requestChange(PayeeBankCardChangeSaveReqVO reqVO) {
        PayeeInfoDO payee = validatePayeeExists(reqVO.getPayeeId());
        if (StrUtil.isBlank(reqVO.getNewBankCardNo())) {
            throw exception(SELLER_BANK_CARD_REQUIRED);
        }
        // 换卡是「已入驻之后」的事：没完成首次收方入驻的，走建档而不是换卡
        if (PayeeOnboardingOutcomeEnum.READY != PayeeOnboardingOutcomeEnum.ofCode(payee.getOnboardingState())) {
            throw exception(PAYEE_BANK_CARD_CHANGE_NOT_ONBOARDED);
        }
        // 同一时刻只允许一笔在途变更，否则工行的「修改审核」会互相覆盖
        if (bankCardChangeMapper.selectPendingByPayeeId(payee.getId()) != null) {
            throw exception(PAYEE_BANK_CARD_CHANGE_ALREADY_PENDING);
        }
        IcbcNaturalPersonDO person = payeeInfoService.ensureNaturalPerson(payee);
        LocalDateTime now = LocalDateTime.now();
        IcbcPayeeBankCardChangeDO change = IcbcPayeeBankCardChangeDO.builder()
                .changeNo(generateChangeNo())
                .payeeId(payee.getId())
                .naturalPersonId(person.getId())
                .status(PayeeBankCardChangeStatusEnum.PENDING_REVIEW.getStatus())
                .oldCardTail(MaskUtils.cardTail(payee.getBankCardNo()))
                .newBankCardNo(reqVO.getNewBankCardNo())
                .newBankName(reqVO.getNewBankName())
                .newBankBranch(reqVO.getNewBankBranch())
                // 「是否我行卡」由换卡发起侧带上（#86）；为空时由提交时兜底为 1-我行用户
                .accountCode(reqVO.getAccountCode())
                // 证件有效期沿用档案，除非这次显式提供
                .idSignDate(StrUtil.blankToDefault(reqVO.getIdSignDate(), payee.getIdSignDate()))
                .idValidityPeriod(StrUtil.blankToDefault(reqVO.getIdValidityPeriod(), payee.getIdValidityPeriod()))
                .requestSource(StrUtil.blankToDefault(reqVO.getRequestSource(), "ADMIN"))
                .requestIp(reqVO.getRequestIp())
                .requestedAt(now)
                .remark(reqVO.getRemark())
                .build();
        bankCardChangeMapper.insert(change);
        log.info("[requestChange][收方 {} 发起换卡 {}：旧卡尾号 {} → 新卡尾号 {}]",
                payee.getId(), change.getChangeNo(), change.getOldCardTail(), MaskUtils.cardTail(change.getNewBankCardNo()));
        return change;
    }

    // ==================== 在途与门禁 ====================

    @Override
    public IcbcPayeeBankCardChangeDO getPending(Long payeeId) {
        return bankCardChangeMapper.selectPendingByPayeeId(payeeId);
    }

    @Override
    public boolean hasPending(Long payeeId) {
        return getPending(payeeId) != null;
    }

    @Override
    public Map<Long, IcbcPayeeBankCardChangeDO> pendingMap(Collection<Long> payeeIds) {
        Map<Long, IcbcPayeeBankCardChangeDO> result = new HashMap<>();
        for (IcbcPayeeBankCardChangeDO change : bankCardChangeMapper.selectPendingListByPayeeIds(payeeIds)) {
            result.putIfAbsent(change.getPayeeId(), change);
        }
        return result;
    }

    @Override
    public void assertPaymentNotSuspended(Long payeeId) {
        // 钱要打到的那张卡正在银行审核里：打旧卡是废卡、打新卡还没批，只能挂起新交易的付款
        if (hasPending(payeeId)) {
            throw exception(PAYEE_BANK_CARD_CHANGE_IN_PROGRESS);
        }
    }

    // ==================== 结果收敛 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IcbcPayeeBankCardChangeDO applyOnboardingResult(Long payeeId, String result, String rejectReason) {
        IcbcPayeeBankCardChangeDO change = bankCardChangeMapper.selectPendingByPayeeId(payeeId);
        if (change == null) {
            return null; // 没有在途变更：本次入驻结果属于首次建档，由建档流程自己收敛
        }
        PayeeOnboardingOutcomeEnum outcome = PayeeOnboardingOutcomeEnum.of(result);
        IcbcPayeeBankCardChangeDO update = new IcbcPayeeBankCardChangeDO();
        update.setId(change.getId());
        update.setAuditResult(StrUtil.blankToDefault(result, null));
        if (outcome != null && !outcome.isInvoiceEligible()) {
            update.setRejectReason(StrUtil.blankToDefault(rejectReason, outcome.getName()));
        }
        if (outcome != null) {
            update.setResolvedAt(LocalDateTime.now());
            if (outcome.isInvoiceEligible()) {
                promoteCard(payeeId, change, result);
                update.setStatus(PayeeBankCardChangeStatusEnum.EFFECTIVE.getStatus());
            } else {
                update.setStatus(PayeeBankCardChangeStatusEnum.REJECTED.getStatus());
            }
        }
        bankCardChangeMapper.updateById(update);
        log.info("[applyOnboardingResult][换卡 {} 收敛：result={} → {}]",
                change.getChangeNo(), result, PayeeBankCardChangeStatusEnum.nameOf(update.getStatus()));
        return bankCardChangeMapper.selectById(change.getId());
    }

    /**
     * 审核通过：把新卡搬到收方档案上。收方档案里的卡**只有这一处会变**，且只在这里变。
     *
     * <p>新卡的开户行 / 支行由自然人自己填，可能为空；工行回执不带这两个字段，所以空就空着——
     * 留着旧卡的开户行会变成一句我们无法核验的话（ADR 0021）。
     */
    private void promoteCard(Long payeeId, IcbcPayeeBankCardChangeDO change, String result) {
        PayeeInfoDO update = new PayeeInfoDO();
        update.setId(payeeId);
        update.setBankCardNo(change.getNewBankCardNo());
        update.setBankName(StrUtil.blankToDefault(change.getNewBankName(), null));
        update.setBankBranch(StrUtil.blankToDefault(change.getNewBankBranch(), null));
        update.setIdSignDate(StrUtil.blankToDefault(change.getIdSignDate(), null));
        update.setIdValidityPeriod(StrUtil.blankToDefault(change.getIdValidityPeriod(), null));
        update.setAuditResult(StrUtil.blankToDefault(result, null));
        update.setOnboardingState(PayeeOnboardingOutcomeEnum.READY.getCode());
        update.setIcbcReceiverStatus("1");
        update.setStatus(IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
        payeeInfoMapper.updateById(update);
    }

    // ==================== 取消与历史 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IcbcPayeeBankCardChangeDO cancelChange(Long changeId, String reason) {
        IcbcPayeeBankCardChangeDO change = changeId == null ? null : bankCardChangeMapper.selectById(changeId);
        if (change == null) {
            throw exception(PAYEE_BANK_CARD_CHANGE_NOT_EXISTS);
        }
        if (!PayeeBankCardChangeStatusEnum.isPending(change.getStatus())) {
            throw exception(PAYEE_BANK_CARD_CHANGE_NOT_CANCELLABLE,
                    PayeeBankCardChangeStatusEnum.nameOf(change.getStatus()));
        }
        IcbcPayeeBankCardChangeDO update = new IcbcPayeeBankCardChangeDO();
        update.setId(change.getId());
        update.setStatus(PayeeBankCardChangeStatusEnum.CANCELLED.getStatus());
        update.setResolvedAt(LocalDateTime.now());
        update.setRejectReason(StrUtil.blankToDefault(reason, "企业侧人工取消"));
        bankCardChangeMapper.updateById(update);
        return bankCardChangeMapper.selectById(change.getId());
    }

    @Override
    public List<IcbcPayeeBankCardChangeDO> listByPayeeId(Long payeeId) {
        return bankCardChangeMapper.selectListByPayeeId(payeeId);
    }

    // ==================== 内部方法 ====================

    private PayeeInfoDO validatePayeeExists(Long payeeId) {
        PayeeInfoDO payee = payeeId == null ? null : payeeInfoMapper.selectById(payeeId);
        if (payee == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        return payee;
    }

    private String generateChangeNo() {
        return "BC" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

}
