package cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 采购订单明细 DO（#46 T08）。
 *
 * <p>一单可含多条品类明细（每条一个品类），**一条明细可被分次收货**：明细的编号被成交记录
 * （{@code icbc_purchase_order_deal}）与后续收购单引用，已收量由成交记录汇总推导，不落字段。
 *
 * <p>定价按明细选：{@link #priceMode} 为「固定单价」时直接用 {@link #unitPrice}；
 * 为「按交货日价格表」时由价格表按交货日取价、价格表没覆盖到的日期回退到 {@link #unitPrice}。
 */
@TableName("icbc_purchase_order_item")
@KeySequence("icbc_purchase_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseOrderItemDO extends TenantBaseDO {

    /** 主键（分次收货引用它，故明细编号必须稳定） */
    @TableId
    private Long id;

    /** 采购订单编号 */
    private Long orderId;

    /** 品类编号（icbc_goods_config） */
    private Long goodsConfigId;

    /** 品类名称快照 */
    private String categoryName;

    /** 计量单位快照 */
    private String unit;

    /** 计划量 */
    private BigDecimal quantity;

    /** 定价方式，枚举 {@code PurchaseOrderPriceModeEnum}：1-固定单价，2-按交货日价格表 */
    private Integer priceMode;

    /** 参考单价（固定单价方式的成交价；价格表方式的兜底价） */
    private BigDecimal unitPrice;

    /** 计划金额 = 计划量 × 参考单价 */
    private BigDecimal amount;

    /** 备注（等级 / 规格等只在本单生效的说明） */
    private String remark;

}
