package cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 「结束本次收货」前的**本班次建议批次**（#33，ADR 0018）。
 *
 * <p>系统的时间窗（同一出售者 + 同一场站 + 同一班次，默认 4 小时可配）只用来**建议**把哪几张
 * 收购单并在一个结算单里，**不自动合并**——现场动作是唯一可信的批次边界。所以这里返回的只是
 * 一个可展示的候选清单，收货员确认后才走 {@code /icbc/settlement/generate}。
 */
@Schema(description = "管理后台 - 本班次建议批次 Response VO")
@Data
public class SettlementBatchSuggestionVO {

    @Schema(description = "出售者（收方）档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "场站编号", example = "3072")
    private Long stationId;

    @Schema(description = "场站名称", example = "城东收货点")
    private String stationName;

    @Schema(description = "班次窗口（小时），默认 4", example = "4")
    private Integer shiftHours;

    @Schema(description = "建议归组的收购单条数", example = "2")
    private Integer count;

    @Schema(description = "口径说明：只作建议，不自动合并", example = "系统按同出售者 + 同场站 + 最近 4 小时建议，是否合并由你点「结束本次收货」决定")
    private String suggestionNote;

    @Schema(description = "建议归组的收购单明细")
    private List<SettlementRespVO.LineVO> acquisitions;

}
