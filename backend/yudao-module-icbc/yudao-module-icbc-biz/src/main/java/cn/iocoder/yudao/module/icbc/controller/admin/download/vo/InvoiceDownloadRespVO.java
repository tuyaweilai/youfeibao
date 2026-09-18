package cn.iocoder.yudao.module.icbc.controller.admin.download.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工行发票下载响应 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行发票下载响应 VO")
@Data
public class InvoiceDownloadRespVO {

    @Schema(description = "下载记录ID", example = "1")
    private Long id;

    @Schema(description = "发票订单ID", example = "1")
    private Long invoiceOrderId;

    @Schema(description = "合作方订单号", example = "ORDER_20231201_001")
    private String partnerOrderId;

    @Schema(description = "工行订单号", example = "ICBC_20231201_001")
    private String orderNumber;

    @Schema(description = "发票号码", example = "12345678901234567890")
    private String invoiceNumber;

    @Schema(description = "发票下载URL", example = "https://example.com/invoice.pdf")
    private String downloadUrl;

    @Schema(description = "本地文件存储路径", example = "/data/invoices/2023/12/invoice.pdf")
    private String filePath;

    @Schema(description = "文件名称", example = "invoice_20231201.pdf")
    private String fileName;

    @Schema(description = "文件大小(字节)", example = "1024000")
    private Long fileSize;

    @Schema(description = "下载状态", example = "2")
    private Integer downloadStatus;

    @Schema(description = "下载状态名称", example = "下载成功")
    private String downloadStatusName;

    @Schema(description = "下载时间", example = "2023-12-01 10:00:00")
    private LocalDateTime downloadTime;

    @Schema(description = "重试次数", example = "0")
    private Integer retryCount;

    @Schema(description = "错误信息", example = "")
    private String errorMsg;

    @Schema(description = "创建时间", example = "2023-12-01 09:00:00")
    private LocalDateTime createTime;

    @Schema(description = "发票文件列表")
    private List<InvoiceFileRespVO> files;

} 