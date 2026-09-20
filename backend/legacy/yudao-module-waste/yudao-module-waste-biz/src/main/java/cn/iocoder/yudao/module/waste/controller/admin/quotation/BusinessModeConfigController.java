package cn.iocoder.yudao.module.waste.controller.admin.quotation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;
import cn.iocoder.yudao.module.waste.service.quotation.BusinessModeConfigService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 业务模式配置管理控制器
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 业务模式配置")
@RestController
@RequestMapping("/waste/business-mode-config")
@Validated
@Slf4j
public class BusinessModeConfigController {

    @Resource
    private BusinessModeConfigService businessModeConfigService;

    @PostMapping("/configure")
    @Operation(summary = "配置企业业务模式")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:config')")
    public CommonResult<Long> configureBusinessMode(@Valid @RequestBody BusinessModeConfigRequest request) {
        Long configId = businessModeConfigService.configureBusinessMode(
                request.getRecyclingEnterpriseId(),
                request.getBusinessMode(),
                request.getDefaultQuotationMode(),
                request.getAllowClientModeSelection(),
                request.getQuotationTimeoutHours(),
                request.getAutoAcceptSingleQuotation(),
                request.getEnablePriceNegotiation()
        );
        return success(configId);
    }

    @GetMapping("/get")
    @Operation(summary = "获取企业业务模式配置")
    @Parameter(name = "recyclingEnterpriseId", description = "回收企业ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:business-mode:query')")
    public CommonResult<RecyclerBusinessConfigDO> getBusinessModeConfig(@RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId) {
        RecyclerBusinessConfigDO config = businessModeConfigService.getBusinessModeConfig(recyclingEnterpriseId);
        return success(config);
    }

    @PutMapping("/update-mode")
    @Operation(summary = "更新企业业务模式")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:update')")
    public CommonResult<Boolean> updateBusinessMode(@Valid @RequestBody UpdateBusinessModeRequest request) {
        businessModeConfigService.updateBusinessMode(request.getRecyclingEnterpriseId(), request.getBusinessMode());
        return success(true);
    }

    @PutMapping("/toggle")
    @Operation(summary = "启用/禁用企业业务配置")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:update')")
    public CommonResult<Boolean> toggleBusinessConfig(@Valid @RequestBody ToggleConfigRequest request) {
        businessModeConfigService.toggleBusinessConfig(request.getRecyclingEnterpriseId(), request.getEnabled());
        return success(true);
    }

    @GetMapping("/list-by-mode")
    @Operation(summary = "获取指定业务模式的企业列表")
    @Parameter(name = "businessMode", description = "业务模式", required = true)
    @PreAuthorize("@ss.hasPermission('waste:business-mode:query')")
    public CommonResult<List<RecyclerBusinessConfigDO>> getEnterprisesByBusinessMode(@RequestParam("businessMode") Integer businessMode) {
        List<RecyclerBusinessConfigDO> enterprises = businessModeConfigService.getEnterprisesByBusinessMode(businessMode);
        return success(enterprises);
    }

    @GetMapping("/bidding-enterprises")
    @Operation(summary = "获取支持竞价模式的企业列表")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:query')")
    public CommonResult<List<RecyclerBusinessConfigDO>> getBiddingModeEnterprises() {
        List<RecyclerBusinessConfigDO> enterprises = businessModeConfigService.getBiddingModeEnterprises();
        return success(enterprises);
    }

    @GetMapping("/independent-enterprises")
    @Operation(summary = "获取支持独立运营模式的企业列表")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:query')")
    public CommonResult<List<RecyclerBusinessConfigDO>> getIndependentModeEnterprises() {
        List<RecyclerBusinessConfigDO> enterprises = businessModeConfigService.getIndependentModeEnterprises();
        return success(enterprises);
    }

    @GetMapping("/hybrid-enterprises")
    @Operation(summary = "获取支持混合模式的企业列表")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:query')")
    public CommonResult<List<RecyclerBusinessConfigDO>> getHybridModeEnterprises() {
        List<RecyclerBusinessConfigDO> enterprises = businessModeConfigService.getHybridModeEnterprises();
        return success(enterprises);
    }

    @GetMapping("/supports-mode")
    @Operation(summary = "检查企业是否支持指定业务模式")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:query')")
    public CommonResult<Boolean> supportsBusinessMode(@RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId,
                                                      @RequestParam("businessMode") Integer businessMode) {
        boolean supports = businessModeConfigService.supportsBusinessMode(recyclingEnterpriseId, businessMode);
        return success(supports);
    }

    @PostMapping("/batch-configure")
    @Operation(summary = "批量配置企业业务模式")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:config')")
    public CommonResult<Integer> batchConfigureBusinessMode(@Valid @RequestBody BatchConfigRequest request) {
        int successCount = businessModeConfigService.batchConfigureBusinessMode(
                request.getRecyclingEnterpriseIds(),
                request.getBusinessMode(),
                request.getDefaultQuotationMode()
        );
        return success(successCount);
    }

    @PostMapping("/reset-default")
    @Operation(summary = "重置企业业务配置为默认值")
    @Parameter(name = "recyclingEnterpriseId", description = "回收企业ID", required = true)
    @PreAuthorize("@ss.hasPermission('waste:business-mode:update')")
    public CommonResult<Boolean> resetToDefaultConfig(@RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId) {
        businessModeConfigService.resetToDefaultConfig(recyclingEnterpriseId);
        return success(true);
    }

    @PostMapping("/validate")
    @Operation(summary = "验证业务模式配置的有效性")
    @PreAuthorize("@ss.hasPermission('waste:business-mode:query')")
    public CommonResult<Boolean> validateBusinessConfig(@Valid @RequestBody RecyclerBusinessConfigDO config) {
        boolean isValid = businessModeConfigService.validateBusinessConfig(config);
        return success(isValid);
    }

    // ========== 内部类 ==========

    @lombok.Data
    public static class BusinessModeConfigRequest {
        @NotNull(message = "回收企业ID不能为空")
        private Long recyclingEnterpriseId;

        @NotNull(message = "业务模式不能为空")
        private Integer businessMode;

        private Integer defaultQuotationMode = 2; // 默认手动报价

        private Boolean allowClientModeSelection = false;

        private Integer quotationTimeoutHours = 24;

        private Boolean autoAcceptSingleQuotation = false;

        private Boolean enablePriceNegotiation = true;
    }

    @lombok.Data
    public static class UpdateBusinessModeRequest {
        @NotNull(message = "回收企业ID不能为空")
        private Long recyclingEnterpriseId;

        @NotNull(message = "业务模式不能为空")
        private Integer businessMode;
    }

    @lombok.Data
    public static class ToggleConfigRequest {
        @NotNull(message = "回收企业ID不能为空")
        private Long recyclingEnterpriseId;

        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
    }

    @lombok.Data
    public static class BatchConfigRequest {
        @NotNull(message = "回收企业ID列表不能为空")
        private List<Long> recyclingEnterpriseIds;

        @NotNull(message = "业务模式不能为空")
        private Integer businessMode;

        private Integer defaultQuotationMode = 2;
    }
} 