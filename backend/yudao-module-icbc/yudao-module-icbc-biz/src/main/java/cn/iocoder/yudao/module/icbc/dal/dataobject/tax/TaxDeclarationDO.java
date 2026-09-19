package cn.iocoder.yudao.module.icbc.dal.dataobject.tax;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 代办税费申报单 DO（issue #13）。
 *
 * <p>回收企业为出售者代办增值税及附加税费、个人所得税，按<b>月</b>申报。一份申报单就是
 * 「本租户 × 本申报月」的一张清单：把当月已开出的蓝票（减去当月红冲）按出售者归集，
 * 算出每人应缴的增值税、附加税费、个税，再合计。财务在次月申报期前看到它、申报、缴款，
 * 缴款成功后把凭证归档并与对应发票关联。
 *
 * <p>口径只有这一处：{@link cn.iocoder.yudao.module.icbc.service.tax.TaxDeclarationService}。
 * 10 万元免征线是「自然人 × 月」的（跨租户），由额度台账提供，不在这里重算。
 */
@TableName("icbc_tax_declaration")
@KeySequence("icbc_tax_declaration_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDeclarationDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 申报单号 */
    private String declarationNo;

    /** 申报月 yyyy-MM（即销售额所属月份） */
    private String periodMonth;

    /** 申报期截止日：次月 15 日 */
    private LocalDate declarationDeadline;

    /** 状态，见 {@link cn.iocoder.yudao.module.icbc.enums.TaxDeclarationStatusEnum} */
    private Integer status;

    /** 涉及出售者数 */
    private Integer sellerCount;

    /** 当月销售额超过 10 万元、需单独列出的出售者数 */
    private Integer overExemptSellerCount;

    /** 当月净销售额合计（元） */
    private BigDecimal totalSalesAmount;

    /** 按 3% 减按 1% 的销售额（元） */
    private BigDecimal amountAtOnePercent;

    /** 放弃减按、按 3% 的销售额（元） */
    private BigDecimal amountAtThreePercent;

    /** 征收率未识别的销售额（元，历史数据兜底） */
    private BigDecimal otherAmount;

    /** 应缴增值税合计（元） */
    private BigDecimal vatAmount;

    /** 应缴附加税费合计（元） */
    private BigDecimal surchargeAmount;

    /** 应缴个人所得税合计（元） */
    private BigDecimal iitAmount;

    /** 应缴税费合计（元） */
    private BigDecimal totalTaxAmount;

    /** 实缴金额（元），缴款成功后写入 */
    private BigDecimal paidAmount;

    /** 申报（报送报告表）时间 */
    private LocalDateTime declaredAt;

    /** 申报人 */
    private String declaredBy;

    /** 申报备注 */
    private String declaredRemark;

    /** 缴款时间 */
    private LocalDateTime paidAt;

    /** 缴纳方式 */
    private String paymentMethod;

    /** 缴款凭证号 */
    private String voucherNo;

    /** 缴款凭证文件地址 */
    private String voucherFileUrl;

    /** 申报数据是否齐备（缺项为 0） */
    private Boolean dataReady;

    /** 缺项数量 */
    private Integer missingDataCount;

    /** 备注 */
    private String remark;

}
