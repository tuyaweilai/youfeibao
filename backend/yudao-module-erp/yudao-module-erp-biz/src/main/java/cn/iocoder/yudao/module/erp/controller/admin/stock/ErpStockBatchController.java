package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockBatchService;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 批次")
@RestController
@RequestMapping("/erp/stock-batch")
@Validated
public class ErpStockBatchController {

    @Resource
    private ErpStockBatchService stockBatchService;

    @PostMapping("/create")
    @Operation(summary = "创建批次")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:create')")
    public CommonResult<Long> createStockBatch(@Valid @RequestBody ErpStockBatchSaveReqVO createReqVO) {
        return success(stockBatchService.createStockBatch(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新批次")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:update')")
    public CommonResult<Boolean> updateStockBatch(@Valid @RequestBody ErpStockBatchSaveReqVO updateReqVO) {
        stockBatchService.updateStockBatch(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除批次")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:delete')")
    public CommonResult<Boolean> deleteStockBatch(@RequestParam("id") Long id) {
        stockBatchService.deleteStockBatch(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得批次")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:query')")
    public CommonResult<ErpStockBatchRespVO> getStockBatch(@RequestParam("id") Long id) {
        ErpStockBatchDO batch = stockBatchService.getStockBatch(id);
        return success(BeanUtils.toBean(batch, ErpStockBatchRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得批次分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:query')")
    public CommonResult<PageResult<ErpStockBatchRespVO>> getStockBatchPage(@Valid ErpStockBatchPageReqVO pageReqVO) {
        PageResult<ErpStockBatchDO> pageResult = stockBatchService.getStockBatchPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpStockBatchRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得批次精简列表", description = "只包含被开启的批次，主要用于前端的下拉选项")
    public CommonResult<List<ErpStockBatchRespVO>> getStockBatchSimpleList() {
        List<ErpStockBatchDO> list = stockBatchService.getStockBatchListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, batch -> new ErpStockBatchRespVO().setId(batch.getId())
                .setBatchNo(batch.getBatchNo()).setGoodsConfigId(batch.getGoodsConfigId())
                .setStatus(batch.getStatus())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出批次 Excel")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockBatchExcel(@Valid ErpStockBatchPageReqVO pageReqVO,
                                      HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpStockBatchDO> list = stockBatchService.getStockBatchPage(pageReqVO).getList();
        ExcelUtils.write(response, "批次.xls", "数据", ErpStockBatchRespVO.class,
                BeanUtils.toBean(list, ErpStockBatchRespVO.class));
    }

}
