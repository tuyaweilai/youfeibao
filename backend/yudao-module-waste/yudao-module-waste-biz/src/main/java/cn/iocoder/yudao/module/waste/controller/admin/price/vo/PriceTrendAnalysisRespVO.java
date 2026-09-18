package cn.iocoder.yudao.module.waste.controller.admin.price.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 价格趋势分析响应 Response VO")
@Data
public class PriceTrendAnalysisRespVO {

    @Schema(description = "日期", example = "2023-12-01")
    private LocalDate date;

    @Schema(description = "废物代码", example = "HW08")
    private String wasteCode;

    @Schema(description = "废物名称", example = "废矿物油")
    private String wasteName;

    @Schema(description = "地区代码", example = "310000")
    private String regionCode;

    @Schema(description = "地区名称", example = "上海市")
    private String regionName;

    @Schema(description = "平均价格", example = "700.00")
    private BigDecimal avgPrice;

    @Schema(description = "最高价格", example = "800.00")
    private BigDecimal maxPrice;

    @Schema(description = "最低价格", example = "600.00")
    private BigDecimal minPrice;

    @Schema(description = "价格变化率", example = "5.2")
    private BigDecimal changeRate;

    @Schema(description = "价格变化金额", example = "35.00")
    private BigDecimal changeAmount;

    @Schema(description = "交易量", example = "150")
    private Integer transactionCount;

    @Schema(description = "交易总量", example = "1500.00")
    private BigDecimal totalVolume;

    @Schema(description = "价格趋势", example = "上涨")
    private String trend; // 上涨、下跌、平稳

    @Schema(description = "波动率", example = "12.5")
    private BigDecimal volatility;

    @Schema(description = "价格预测值", example = "720.00")
    private BigDecimal predictedPrice;

    @Schema(description = "预测置信度", example = "85.5")
    private BigDecimal confidence;

    @Schema(description = "历史价格数据")
    private List<PricePoint> priceHistory;

    @Schema(description = "价格点数据")
    @Data
    public static class PricePoint {
        @Schema(description = "日期", example = "2023-12-01")
        private LocalDate date;

        @Schema(description = "价格", example = "700.00")
        private BigDecimal price;

        @Schema(description = "交易量", example = "50")
        private Integer volume;
    }
} 