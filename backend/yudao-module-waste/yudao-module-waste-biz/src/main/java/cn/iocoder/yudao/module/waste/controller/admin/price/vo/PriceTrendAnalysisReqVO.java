package cn.iocoder.yudao.module.waste.controller.admin.price.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 价格趋势分析请求 Request VO")
@Data
public class PriceTrendAnalysisReqVO {

    @Schema(description = "废物代码", example = "HW08")
    private String wasteCode;

    @Schema(description = "废物代码列表", example = "[\"HW08\", \"HW09\"]")
    private List<String> wasteCodes;

    @Schema(description = "地区代码", example = "310000")
    private String regionCode;

    @Schema(description = "开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "分析类型", example = "1")
    private Integer analysisType; // 1-趋势分析 2-波动分析 3-对比分析

    @Schema(description = "时间粒度", example = "day")
    private String timeGranularity; // day, week, month

    @Schema(description = "是否包含预测", example = "false")
    private Boolean includePrediction;

    @Schema(description = "预测天数", example = "30")
    private Integer predictDays;
} 