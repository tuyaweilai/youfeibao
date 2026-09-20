package cn.iocoder.yudao.module.icbc.controller.admin.tax;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SellerSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderHandleReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.tax.AnnualSettlementService;
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
 * 管理后台 - 出售者汇算清缴（issue #13）。
 *
 * <p>出售者须在次年 3 月 31 日前自行汇算清缴；这里生成提醒、把开票与已缴税款对账单交给他，
 * 也可以直接取某个出售者的对账单。
 */
@Tag(name = "管理后台 - 出售者汇算清缴")
@RestController
@RequestMapping("/icbc/settlement-reminder")
@Validated
public class SettlementController {

    @Resource
    private AnnualSettlementService annualSettlementService;

    @GetMapping("/page")
    @Operation(summary = "分页获得汇算清缴提醒")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SETTLEMENT_QUERY + "')")
    public CommonResult<PageResult<SettlementReminderRespVO>> getPage(@Valid SettlementReminderPageReqVO reqVO) {
        return success(annualSettlementService.getPage(reqVO));
    }

    @GetMapping("/statement")
    @Operation(summary = "取出售者某年度的开票与已缴税款对账单")
    @Parameter(name = "payeeId", description = "出售者档案编号", required = true, example = "1024")
    @Parameter(name = "taxYear", description = "纳税年度，为空取当前应提醒年度", example = "2026")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SETTLEMENT_QUERY + "')")
    public CommonResult<SellerSettlementStatementRespVO> getStatement(@RequestParam("payeeId") Long payeeId,
                                                                     @RequestParam(value = "taxYear", required = false)
                                                                     Integer taxYear) {
        return success(annualSettlementService.getStatement(payeeId, taxYear));
    }

    @PostMapping("/remind")
    @Operation(summary = "生成某纳税年度的汇算清缴提醒")
    @Parameter(name = "taxYear", description = "纳税年度，为空取当前应提醒年度", example = "2026")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SETTLEMENT_REMIND + "')")
    public CommonResult<Integer> remind(@RequestParam(value = "taxYear", required = false) Integer taxYear) {
        return success(annualSettlementService.remind(taxYear));
    }

    @PostMapping("/handle")
    @Operation(summary = "标记汇算清缴提醒为已提醒")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SETTLEMENT_REMIND + "')")
    public CommonResult<SettlementReminderRespVO> handle(@Valid @RequestBody SettlementReminderHandleReqVO reqVO) {
        return success(annualSettlementService.handle(reqVO));
    }

}
