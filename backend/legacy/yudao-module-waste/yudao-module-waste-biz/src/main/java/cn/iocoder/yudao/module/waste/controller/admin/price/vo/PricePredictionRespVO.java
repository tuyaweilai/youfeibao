package cn.iocoder.yudao.module.waste.controller.admin.price.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 价格预测响应 Response VO")
@Data
public class PricePredictionRespVO {

    @Schema(description = "废物代码", example = "HW08")
    private String wasteCode;

    @Schema(description = "废物名称", example = "废矿物油")
    private String wasteName;

    @Schema(description = "地区代码", example = "310000")
    private String regionCode;

    @Schema(description = "地区名称", example = "上海市")
    private String regionName;

    @Schema(description = "当前价格", example = "700.00")
    private BigDecimal currentPrice;

    @Schema(description = "预测价格", example = "720.00")
    private BigDecimal predictedPrice;

    @Schema(description = "预测变化率", example = "2.86")
    private BigDecimal predictedChangeRate;

    @Schema(description = "预测置信度", example = "85.5")
    private BigDecimal confidence;

    @Schema(description = "预测日期", example = "2024-01-01")
    private LocalDate predictDate;

    @Schema(description = "预测天数", example = "30")
    private Integer predictDays;

    @Schema(description = "预测模型", example = "线性回归")
    private String predictionModel;

    @Schema(description = "历史准确率", example = "78.5")
    private BigDecimal historicalAccuracy;

    @Schema(description = "价格区间预测")
    private PriceRange priceRange;

    @Schema(description = "预测数据点列表")
    private List<PredictionPoint> predictionPoints;

    @Schema(description = "影响因素分析")
    private List<InfluenceFactor> influenceFactors;

    @Schema(description = "价格区间")
    @Data
    public static class PriceRange {
        @Schema(description = "最低预测价格", example = "680.00")
        private BigDecimal minPrice;

        @Schema(description = "最高预测价格", example = "760.00")
        private BigDecimal maxPrice;

        @Schema(description = "置信区间", example = "95%")
        private String confidenceInterval;
    }

    @Schema(description = "预测数据点")
    @Data
    public static class PredictionPoint {
        @Schema(description = "预测日期", example = "2024-01-01")
        private LocalDate date;

        @Schema(description = "预测价格", example = "720.00")
        private BigDecimal price;

        @Schema(description = "置信度", example = "85.5")
        private BigDecimal confidence;

        @Schema(description = "上限价格", example = "750.00")
        private BigDecimal upperBound;

        @Schema(description = "下限价格", example = "690.00")
        private BigDecimal lowerBound;
    }

    @Schema(description = "影响因素")
    @Data
    public static class InfluenceFactor {
        @Schema(description = "因素名称", example = "市场供需")
        private String factorName;

        @Schema(description = "影响权重", example = "0.35")
        private BigDecimal weight;

        @Schema(description = "影响方向", example = "正向")
        private String direction; // 正向、负向、中性

        @Schema(description = "影响描述", example = "市场需求增加推高价格")
        private String description;
    }
} 