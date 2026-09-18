package cn.iocoder.yudao.module.icbc.controller.admin.evidence;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.*;
import cn.iocoder.yudao.module.icbc.enums.IcbcEvidenceTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.evidence.InvoiceEvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 一票一档证据链。
 *
 * <p>一张票 = 一笔收购。这里把票背后的五流证据展开、度量齐备率、导出证据包与收购台账。
 */
@Tag(name = "管理后台 - 一票一档证据链")
@RestController
@RequestMapping("/icbc/evidence")
@Validated
@Slf4j
public class InvoiceEvidenceController {

    @Resource
    private InvoiceEvidenceService invoiceEvidenceService;

    @GetMapping("/get")
    @Operation(summary = "获得一张票的五流证据链")
    @Parameter(name = "partnerOrderId", description = "合作方订单号", required = true)
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_QUERY + "')")
    public CommonResult<EvidenceChainRespVO> getEvidenceChain(@RequestParam("partnerOrderId") String partnerOrderId) {
        return success(invoiceEvidenceService.getEvidenceChain(partnerOrderId));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得一票一档（含齐备率）")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_QUERY + "')")
    public CommonResult<PageResult<EvidenceChainRespVO>> getEvidencePage(@Valid EvidencePageReqVO pageReqVO) {
        return success(invoiceEvidenceService.getEvidencePage(pageReqVO));
    }

    @GetMapping("/completeness")
    @Operation(summary = "获得批量齐备率")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_QUERY + "')")
    public CommonResult<EvidenceCompletenessSummaryRespVO> getCompleteness(@Valid EvidenceScopeReqVO scopeReqVO) {
        return success(invoiceEvidenceService.getCompleteness(scopeReqVO));
    }

    @GetMapping("/ledger")
    @Operation(summary = "获得收购台账")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_QUERY + "')")
    public CommonResult<List<AcquisitionLedgerRespVO>> getLedger(@Valid AcquisitionLedgerReqVO reqVO) {
        return success(invoiceEvidenceService.getLedgerRows(reqVO));
    }

    @GetMapping("/types")
    @Operation(summary = "获得可补录的证据类型")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_QUERY + "')")
    public CommonResult<List<EvidenceTypeRespVO>> getEvidenceTypes() {
        List<EvidenceTypeRespVO> types = Arrays.stream(IcbcEvidenceTypeEnum.values()).map(type -> {
            EvidenceTypeRespVO vo = new EvidenceTypeRespVO();
            vo.setCode(type.getCode());
            vo.setName(type.getName());
            vo.setFlow(type.getFlow().getCode());
            vo.setFlowName(type.getFlow().getName());
            return vo;
        }).collect(Collectors.toList());
        return success(types);
    }

    @PostMapping("/attach")
    @Operation(summary = "补录一条证据")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_ATTACH + "')")
    public CommonResult<Long> attachEvidence(@Valid @RequestBody EvidenceAttachReqVO reqVO) {
        return success(invoiceEvidenceService.attachEvidence(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除一条证据")
    @Parameter(name = "id", description = "证据ID", required = true)
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_DELETE + "')")
    public CommonResult<Boolean> deleteEvidence(@RequestParam("id") Long id) {
        invoiceEvidenceService.deleteEvidence(id);
        return success(true);
    }

    @GetMapping("/export")
    @Operation(summary = "导出单张票的证据包")
    @Parameter(name = "partnerOrderId", description = "合作方订单号", required = true)
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_EXPORT + "')")
    public void exportEvidencePackage(@RequestParam("partnerOrderId") String partnerOrderId,
                                      HttpServletResponse response) {
        invoiceEvidenceService.exportEvidencePackage(partnerOrderId, response);
    }

    @PostMapping("/export-batch")
    @Operation(summary = "批量导出证据包")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_EXPORT + "')")
    public void exportEvidencePackageBatch(@Valid @RequestBody EvidenceScopeReqVO scopeReqVO,
                                           HttpServletResponse response) {
        invoiceEvidenceService.exportEvidencePackageBatch(scopeReqVO, response);
    }

    @GetMapping("/ledger/export")
    @Operation(summary = "导出收购台账")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.EVIDENCE_EXPORT + "')")
    public void exportAcquisitionLedger(@Valid AcquisitionLedgerReqVO reqVO, HttpServletResponse response) {
        invoiceEvidenceService.exportAcquisitionLedger(reqVO, response);
    }

}
