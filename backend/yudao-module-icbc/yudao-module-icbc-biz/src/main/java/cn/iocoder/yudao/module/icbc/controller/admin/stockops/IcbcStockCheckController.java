package cn.iocoder.yudao.module.icbc.controller.admin.stockops;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckSaveReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.stockops.StockCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 盘点调整（#54 T16，ADR 0025）。
 *
 * <p>登记实盘数，过账时把余额对齐到实盘数（盘盈 / 盘亏写流水），差额落明细可追溯。
 */
@Tag(name = "管理后台 - 盘点调整")
@RestController
@RequestMapping("/icbc/stock-check")
@Validated
public class IcbcStockCheckController {

    @Resource
    private StockCheckService stockCheckService;

    @PostMapping("/create")
    @Operation(summary = "登记盘点单（待过账，不动库存）", description = "登记时只录实盘数")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_CHECK_MANAGE + "')")
    public CommonResult<Long> createStockCheck(@Valid @RequestBody StockCheckSaveReqVO reqVO) {
        return success(stockCheckService.createStockCheck(reqVO));
    }

    @PostMapping("/confirm")
    @Operation(summary = "登记并过账（一次成型）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_CHECK_MANAGE + "')")
    public CommonResult<Long> confirmStockCheck(@Valid @RequestBody StockCheckSaveReqVO reqVO) {
        return success(stockCheckService.confirmStockCheck(reqVO));
    }

    @PostMapping("/post")
    @Operation(summary = "过账盘点单", description = "把余额对齐到实盘数，盘盈 / 盘亏写流水；账实相符不写")
    @Parameter(name = "id", description = "盘点单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_CHECK_MANAGE + "')")
    public CommonResult<Boolean> postStockCheck(@RequestParam("id") Long id) {
        stockCheckService.postStockCheck(id);
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "作废盘点单", description = "已过账的按记录的差额冲销")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_CHECK_MANAGE + "')")
    public CommonResult<Boolean> cancelStockCheck(@Valid @RequestBody StockCheckCancelReqVO reqVO) {
        stockCheckService.cancelStockCheck(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得盘点单详情（含账面 / 实盘 / 差额）")
    @Parameter(name = "id", description = "盘点单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_CHECK_QUERY + "')")
    public CommonResult<StockCheckRespVO> getStockCheck(@RequestParam("id") Long id) {
        return success(stockCheckService.getStockCheck(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得盘点单分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_CHECK_QUERY + "')")
    public CommonResult<PageResult<StockCheckRespVO>> getStockCheckPage(@Valid StockCheckPageReqVO pageReqVO) {
        return success(stockCheckService.getStockCheckPage(pageReqVO));
    }

}
