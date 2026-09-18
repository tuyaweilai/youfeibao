package cn.iocoder.yudao.module.waste.controller.admin.recycler;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;
import cn.iocoder.yudao.module.waste.service.recycler.RecyclerBusinessConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 回收企业业务模式配置")
@RestController
@RequestMapping("/waste/recycler-business-config")
@Validated
public class RecyclerBusinessConfigController {

    @Resource
    private RecyclerBusinessConfigService recyclerBusinessConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建回收企业业务模式配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:create')")
    public CommonResult<Long> createRecyclerBusinessConfig(@Valid @RequestBody RecyclerBusinessConfigCreateReqVO createReqVO) {
        return success(recyclerBusinessConfigService.createRecyclerBusinessConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新回收企业业务模式配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:update')")
    public CommonResult<Boolean> updateRecyclerBusinessConfig(@Valid @RequestBody RecyclerBusinessConfigUpdateReqVO updateReqVO) {
        recyclerBusinessConfigService.updateRecyclerBusinessConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除回收企业业务模式配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:delete')")
    public CommonResult<Boolean> deleteRecyclerBusinessConfig(@RequestParam("id") Long id) {
        recyclerBusinessConfigService.deleteRecyclerBusinessConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得回收企业业务模式配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:query')")
    public CommonResult<RecyclerBusinessConfigRespVO> getRecyclerBusinessConfig(@RequestParam("id") Long id) {
        RecyclerBusinessConfigDO recyclerBusinessConfig = recyclerBusinessConfigService.getRecyclerBusinessConfig(id);
        return success(BeanUtils.toBean(recyclerBusinessConfig, RecyclerBusinessConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得回收企业业务模式配置分页")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:query')")
    public CommonResult<PageResult<RecyclerBusinessConfigRespVO>> getRecyclerBusinessConfigPage(@Valid RecyclerBusinessConfigPageReqVO pageReqVO) {
        PageResult<RecyclerBusinessConfigDO> pageResult = recyclerBusinessConfigService.getRecyclerBusinessConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, RecyclerBusinessConfigRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出回收企业业务模式配置 Excel")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRecyclerBusinessConfigExcel(@Valid RecyclerBusinessConfigPageReqVO pageReqVO,
                                                  HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RecyclerBusinessConfigDO> list = recyclerBusinessConfigService.getRecyclerBusinessConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "回收企业业务模式配置.xls", "数据", RecyclerBusinessConfigRespVO.class,
                BeanUtils.toBean(list, RecyclerBusinessConfigRespVO.class));
    }

    // ==================== 业务方法 ====================

    @GetMapping("/by-enterprise")
    @Operation(summary = "根据企业ID获取业务模式配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:query')")
    public CommonResult<RecyclerBusinessConfigRespVO> getConfigByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        RecyclerBusinessConfigDO config = recyclerBusinessConfigService.getConfigByEnterpriseId(enterpriseId);
        return success(BeanUtils.toBean(config, RecyclerBusinessConfigRespVO.class));
    }

    @GetMapping("/by-business-mode")
    @Operation(summary = "根据业务模式获取配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:query')")
    public CommonResult<List<RecyclerBusinessConfigRespVO>> getConfigsByBusinessMode(@RequestParam("businessMode") Integer businessMode) {
        List<RecyclerBusinessConfigDO> list = recyclerBusinessConfigService.getConfigsByBusinessMode(businessMode);
        return success(BeanUtils.toBean(list, RecyclerBusinessConfigRespVO.class));
    }

    @GetMapping("/by-quotation-mode")
    @Operation(summary = "根据报价模式获取配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:query')")
    public CommonResult<List<RecyclerBusinessConfigRespVO>> getConfigsByQuotationMode(@RequestParam("quotationMode") Integer quotationMode) {
        List<RecyclerBusinessConfigDO> list = recyclerBusinessConfigService.getConfigsByQuotationMode(quotationMode);
        return success(BeanUtils.toBean(list, RecyclerBusinessConfigRespVO.class));
    }

    @GetMapping("/competitive-bidding-configs")
    @Operation(summary = "获取竞价模式配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:query')")
    public CommonResult<List<RecyclerBusinessConfigRespVO>> getCompetitiveBiddingConfigs() {
        List<RecyclerBusinessConfigDO> list = recyclerBusinessConfigService.getCompetitiveBiddingConfigs();
        return success(BeanUtils.toBean(list, RecyclerBusinessConfigRespVO.class));
    }

    @GetMapping("/negotiation-configs")
    @Operation(summary = "获取议价模式配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:query')")
    public CommonResult<List<RecyclerBusinessConfigRespVO>> getNegotiationConfigs() {
        List<RecyclerBusinessConfigDO> list = recyclerBusinessConfigService.getNegotiationConfigs();
        return success(BeanUtils.toBean(list, RecyclerBusinessConfigRespVO.class));
    }

    @GetMapping("/fixed-price-configs")
    @Operation(summary = "获取固定价格模式配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:query')")
    public CommonResult<List<RecyclerBusinessConfigRespVO>> getFixedPriceConfigs() {
        List<RecyclerBusinessConfigDO> list = recyclerBusinessConfigService.getFixedPriceConfigs();
        return success(BeanUtils.toBean(list, RecyclerBusinessConfigRespVO.class));
    }

    @PostMapping("/set-quotation-rules")
    @Operation(summary = "设置报价规则")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:update')")
    public CommonResult<Boolean> setQuotationRules(@RequestParam("enterpriseId") Long enterpriseId,
                                                   @RequestParam("quotationTimeoutMinutes") Integer quotationTimeoutMinutes,
                                                   @RequestParam("autoAcceptEnabled") Boolean autoAcceptEnabled,
                                                   @RequestParam("autoAcceptThreshold") String autoAcceptThreshold) {
        recyclerBusinessConfigService.setQuotationRules(enterpriseId, quotationTimeoutMinutes, autoAcceptEnabled, autoAcceptThreshold);
        return success(true);
    }

    @PostMapping("/set-negotiation-config")
    @Operation(summary = "设置价格协商配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:update')")
    public CommonResult<Boolean> setNegotiationConfig(@RequestParam("enterpriseId") Long enterpriseId,
                                                      @RequestParam("priceNegotiationEnabled") Boolean priceNegotiationEnabled,
                                                      @RequestParam("maxNegotiationRounds") Integer maxNegotiationRounds,
                                                      @RequestParam("negotiationTimeoutHours") Integer negotiationTimeoutHours) {
        recyclerBusinessConfigService.setNegotiationConfig(enterpriseId, priceNegotiationEnabled, maxNegotiationRounds, negotiationTimeoutHours);
        return success(true);
    }

    @PostMapping("/set-customer-mode")
    @Operation(summary = "设置客户模式选择")
    @PreAuthorize("@ss.hasPermission('waste:recycler-business-config:update')")
    public CommonResult<Boolean> setCustomerMode(@RequestParam("enterpriseId") Long enterpriseId,
                                                 @RequestParam("customerModeSelection") Integer customerModeSelection) {
        recyclerBusinessConfigService.setCustomerMode(enterpriseId, customerModeSelection);
        return success(true);
    }

} 