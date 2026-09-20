package cn.iocoder.yudao.module.icbc.dal.dataobject.inputinvoice;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 进项发票 DO（#49 T11，ADR 0029）。
 *
 * <p>单位供货方（企业 / 个体工商户 / 个人独资企业 / 合伙企业 / 农民专业合作社）向回收企业
 * 开具增值税发票后，由财务在这里登记票面事实，再勾稽到采购单据，让**票、货、款三者对得上**。
 * 自然人出售者不在本链路里——他们走反向开票（{@code icbc_invoice_order}）。
 *
 * <p>三条约定的落点：
 * <ul>
 *     <li>**按销方 + 发票号码唯一**：同一张票不能重复登记。{@link #sellerKey} 是销方税号
 *         （没填税号时退化用销方名称），与 {@link #invoiceNo} 一起构成唯一键；</li>
 *     <li>**状态由勾稽金额推导并落库**：{@link #linkedAmount} 是已勾稽合计，
 *         {@link #status} 由「已勾稽金额与价税合计」的关系推出（见 {@code InputInvoiceStatusEnum}）；</li>
 *     <li>**一张票可勾稽多张单据**：明细落在 {@code icbc_input_invoice_link}（通用关联表）。</li>
 * </ul>
 */
@TableName("icbc_input_invoice")
@KeySequence("icbc_input_invoice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcInputInvoiceDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    // ==================== 票面事实 ====================

    /** 发票号码（销方 + 号码唯一） */
    private String invoiceNo;

    /** 发票代码（数电票可空） */
    private String invoiceCode;

    /** 票种，枚举 {@code InputInvoiceTypeEnum}：1-专票，2-普票 */
    private Integer invoiceType;

    /** 开票日期 */
    private LocalDate invoiceDate;

    /** 销方名称 */
    private String sellerName;

    /** 销方纳税人识别号（可空；为空时按销方名称参与唯一性判定） */
    private String sellerTaxNo;

    /** 销方唯一标识：有税号用税号，否则用名称（服务层计算后落库，供唯一索引使用） */
    private String sellerKey;

    /** 不含税金额 */
    private BigDecimal amount;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 价税合计 = 不含税金额 + 税额（勾稽时可勾稽上限就是它） */
    private BigDecimal totalAmount;

    // ==================== 勾稽状态 ====================

    /** 已勾稽金额合计（每次勾稽 / 取消勾稽后重算） */
    private BigDecimal linkedAmount;

    /** 状态，枚举 {@code InputInvoiceStatusEnum}：0-已登记，1-部分勾稽，2-已勾稽 */
    private Integer status;

    /** 备注 */
    private String remark;

}
