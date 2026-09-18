package cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生成公开令牌 Response VO")
@Data
public class PublicTokenRespVO {

    @Schema(description = "令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @Schema(description = "用途", example = "INVOICE_DOWNLOAD")
    private String purpose;

    @Schema(description = "用途名称", example = "发票下载")
    private String purposeName;

    @Schema(description = "业务键", example = "ORDER_20231201_001")
    private String businessKey;

    @Schema(description = "过期时间")
    private LocalDateTime expiresTime;

    @Schema(description = "允许使用次数", example = "1")
    private Integer maxUses;

}
