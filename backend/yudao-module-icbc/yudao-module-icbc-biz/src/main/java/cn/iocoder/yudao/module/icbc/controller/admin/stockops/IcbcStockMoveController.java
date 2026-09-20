package cn.iocoder.yudao.module.icbc.controller.admin.stockops;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMovePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveSaveReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.stockops.StockMoveService;
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
 * 管理后台 - 跨仓调拨（#54 T16，ADR 0025）。
 *
 * <p>源减目标加，余额与流水始终一致；过账经 {@code StockApi#move} 一次写两条流水。
 */
@Tag(name = "管理后台 - 跨仓调拨")
@RestController
@RequestMapping("/icbc/stock-move")
@Validated
public class IcbcStockMoveController {

    @Resource
    private StockMoveService stockMoveService;

    @PostMapping("/create")
    @Operation(summary = "登记调拨单（待过账，不动库存）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_MOVE_MANAGE + "')")
    public CommonResult<Long> createStockMove(@Valid @RequestBody StockMoveSaveReqVO reqVO) {
        return success(stockMoveService.createStockMove(reqVO));
    }

    @PostMapping("/confirm")
    @Operation(summary = "登记并过账（一次成型）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_MOVE_MANAGE + "')")
    public CommonResult<Long> confirmStockMove(@Valid @RequestBody StockMoveSaveReqVO reqVO) {
        return success(stockMoveService.confirmStockMove(reqVO));
    }

    @PostMapping("/post")
    @Operation(summary = "过账调拨单", description = "源库存不足时整单回滚，不做部分调拨")
    @Parameter(name = "id", description = "调拨单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_MOVE_MANAGE + "')")
    public CommonResult<Boolean> postStockMove(@RequestParam("id") Long id) {
        stockMoveService.postStockMove(id);
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "作废调拨单", description = "已过账的先按相反方向调回")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_MOVE_MANAGE + "')")
    public CommonResult<Boolean> cancelStockMove(@Valid @RequestBody StockMoveCancelReqVO reqVO) {
        stockMoveService.cancelStockMove(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得调拨单详情（含明细）")
    @Parameter(name = "id", description = "调拨单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_MOVE_QUERY + "')")
    public CommonResult<StockMoveRespVO> getStockMove(@RequestParam("id") Long id) {
        return success(stockMoveService.getStockMove(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得跨仓调拨单分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_MOVE_QUERY + "')")
    public CommonResult<PageResult<StockMoveRespVO>> getStockMovePage(@Valid StockMovePageReqVO pageReqVO) {
        return success(stockMoveService.getStockMovePage(pageReqVO));
    }

}
