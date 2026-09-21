package cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 作废公开令牌 Request VO")
@Data
public class PublicTokenRevokeReqVO {

    @Schema(description = "要作废的令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "令牌不能为空")
    private String token;

}
