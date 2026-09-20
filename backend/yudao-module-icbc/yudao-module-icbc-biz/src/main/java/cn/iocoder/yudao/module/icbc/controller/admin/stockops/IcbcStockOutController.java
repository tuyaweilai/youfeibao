package cn.iocoder.yudao.module.icbc.controller.admin.stockops;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutSaveReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.stockops.StockOutService;
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
 * 管理后台 - 非销售出库（#54 T16，ADR 0025）。
 *
 * <p>报损 / 退货出库 / 内部领用，**不挂客户**：只有过账才减库存，重复过账幂等，
 * 作废已过账的单按相反方向冲销。
 */
@Tag(name = "管理后台 - 非销售出库")
@RestController
@RequestMapping("/icbc/stock-out")
@Validated
public class IcbcStockOutController {

    @Resource
    private StockOutService stockOutService;

    @PostMapping("/create")
    @Operation(summary = "登记非销售出库单（待过账，不动库存）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OUT_MANAGE + "')")
    public CommonResult<Long> createStockOut(@Valid @RequestBody StockOutSaveReqVO reqVO) {
        return success(stockOutService.createStockOut(reqVO));
    }

    @PostMapping("/confirm")
    @Operation(summary = "登记并过账（一次成型）", description = "过账才写库存流水；重复确认幂等")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OUT_MANAGE + "')")
    public CommonResult<Long> confirmStockOut(@Valid @RequestBody StockOutSaveReqVO reqVO) {
        return success(stockOutService.confirmStockOut(reqVO));
    }

    @PostMapping("/post")
    @Operation(summary = "过账出库单", description = "只有过账的非销售出库才减库存")
    @Parameter(name = "id", description = "出库单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OUT_MANAGE + "')")
    public CommonResult<Boolean> postStockOut(@RequestParam("id") Long id) {
        stockOutService.postStockOut(id);
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "作废出库单", description = "已过账的先按相反方向冲销库存")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OUT_MANAGE + "')")
    public CommonResult<Boolean> cancelStockOut(@Valid @RequestBody StockOutCancelReqVO reqVO) {
        stockOutService.cancelStockOut(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得出库单详情（含明细）")
    @Parameter(name = "id", description = "出库单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OUT_QUERY + "')")
    public CommonResult<StockOutRespVO> getStockOut(@RequestParam("id") Long id) {
        return success(stockOutService.getStockOut(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得非销售出库单分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OUT_QUERY + "')")
    public CommonResult<PageResult<StockOutRespVO>> getStockOutPage(@Valid StockOutPageReqVO pageReqVO) {
        return success(stockOutService.getStockOutPage(pageReqVO));
    }

}
