package cn.iocoder.yudao.module.waste.controller.admin.price;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.RecyclerPriceConfigDO;
import cn.iocoder.yudao.module.waste.service.price.RecyclerPriceConfigService;
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

@Tag(name = "管理后台 - 回收企业价格配置")
@RestController
@RequestMapping("/waste/recycler-price-config")
@Validated
public class RecyclerPriceConfigController {

    @Resource
    private RecyclerPriceConfigService recyclerPriceConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建回收企业价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:create')")
    public CommonResult<Long> createRecyclerPriceConfig(@Valid @RequestBody RecyclerPriceConfigCreateReqVO createReqVO) {
        return success(recyclerPriceConfigService.createRecyclerPriceConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新回收企业价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:update')")
    public CommonResult<Boolean> updateRecyclerPriceConfig(@Valid @RequestBody RecyclerPriceConfigUpdateReqVO updateReqVO) {
        recyclerPriceConfigService.updateRecyclerPriceConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除回收企业价格配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:delete')")
    public CommonResult<Boolean> deleteRecyclerPriceConfig(@RequestParam("id") Long id) {
        recyclerPriceConfigService.deleteRecyclerPriceConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得回收企业价格配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:query')")
    public CommonResult<RecyclerPriceConfigRespVO> getRecyclerPriceConfig(@RequestParam("id") Long id) {
        RecyclerPriceConfigDO recyclerPriceConfig = recyclerPriceConfigService.getRecyclerPriceConfig(id);
        return success(BeanUtils.toBean(recyclerPriceConfig, RecyclerPriceConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得回收企业价格配置分页")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:query')")
    public CommonResult<PageResult<RecyclerPriceConfigRespVO>> getRecyclerPriceConfigPage(@Valid RecyclerPriceConfigPageReqVO pageReqVO) {
        PageResult<RecyclerPriceConfigRespVO> pageResult = recyclerPriceConfigService.getRecyclerPriceConfigPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出回收企业价格配置 Excel")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRecyclerPriceConfigExcel(@Valid RecyclerPriceConfigPageReqVO pageReqVO,
                                               HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RecyclerPriceConfigRespVO> list = recyclerPriceConfigService.getRecyclerPriceConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "回收企业价格配置.xls", "数据", RecyclerPriceConfigRespVO.class, list);
    }

    // ==================== 业务方法 ====================

    @GetMapping("/effective-config")
    @Operation(summary = "获取生效价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:query')")
    public CommonResult<RecyclerPriceConfigRespVO> getEffectiveConfig(@RequestParam("enterpriseId") Long enterpriseId,
                                                                     @RequestParam("wasteCode") String wasteCode,
                                                                     @RequestParam("region") String region) {
        RecyclerPriceConfigDO config = recyclerPriceConfigService.getEffectiveConfig(enterpriseId, wasteCode, region);
        return success(BeanUtils.toBean(config, RecyclerPriceConfigRespVO.class));
    }

    @GetMapping("/by-enterprise")
    @Operation(summary = "根据企业ID获取价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:query')")
    public CommonResult<List<RecyclerPriceConfigRespVO>> getConfigsByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        List<RecyclerPriceConfigDO> list = recyclerPriceConfigService.getConfigsByEnterpriseId(enterpriseId);
        return success(BeanUtils.toBean(list, RecyclerPriceConfigRespVO.class));
    }

    @GetMapping("/by-waste-code")
    @Operation(summary = "根据废物代码获取价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:query')")
    public CommonResult<List<RecyclerPriceConfigRespVO>> getConfigsByWasteCode(@RequestParam("wasteCode") String wasteCode) {
        List<RecyclerPriceConfigDO> list = recyclerPriceConfigService.getConfigsByWasteCode(wasteCode);
        return success(BeanUtils.toBean(list, RecyclerPriceConfigRespVO.class));
    }

    @GetMapping("/by-region")
    @Operation(summary = "根据地区获取价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:query')")
    public CommonResult<List<RecyclerPriceConfigRespVO>> getConfigsByRegion(@RequestParam("region") String region) {
        List<RecyclerPriceConfigDO> list = recyclerPriceConfigService.getConfigsByRegion(region);
        return success(BeanUtils.toBean(list, RecyclerPriceConfigRespVO.class));
    }

    @GetMapping("/effective-configs")
    @Operation(summary = "获取所有生效的价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:query')")
    public CommonResult<List<RecyclerPriceConfigRespVO>> getEffectiveConfigs() {
        List<RecyclerPriceConfigDO> list = recyclerPriceConfigService.getEffectiveConfigs();
        return success(BeanUtils.toBean(list, RecyclerPriceConfigRespVO.class));
    }

    @GetMapping("/negotiable-configs")
    @Operation(summary = "获取议价配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:query')")
    public CommonResult<List<RecyclerPriceConfigRespVO>> getNegotiableConfigs() {
        List<RecyclerPriceConfigDO> list = recyclerPriceConfigService.getNegotiableConfigs();
        return success(BeanUtils.toBean(list, RecyclerPriceConfigRespVO.class));
    }

    @PostMapping("/auto-expire")
    @Operation(summary = "自动过期处理")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:update')")
    public CommonResult<Boolean> autoExpireProcess() {
        recyclerPriceConfigService.autoExpireProcess();
        return success(true);
    }

    @PutMapping("/batch-update-status")
    @Operation(summary = "批量更新状态")
    @PreAuthorize("@ss.hasPermission('waste:recycler-price-config:update')")
    public CommonResult<Boolean> batchUpdateStatus(@RequestParam("ids") List<Long> ids,
                                                   @RequestParam("status") Integer status) {
        recyclerPriceConfigService.batchUpdateStatus(ids, status);
        return success(true);
    }

} 