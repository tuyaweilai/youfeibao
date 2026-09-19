package cn.iocoder.yudao.module.icbc.dal.dataobject.invoice;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 红字发票（红冲）DO。
 *
 * <p>一张蓝票同时最多有一张生效中的红票，红蓝一一对应。红冲不是「把蓝票删掉」，
 * 而是另开一张红票冲销它，所以本表与 {@code icbc_invoice_order}（蓝票）分开存，
 * 通过 {@code partnerOrderId} 一一挂回。
 *
 * <p>{@link #redOffsetStatus} 是平台侧收敛后的状态；{@link #redOffsetStatusCode}
 * 原样保留工行 {@code redOffsetStatus}（00–11），联调期间不丢信息。
 */
@TableName("icbc_red_invoice")
@KeySequence("icbc_red_invoice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedInvoiceDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 红冲流水号（我方生成，工行 {@code outRedOffsetId}，幂等键）
     */
    private String redOffsetNo;

    /**
     * 蓝票开票订单编号
     */
    private Long invoiceOrderId;

    /**
     * 原蓝字合作方订单编号（工行 {@code outOrderId}）
     */
    private String partnerOrderId;

    /**
     * 来源收购单编号
     */
    private Long acquisitionId;

    /**
     * 红冲原因：01 开票有误，02 销货退回，03 服务中止，04 销售折让
     *
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.RedInvoiceReasonEnum}
     */
    private String reason;

    /**
     * 红字冲销金额
     */
    private BigDecimal amount;

    /**
     * 红字冲销税额
     */
    private BigDecimal taxAmount;

    /**
     * 平台侧红冲状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum}
     */
    private Integer redOffsetStatus;

    /**
     * 工行原始红字确认单状态码（00–11），保留原始值不丢信息
     */
    private String redOffsetStatusCode;

    /**
     * 红冲发票号（工行 {@code redOffsetInvoiceCode}）
     */
    private String redInvoiceNo;

    /**
     * 红票开具日期
     */
    private LocalDateTime redInvoiceDate;

    /**
     * 撤销结果（工行 {@code revokeStatus}：10 撤销成功 / 11 撤销失败）
     */
    private String revokeStatus;

    /**
     * 撤销发起时间
     */
    private LocalDateTime revokeTime;

    /**
     * 备注
     */
    private String remark;
}
