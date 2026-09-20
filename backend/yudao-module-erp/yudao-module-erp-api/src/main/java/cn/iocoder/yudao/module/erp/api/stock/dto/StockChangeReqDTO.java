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
