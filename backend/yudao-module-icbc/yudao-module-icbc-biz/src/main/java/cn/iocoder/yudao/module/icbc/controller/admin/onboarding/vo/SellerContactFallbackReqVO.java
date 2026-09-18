package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 收方入驻失败时留下联系方式
 */
@Schema(description = "管理后台 - 出售者留联系方式请求")
@Data
public class SellerContactFallbackReqVO {

    @Schema(description = "出售者（收方）编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出售者编号不能为空")
    private Long payeeId;

    @Schema(description = "联系方式（手机号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotEmpty(message = "联系方式不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String mobile;

    @Schema(description = "备注", example = "审核拒绝，等待回电")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

}
