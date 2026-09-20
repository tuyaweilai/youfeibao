package cn.iocoder.yudao.module.icbc.controller.admin.platform;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceCompletenessSummaryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.platform.vo.PlatformExceptionInvoiceRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.platform.PlatformEvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 平台运营：全平台五流齐备率与异常票（#15）。
 *
 * <p>只有平台运营角色（或超管）能进；回收企业租户内的任何角色都进不来。
 */
@Tag(name = "管理后台 - 平台运营：全平台证据与异常票")
@RestController
@RequestMapping("/icbc/platform/evidence")
@Validated
public class PlatformEvidenceController {

    @Resource
    private PlatformEvidenceService platformEvidenceService;

    @GetMapping("/completeness")
    @Operation(summary = "获得全平台五流齐备率（跨租户）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_EVIDENCE_QUERY + "')")
    public CommonResult<EvidenceCompletenessSummaryRespVO> getPlatformCompleteness() {
        return success(platformEvidenceService.getPlatformCompleteness());
    }

    @GetMapping("/exception-invoice/list")
    @Operation(summary = "获得全平台异常票清单（跨租户）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_EVIDENCE_QUERY + "')")
    public CommonResult<List<PlatformExceptionInvoiceRespVO>> getExceptionInvoiceList() {
        return success(platformEvidenceService.getExceptionInvoiceList());
    }

}
