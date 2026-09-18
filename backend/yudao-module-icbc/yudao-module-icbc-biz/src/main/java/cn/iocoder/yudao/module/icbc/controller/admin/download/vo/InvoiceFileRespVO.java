package cn.iocoder.yudao.module.icbc.controller.admin.download.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工行发票文件响应 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行发票文件响应 VO")
@Data
public class InvoiceFileRespVO {

    @Schema(description = "文件ID", example = "1")
    private Long id;

    @Schema(description = "下载记录ID", example = "1")
    private Long downloadId;

    @Schema(description = "发票号码", example = "12345678901234567890")
    private String invoiceNumber;

    @Schema(description = "文件类型", example = "PDF")
    private String fileType;

    @Schema(description = "文件存储路径", example = "/data/invoices/2023/12/invoice.pdf")
    private String filePath;

    @Schema(description = "文件名称", example = "invoice_20231201.pdf")
    private String fileName;

    @Schema(description = "文件大小(字节)", example = "1024000")
    private Long fileSize;

    @Schema(description = "文件MD5值", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String fileMd5;

    @Schema(description = "上传时间", example = "2023-12-01 10:00:00")
    private LocalDateTime uploadTime;

    @Schema(description = "访问次数", example = "5")
    private Integer accessCount;

    @Schema(description = "最后访问时间", example = "2023-12-01 15:30:00")
    private LocalDateTime lastAccessTime;

    @Schema(description = "创建时间", example = "2023-12-01 10:00:00")
    private LocalDateTime createTime;

} 