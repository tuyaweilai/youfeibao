package cn.iocoder.yudao.module.logistics.dal.dataobject.freight;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 承运商运费单 DO（V8 #75）。
 *
 * <p>CONTEXT.md「运费」：回收企业向**承运商**支付的运输服务费用，是**另一笔账**，
 * <b>不改变收购单金额与发票金额</b>——与收购单调整项里那个「运费」（出售者货款上的加减项，
 * ADR 0019）不是同一个东西。因此本表不与收购单 / 发票发生任何金额往来。
 *
 * <p>**一趟一张**（按趟次汇集）：任务编号在租户内唯一。运价来自承运合同并被**快照**下来
 *（合同改了不影响已汇集的账）。
 *
 * <p>差异用两个数加一条原因表达，**不抹平**：
 * <ul>
 *   <li>{@code expectedAmount} 按合同算出的应有应付（计费量 × 运价 ± 附加费承担）；</li>
 *   <li>{@code actualAmount} 对账后确认的实际应付；</li>
 *   <li>{@code varianceAmount} = 实际 − 应有；{@code varianceReason} 非零差异必填。</li>
 * </ul>
 *
 * <p>付款只**登记外部付款凭证**（不接对公付款通道，ADR 0006）：凭证号 / 附件 / 付款时间留在本表。
 */
@TableName("logistics_freight_order")
@KeySequence("logistics_freight_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsFreightOrderDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 运费单号（租户内唯一，对外可见） */
    private String freightNo;

    /** 运输任务编号 */
    private Long taskId;
    /** 运输任务单号（冗余） */
    private String taskNo;

    /** 承运商编号（承运商运费必有；自有车不允许有） */
    private Long carrierId;
    /** 承运商名称快照 */
    private String carrierName;

    /** 承运合同编号 */
    private Long contractId;
    /** 承运合同号快照 */
    private String contractNo;

    /**
     * 计费方式（合同快照）
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBillingModeEnum}
     */
    private Integer billingMode;

    /** 计费量（按车 = 趟数、按吨 = 吨数、按公里 = 公里数） */
    private BigDecimal billQuantity;

    /** 运价（合同快照） */
    private BigDecimal billUnitPrice;

    /** 基础运费（计费量 × 运价） */
    private BigDecimal baseAmount;

    /** 附加费净额（本企业承担的 − 承运商承担的；合同快照并计入应有应付） */
    private BigDecimal surchargeAmount;

    /** 应有应付（基础运费 + 附加费净额） */
    private BigDecimal expectedAmount;

    /** 实际应付（对账确认；确认前可空） */
    private BigDecimal actualAmount;

    /** 差异（实际 − 应有；有实际应付后才有值） */
    private BigDecimal varianceAmount;

    /** 差异原因（差异非零必填，不抹平） */
    private String varianceReason;

    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsFreightStatusEnum}
     */
    private Integer status;

    /** 确认应付人（系统用户编号） */
    private Long confirmBy;
    /** 确认应付人姓名 */
    private String confirmByName;
    /** 确认应付时间 */
    private LocalDateTime confirmTime;
    /** 确认应付备注 */
    private String confirmRemark;

    /** 外部付款凭证号 */
    private String paymentVoucherNo;
    /** 外部付款凭证附件 URL */
    private String paymentVoucherUrl;
    /** 实付金额 */
    private BigDecimal paymentAmount;
    /** 付款时间 */
    private LocalDateTime paidAt;
    /** 付款备注 */
    private String paymentRemark;

    /** 备注 */
    private String remark;

}
