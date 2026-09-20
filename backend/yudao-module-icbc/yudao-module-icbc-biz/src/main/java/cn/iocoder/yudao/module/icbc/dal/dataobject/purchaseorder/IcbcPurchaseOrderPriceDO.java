package cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购订单交货日价格表 DO（#46 T08，用户故事 18）。
 *
 * <p>「按交货日价格表」定价的明细用它：一条 = 某个交货日生效的单价。取值语义是
 * **「交货日不晚于当日的最新一条」**，没被覆盖到的日期回退到明细的参考单价。
 * 改价就整组重建（逻辑删旧行 + 插新行），与采购合同适用品类同一做法。
 */
@TableName("icbc_purchase_order_price")
@KeySequence("icbc_purchase_order_price_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseOrderPriceDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 采购订单编号 */
    private Long orderId;

    /** 采购订单明细编号 */
    private Long itemId;

    /** 生效交货日 */
    private LocalDate deliveryDate;

    /** 该日生效单价 */
    private BigDecimal unitPrice;

    /** 备注 */
    private String remark;

}
