package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量齐备率。{@code completenessRate} 是「所有票齐备的流数之和 /（票数 × 5）」，
 * 不是各票齐备率的简单平均，口径见 #11。
 */
@Schema(description = "管理后台 - 批量齐备率 Response VO")
@Data
public class EvidenceCompletenessSummaryRespVO {

    @Schema(description = "统计的票数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer invoiceCount;

    @Schema(description = "五流齐备的票数", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer completeCount;

    @Schema(description = "批量齐备率（百分比，0-100）", requiredMode = Schema.RequiredMode.REQUIRED, example = "46.00")
    private BigDecimal completenessRate;

    @Schema(description = "逐票齐备率")
    private List<EvidenceCompletenessItemRespVO> items;

}
