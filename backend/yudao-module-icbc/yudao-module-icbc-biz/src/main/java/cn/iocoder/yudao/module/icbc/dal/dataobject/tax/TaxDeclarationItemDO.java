package cn.iocoder.yudao.module.icbc.dal.dataobject.tax;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代办税费申报明细 DO：一个出售者在申报月内的一条。
 *
 * <p>它是「当月销售额超过 10 万元的出售者被单独列出」的载体——{@link #overExempt} 为真
 * 的那条就是需要单独申报的。10 万元免征线按<b>自然人 × 月跨租户</b>判定（见
 * {@link #crossTenantMonthAmount}），而本条应缴金额只按本租户当月开票金额计算。
 */
@TableName("icbc_tax_declaration_item")
@KeySequence("icbc_tax_declaration_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDeclarationItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 申报单编号 */
    private Long declarationId;

    /** 申报月 yyyy-MM */
    private String periodMonth;

    /** 出售者档案编号 */
    private Long payeeId;

    /** 出售者姓名 */
    private String sellerName;

    /** 身份证号码 */
    private String idCardNo;

    /** 当月本租户开票张数 */
    private Integer invoiceCount;

    /** 当月本租户净销售额（元） */
    private BigDecimal salesAmount;

    /** 按 3% 减按 1% 的销售额（元） */
    private BigDecimal amountAtOnePercent;

    /** 放弃减按、按 3% 的销售额（元） */
    private BigDecimal amountAtThreePercent;

    /** 征收率未识别的销售额（元） */
    private BigDecimal otherAmount;

    /** 该自然人当月在<b>本平台各租户</b>的净销售额合计（10 万元免征线口径） */
    private BigDecimal crossTenantMonthAmount;

    /** 增值税是否免征（未超 10 万元） */
    private Boolean vatExempt;

    /** 当月销售额（跨租户）是否超过 10 万元，需要单独列出代办申报 */
    private Boolean overExempt;

    /** 应缴增值税（元） */
    private BigDecimal vatAmount;

    /** 应缴附加税费（元） */
    private BigDecimal surchargeAmount;

    /** 应缴个人所得税（元） */
    private BigDecimal iitAmount;

    /** 应缴税费合计（元） */
    private BigDecimal totalTaxAmount;

    /** 实缴金额（元） */
    private BigDecimal paidAmount;

    /** 状态，同申报单状态线 */
    private Integer status;

    /** 缴款时间 */
    private LocalDateTime paidAt;

    /** 备注 */
    private String remark;

}
