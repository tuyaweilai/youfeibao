package cn.iocoder.yudao.module.erp.api.stock.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ERP 跨仓调拨 Request DTO。
 *
 * <p>把 {@link #count} 数量的同一品类货从「源维度」调到「目标维度」：源减、目标加，一次写两条流水
 * （{@code MOVE_OUT(32)} / {@code MOVE_IN(30)}）。同一业务项（业务类型 + 业务编号 + 业务项编号）
 * 只调拨一次，重复调用不再移动。源库存不足时报 {@code STOCK_COUNT_NEGATIVE}。
 *
 * <p>调用方（回收业务模块）负责给出业务单据编号与单号；ERP 不知道业务含义，只保证
 * 「账户之间搬数量」这一件事是原子的、余额与流水一致的。
 *
 * @author 芋道源码
 */
@Data
public class StockMoveReqDTO {

    /**
     * 品类编号（icbc_goods_config.id）
     */
    @NotNull(message = "品类编号不能为空")
    private Long goodsConfigId;

    /**
     * 源仓库编号
     */
    @NotNull(message = "源仓库不能为空")
    private Long fromWarehouseId;

    /**
     * 源库位编号（为空或 0 表示未指定）
     */
    private Long fromLocationId;

    /**
     * 源批次编号（为空或 0 表示未指定）
     */
    private Long fromBatchId;

    /**
     * 目标仓库编号
     */
    @NotNull(message = "目标仓库不能为空")
    private Long toWarehouseId;

    /**
     * 目标库位编号（为空或 0 表示未指定）
     */
    private Long toLocationId;

    /**
     * 目标批次编号（为空或 0 表示未指定）
     */
    private Long toBatchId;

    /**
     * 调拨数量（正数）
     */
    @NotNull(message = "调拨数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "调拨数量必须大于 0")
    private BigDecimal count;

    /**
     * 业务编号（调拨单编号）
     */
    @NotNull(message = "业务编号不能为空")
    private Long bizId;

    /**
     * 业务项编号（调拨明细编号；同一明细只调拨一次）
     */
    @NotNull(message = "业务项编号不能为空")
    private Long bizItemId;

    /**
     * 业务单号（调拨单号，落流水便于追溯）
     */
    @NotNull(message = "业务单号不能为空")
    private String bizNo;

}
