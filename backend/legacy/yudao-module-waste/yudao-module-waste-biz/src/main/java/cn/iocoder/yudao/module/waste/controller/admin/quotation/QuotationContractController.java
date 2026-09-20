package cn.iocoder.yudao.module.waste.controller.admin.quotation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.QuotationContractGenerateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.QuotationContractGenerateRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.QuotationContractBatchGenerateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.QuotationContractBatchGenerateRespVO;
import cn.iocoder.yudao.module.waste.service.quotation.QuotationContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 基于报价的快速电子合同生成 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 基于报价的快速电子合同生成")
@RestController
@RequestMapping("/waste/quotation-contract")
@Validated
@Slf4j
public class QuotationContractController {

    @Resource
    private QuotationContractService quotationContractService;

    @PostMapping("/generate")
    @Operation(summary = "基于报价生成电子合同")
    @PreAuthorize("@ss.hasPermission('waste:contract:create')")
    public CommonResult<QuotationContractGenerateRespVO> generateContractFromQuotation(@Valid @RequestBody QuotationContractGenerateReqVO createReqVO) {
        QuotationContractGenerateRespVO respVO = quotationContractService.generateContractFromQuotation(createReqVO);
        return success(respVO);
    }

    @PostMapping("/generate-default")
    @Operation(summary = "基于报价生成电子合同（使用默认模板）")
    @PreAuthorize("@ss.hasPermission('waste:contract:create')")
    public CommonResult<QuotationContractGenerateRespVO> generateContractFromQuotationDefault(
            @Parameter(description = "报价ID", required = true) @RequestParam("quotationId") Long quotationId) {
        
        Long contractId = quotationContractService.generateContractFromQuotation(quotationId);
        
        QuotationContractGenerateRespVO respVO = new QuotationContractGenerateRespVO();
        respVO.setContractId(contractId);
        respVO.setSuccess(true);
        respVO.setMessage("基于报价生成电子合同成功（使用默认模板）");
        
        return success(respVO);
    }

    @PostMapping("/accept-and-generate")
    @Operation(summary = "一键接受报价并生成合同")
    @PreAuthorize("@ss.hasPermission('waste:contract:create')")
    public CommonResult<QuotationContractGenerateRespVO> acceptQuotationAndGenerateContract(
            @Parameter(description = "报价ID", required = true) @RequestParam("quotationId") Long quotationId,
            @Parameter(description = "合同类型ID", required = false) @RequestParam(value = "contractTypeId", required = false) Long contractTypeId,
            @Parameter(description = "模板ID", required = false) @RequestParam(value = "templateId", required = false) Long templateId) {
        
        QuotationContractGenerateRespVO respVO = quotationContractService.acceptQuotationAndGenerateContract(quotationId, contractTypeId, templateId);
        
        return success(respVO);
    }

    @PostMapping("/batch-generate")
    @Operation(summary = "批量生成合同")
    @PreAuthorize("@ss.hasPermission('waste:quotation-contract:create')")
    public CommonResult<QuotationContractBatchGenerateRespVO> batchGenerateContracts(@Valid @RequestBody QuotationContractBatchGenerateReqVO createReqVO) {
        QuotationContractBatchGenerateRespVO result = quotationContractService.batchGenerateContracts(createReqVO);
        return success(result);
    }

    @PostMapping("/initiate-signing")
    @Operation(summary = "发起合同签署流程")
    @PreAuthorize("@ss.hasPermission('waste:contract:sign')")
    public CommonResult<Long> initiateContractSigning(
            @Parameter(description = "合同ID", required = true) @RequestParam("contractId") Long contractId,
            @Parameter(description = "签署企业ID列表", required = true) @RequestParam("signerEnterpriseIds") List<Long> signerEnterpriseIds) {
        
        Long signingProcessId = quotationContractService.initiateContractSigning(contractId, signerEnterpriseIds);
        
        return success(signingProcessId);
    }

    @GetMapping("/can-generate")
    @Operation(summary = "验证报价是否可以生成合同")
    @PreAuthorize("@ss.hasPermission('waste:contract:query')")
    public CommonResult<Boolean> canGenerateContract(
            @Parameter(description = "报价ID", required = true) @RequestParam("quotationId") Long quotationId) {
        
        boolean canGenerate = quotationContractService.canGenerateContract(quotationId);
        
        return success(canGenerate);
    }

    @GetMapping("/default-contract-type")
    @Operation(summary = "获取报价对应的默认合同类型")
    @PreAuthorize("@ss.hasPermission('waste:contract:query')")
    public CommonResult<Long> getDefaultContractTypeForQuotation(
            @Parameter(description = "报价ID", required = true) @RequestParam("quotationId") Long quotationId) {
        
        Long contractTypeId = quotationContractService.getDefaultContractTypeForQuotation(quotationId);
        
        return success(contractTypeId);
    }

    @PostMapping("/fill-template")
    @Operation(summary = "填充合同模板内容")
    @PreAuthorize("@ss.hasPermission('waste:contract:query')")
    public CommonResult<String> fillContractTemplate(
            @Parameter(description = "报价ID", required = true) @RequestParam("quotationId") Long quotationId,
            @Parameter(description = "模板内容", required = true) @RequestBody String templateContent) {
        
        String filledContent = quotationContractService.fillContractTemplate(quotationId, templateContent);
        
        return success(filledContent);
    }
} 