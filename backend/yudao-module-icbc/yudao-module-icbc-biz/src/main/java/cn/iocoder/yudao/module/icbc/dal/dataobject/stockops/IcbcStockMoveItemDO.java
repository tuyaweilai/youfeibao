package cn.iocoder.yudao.module.icbc.dal.dataobject.stockops;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 跨仓调拨单明细 DO（#54 T16）。
 *
 * <p>一条明细 = 「多少量、从哪个源维度、到哪个目标维度」。源与目标都带仓库 / 库位 / 批次，
 * 因为同一仓库内换库位也是调拨。
 */
@TableName("icbc_stock_move_item")
@KeySequence("icbc_stock_move_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockMoveItemDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 调拨单编号 */
    private Long moveId;

    /** 品类编号（icbc_goods_config.id） */
    private Long goodsConfigId;

    /** 源仓库编号（erp_warehouse.id） */
    private Long fromWarehouseId;

    /** 源库位编号；0 = 未指定 */
    private Long fromLocationId;

    /** 源批次编号；0 = 未指定 */
    private Long fromBatchId;

    /** 目标仓库编号（erp_warehouse.id） */
    private Long toWarehouseId;

    /** 目标库位编号；0 = 未指定 */
    private Long toLocationId;

    /** 目标批次编号；0 = 未指定 */
    private Long toBatchId;

    /** 调拨数量（正数） */
    private BigDecimal quantity;

    /** 备注 */
    private String remark;

}
