package cn.iocoder.yudao.module.erp.api.stock.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ERP 盘点调整 Request DTO。
 *
 * <p>把某个库存维度的余额**调整到盘点实盘数** {@link #targetCount}：ERP 在同一个事务里先取当前余额、
 * 再算差额，盘盈写 {@code CHECK_MORE_IN(40)}、盘亏写 {@code CHECK_LESS_OUT(42)}，差额为 0 不写流水。
 * 余额与流水始终一致（余额怎么变，流水就写多少）。
 *
 * <p>这是盘点与调拨的本质区别：调拨搬数量，盘点把余额对齐到一个实测值。调用方不用自己算差额，
 * 也不用担心并发下「读了余额再写」的窗口。
 *
 * @author 芋道源码
 */
@Data
public class StockAdjustReqDTO {

    /**
     * 品类编号（icbc_goods_config.id）
     */
    @NotNull(message = "品类编号不能为空")
    private Long goodsConfigId;

    /**
     * 仓库编号
     */
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    /**
     * 库位编号（为空或 0 表示未指定）
     */
    private Long locationId;

    /**
     * 批次编号（为空或 0 表示未指定）
     */
    private Long batchId;

    /**
     * 盘点实盘数（目标余额，不小于 0）
     */
    @NotNull(message = "盘点实盘数不能为空")
    @DecimalMin(value = "0", message = "盘点实盘数不能为负")
    private BigDecimal targetCount;

    /**
     * 业务编号（盘点单编号）
     */
    @NotNull(message = "业务编号不能为空")
    private Long bizId;

    /**
     * 业务项编号（盘点明细编号；同一明细只调整一次）
     */
    @NotNull(message = "业务项编号不能为空")
    private Long bizItemId;

    /**
     * 业务单号（盘点单号，落流水便于追溯）
     */
    @NotNull(message = "业务单号不能为空")
    private String bizNo;

}
