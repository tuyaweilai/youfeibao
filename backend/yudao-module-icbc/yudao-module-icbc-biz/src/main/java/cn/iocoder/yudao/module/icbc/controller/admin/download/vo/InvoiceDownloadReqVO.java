package cn.iocoder.yudao.module.icbc.controller.admin.download.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 工行发票下载请求 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行发票下载请求 VO")
@Data
public class InvoiceDownloadReqVO {

    @Schema(description = "合作方订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_20231201_001")
    @NotBlank(message = "合作方订单号不能为空")
    private String partnerOrderId;

    @Schema(description = "发票号码", example = "12345678901234567890")
    private String invoiceNumber;

    @Schema(description = "文件类型", example = "PDF")
    private String fileType;

} 