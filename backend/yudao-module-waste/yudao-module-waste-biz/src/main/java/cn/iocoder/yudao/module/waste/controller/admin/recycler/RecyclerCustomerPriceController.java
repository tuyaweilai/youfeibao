package cn.iocoder.yudao.module.waste.controller.admin.recycler;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerCustomerPriceDO;
import cn.iocoder.yudao.module.waste.service.recycler.RecyclerCustomerPriceService;
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
import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 回收企业客户专属价格配置")
@RestController
@RequestMapping("/waste/recycler-customer-price")
@Validated
public class RecyclerCustomerPriceController {

    @Resource
    private RecyclerCustomerPriceService recyclerCustomerPriceService;

    @PostMapping("/create")
    @Operation(summary = "创建回收企业客户专属价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:create')")
    public CommonResult<Long> createRecyclerCustomerPrice(@Valid @RequestBody RecyclerCustomerPriceCreateReqVO createReqVO) {
        return success(recyclerCustomerPriceService.createRecyclerCustomerPrice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新回收企业客户专属价格配置")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:update')")
    public CommonResult<Boolean> updateRecyclerCustomerPrice(@Valid @RequestBody RecyclerCustomerPriceUpdateReqVO updateReqVO) {
        recyclerCustomerPriceService.updateRecyclerCustomerPrice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除回收企业客户专属价格配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:delete')")
    public CommonResult<Boolean> deleteRecyclerCustomerPrice(@RequestParam("id") Long id) {
        recyclerCustomerPriceService.deleteRecyclerCustomerPrice(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得回收企业客户专属价格配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:query')")
    public CommonResult<RecyclerCustomerPriceRespVO> getRecyclerCustomerPrice(@RequestParam("id") Long id) {
        RecyclerCustomerPriceDO recyclerCustomerPrice = recyclerCustomerPriceService.getRecyclerCustomerPrice(id);
        return success(BeanUtils.toBean(recyclerCustomerPrice, RecyclerCustomerPriceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得回收企业客户专属价格配置分页")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:query')")
    public CommonResult<PageResult<RecyclerCustomerPriceRespVO>> getRecyclerCustomerPricePage(@Valid RecyclerCustomerPricePageReqVO pageReqVO) {
        PageResult<RecyclerCustomerPriceDO> pageResult = recyclerCustomerPriceService.getRecyclerCustomerPricePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, RecyclerCustomerPriceRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出回收企业客户专属价格配置 Excel")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRecyclerCustomerPriceExcel(@Valid RecyclerCustomerPricePageReqVO pageReqVO,
                                                 HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RecyclerCustomerPriceDO> list = recyclerCustomerPriceService.getRecyclerCustomerPricePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "回收企业客户专属价格配置.xls", "数据", RecyclerCustomerPriceRespVO.class,
                BeanUtils.toBean(list, RecyclerCustomerPriceRespVO.class));
    }

    // ==================== 业务方法 ====================

    @GetMapping("/calculate-price")
    @Operation(summary = "计算专属价格")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:query')")
    public CommonResult<BigDecimal> calculatePrice(@RequestParam("recyclerEnterpriseId") Long recyclerEnterpriseId,
                                                   @RequestParam("customerEnterpriseId") Long customerEnterpriseId,
                                                   @RequestParam("wasteCode") String wasteCode,
                                                   @RequestParam("quantity") BigDecimal quantity) {
        BigDecimal price = recyclerCustomerPriceService.calculatePrice(recyclerEnterpriseId, customerEnterpriseId, wasteCode, quantity);
        return success(price);
    }

    @GetMapping("/by-recycler-customer")
    @Operation(summary = "根据回收企业和客户获取专属价格")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:query')")
    public CommonResult<List<RecyclerCustomerPriceRespVO>> getPricesByRecyclerAndCustomer(@RequestParam("recyclerEnterpriseId") Long recyclerEnterpriseId,
                                                                                          @RequestParam("customerEnterpriseId") Long customerEnterpriseId) {
        List<RecyclerCustomerPriceDO> list = recyclerCustomerPriceService.getPricesByRecyclerAndCustomer(recyclerEnterpriseId, customerEnterpriseId);
        return success(BeanUtils.toBean(list, RecyclerCustomerPriceRespVO.class));
    }

    @GetMapping("/by-waste-code")
    @Operation(summary = "根据废物代码获取专属价格")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:query')")
    public CommonResult<List<RecyclerCustomerPriceRespVO>> getPricesByWasteCode(@RequestParam("wasteCode") String wasteCode) {
        List<RecyclerCustomerPriceDO> list = recyclerCustomerPriceService.getPricesByWasteCode(wasteCode);
        return success(BeanUtils.toBean(list, RecyclerCustomerPriceRespVO.class));
    }

    @GetMapping("/by-price-type")
    @Operation(summary = "根据价格类型获取专属价格")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:query')")
    public CommonResult<List<RecyclerCustomerPriceRespVO>> getPricesByPriceType(@RequestParam("priceType") Integer priceType) {
        List<RecyclerCustomerPriceDO> list = recyclerCustomerPriceService.getPricesByPriceType(priceType);
        return success(BeanUtils.toBean(list, RecyclerCustomerPriceRespVO.class));
    }

    @GetMapping("/effective-prices")
    @Operation(summary = "获取生效的专属价格")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:query')")
    public CommonResult<List<RecyclerCustomerPriceRespVO>> getEffectivePrices() {
        List<RecyclerCustomerPriceDO> list = recyclerCustomerPriceService.getEffectivePrices();
        return success(BeanUtils.toBean(list, RecyclerCustomerPriceRespVO.class));
    }

    @GetMapping("/by-contract")
    @Operation(summary = "根据合同获取专属价格")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:query')")
    public CommonResult<List<RecyclerCustomerPriceRespVO>> getPricesByContract(@RequestParam("contractId") Long contractId) {
        List<RecyclerCustomerPriceDO> list = recyclerCustomerPriceService.getPricesByContract(contractId);
        return success(BeanUtils.toBean(list, RecyclerCustomerPriceRespVO.class));
    }

    @PostMapping("/set-contract-price")
    @Operation(summary = "设置合同关联价格")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:update')")
    public CommonResult<Boolean> setContractPrice(@RequestParam("id") Long id,
                                                  @RequestParam("contractId") Long contractId) {
        recyclerCustomerPriceService.setContractPrice(id, contractId);
        return success(true);
    }

    @PutMapping("/batch-update-status")
    @Operation(summary = "批量更新状态")
    @PreAuthorize("@ss.hasPermission('waste:recycler-customer-price:update')")
    public CommonResult<Boolean> batchUpdateStatus(@RequestParam("ids") List<Long> ids,
                                                   @RequestParam("status") Integer status) {
        recyclerCustomerPriceService.batchUpdateStatus(ids, status);
        return success(true);
    }

} 