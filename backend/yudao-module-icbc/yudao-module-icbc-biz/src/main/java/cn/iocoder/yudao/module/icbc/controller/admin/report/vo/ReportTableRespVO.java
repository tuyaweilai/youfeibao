package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 经营报表的表清单与口径说明 Response VO（#57 T19）。
 *
 * <p>五张表各自的口径定义随响应返回，前端直接展示；不在前端另写一遍口径。
 */
@Schema(description = "管理后台 - 经营报表表清单 Response VO")
@Data
public class ReportTableRespVO {

    @Schema(description = "表编码：PURCHASE_PERFORMANCE / ACQUISITION_LEDGER / STOCK / SETTLEMENT_PAYMENT / ANOMALY")
    private String code;

    @Schema(description = "表名")
    private String name;

    @Schema(description = "口径说明：这张表的数字从哪来、代表什么")
    private String definition;

}
