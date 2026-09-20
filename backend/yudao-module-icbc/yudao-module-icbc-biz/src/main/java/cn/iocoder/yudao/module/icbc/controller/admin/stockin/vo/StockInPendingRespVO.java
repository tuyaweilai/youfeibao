package cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待入库 Response VO（#52 T14）。
 *
 * <p>一条 = 一张「已验收、尚未全部入库」的收购单，带三个数量：
 * 可入库实物量（实物口径）、累计入库（当前已过账）、剩余可入库。
 * 数量口径只有一处取数（{@code StockInService#resolveAvailableQuantity}）。
 */
@Schema(description = "管理后台 - 待入库 Response VO")
@Data
public class StockInPendingRespVO {

    @Schema(description = "收购单编号", example = "1024")
    private Long acquisitionId;

    @Schema(description = "收购单号", example = "ACQ2026092000001")
    private String acquisitionNo;

    @Schema(description = "出售者档案编号", example = "1")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

    @Schema(description = "品类名称", example = "废钢")
    private String categoryName;

    @Schema(description = "计量单位", example = "吨")
    private String unit;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "净重（毛重 − 皮重，实物口径）", example = "12500")
    private BigDecimal netWeight;

    @Schema(description = "可入库实物量", example = "12500")
    private BigDecimal availableQuantity;

    @Schema(description = "累计入库（当前已过账）", example = "5000")
    private BigDecimal stockedQuantity;

    @Schema(description = "剩余可入库", example = "7500")
    private BigDecimal remainingQuantity;

}
