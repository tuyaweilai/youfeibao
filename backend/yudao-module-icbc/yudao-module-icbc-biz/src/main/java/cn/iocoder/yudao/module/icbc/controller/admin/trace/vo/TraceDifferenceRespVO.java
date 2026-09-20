package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台 - 关联单据查询的差异 / 缺失关联提示（#55 T17，AC4）。
 *
 * <p>入库量与结算量**不能默认一对一**。这里把差额算出来、把缺失的关联点出来，
 * 由人核实；不做抹平，也不用库存数量反推结算金额（ADR 0028）。
 */
@Schema(description = "管理后台 - 关联单据查询差异提示")
@Data
public class TraceDifferenceRespVO {

    @Schema(description = "差异编码，枚举 TraceDifferenceCodeEnum", example = "STOCK_IN_VS_SETTLEMENT")
    private String code;

    @Schema(description = "差异名称", example = "入库量与结算量不一致")
    private String name;

    @Schema(description = "口径说明")
    private String definition;

    @Schema(description = "左侧应有量（如结算重量）", example = "28.3300")
    private BigDecimal expectedWeight;

    @Schema(description = "右侧实际量（如已过账入库量）", example = "14.7400")
    private BigDecimal actualWeight;

    @Schema(description = "差额（实际 − 应有）", example = "-13.5900")
    private BigDecimal difference;

    @Schema(description = "给人看的差异说明")
    private String note;

}
