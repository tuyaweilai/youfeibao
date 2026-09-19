package cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * 自然人出售者 - 手机号 + 短信验证码登录 Request VO
 */
@Schema(description = "自然人出售者 - 短信验证码登录 Request VO")
@Data
public class SellerSmsLoginReqVO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @Schema(description = "短信验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1234")
    @NotEmpty(message = "短信验证码不能为空")
    private String code;

}
