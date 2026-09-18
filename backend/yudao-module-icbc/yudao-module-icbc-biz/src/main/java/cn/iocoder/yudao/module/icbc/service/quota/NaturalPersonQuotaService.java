package cn.iocoder.yudao.module.icbc.service.quota;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自然人额度查询：连续 12 个月滚动窗口内的反向开票累计销售额与 500 万上限的余量。
 *
 * <p>同一个自然人在本平台多个租户下的开票额合并计算——这是「自然人」的额度，不是某个
 * 租户的额度，所以查询在 {@link TenantUtils#executeIgnore} 下跨租户汇总。
 *
 * <p>本服务只做<b>读</b>的汇总，不做开票前拦截；硬校验属于 #12。
 */
@Service
public class NaturalPersonQuotaService {

    /** 连续 12 个月滚动窗口上限（元） */
    private static final BigDecimal CAP_AMOUNT = new BigDecimal("5000000.00");

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    public PublicQuotaRespVO getQuota(PayeeInfoDO payee) {
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusMonths(12);

        BigDecimal usedAmount = TenantUtils.executeIgnore(() -> sumUsedAmount(payee, windowStart));

        PublicQuotaRespVO respVO = new PublicQuotaRespVO();
        respVO.setName(payee.getName());
        respVO.setIdCardMasked(maskIdCard(payee.getIdCardNo()));
        respVO.setCapAmount(CAP_AMOUNT);
        respVO.setUsedAmount(usedAmount);
        respVO.setRemainingAmount(CAP_AMOUNT.subtract(usedAmount).max(BigDecimal.ZERO));
        respVO.setWindowStart(windowStart);
        respVO.setWindowEnd(windowEnd);
        return respVO;
    }

    private BigDecimal sumUsedAmount(PayeeInfoDO payee, LocalDateTime windowStart) {
        // 同一自然人可能在本平台多个租户各有一份收方档案，按身份证号把它们找齐
        List<PayeeInfoDO> payees = StrUtil.isNotBlank(payee.getIdCardNo())
                ? payeeInfoMapper.selectList(PayeeInfoDO::getIdCardNo, payee.getIdCardNo())
                : List.of(payee);
        Set<Long> payeeIds = payees.stream().map(PayeeInfoDO::getId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> payeeNos = new LinkedHashSet<>();
        for (PayeeInfoDO item : payees) {
            if (StrUtil.isNotBlank(item.getPayeeNo())) {
                payeeNos.add(item.getPayeeNo());
            }
            if (StrUtil.isNotBlank(item.getPartnerPayeeId())) {
                payeeNos.add(item.getPartnerPayeeId());
            }
        }
        if (payeeIds.isEmpty() && payeeNos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LambdaQueryWrapper<InvoiceOrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InvoiceOrderDO::getCreateTime, windowStart);
        wrapper.and(query -> {
            if (CollUtil.isNotEmpty(payeeIds)) {
                query.in(InvoiceOrderDO::getPayeeId, payeeIds);
            }
            if (CollUtil.isNotEmpty(payeeNos)) {
                if (CollUtil.isNotEmpty(payeeIds)) {
                    query.or();
                }
                query.in(InvoiceOrderDO::getPayeeNo, payeeNos);
            }
        });
        return invoiceOrderMapper.selectList(wrapper).stream()
                .map(InvoiceOrderDO::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String maskIdCard(String idCardNo) {
        if (StrUtil.isBlank(idCardNo) || idCardNo.length() < 8) {
            return idCardNo;
        }
        return idCardNo.substring(0, 6)
                + StrUtil.repeat('*', idCardNo.length() - 10)
                + idCardNo.substring(idCardNo.length() - 4);
    }

}
