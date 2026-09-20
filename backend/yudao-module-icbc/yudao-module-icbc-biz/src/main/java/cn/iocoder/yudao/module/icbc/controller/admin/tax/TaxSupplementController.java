package cn.iocoder.yudao.module.icbc.controller.admin.tax;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPayReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementSummaryRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.tax.TaxSupplementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 需补缴税费（issue #13）。
 */
@Tag(name = "管理后台 - 需补缴税费")
@RestController
@RequestMapping("/icbc/tax-supplement")
@Validated
public class TaxSupplementController {

    @Resource
    private TaxSupplementService taxSupplementService;

    @PostMapping("/create")
    @Operation(summary = "登记一条需补缴税费")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TAX_DECLARATION_MANAGE + "')")
    public CommonResult<TaxSupplementRespVO> create(@Valid @RequestBody TaxSupplementCreateReqVO reqVO) {
        return success(taxSupplementService.create(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得需补缴税费")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TAX_DECLARATION_QUERY + "')")
    public CommonResult<PageResult<TaxSupplementRespVO>> getPage(@Valid TaxSupplementPageReqVO reqVO) {
        return success(taxSupplementService.getPage(reqVO));
    }

    @GetMapping("/summary")
    @Operation(summary = "待补缴累计金额（按 1% 与 3% 分列）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TAX_DECLARATION_QUERY + "')")
    public CommonResult<TaxSupplementSummaryRespVO> getSummary() {
        return success(taxSupplementService.getSummary());
    }

    @PostMapping("/pay")
    @Operation(summary = "缴清一条补缴记录并归档凭证")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TAX_SUPPLEMENT_MANAGE + "')")
    public CommonResult<TaxSupplementRespVO> pay(@Valid @RequestBody TaxSupplementPayReqVO reqVO) {
        return success(taxSupplementService.recordPayment(reqVO));
    }

}
