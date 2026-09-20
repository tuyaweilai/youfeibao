package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 预约报价统计信息 Response VO")
@Data
public class AppointmentQuotationStatisticsVO {

    @Schema(description = "总报价数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer totalCount;

    @Schema(description = "有效报价数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer validCount;

    @Schema(description = "已接受报价数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer acceptedCount;

    @Schema(description = "已拒绝报价数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer rejectedCount;

    @Schema(description = "已过期报价数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer expiredCount;

    @Schema(description = "最低报价", example = "800.00")
    private BigDecimal minPrice;

    @Schema(description = "最高报价", example = "1200.00")
    private BigDecimal maxPrice;

    @Schema(description = "平均报价", example = "1000.00")
    private BigDecimal avgPrice;

} 