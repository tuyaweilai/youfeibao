package cn.iocoder.yudao.module.icbc.controller.admin.invoice;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceTaxCertificateRespVO;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceTaxCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.CREATE;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.GET;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工行反向开票订单
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 工行反向开票订单")
@RestController
@RequestMapping("/icbc/invoice-order")
@Validated
@Slf4j
public class InvoiceOrderController {

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Resource
    private InvoiceTaxCertificateService invoiceTaxCertificateService;

    @PostMapping("/pre-order")
    @Operation(summary = "创建反向开票预下单")
    @PreAuthorize("@icbc.hasPermission('icbc:invoice-order:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<InvoicePreOrderRespVO> createPreOrder(@Valid @RequestBody InvoicePreOrderReqVO createReqVO) {
        log.info("收到反向开票预下单请求 - partnerOrderId: {}", createReqVO.getOutOrderId());
        InvoicePreOrderRespVO response = invoiceOrderService.createPreOrder(createReqVO);
        return success(response);
    }

    @GetMapping("/query")
    @Operation(summary = "查询反向开票信息")
    @PreAuthorize("@icbc.hasPermission('icbc:invoice-order:query')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<InvoiceQueryRespVO> queryInvoiceInfo(@Valid InvoiceQueryReqVO queryReqVO) {
        log.info("收到反向开票查询请求 - partnerOrderId: {}", queryReqVO.getOutOrderId());
        InvoiceQueryRespVO response = invoiceOrderService.queryInvoiceInfo(queryReqVO);
        return success(response);
    }

    @PostMapping("/query")
    @Operation(summary = "查询反向开票信息（POST方式）")
    @PreAuthorize("@icbc.hasPermission('icbc:invoice-order:query')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<InvoiceQueryRespVO> queryInvoiceInfoPost(@Valid @RequestBody InvoiceQueryReqVO queryReqVO) {
        log.info("收到反向开票查询请求（POST） - partnerOrderId: {}", queryReqVO.getOutOrderId());
        InvoiceQueryRespVO response = invoiceOrderService.queryInvoiceInfo(queryReqVO);
        return success(response);
    }

    @GetMapping("/tax-certificate")
    @Operation(summary = "查询代办税费缴税凭证")
    @PreAuthorize("@icbc.hasPermission('icbc:invoice-order:query')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<InvoiceTaxCertificateRespVO> getTaxCertificate(@RequestParam("partnerOrderId") String partnerOrderId) {
        return success(invoiceTaxCertificateService.getCertificate(partnerOrderId));
    }

    @GetMapping("/tax-certificate/export")
    @Operation(summary = "导出代办税费缴税凭证")
    @PreAuthorize("@icbc.hasPermission('icbc:invoice-order:query')")
    @ApiAccessLog(operateType = GET)
    public void exportTaxCertificate(@RequestParam("partnerOrderId") String partnerOrderId,
                                     HttpServletResponse response) {
        invoiceTaxCertificateService.exportCertificate(partnerOrderId, response);
    }

}
