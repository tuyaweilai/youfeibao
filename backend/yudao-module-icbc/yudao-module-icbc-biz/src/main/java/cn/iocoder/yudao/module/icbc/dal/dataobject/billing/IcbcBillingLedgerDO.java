package cn.iocoder.yudao.module.icbc.dal.dataobject.billing;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台计费计量台账 DO（#16）。
 *
 * <p>平台自己的账，不随租户可写：按「租户 × 月份」记下本期成功开具的报废产品收购发票张数、
 * 被红冲的张数与应计费用。{@code tenantId} 是「被计费的租户」，不是租户隔离维度，
 * 因此本表继承 {@link BaseDO} 而不带租户过滤，需登记进 {@code yudao.tenant.ignore-tables}。
 */
@TableName("icbc_billing_ledger")
@KeySequence("icbc_billing_ledger_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcBillingLedgerDO extends BaseDO {

    @TableId
    private Long id;

    /** 被计费的回收企业租户编号 */
    private Long tenantId;

    /** 计费期间（yyyy-MM，按开票日期归属） */
    private String periodMonth;

    /** 本期成功开具的报废产品收购发票张数 */
    private Integer issuedCount;

    /** 本期蓝票中被成功红冲的张数（红票本身不计） */
    private Integer reversedCount;

    /** 计费张数 = issuedCount − reversedCount */
    private Integer billableCount;

    /** 计费单价（元/张） */
    private BigDecimal unitPrice;

    /** 应计费用 = billableCount × unitPrice */
    private BigDecimal amount;

    /** 本次计量时间 */
    private LocalDateTime generatedTime;

}
