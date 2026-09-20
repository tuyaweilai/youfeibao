package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 品类库存")
@RestController
@RequestMapping("/erp/stock")
@Validated
public class ErpStockController {

    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseService warehouseService;

    @GetMapping("/get")
    @Operation(summary = "获得品类库存")
    @Parameters({
            @Parameter(name = "id", description = "编号", example = "1"), // 方案一：传递 id
            @Parameter(name = "goodsConfigId", description = "品类编号", example = "10"), // 方案二：传递 goodsConfigId + warehouseId
            @Parameter(name = "warehouseId", description = "仓库编号", example = "2")
    })
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<ErpStockRespVO> getStock(@RequestParam(value = "id", required = false) Long id,
                                                 @RequestParam(value = "goodsConfigId", required = false) Long goodsConfigId,
                                                 @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        ErpStockDO stock = id != null ? stockService.getStock(id) : stockService.getStock(goodsConfigId, warehouseId);
        return success(BeanUtils.toBean(stock, ErpStockRespVO.class));
    }

    @GetMapping("/get-count")
    @Operation(summary = "获得品类库存数量")
    @Parameter(name = "goodsConfigId", description = "品类编号", example = "10")
    public CommonResult<BigDecimal> getStockCount(@RequestParam("goodsConfigId") Long goodsConfigId) {
        return success(stockService.getStockCount(goodsConfigId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得品类库存分页")
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<PageResult<ErpStockRespVO>> getStockPage(@Valid ErpStockPageReqVO pageReqVO) {
        PageResult<ErpStockDO> pageResult = stockService.getStockPage(pageReqVO);
        return success(buildStockVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出品类库存 Excel")
    @PreAuthorize("@ss.hasPermission('erp:stock:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockExcel(@Valid ErpStockPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpStockRespVO> list = buildStockVOPageResult(stockService.getStockPage(pageReqVO)).getList();
        // 导出 Excel
        ExcelUtils.write(response, "品类库存.xls", "数据", ErpStockRespVO.class, list);
    }

    private PageResult<ErpStockRespVO> buildStockVOPageResult(PageResult<ErpStockDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(pageResult.getList(), ErpStockDO::getWarehouseId));
        return BeanUtils.toBean(pageResult, ErpStockRespVO.class, stock -> {
            MapUtils.findAndThen(warehouseMap, stock.getWarehouseId(), warehouse -> stock.setWarehouseName(warehouse.getName()));
        });
    }

}