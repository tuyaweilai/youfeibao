package cn.iocoder.yudao.module.icbc.controller.admin.tax;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.*;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.tax.TaxDeclarationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 代办税费申报（issue #13）。
 *
 * <p>财务按月看清单与合计金额、申报、缴款；申报期临近与逾期未缴的预警也在这里。
 * 金额口径由服务层单点持有，本控制器只做暴露与鉴权。
 */
@Tag(name = "管理后台 - 代办税费申报")
@RestController
@RequestMapping("/icbc/tax-declaration")
@Validated
public class TaxDeclarationController {

    @Resource
    private TaxDeclarationService taxDeclarationService;

    @GetMapping("/get")
    @Operation(summary = "取某申报月的代办税费申报单（未生成则即时生成）")
    @Parameter(name = "periodMonth", description = "申报月 yyyy-MM", required = true, example = "2026-08")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.TAX_DECLARATION_QUERY + "')")
    public CommonResult<TaxDeclarationRespVO> getDeclaration(@RequestParam("periodMonth") String periodMonth) {
        return success(taxDeclarationService.getDeclaration(periodMonth));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得代办税费申报单")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.TAX_DECLARATION_QUERY + "')")
    public CommonResult<PageResult<TaxDeclarationRespVO>> getDeclarationPage(@Valid TaxDeclarationPageReqVO reqVO) {
        return success(taxDeclarationService.getDeclarationPage(reqVO));
    }

    @GetMapping("/item/page")
    @Operation(summary = "分页获得代办税费申报明细（含当月超 10 万元的出售者）")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.TAX_DECLARATION_QUERY + "')")
    public CommonResult<PageResult<TaxDeclarationItemRespVO>> getItemPage(@Valid TaxDeclarationItemPageReqVO reqVO) {
        return success(taxDeclarationService.getItemPage(reqVO));
    }

    @GetMapping("/precheck")
    @Operation(summary = "检查申报数据齐备性：还缺哪些数据")
    @Parameter(name = "periodMonth", description = "申报月 yyyy-MM", required = true, example = "2026-08")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.TAX_DECLARATION_QUERY + "')")
    public CommonResult<TaxDeclarationPrecheckRespVO> precheck(@RequestParam("periodMonth") String periodMonth) {
        return success(taxDeclarationService.precheck(periodMonth));
    }

    @GetMapping("/warning/list")
    @Operation(summary = "获得待处理的申报预警（申报期临近 / 逾期可能被暂停开票资格 / 数据不齐）")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.TAX_DECLARATION_QUERY + "')")
    public CommonResult<List<TaxDeclarationWarningRespVO>> getWarnings() {
        return success(taxDeclarationService.getWarnings());
    }

    @PostMapping("/generate")
    @Operation(summary = "生成（或刷新）某申报月的申报清单与合计金额")
    @Parameter(name = "periodMonth", description = "申报月 yyyy-MM", required = true, example = "2026-08")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.TAX_DECLARATION_MANAGE + "')")
    public CommonResult<TaxDeclarationRespVO> generate(@RequestParam("periodMonth") String periodMonth) {
        return success(taxDeclarationService.generate(periodMonth));
    }

    @PostMapping("/declare")
    @Operation(summary = "报送《代办税费报告表》《代办税费明细报告表》")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.TAX_DECLARATION_MANAGE + "')")
    public CommonResult<TaxDeclarationRespVO> declare(@Valid @RequestBody TaxDeclarationDeclareReqVO reqVO) {
        return success(taxDeclarationService.declare(reqVO));
    }

    @PostMapping("/pay")
    @Operation(summary = "缴款成功：归档凭证并与对应发票关联")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.TAX_DECLARATION_MANAGE + "')")
    public CommonResult<TaxDeclarationRespVO> pay(@Valid @RequestBody TaxDeclarationPayReqVO reqVO) {
        return success(taxDeclarationService.recordPayment(reqVO));
    }

}
