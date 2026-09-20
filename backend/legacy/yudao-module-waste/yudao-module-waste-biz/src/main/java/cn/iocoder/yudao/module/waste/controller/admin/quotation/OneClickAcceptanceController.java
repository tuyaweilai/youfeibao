package cn.iocoder.yudao.module.waste.controller.admin.quotation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.waste.service.quotation.OneClickAcceptanceService;
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
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 一键接受报价控制器
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 一键接受报价")
@RestController
@RequestMapping("/waste/one-click-acceptance")
@Validated
@Slf4j
public class OneClickAcceptanceController {

    @Resource
    private OneClickAcceptanceService oneClickAcceptanceService;

    @PostMapping("/accept-quotation")
    @Operation(summary = "一键接受报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:accept')")
    public CommonResult<Map<String, Object>> oneClickAcceptQuotation(@Valid @RequestBody AcceptQuotationRequest request) {
        Map<String, Object> result = oneClickAcceptanceService.oneClickAcceptQuotation(
                request.getQuotationId(),
                request.getAcceptedBy(),
                request.getAcceptReason()
        );
        return success(result);
    }

    @PostMapping("/accept-best")
    @Operation(summary = "一键接受最优报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:accept')")
    public CommonResult<Map<String, Object>> oneClickAcceptBestQuotation(@Valid @RequestBody AcceptBestQuotationRequest request) {
        Map<String, Object> result = oneClickAcceptanceService.oneClickAcceptBestQuotation(
                request.getAppointmentId(),
                request.getAcceptedBy()
        );
        return success(result);
    }

    @PostMapping("/accept-lowest-price")
    @Operation(summary = "一键接受最低价报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:accept')")
    public CommonResult<Map<String, Object>> oneClickAcceptLowestPriceQuotation(@Valid @RequestBody AcceptBestQuotationRequest request) {
        Map<String, Object> result = oneClickAcceptanceService.oneClickAcceptLowestPriceQuotation(
                request.getAppointmentId(),
                request.getAcceptedBy()
        );
        return success(result);
    }

    @GetMapping("/validate")
    @Operation(summary = "验证是否可以接受报价")
    @Parameter(name = "quotationId", description = "报价ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> validateQuotationAcceptance(@RequestParam("quotationId") Long quotationId) {
        Map<String, Object> validation = oneClickAcceptanceService.validateQuotationAcceptance(quotationId);
        return success(validation);
    }

    @GetMapping("/supports-one-click")
    @Operation(summary = "检查企业是否支持一键接受")
    @Parameter(name = "recyclingEnterpriseId", description = "回收企业ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Boolean> supportsOneClickAcceptance(@RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId) {
        boolean supports = oneClickAcceptanceService.supportsOneClickAcceptance(recyclingEnterpriseId);
        return success(supports);
    }

    @GetMapping("/config")
    @Operation(summary = "获取一键接受的配置信息")
    @Parameter(name = "recyclingEnterpriseId", description = "回收企业ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Map<String, Object>> getOneClickAcceptanceConfig(@RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId) {
        Map<String, Object> config = oneClickAcceptanceService.getOneClickAcceptanceConfig(recyclingEnterpriseId);
        return success(config);
    }

    @PostMapping("/batch-accept")
    @Operation(summary = "批量接受报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:accept')")
    public CommonResult<Map<String, Object>> batchAcceptQuotations(@Valid @RequestBody BatchAcceptRequest request) {
        Map<String, Object> result = oneClickAcceptanceService.batchAcceptQuotations(
                request.getQuotationIds(),
                request.getAcceptedBy()
        );
        return success(result);
    }

    @PostMapping("/conditional-accept")
    @Operation(summary = "条件接受报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:accept')")
    public CommonResult<Map<String, Object>> conditionalAcceptQuotation(@Valid @RequestBody ConditionalAcceptRequest request) {
        Map<String, Object> result = oneClickAcceptanceService.conditionalAcceptQuotation(
                request.getAppointmentId(),
                request.getConditions()
        );
        return success(result);
    }

    @GetMapping("/acceptance-history")
    @Operation(summary = "获取接受报价的历史记录")
    @Parameter(name = "appointmentId", description = "预约单ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<List<Map<String, Object>>> getAcceptanceHistory(@RequestParam("appointmentId") Long appointmentId) {
        List<Map<String, Object>> history = oneClickAcceptanceService.getAcceptanceHistory(appointmentId);
        return success(history);
    }

    // ========== 内部类 ==========

    @lombok.Data
    public static class AcceptQuotationRequest {
        @NotNull(message = "报价ID不能为空")
        private Long quotationId;

        @NotNull(message = "接受人不能为空")
        private String acceptedBy;

        private String acceptReason;
    }

    @lombok.Data
    public static class AcceptBestQuotationRequest {
        @NotNull(message = "预约单ID不能为空")
        private Long appointmentId;

        @NotNull(message = "接受人不能为空")
        private String acceptedBy;
    }

    @lombok.Data
    public static class BatchAcceptRequest {
        @NotNull(message = "报价ID列表不能为空")
        private List<Long> quotationIds;

        @NotNull(message = "接受人不能为空")
        private String acceptedBy;
    }

    @lombok.Data
    public static class ConditionalAcceptRequest {
        @NotNull(message = "预约单ID不能为空")
        private Long appointmentId;

        @NotNull(message = "接受条件不能为空")
        private Map<String, Object> conditions;
    }
} 