package cn.iocoder.yudao.module.waste.controller.admin.price;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceTrendAnalysisReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceTrendAnalysisRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PricePredictionRespVO;
import cn.iocoder.yudao.module.waste.service.price.PriceTrendAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 价格趋势分析")
@RestController
@RequestMapping("/waste/price-trend-analysis")
@Validated
public class PriceTrendAnalysisController {

    @Resource
    private PriceTrendAnalysisService priceTrendAnalysisService;

    @GetMapping("/analysis")
    @Operation(summary = "获取价格趋势分析")
    @PreAuthorize("@ss.hasPermission('waste:price-trend:query')")
    public CommonResult<List<PriceTrendAnalysisRespVO>> getPriceTrendAnalysis(@Valid PriceTrendAnalysisReqVO reqVO) {
        List<PriceTrendAnalysisRespVO> result = priceTrendAnalysisService.getPriceTrendAnalysis(reqVO);
        return success(result);
    }

    @GetMapping("/trend-by-waste-code")
    @Operation(summary = "获取废物代码的价格趋势")
    @PreAuthorize("@ss.hasPermission('waste:price-trend:query')")
    public CommonResult<List<PriceTrendAnalysisRespVO>> getPriceTrendByWasteCode(
            @Parameter(description = "废物代码", required = true) @RequestParam String wasteCode,
            @Parameter(description = "开始日期", required = true) @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam LocalDate endDate) {
        List<PriceTrendAnalysisRespVO> result = priceTrendAnalysisService.getPriceTrendByWasteCode(wasteCode, startDate, endDate);
        return success(result);
    }

    @GetMapping("/trend-by-region")
    @Operation(summary = "获取地区价格趋势")
    @PreAuthorize("@ss.hasPermission('waste:price-trend:query')")
    public CommonResult<List<PriceTrendAnalysisRespVO>> getPriceTrendByRegion(
            @Parameter(description = "地区代码", required = true) @RequestParam String regionCode,
            @Parameter(description = "开始日期", required = true) @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam LocalDate endDate) {
        List<PriceTrendAnalysisRespVO> result = priceTrendAnalysisService.getPriceTrendByRegion(regionCode, startDate, endDate);
        return success(result);
    }

    @GetMapping("/predict")
    @Operation(summary = "价格预测")
    @PreAuthorize("@ss.hasPermission('waste:price-trend:predict')")
    public CommonResult<PricePredictionRespVO> predictPrice(
            @Parameter(description = "废物代码", required = true) @RequestParam String wasteCode,
            @Parameter(description = "地区代码") @RequestParam(required = false) String regionCode,
            @Parameter(description = "预测天数", required = true) @RequestParam Integer predictDays) {
        PricePredictionRespVO result = priceTrendAnalysisService.predictPrice(wasteCode, regionCode, predictDays);
        return success(result);
    }

    @GetMapping("/volatility")
    @Operation(summary = "获取价格波动分析")
    @PreAuthorize("@ss.hasPermission('waste:price-trend:query')")
    public CommonResult<PriceTrendAnalysisRespVO> getPriceVolatilityAnalysis(
            @Parameter(description = "废物代码", required = true) @RequestParam String wasteCode,
            @Parameter(description = "地区代码") @RequestParam(required = false) String regionCode,
            @Parameter(description = "分析天数", required = true) @RequestParam Integer days) {
        PriceTrendAnalysisRespVO result = priceTrendAnalysisService.getPriceVolatilityAnalysis(wasteCode, regionCode, days);
        return success(result);
    }

    @GetMapping("/comparison")
    @Operation(summary = "获取价格对比分析")
    @PreAuthorize("@ss.hasPermission('waste:price-trend:query')")
    public CommonResult<List<PriceTrendAnalysisRespVO>> getPriceComparisonAnalysis(
            @Parameter(description = "废物代码列表", required = true) @RequestParam List<String> wasteCodes,
            @Parameter(description = "地区代码") @RequestParam(required = false) String regionCode,
            @Parameter(description = "开始日期", required = true) @RequestParam LocalDate startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam LocalDate endDate) {
        List<PriceTrendAnalysisRespVO> result = priceTrendAnalysisService.getPriceComparisonAnalysis(wasteCodes, regionCode, startDate, endDate);
        return success(result);
    }

    @GetMapping("/popular-ranking")
    @Operation(summary = "获取热门废物价格排行")
    @PreAuthorize("@ss.hasPermission('waste:price-trend:query')")
    public CommonResult<List<PriceTrendAnalysisRespVO>> getPopularWastePriceRanking(
            @Parameter(description = "地区代码") @RequestParam(required = false) String regionCode,
            @Parameter(description = "排行数量") @RequestParam(defaultValue = "10") Integer limit) {
        List<PriceTrendAnalysisRespVO> result = priceTrendAnalysisService.getPopularWastePriceRanking(regionCode, limit);
        return success(result);
    }
} 