package cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * 自然人主体身份登记 Request VO。
 *
 * <p>「身份登记」是平台自行采集的姓名 + 身份证件号码 + 手机号（见 CONTEXT），
 * 与工行的「实人认证」（活体核验）是两件事，不可混用这两个词。
 */
@Schema(description = "自然人主体 - 身份登记 Request VO")
@Data
public class NaturalPersonRegisterReqVO {

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "姓名不能为空")
    private String name;

    @Schema(description = "身份证件号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @NotEmpty(message = "身份证件号码不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
            message = "身份证件号码格式不正确")
    private String idCardNo;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

}
