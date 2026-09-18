package cn.iocoder.yudao.module.waste.controller.admin.quotation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import cn.iocoder.yudao.module.waste.service.quotation.BiddingComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 竞价比较控制器
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 竞价比较")
@RestController
@RequestMapping("/waste/bidding-comparison")
@Validated
@Slf4j
public class BiddingComparisonController {

    @Resource
    private BiddingComparisonService biddingComparisonService;

    @GetMapping("/compare")
    @Operation(summary = "比较预约单的所有报价")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> compareQuotations(@RequestParam("appointmentId") Long appointmentId) {
        Map<String, Object> comparison = biddingComparisonService.compareQuotations(appointmentId);
        return success(comparison);
    }

    @GetMapping("/sort-by-price")
    @Operation(summary = "按价格排序报价")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<List<AppointmentQuotationDO>> sortQuotationsByPrice(@RequestParam("appointmentId") Long appointmentId) {
        List<AppointmentQuotationDO> sortedQuotations = biddingComparisonService.getValidQuotationsSorted(appointmentId);
        return success(sortedQuotations);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取报价统计分析")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> getQuotationStatistics(@RequestParam("appointmentId") Long appointmentId) {
        Map<String, Object> statistics = biddingComparisonService.getQuotationStatistics(appointmentId);
        return success(statistics);
    }

    @GetMapping("/best-quotation")
    @Operation(summary = "获取最优报价")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<AppointmentQuotationDO> getBestQuotation(@RequestParam("appointmentId") Long appointmentId) {
        AppointmentQuotationDO bestQuotation = biddingComparisonService.getBestQuotation(appointmentId);
        return success(bestQuotation);
    }

    @GetMapping("/detect-abnormal")
    @Operation(summary = "检测异常报价")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<List<AppointmentQuotationDO>> detectAbnormalQuotations(@RequestParam("appointmentId") Long appointmentId) {
        List<AppointmentQuotationDO> abnormalQuotations = biddingComparisonService.getAbnormalQuotations(appointmentId);
        return success(abnormalQuotations);
    }

    @GetMapping("/competition-intensity")
    @Operation(summary = "分析竞争激烈程度")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> analyzeCompetitionIntensity(@RequestParam("appointmentId") Long appointmentId) {
        Map<String, Object> analysis = biddingComparisonService.analyzeCompetitionIntensity(appointmentId);
        return success(analysis);
    }

    @GetMapping("/bidding-progress")
    @Operation(summary = "获取竞价进度")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> getBiddingProgress(@RequestParam("appointmentId") Long appointmentId) {
        Map<String, Object> progress = biddingComparisonService.getBiddingProgress(appointmentId);
        return success(progress);
    }

    @PostMapping("/auto-select-best")
    @Operation(summary = "自动选择最优报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:accept')")
    public CommonResult<Long> autoSelectBestQuotation(@Valid @RequestBody AutoSelectRequest request) {
        Long selectedQuotationId = biddingComparisonService.autoSelectBestQuotation(request.getAppointmentId());
        return success(selectedQuotationId);
    }

    @GetMapping("/enterprise-performance")
    @Operation(summary = "分析企业历史表现")
    @Parameter(name = "recyclingEnterpriseId", description = "回收企业ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> analyzeEnterprisePerformance(@RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId) {
        Map<String, Object> performance = biddingComparisonService.getEnterpriseHistoryPerformance(recyclingEnterpriseId);
        return success(performance);
    }

    @GetMapping("/recommended-quotations")
    @Operation(summary = "获取推荐报价列表")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<List<AppointmentQuotationDO>> getRecommendedQuotations(@RequestParam("appointmentId") Long appointmentId) {
        List<AppointmentQuotationDO> recommendations = biddingComparisonService.getRecommendedQuotations(appointmentId);
        return success(recommendations);
    }

    @GetMapping("/price-distribution")
    @Operation(summary = "获取价格分布信息")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> getPriceDistribution(@RequestParam("appointmentId") Long appointmentId) {
        Map<String, Object> distribution = biddingComparisonService.getPriceDistribution(appointmentId);
        return success(distribution);
    }

    @GetMapping("/price-differences")
    @Operation(summary = "分析价格差异")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> analyzePriceDifferences(@RequestParam("appointmentId") Long appointmentId) {
        Map<String, Object> analysis = biddingComparisonService.analyzePriceDifferences(appointmentId);
        return success(analysis);
    }

    @GetMapping("/bidding-report")
    @Operation(summary = "生成竞价报告")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> generateBiddingReport(@RequestParam("appointmentId") Long appointmentId) {
        Map<String, Object> report = biddingComparisonService.generateBiddingReport(appointmentId);
        return success(report);
    }

    @GetMapping("/is-bidding-finished")
    @Operation(summary = "检查竞价是否结束")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Boolean> isBiddingFinished(@RequestParam("appointmentId") Long appointmentId) {
        boolean finished = biddingComparisonService.isBiddingFinished(appointmentId);
        return success(finished);
    }

    @PostMapping("/notify-result")
    @Operation(summary = "通知竞价结果")
    @PreAuthorize("@ss.hasPermission('waste:quotation:accept')")
    public CommonResult<Boolean> notifyBiddingResult(@Valid @RequestBody NotifyResultRequest request) {
        biddingComparisonService.notifyBiddingResult(request.getAppointmentId(), request.getSelectedQuotationId());
        return success(true);
    }

    // ========== 内部类 ==========

    @lombok.Data
    public static class AutoSelectRequest {
        @NotNull(message = "预约单ID不能为空")
        private Long appointmentId;
    }

    @lombok.Data
    public static class NotifyResultRequest {
        @NotNull(message = "预约单ID不能为空")
        private Long appointmentId;

        @NotNull(message = "选中的报价ID不能为空")
        private Long selectedQuotationId;
    }
} 