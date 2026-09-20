package cn.iocoder.yudao.module.icbc.dal.dataobject.stockin;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 入库单明细 DO（#52 T14）。
 *
 * <p>一条明细 = 「这批货的多少量、堆在哪个仓库 / 库位 / 批次」。同一张收购单的货可拆成多条明细
 * （拆堆不越界），也可分多次入库（多张入库单）。
 *
 * <p>它与 {@code StockApi} 的幂等键一一对应：{@code bizId} = 收购单编号（跨入库单累计上限），
 * {@code bizItemId} = 本明细编号（同一明细只写一次流水，重复确认不重复加库存）。
 *
 * <p>只存维度编号，不存仓库 / 库位 / 批次名称快照：icbc 只能依赖 {@code erp-api}，
 * 看不到 ERP 的仓库表，名称由前端用 ERP 的 simple-list 接口解析。
 */
@TableName("icbc_stock_in_item")
@KeySequence("icbc_stock_in_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockInItemDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 入库单编号 */
    private Long stockInId;

    /** 仓库编号（erp_warehouse.id） */
    private Long warehouseId;

    /** 库位编号（erp_stock_location.id）；0 = 未指定库位 */
    private Long locationId;

    /** 批次编号（erp_stock_batch.id）；0 = 未指定批次 */
    private Long batchId;

    /** 入库数量（正数） */
    private BigDecimal quantity;

    /** 备注 */
    private String remark;

}
