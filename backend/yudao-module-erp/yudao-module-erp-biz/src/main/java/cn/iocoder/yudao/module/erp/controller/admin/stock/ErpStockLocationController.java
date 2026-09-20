package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLocationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockLocationService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
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
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 库位")
@RestController
@RequestMapping("/erp/stock-location")
@Validated
public class ErpStockLocationController {

    @Resource
    private ErpStockLocationService stockLocationService;
    @Resource
    private ErpWarehouseService warehouseService;

    @PostMapping("/create")
    @Operation(summary = "创建库位")
    @PreAuthorize("@ss.hasPermission('erp:stock-location:create')")
    public CommonResult<Long> createStockLocation(@Valid @RequestBody ErpStockLocationSaveReqVO createReqVO) {
        return success(stockLocationService.createStockLocation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新库位")
    @PreAuthorize("@ss.hasPermission('erp:stock-location:update')")
    public CommonResult<Boolean> updateStockLocation(@Valid @RequestBody ErpStockLocationSaveReqVO updateReqVO) {
        stockLocationService.updateStockLocation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除库位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-location:delete')")
    public CommonResult<Boolean> deleteStockLocation(@RequestParam("id") Long id) {
        stockLocationService.deleteStockLocation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得库位")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-location:query')")
    public CommonResult<ErpStockLocationRespVO> getStockLocation(@RequestParam("id") Long id) {
        ErpStockLocationDO location = stockLocationService.getStockLocation(id);
        return success(BeanUtils.toBean(location, ErpStockLocationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库位分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-location:query')")
    public CommonResult<PageResult<ErpStockLocationRespVO>> getStockLocationPage(
            @Valid ErpStockLocationPageReqVO pageReqVO) {
        PageResult<ErpStockLocationDO> pageResult = stockLocationService.getStockLocationPage(pageReqVO);
        return success(buildLocationVOPageResult(pageResult));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得库位精简列表", description = "只包含被开启的库位，可按仓库筛选，主要用于前端的下拉选项")
    @Parameter(name = "warehouseId", description = "仓库编号", example = "1024")
    public CommonResult<List<ErpStockLocationRespVO>> getStockLocationSimpleList(
            @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        List<ErpStockLocationDO> list = warehouseId != null
                ? stockLocationService.getStockLocationListByWarehouseId(warehouseId)
                : stockLocationService.getStockLocationListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, location -> new ErpStockLocationRespVO().setId(location.getId())
                .setWarehouseId(location.getWarehouseId()).setName(location.getName())
                .setStatus(location.getStatus())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出库位 Excel")
    @PreAuthorize("@ss.hasPermission('erp:stock-location:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockLocationExcel(@Valid ErpStockLocationPageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpStockLocationDO> list = stockLocationService.getStockLocationPage(pageReqVO).getList();
        ExcelUtils.write(response, "库位.xls", "数据", ErpStockLocationRespVO.class,
                BeanUtils.toBean(list, ErpStockLocationRespVO.class));
    }

    private PageResult<ErpStockLocationRespVO> buildLocationVOPageResult(PageResult<ErpStockLocationDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertList(pageResult.getList(), ErpStockLocationDO::getWarehouseId));
        return BeanUtils.toBean(pageResult, ErpStockLocationRespVO.class, location ->
                MapUtils.findAndThen(warehouseMap, location.getWarehouseId(),
                        warehouse -> location.setWarehouseName(warehouse.getName())));
    }

}
