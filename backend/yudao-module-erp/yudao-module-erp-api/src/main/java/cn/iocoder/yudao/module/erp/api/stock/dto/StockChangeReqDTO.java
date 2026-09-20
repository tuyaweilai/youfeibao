package cn.iocoder.yudao.module.erp.api.stock.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ERP 库存变更 Request DTO。
 *
 * <p>入参数量恒为正数；方向由 {@link cn.iocoder.yudao.module.erp.api.stock.StockApi#in} /
 * {@link cn.iocoder.yudao.module.erp.api.stock.StockApi#out} 决定。
 *
 * @author 芋道源码
 */
@Data
public class StockChangeReqDTO {

    /**
     * 品类编号（icbc_goods_config.id）
     */
    @NotNull(message = "品类编号不能为空")
    private Long goodsConfigId;

    /**
     * 仓库编号
     */
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    /**
     * 库位编号（erp_stock_location.id）
     *
     * 为空或 0 表示未指定库位。
     */
    private Long locationId;

    /**
     * 批次编号（erp_stock_batch.id）
     *
     * 为空或 0 表示未指定批次。
     */
    private Long batchId;

    /**
     * 可入库量上限（可选）
     *
     * 传入时，本业务（业务类型 + 业务编号 + 品类）累计入库不得超过该值；
     * 用于「同一品类的货拆到两个库位，合计不超过可入库量」。为空表示不校验上限。
     */
    private BigDecimal maxCount;

    /**
     * 变更数量（正数）
     */
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "数量必须大于 0")
    private BigDecimal count;

    /**
     * 业务类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum}
     */
    @NotNull(message = "业务类型不能为空")
    private Integer bizType;

    /**
     * 业务编号
     */
    @NotNull(message = "业务编号不能为空")
    private Long bizId;

    /**
     * 业务项编号
     */
    @NotNull(message = "业务项编号不能为空")
    private Long bizItemId;

    /**
     * 业务单号
     */
    @NotNull(message = "业务单号不能为空")
    private String bizNo;

}
