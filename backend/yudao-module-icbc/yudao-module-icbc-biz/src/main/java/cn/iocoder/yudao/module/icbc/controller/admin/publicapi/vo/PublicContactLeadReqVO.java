package cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "公开端点 - 失败留联系方式 Request VO")
@Data
public class PublicContactLeadReqVO {

    @Schema(description = "令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "令牌不能为空")
    private String token;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "姓名不能为空")
    private String name;

    @Schema(description = "联系方式（手机号）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "13800138000")
    @NotBlank(message = "联系方式不能为空")
    private String mobile;

    @Schema(description = "备注 / 失败原因", example = "收方入驻失败，银行卡未识别")
    private String remark;

}
