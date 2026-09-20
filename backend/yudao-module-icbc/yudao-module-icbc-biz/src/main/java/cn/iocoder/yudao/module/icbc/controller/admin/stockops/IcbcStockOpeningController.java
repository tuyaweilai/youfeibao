package cn.iocoder.yudao.module.icbc.controller.admin.stockops;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningImportReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.stockops.StockOpeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 期初导入（#54 T16）。
 *
 * <p>启用平台之前的存量先录进来，余额才可能被称作「当前库存」。同一维度只允许一条生效期初。
 */
@Tag(name = "管理后台 - 期初导入")
@RestController
@RequestMapping("/icbc/stock-opening")
@Validated
public class IcbcStockOpeningController {

    @Resource
    private StockOpeningService stockOpeningService;

    @PostMapping("/import")
    @Operation(summary = "导入期初（整批过账并写库存流水）",
            description = "一行 = 一个「品类 + 仓库 + 库位 + 批次」；同一维度已有生效期初时整批拒绝")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OPENING_MANAGE + "')")
    public CommonResult<String> importOpening(@Valid @RequestBody StockOpeningImportReqVO reqVO) {
        return success(stockOpeningService.importOpening(reqVO));
    }

    @PostMapping("/cancel")
    @Operation(summary = "作废期初", description = "冲销导入时加的库存；作废后该维度可重新导入")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OPENING_MANAGE + "')")
    public CommonResult<Boolean> cancelOpening(@Valid @RequestBody StockOpeningCancelReqVO reqVO) {
        stockOpeningService.cancelOpening(reqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得期初记录分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_OPENING_QUERY + "')")
    public CommonResult<PageResult<StockOpeningRespVO>> getOpeningPage(@Valid StockOpeningPageReqVO pageReqVO) {
        return success(stockOpeningService.getOpeningPage(pageReqVO));
    }

}
