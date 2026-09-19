package cn.iocoder.yudao.module.icbc.dal.dataobject.tax;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 申报单与发票的关联 DO：缴款成功后「凭证与对应发票关联」靠它落地。
 *
 * <p>一条记录 = 一份申报单覆盖到的一张票（蓝票计入、红票冲减）。税务抽查时可以从申报单
 * 反查发票，也可以从发票反查它算在哪一份申报里。
 */
@TableName("icbc_tax_declaration_invoice")
@KeySequence("icbc_tax_declaration_invoice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDeclarationInvoiceDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 申报单编号 */
    private Long declarationId;

    /** 申报月 yyyy-MM */
    private String periodMonth;

    /** 开票订单编号 */
    private Long invoiceOrderId;

    /** 合作方订单号 */
    private String partnerOrderId;

    /** 发票号码 */
    private String invoiceNo;

    /** 出售者档案编号 */
    private Long payeeId;

    /** 方向：BLUE-蓝票计入，RED-红票冲减 */
    private String direction;

    /** 金额（元） */
    private BigDecimal amount;

    /** 适用征收率 */
    private BigDecimal taxRate;

}
