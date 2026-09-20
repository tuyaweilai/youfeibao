package cn.iocoder.yudao.module.icbc.dal.dataobject.stockops;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 非销售出库单明细 DO（#54 T16）。
 *
 * <p>一条明细 = 「哪个仓库 / 库位 / 批次的哪个品类少了多少」。只存维度编号不存名称：
 * icbc 只依赖 {@code erp-api}，名称由前端用 ERP 的 simple-list 解析。
 */
@TableName("icbc_stock_out_item")
@KeySequence("icbc_stock_out_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockOutItemDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 出库单编号 */
    private Long stockOutId;

    /** 品类编号（icbc_goods_config.id） */
    private Long goodsConfigId;

    /** 仓库编号（erp_warehouse.id） */
    private Long warehouseId;

    /** 库位编号（erp_stock_location.id）；0 = 未指定库位 */
    private Long locationId;

    /** 批次编号（erp_stock_batch.id）；0 = 未指定批次 */
    private Long batchId;

    /** 出库数量（正数） */
    private BigDecimal quantity;

    /** 备注 */
    private String remark;

}
