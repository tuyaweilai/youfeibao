package cn.iocoder.yudao.module.icbc.controller.admin.platform;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.billing.vo.IcbcBillingLedgerPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.billing.vo.IcbcBillingLedgerRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.billing.IcbcBillingLedgerDO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.billing.PlatformBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 平台运营：计费计量（#16）。
 *
 * <p>按成功开具的报废产品收购发票张数计量，运营可按租户、按期间查看计量结果与应计费用。
 * 计量台账是平台自己的账，回收企业租户没有任何读写入口。
 */
@Tag(name = "管理后台 - 平台运营：计费计量")
@RestController
@RequestMapping("/icbc/platform/billing")
@Validated
public class PlatformBillingController {

    @Resource
    private PlatformBillingService platformBillingService;

    @GetMapping("/page")
    @Operation(summary = "分页获得计费计量台账（跨租户）")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.PLATFORM_BILLING_QUERY + "')")
    public CommonResult<PageResult<IcbcBillingLedgerRespVO>> getBillingPage(
            @Valid IcbcBillingLedgerPageReqVO pageReqVO) {
        PageResult<IcbcBillingLedgerDO> page = platformBillingService.getPage(pageReqVO);
        return success(BeanUtils.toBean(page, IcbcBillingLedgerRespVO.class));
    }

    @PostMapping("/generate")
    @Operation(summary = "重新计量某期间全部租户并落台账")
    @Parameter(name = "periodMonth", description = "计费期间（yyyy-MM）", required = true, example = "2026-09")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.PLATFORM_BILLING_MANAGE + "')")
    public CommonResult<List<IcbcBillingLedgerRespVO>> generate(
            @RequestParam("periodMonth") String periodMonth) {
        List<IcbcBillingLedgerRespVO> list = platformBillingService.generate(periodMonth).stream()
                .map(ledger -> BeanUtils.toBean(ledger, IcbcBillingLedgerRespVO.class))
                .collect(Collectors.toList());
        return success(list);
    }

    @PostMapping("/generate-tenant")
    @Operation(summary = "重新计量某个租户某期间并落台账")
    @Parameter(name = "tenantId", description = "租户编号", required = true, example = "1")
    @Parameter(name = "periodMonth", description = "计费期间（yyyy-MM）", required = true, example = "2026-09")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.PLATFORM_BILLING_MANAGE + "')")
    public CommonResult<IcbcBillingLedgerRespVO> generateTenant(
            @RequestParam("tenantId") Long tenantId,
            @RequestParam("periodMonth") String periodMonth) {
        return success(BeanUtils.toBean(platformBillingService.generate(tenantId, periodMonth),
                IcbcBillingLedgerRespVO.class));
    }

}
