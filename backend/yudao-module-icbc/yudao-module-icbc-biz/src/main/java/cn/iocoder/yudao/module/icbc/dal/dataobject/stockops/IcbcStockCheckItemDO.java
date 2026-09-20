package cn.iocoder.yudao.module.icbc.dal.dataobject.stockops;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 盘点单明细 DO（#54 T16）。
 *
 * <p>一条明细 = 「某个维度实盘多少」。账面数量与差额在**过账时**落库：账面取当时的余额
 * （ERP 侧算差额，见 {@code StockApi#adjustTo}），避免登记与过账之间发生出入库后差额对不上。
 */
@TableName("icbc_stock_check_item")
@KeySequence("icbc_stock_check_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockCheckItemDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 盘点单编号 */
    private Long checkId;

    /** 品类编号（icbc_goods_config.id） */
    private Long goodsConfigId;

    /** 仓库编号（erp_warehouse.id） */
    private Long warehouseId;

    /** 库位编号；0 = 未指定 */
    private Long locationId;

    /** 批次编号；0 = 未指定 */
    private Long batchId;

    /** 盘点实盘数（录入值） */
    private BigDecimal actualQuantity;

    /** 账面数量（过账时的余额快照） */
    private BigDecimal bookQuantity;

    /** 差额 = 实盘 − 账面；正数盘盈，负数盘亏 */
    private BigDecimal differenceQuantity;

    /** 备注 */
    private String remark;

}
