package cn.iocoder.yudao.module.icbc.dal.dataobject.inputinvoice;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 进项发票勾稽关联 DO（#49 T11）。
 *
 * <p>通用关联表：一行表示「这张进项票的某段金额勾到了哪张单据」。{@link #bizType} 是
 * {@code InputInvoiceBizTypeEnum} 的稳定编码（ACQUISITION / PURCHASE_ORDER / STOCK_IN），
 * 未来接更多单据类型时**不再新建关联表**。
 *
 * <p>金额上限按**调用方给出的** {@link #bizAmount} 校验（并行约定：本分支不 import 采购订单 /
 * 入库单的类，单据号与单据金额一律由调用方传入），且同一单据的累计已勾稽金额不得超过它。
 * {@link #bizNo} 是单据号快照，单据改名 / 删除后勾稽记录仍可读。
 */
@TableName("icbc_input_invoice_link")
@KeySequence("icbc_input_invoice_link_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcInputInvoiceLinkDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 进项发票编号 */
    private Long invoiceId;

    /** 单据类型，枚举 {@code InputInvoiceBizTypeEnum} */
    private String bizType;

    /** 单据编号（收购单 / 采购订单 / 入库单的主键） */
    private Long bizId;

    /** 单据号快照 */
    private String bizNo;

    /** 单据金额（调用方传入，作为勾稽金额上限） */
    private BigDecimal bizAmount;

    /** 本次勾稽金额 */
    private BigDecimal linkedAmount;

    /** 备注 */
    private String remark;

}
