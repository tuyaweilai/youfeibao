package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 「当前库存」口径就绪 Response VO（#54 T16，规格 #38 user story 37）。
 *
 * <p>只有入库记录时，余额只是平台内的**累计入库**；要敢称「当前库存」，必须同时具备
 * 期初、出库、调拨与盘点四项能力（ADR 0025），并且**已经导入期初**——否则数字漏掉启用平台
 * 之前的存量，会被误读。本 VO 把四件事逐项摊开，页面的标题与提示都据此显示，不靠前端猜。
 */
@Schema(description = "管理后台 - 「当前库存」口径就绪 Response VO")
@Data
public class StockOpsReadinessRespVO {

    @Schema(description = "期初导入能力是否具备（#54 起恒为 true）", example = "true")
    private Boolean openingSupported;

    @Schema(description = "非销售出库能力是否具备（报损 / 退货出库 / 内部领用）", example = "true")
    private Boolean outboundSupported;

    @Schema(description = "跨仓调拨能力是否具备", example = "true")
    private Boolean moveSupported;

    @Schema(description = "盘点调整能力是否具备", example = "true")
    private Boolean checkSupported;

    @Schema(description = "是否已导入期初（数据事实，不是能力）", example = "true")
    private Boolean openingImported;

    @Schema(description = "四项能力是否齐备（#54 起恒为 true）", example = "true")
    private Boolean capabilitiesReady;

    @Schema(description = "页面是否可以用「当前库存」称呼余额：四项能力齐备且已导入期初", example = "true")
    private Boolean currentStockReady;

    @Schema(description = "余额的称呼：当前库存 / 累计入库", example = "当前库存")
    private String label;

    @Schema(description = "尚未满足的事项（为空表示可直接称「当前库存」）")
    private List<String> missingItems;

    @Schema(description = "给用户的一句话提示")
    private String notice;

}
