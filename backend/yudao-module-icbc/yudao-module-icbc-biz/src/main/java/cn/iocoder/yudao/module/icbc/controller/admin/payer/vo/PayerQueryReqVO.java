package cn.iocoder.yudao.module.icbc.controller.admin.payer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Schema(description = "管理后台 - 工行付方查询接口 Request VO")
@Data
public class PayerQueryReqVO {

    @Schema(description = "统一社会信用代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "91110105MA01R2278M")
    @NotEmpty(message = "统一社会信用代码不能为空")
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "统一社会信用代码格式不正确")
    private String creditCode;

    @Schema(description = "纳税人识别号", example = "91110105MA01R2278M")
    @Pattern(regexp = "^[0-9A-Z]{15,20}$", message = "纳税人识别号格式不正确")
    private String taxNo;

} 