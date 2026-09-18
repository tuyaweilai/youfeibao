package cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 生成公开令牌 Request VO")
@Data
public class PublicTokenCreateReqVO {

    @Schema(description = "令牌用途", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "INVOICE_DOWNLOAD")
    @NotBlank(message = "令牌用途不能为空")
    private String purpose;

    @Schema(description = "合作方订单号（用途=发票下载时必填）", example = "ORDER_20231201_001")
    private String partnerOrderId;

    @Schema(description = "收方 ID（用途=留联系方式/额度查询时必填）", example = "1024")
    private Long payeeId;

}
