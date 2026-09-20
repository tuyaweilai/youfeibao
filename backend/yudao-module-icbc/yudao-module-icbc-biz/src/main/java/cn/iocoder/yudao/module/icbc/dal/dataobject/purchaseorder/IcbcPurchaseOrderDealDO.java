package cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单成交记录 DO（#46 T08，用户故事 18）。
 *
 * <p>每次「成交」留一条价格快照：这一笔按什么价成交、参考价是多少、与参考价不一致时的调整原因。
 * 事后对账看得到每一笔的定价依据，而不是只有一个当前价。
 *
 * <p>它也是**分次收货**在采购订单侧的载体：同一条明细可以有多条成交记录，
 * 已收量由这些记录的数量汇总推导。收购单（#51）可经 {@code sourceType / sourceId / sourceNo}
 * 挂回对应的业务单据；不挂业务单据的手工登记同样留痕。
 */
@TableName("icbc_purchase_order_deal")
@KeySequence("icbc_purchase_order_deal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseOrderDealDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 采购订单编号 */
    private Long orderId;

    /** 采购订单明细编号（一条明细可分多次成交） */
    private Long itemId;

    /** 成交单号（平台生成） */
    private String dealNo;

    /** 成交时间 */
    private LocalDateTime dealTime;

    /** 交货日（按交货日价格表取价用的日期） */
    private LocalDate deliveryDate;

    /** 成交数量 */
    private BigDecimal quantity;

    /** 成交单价（价格快照） */
    private BigDecimal unitPrice;

    /** 参考单价（下单时的固定价，或该交货日价格表取到的价） */
    private BigDecimal referenceUnitPrice;

    /** 成交价是否做过调整（与参考价不一致） */
    private Boolean priceAdjusted;

    /** 调整原因（调整时必须填） */
    private String adjustReason;

    /** 关联业务来源类型（如 ACQUISITION；可空表示手工登记） */
    private String sourceType;

    /** 关联业务来源编号 */
    private Long sourceId;

    /** 关联业务来源单号 */
    private String sourceNo;

    /** 备注 */
    private String remark;

}
