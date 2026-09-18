package cn.iocoder.yudao.module.waste.controller.admin.quotation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.SmartQuotationGenerateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.SmartQuotationGenerateRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.SmartQuotationBatchGenerateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.SmartQuotationBatchGenerateRespVO;
import cn.iocoder.yudao.module.waste.service.quotation.SmartQuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 智能报价生成 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 智能报价生成")
@RestController
@RequestMapping("/waste/smart-quotation")
@Validated
@Slf4j
public class SmartQuotationController {

    @Resource
    private SmartQuotationService smartQuotationService;

    @PostMapping("/generate")
    @Operation(summary = "生成智能报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:create')")
    public CommonResult<SmartQuotationGenerateRespVO> generateSmartQuotation(@Valid @RequestBody SmartQuotationGenerateReqVO createReqVO) {
        BigDecimal quotedPrice = smartQuotationService.generateSmartQuotation(
                createReqVO.getAppointmentId(),
                createReqVO.getRecyclingEnterpriseId(),
                createReqVO.getWasteCode(),
                createReqVO.getQuantity(),
                createReqVO.getCustomerEnterpriseId(),
                createReqVO.getRegion()
        );
        
        SmartQuotationGenerateRespVO respVO = new SmartQuotationGenerateRespVO();
        respVO.setQuotedPrice(quotedPrice);
        respVO.setSuccess(quotedPrice.compareTo(BigDecimal.ZERO) > 0);
        respVO.setMessage(quotedPrice.compareTo(BigDecimal.ZERO) > 0 ? "智能报价生成成功" : "未找到适用的价格策略");
        
        return success(respVO);
    }

    @PostMapping("/batch-generate")
    @Operation(summary = "批量生成智能报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:create')")
    public CommonResult<SmartQuotationBatchGenerateRespVO> batchGenerateSmartQuotations(@Valid @RequestBody SmartQuotationBatchGenerateReqVO createReqVO) {
        List<Long> quotationIds = smartQuotationService.batchGenerateSmartQuotations(
                createReqVO.getAppointmentId(),
                createReqVO.getRecyclingEnterpriseIds(),
                createReqVO.getWasteCode(),
                createReqVO.getQuantity(),
                createReqVO.getCustomerEnterpriseId(),
                createReqVO.getRegion()
        );
        
        SmartQuotationBatchGenerateRespVO respVO = new SmartQuotationBatchGenerateRespVO();
        respVO.setQuotationIds(quotationIds);
        respVO.setSuccessCount(quotationIds.size());
        respVO.setTotalCount(createReqVO.getRecyclingEnterpriseIds().size());
        respVO.setMessage("批量智能报价生成完成，成功生成 " + quotationIds.size() + " 个报价");
        
        return success(respVO);
    }

    @PostMapping("/auto-generate")
    @Operation(summary = "根据业务模式自动生成报价")
    @PreAuthorize("@ss.hasPermission('waste:quotation:create')")
    public CommonResult<Integer> autoGenerateQuotationsByBusinessMode(
            @Parameter(description = "预约单ID", required = true) @RequestParam("appointmentId") Long appointmentId,
            @Parameter(description = "废物代码", required = true) @RequestParam("wasteCode") String wasteCode,
            @Parameter(description = "数量", required = true) @RequestParam("quantity") BigDecimal quantity,
            @Parameter(description = "客户企业ID", required = true) @RequestParam("customerEnterpriseId") Long customerEnterpriseId,
            @Parameter(description = "地区") @RequestParam(value = "region", required = false) String region) {
        
        int count = smartQuotationService.autoGenerateQuotationsByBusinessMode(
                appointmentId, wasteCode, quantity, customerEnterpriseId, region);
        
        return success(count);
    }

    @GetMapping("/calculate-price")
    @Operation(summary = "计算价格策略")
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<BigDecimal> calculatePriceByStrategy(
            @Parameter(description = "回收企业ID", required = true) @RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId,
            @Parameter(description = "废物代码", required = true) @RequestParam("wasteCode") String wasteCode,
            @Parameter(description = "数量", required = true) @RequestParam("quantity") BigDecimal quantity,
            @Parameter(description = "客户企业ID") @RequestParam(value = "customerEnterpriseId", required = false) Long customerEnterpriseId,
            @Parameter(description = "地区") @RequestParam(value = "region", required = false) String region) {
        
        BigDecimal price = smartQuotationService.calculatePriceByStrategy(
                recyclingEnterpriseId, wasteCode, quantity, customerEnterpriseId, region);
        
        return success(price);
    }

    @PostMapping("/adjust-price")
    @Operation(summary = "调整报价价格")
    @PreAuthorize("@ss.hasPermission('waste:quotation:update')")
    public CommonResult<BigDecimal> adjustQuotationPrice(
            @Parameter(description = "报价ID", required = true) @RequestParam("quotationId") Long quotationId,
            @Parameter(description = "调整系数", required = true) @RequestParam("adjustmentFactor") BigDecimal adjustmentFactor,
            @Parameter(description = "调整原因", required = true) @RequestParam("adjustmentReason") String adjustmentReason) {
        
        BigDecimal adjustedPrice = smartQuotationService.adjustQuotationPrice(
                quotationId, adjustmentFactor, adjustmentReason);
        
        return success(adjustedPrice);
    }

    @GetMapping("/validate-reasonableness")
    @Operation(summary = "验证报价合理性")
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<Boolean> validateQuotationReasonableness(
            @Parameter(description = "回收企业ID", required = true) @RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId,
            @Parameter(description = "废物代码", required = true) @RequestParam("wasteCode") String wasteCode,
            @Parameter(description = "报价", required = true) @RequestParam("quotedPrice") BigDecimal quotedPrice,
            @Parameter(description = "数量", required = true) @RequestParam("quantity") BigDecimal quantity) {
        
        boolean isReasonable = smartQuotationService.validateQuotationReasonableness(
                recyclingEnterpriseId, wasteCode, quotedPrice, quantity);
        
        return success(isReasonable);
    }

    @GetMapping("/customer-price")
    @Operation(summary = "获取客户专属价格")
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<BigDecimal> getCustomerSpecificPrice(
            @Parameter(description = "回收企业ID", required = true) @RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId,
            @Parameter(description = "客户企业ID", required = true) @RequestParam("customerEnterpriseId") Long customerEnterpriseId,
            @Parameter(description = "废物代码", required = true) @RequestParam("wasteCode") String wasteCode) {
        
        BigDecimal price = smartQuotationService.getCustomerSpecificPrice(
                recyclingEnterpriseId, customerEnterpriseId, wasteCode);
        
        return success(price);
    }

    @GetMapping("/regional-price")
    @Operation(summary = "获取区域价格")
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<BigDecimal> getRegionalPrice(
            @Parameter(description = "回收企业ID", required = true) @RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId,
            @Parameter(description = "废物代码", required = true) @RequestParam("wasteCode") String wasteCode,
            @Parameter(description = "地区", required = true) @RequestParam("region") String region) {
        
        BigDecimal price = smartQuotationService.getRegionalPrice(
                recyclingEnterpriseId, wasteCode, region);
        
        return success(price);
    }

    @GetMapping("/base-price")
    @Operation(summary = "获取基础价格")
    @PreAuthorize("@ss.hasPermission('waste:quotation:query')")
    public CommonResult<BigDecimal> getBasePrice(
            @Parameter(description = "回收企业ID", required = true) @RequestParam("recyclingEnterpriseId") Long recyclingEnterpriseId,
            @Parameter(description = "废物代码", required = true) @RequestParam("wasteCode") String wasteCode) {
        
        BigDecimal price = smartQuotationService.getBasePrice(recyclingEnterpriseId, wasteCode);
        
        return success(price);
    }
} 