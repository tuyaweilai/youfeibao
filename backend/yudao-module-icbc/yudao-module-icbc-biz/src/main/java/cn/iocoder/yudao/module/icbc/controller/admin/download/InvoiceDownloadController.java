package cn.iocoder.yudao.module.icbc.controller.admin.download;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadRespVO;
import cn.iocoder.yudao.module.icbc.service.download.InvoiceDownloadService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工行发票下载控制器
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 工行发票下载")
@RestController
@RequestMapping("/icbc/invoice-download")
@Validated
@Slf4j
public class InvoiceDownloadController {

    @Resource
    private InvoiceDownloadService invoiceDownloadService;

    @PostMapping("/download")
    @Operation(summary = "下载发票")
    @PreAuthorize("@ss.hasPermission('icbc:invoice-download:download')")
    public CommonResult<InvoiceDownloadRespVO> downloadInvoice(@Valid @RequestBody InvoiceDownloadReqVO reqVO) {
        InvoiceDownloadRespVO result = invoiceDownloadService.downloadInvoice(reqVO);
        return success(result);
    }

    @GetMapping("/get")
    @Operation(summary = "获取发票下载记录")
    @Parameter(name = "partnerOrderId", description = "合作方订单号", required = true)
    @PreAuthorize("@ss.hasPermission('icbc:invoice-download:query')")
    public CommonResult<InvoiceDownloadRespVO> getDownloadRecord(@RequestParam("partnerOrderId") String partnerOrderId) {
        InvoiceDownloadRespVO result = invoiceDownloadService.getDownloadRecord(partnerOrderId);
        return success(result);
    }

    @GetMapping("/get-by-invoice")
    @Operation(summary = "根据发票号码获取下载记录")
    @Parameter(name = "invoiceNumber", description = "发票号码", required = true)
    @PreAuthorize("@ss.hasPermission('icbc:invoice-download:query')")
    public CommonResult<InvoiceDownloadRespVO> getDownloadRecordByInvoiceNumber(@RequestParam("invoiceNumber") String invoiceNumber) {
        InvoiceDownloadRespVO result = invoiceDownloadService.getDownloadRecordByInvoiceNumber(invoiceNumber);
        return success(result);
    }

    @PostMapping("/retry/{downloadId}")
    @Operation(summary = "重试下载")
    @Parameter(name = "downloadId", description = "下载记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('icbc:invoice-download:retry')")
    public CommonResult<InvoiceDownloadRespVO> retryDownload(@PathVariable("downloadId") Long downloadId) {
        InvoiceDownloadRespVO result = invoiceDownloadService.retryDownload(downloadId);
        return success(result);
    }

    @GetMapping("/download-file")
    @Operation(summary = "下载发票文件")
    @Parameter(name = "downloadId", description = "下载记录ID", required = true)
    @Parameter(name = "fileType", description = "文件类型", required = true)
    @PreAuthorize("@ss.hasPermission('icbc:invoice-download:download-file')")
    public void downloadFile(@RequestParam("downloadId") Long downloadId,
                           @RequestParam("fileType") String fileType,
                           HttpServletResponse response) {
        invoiceDownloadService.downloadFile(downloadId, fileType, response);
    }

} 