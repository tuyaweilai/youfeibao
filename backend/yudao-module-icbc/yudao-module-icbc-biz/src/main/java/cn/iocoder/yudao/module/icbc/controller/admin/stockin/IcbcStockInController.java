package cn.iocoder.yudao.module.icbc.controller.admin.stockin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInSaveReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.stockin.StockInService;
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
 * 管理后台 - 待入库与入库单（#52 T14，ADR 0027）。
 *
 * <p>入库是收购单派生的**单向动作**：验收后的货进待入库，仓管选仓库 / 库位 / 批次确认实际入库量。
 * 只有过账的入库才增加正式库存；重复确认幂等，累计入库不得超过可入库实物量。
 */
@Tag(name = "管理后台 - 待入库与入库单")
@RestController
@RequestMapping("/icbc/stock-in")
@Validated
public class IcbcStockInController {

    @Resource
    private StockInService stockInService;

    @GetMapping("/pending/page")
    @Operation(summary = "待入库分页", description = "已验收（已归入结算单）、未作废、且尚未全部入库的收购单；"
            + "带可入库实物量 / 累计入库 / 剩余可入库")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_IN_QUERY + "')")
    public CommonResult<PageResult<StockInPendingRespVO>> getPendingPage(
            @Valid StockInPendingPageReqVO pageReqVO) {
        return success(stockInService.getPendingPage(pageReqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "建入库单（待过账，不动库存）", description = "可拆多个库位 / 批次，累计不得超过可入库实物量")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_IN_MANAGE + "')")
    public CommonResult<Long> createStockIn(@Valid @RequestBody StockInSaveReqVO reqVO) {
        return success(stockInService.createStockIn(reqVO));
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认入库（建单并过账，仓管一次成型）",
            description = "过账才写库存流水；重复确认幂等，不重复加库存")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_IN_MANAGE + "')")
    public CommonResult<Long> confirmStockIn(@Valid @RequestBody StockInSaveReqVO reqVO) {
        return success(stockInService.confirmStockIn(reqVO));
    }

    @PostMapping("/post")
    @Operation(summary = "过账入库单", description = "只有过账的入库才增加正式库存")
    @Parameter(name = "id", description = "入库单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_IN_MANAGE + "')")
    public CommonResult<Boolean> postStockIn(@RequestParam("id") Long id) {
        stockInService.postStockIn(id);
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "作废入库单", description = "已过账的先按相反方向冲销库存（RECEIPT_IN_CANCEL）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_IN_MANAGE + "')")
    public CommonResult<Boolean> cancelStockIn(@Valid @RequestBody StockInCancelReqVO reqVO) {
        stockInService.cancelStockIn(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得入库单详情（含明细）")
    @Parameter(name = "id", description = "入库单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_IN_QUERY + "')")
    public CommonResult<StockInRespVO> getStockIn(@RequestParam("id") Long id) {
        return success(stockInService.getStockIn(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得入库单分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_IN_QUERY + "')")
    public CommonResult<PageResult<StockInRespVO>> getStockInPage(@Valid StockInPageReqVO pageReqVO) {
        return success(stockInService.getStockInPage(pageReqVO));
    }

}
