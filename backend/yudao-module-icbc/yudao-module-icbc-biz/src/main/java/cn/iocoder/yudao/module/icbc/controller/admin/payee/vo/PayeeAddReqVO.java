package cn.iocoder.yudao.module.icbc.controller.admin.payee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Schema(description = "工行收方新增接口 Request VO")
@Data
public class PayeeAddReqVO {

    @Schema(description = "外部用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER001")
    @NotEmpty(message = "外部用户编号不能为空")
    private String outUserId;

    @Schema(description = "收方类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotEmpty(message = "收方类型不能为空")
    private String receiverType = "3"; // 3:自然人

    @Schema(description = "收方户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "收方户名不能为空")
    private String receiverName;

    @Schema(description = "收方账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6222021234567890123")
    @NotEmpty(message = "收方账号不能为空")
    private String receiverAccount;

    @Schema(description = "证件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotEmpty(message = "证件类型不能为空")
    private String idType = "0"; // 0:身份证

    @Schema(description = "证件号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @NotEmpty(message = "证件号码不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", 
             message = "身份证号码格式不正确")
    private String idNo;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String mobile;

    @Schema(description = "职业代码", example = "001")
    private String occupation;

    @Schema(description = "常用住址", example = "北京市朝阳区xxx街道")
    private String address;

} 