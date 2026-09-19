package cn.iocoder.yudao.module.icbc.service.tax.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPayReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementSummaryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxSupplementDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.tax.TaxSupplementMapper;
import cn.iocoder.yudao.module.icbc.enums.TaxSupplementStatusEnum;
import cn.iocoder.yudao.module.icbc.service.tax.TaxSupplementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.TAX_SUPPLEMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.TAX_SUPPLEMENT_STATUS_INVALID;

/**
 * 需补缴税费 Service 实现。
 *
 * <p>补缴金额按征收率分列（1% / 3%），因为申报表的「3% 征收率减按 1%」与「放弃减按」
 * 是两栏，补缴也得对得上。待补缴累计是这两个分列的合计，不是一个笼统的数字。
 */
@Service
@Validated
public class TaxSupplementServiceImpl implements TaxSupplementService {

    @Resource
    private TaxSupplementMapper supplementMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxSupplementRespVO create(TaxSupplementCreateReqVO reqVO) {
        BigDecimal one = nullToZero(reqVO.getAmountAtOnePercent());
        BigDecimal three = nullToZero(reqVO.getAmountAtThreePercent());
        BigDecimal amount = one.add(three).max(BigDecimal.ZERO);

        PayeeInfoDO payee = reqVO.getPayeeId() != null ? payeeInfoMapper.selectById(reqVO.getPayeeId()) : null;
        TaxSupplementDO supplement = TaxSupplementDO.builder()
                .supplementNo(buildSupplementNo(reqVO.getPeriodMonth()))
                .declarationId(reqVO.getDeclarationId())
                .periodMonth(reqVO.getPeriodMonth())
                .payeeId(reqVO.getPayeeId())
                .sellerName(payee == null ? null : payee.getName())
                .reason(reqVO.getReason())
                .amountAtOnePercent(one)
                .amountAtThreePercent(three)
                .amount(amount)
                .status(TaxSupplementStatusEnum.PENDING.getStatus())
                .remark(reqVO.getRemark())
                .build();
        supplementMapper.insert(supplement);
        return toResp(supplement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAuto(Long declarationId, String periodMonth, BigDecimal amountAtOnePercent,
                           BigDecimal amountAtThreePercent, String reason) {
        BigDecimal one = nullToZero(amountAtOnePercent);
        BigDecimal three = nullToZero(amountAtThreePercent);
        if (one.signum() == 0 && three.signum() == 0) {
            return;
        }
        // 同一申报单同时只保留一条未缴清的补缴；job 每日重新计算，所以用「覆盖」而非「累加」，保证幂等
        List<TaxSupplementDO> pending = supplementMapper.selectPendingByDeclarationId(declarationId);
        if (!pending.isEmpty()) {
            TaxSupplementDO existing = pending.get(0);
            TaxSupplementDO update = new TaxSupplementDO();
            update.setId(existing.getId());
            update.setAmountAtOnePercent(one);
            update.setAmountAtThreePercent(three);
            update.setAmount(one.add(three));
            update.setReason(reason);
            supplementMapper.updateById(update);
            return;
        }
        supplementMapper.insert(TaxSupplementDO.builder()
                .supplementNo(buildSupplementNo(periodMonth))
                .declarationId(declarationId)
                .periodMonth(periodMonth)
                .reason(reason)
                .amountAtOnePercent(one)
                .amountAtThreePercent(three)
                .amount(one.add(three))
                .status(TaxSupplementStatusEnum.PENDING.getStatus())
                .build());
    }

    @Override
    public PageResult<TaxSupplementRespVO> getPage(TaxSupplementPageReqVO reqVO) {
        PageResult<TaxSupplementDO> page = supplementMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public TaxSupplementSummaryRespVO getSummary() {
        BigDecimal one = BigDecimal.ZERO;
        BigDecimal three = BigDecimal.ZERO;
        int count = 0;
        for (TaxSupplementDO supplement : supplementMapper.selectPending()) {
            one = one.add(nullToZero(supplement.getAmountAtOnePercent()));
            three = three.add(nullToZero(supplement.getAmountAtThreePercent()));
            count++;
        }
        TaxSupplementSummaryRespVO resp = new TaxSupplementSummaryRespVO();
        resp.setPendingCount(count);
        resp.setPendingAmountAtOnePercent(one);
        resp.setPendingAmountAtThreePercent(three);
        resp.setPendingAmount(one.add(three));
        resp.setMessage(count == 0
                ? "当前没有待补缴税费"
                : StrUtil.format("待补缴 {} 笔，合计 {} 元（其中 1% 部分 {} 元、3% 部分 {} 元）",
                        count, one.add(three).toPlainString(), one.toPlainString(), three.toPlainString()));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxSupplementRespVO recordPayment(TaxSupplementPayReqVO reqVO) {
        TaxSupplementDO supplement = supplementMapper.selectById(reqVO.getId());
        if (supplement == null) {
            throw exception(TAX_SUPPLEMENT_NOT_EXISTS);
        }
        if (!TaxSupplementStatusEnum.PENDING.getStatus().equals(supplement.getStatus())) {
            throw exception(TAX_SUPPLEMENT_STATUS_INVALID, "缴款", TaxSupplementStatusEnum.nameOf(supplement.getStatus()),
                    "已补缴的记录不能重复缴款");
        }
        TaxSupplementDO update = new TaxSupplementDO();
        update.setId(supplement.getId());
        update.setStatus(TaxSupplementStatusEnum.PAID.getStatus());
        update.setPaidAmount(reqVO.getPaidAmount() == null ? supplement.getAmount() : reqVO.getPaidAmount());
        update.setPaidAt(reqVO.getPaidAt() == null ? LocalDateTime.now() : reqVO.getPaidAt());
        update.setVoucherNo(reqVO.getVoucherNo());
        update.setVoucherFileUrl(reqVO.getVoucherFileUrl());
        update.setRemark(reqVO.getRemark());
        supplementMapper.updateById(update);
        return toResp(supplementMapper.selectById(supplement.getId()));
    }

    private TaxSupplementRespVO toResp(TaxSupplementDO supplement) {
        TaxSupplementRespVO resp = new TaxSupplementRespVO();
        resp.setId(supplement.getId());
        resp.setSupplementNo(supplement.getSupplementNo());
        resp.setDeclarationId(supplement.getDeclarationId());
        resp.setPeriodMonth(supplement.getPeriodMonth());
        resp.setPayeeId(supplement.getPayeeId());
        resp.setSellerName(supplement.getSellerName());
        resp.setReason(supplement.getReason());
        resp.setAmountAtOnePercent(supplement.getAmountAtOnePercent());
        resp.setAmountAtThreePercent(supplement.getAmountAtThreePercent());
        resp.setAmount(supplement.getAmount());
        resp.setStatus(supplement.getStatus());
        resp.setStatusName(TaxSupplementStatusEnum.nameOf(supplement.getStatus()));
        resp.setNextAction(TaxSupplementStatusEnum.nextActionOf(supplement.getStatus()));
        resp.setPaidAmount(supplement.getPaidAmount());
        resp.setPaidAt(supplement.getPaidAt());
        resp.setVoucherNo(supplement.getVoucherNo());
        resp.setVoucherFileUrl(supplement.getVoucherFileUrl());
        resp.setRemark(supplement.getRemark());
        return resp;
    }

    private String buildSupplementNo(String periodMonth) {
        return "TAXSUP-" + periodMonth + "-" + System.currentTimeMillis() + "-" + nextSuffix();
    }

    private static long suffixCounter = 0;

    private static synchronized long nextSuffix() {
        return ++suffixCounter;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
