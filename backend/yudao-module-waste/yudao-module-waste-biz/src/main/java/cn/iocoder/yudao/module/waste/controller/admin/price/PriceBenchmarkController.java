package cn.iocoder.yudao.module.waste.controller.admin.price;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import cn.iocoder.yudao.module.waste.service.price.PriceBenchmarkService;
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

@Tag(name = "管理后台 - 危险废物市场价格基准")
@RestController
@RequestMapping("/waste/price-benchmark")
@Validated
public class PriceBenchmarkController {

    @Resource
    private PriceBenchmarkService priceBenchmarkService;

    @PostMapping("/create")
    @Operation(summary = "创建危险废物市场价格基准")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:create')")
    public CommonResult<Long> createPriceBenchmark(@Valid @RequestBody PriceBenchmarkCreateReqVO createReqVO) {
        return success(priceBenchmarkService.createPriceBenchmark(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新危险废物市场价格基准")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:update')")
    public CommonResult<Boolean> updatePriceBenchmark(@Valid @RequestBody PriceBenchmarkUpdateReqVO updateReqVO) {
        priceBenchmarkService.updatePriceBenchmark(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除危险废物市场价格基准")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:delete')")
    public CommonResult<Boolean> deletePriceBenchmark(@RequestParam("id") Long id) {
        priceBenchmarkService.deletePriceBenchmark(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得危险废物市场价格基准")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:query')")
    public CommonResult<PriceBenchmarkRespVO> getPriceBenchmark(@RequestParam("id") Long id) {
        PriceBenchmarkDO priceBenchmark = priceBenchmarkService.getPriceBenchmark(id);
        return success(BeanUtils.toBean(priceBenchmark, PriceBenchmarkRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得危险废物市场价格基准分页")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:query')")
    public CommonResult<PageResult<PriceBenchmarkRespVO>> getPriceBenchmarkPage(@Valid PriceBenchmarkPageReqVO pageReqVO) {
        PageResult<PriceBenchmarkRespVO> pageResult = priceBenchmarkService.getPriceBenchmarkPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出危险废物市场价格基准 Excel")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPriceBenchmarkExcel(@Valid PriceBenchmarkPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PriceBenchmarkRespVO> list = priceBenchmarkService.getPriceBenchmarkPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "危险废物市场价格基准.xls", "数据", PriceBenchmarkRespVO.class, list);
    }

    // ==================== 业务方法 ====================

    @GetMapping("/effective-price")
    @Operation(summary = "获取生效价格基准")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:query')")
    public CommonResult<PriceBenchmarkRespVO> getEffectivePrice(@RequestParam("wasteCode") String wasteCode,
                                                               @RequestParam("region") String region) {
        PriceBenchmarkDO priceBenchmark = priceBenchmarkService.getEffectivePrice(wasteCode, region);
        return success(BeanUtils.toBean(priceBenchmark, PriceBenchmarkRespVO.class));
    }

    @GetMapping("/by-waste-code")
    @Operation(summary = "根据废物代码获取价格基准")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:query')")
    public CommonResult<List<PriceBenchmarkRespVO>> getPricesByWasteCode(@RequestParam("wasteCode") String wasteCode) {
        List<PriceBenchmarkDO> list = priceBenchmarkService.getPricesByWasteCode(wasteCode);
        return success(BeanUtils.toBean(list, PriceBenchmarkRespVO.class));
    }

    @GetMapping("/by-region")
    @Operation(summary = "根据地区获取价格基准")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:query')")
    public CommonResult<List<PriceBenchmarkRespVO>> getPricesByRegion(@RequestParam("region") String region) {
        List<PriceBenchmarkDO> list = priceBenchmarkService.getPricesByRegion(region);
        return success(BeanUtils.toBean(list, PriceBenchmarkRespVO.class));
    }

    @GetMapping("/effective-prices")
    @Operation(summary = "获取所有生效的价格基准")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:query')")
    public CommonResult<List<PriceBenchmarkRespVO>> getEffectivePrices() {
        List<PriceBenchmarkDO> list = priceBenchmarkService.getEffectivePrices();
        return success(BeanUtils.toBean(list, PriceBenchmarkRespVO.class));
    }

    @GetMapping("/expired-prices")
    @Operation(summary = "获取已过期的价格基准")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:query')")
    public CommonResult<List<PriceBenchmarkRespVO>> getExpiredPrices() {
        List<PriceBenchmarkDO> list = priceBenchmarkService.getExpiredPrices();
        return success(BeanUtils.toBean(list, PriceBenchmarkRespVO.class));
    }

    @PostMapping("/auto-expire")
    @Operation(summary = "自动过期处理")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:update')")
    public CommonResult<Boolean> autoExpireProcess() {
        priceBenchmarkService.autoExpireProcess();
        return success(true);
    }

    @PutMapping("/batch-update-status")
    @Operation(summary = "批量更新状态")
    @PreAuthorize("@ss.hasPermission('waste:price-benchmark:update')")
    public CommonResult<Boolean> batchUpdateStatus(@RequestParam("ids") List<Long> ids,
                                                   @RequestParam("status") Integer status) {
        priceBenchmarkService.batchUpdateStatus(ids, status);
        return success(true);
    }

} 