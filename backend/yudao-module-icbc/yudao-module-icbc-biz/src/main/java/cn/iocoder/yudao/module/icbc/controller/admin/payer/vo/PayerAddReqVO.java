package cn.iocoder.yudao.module.icbc.controller.admin.payer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 工行付方新增接口 Request VO")
@Data
public class PayerAddReqVO {

    @Schema(description = "企业名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "XX科技有限公司")
    @NotEmpty(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称长度不能超过100个字符")
    private String name;

    @Schema(description = "统一社会信用代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "91110105MA01R2278M")
    @NotEmpty(message = "统一社会信用代码不能为空")
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "统一社会信用代码格式不正确")
    private String creditCode;

    @Schema(description = "纳税人识别号", requiredMode = Schema.RequiredMode.REQUIRED, example = "91110105MA01R2278M")
    @NotEmpty(message = "纳税人识别号不能为空")
    @Pattern(regexp = "^[0-9A-Z]{15,20}$", message = "纳税人识别号格式不正确")
    private String taxNo;

    @Schema(description = "银行账户", example = "6222021234567890123")
    @Pattern(regexp = "^\\d{16,19}$", message = "银行账户格式不正确")
    private String bankAccount;

    @Schema(description = "开户行名称", example = "中国工商银行北京分行")
    @Size(max = 100, message = "开户行名称长度不能超过100个字符")
    private String bankName;

    @Schema(description = "企业地址", example = "北京市朝阳区xxx路xxx号")
    @Size(max = 500, message = "企业地址长度不能超过500个字符")
    private String address;

    @Schema(description = "企业电话", example = "010-12345678")
    @Pattern(regexp = "^\\d{3,4}-?\\d{7,8}$", message = "企业电话格式不正确")
    private String telephone;

    @Schema(description = "联系人姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "联系人姓名不能为空")
    @Size(max = 50, message = "联系人姓名长度不能超过50个字符")
    private String contactName;

    @Schema(description = "联系人手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "联系人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系人手机号格式不正确")
    private String contactMobile;

    @Schema(description = "纳税人类型：01-一般纳税人，02-小规模纳税人", example = "01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "纳税人类型不能为空")
    @Pattern(regexp = "^(01|02)$", message = "纳税人类型不正确")
    private String taxpayerType;

    @Schema(description = "业务类型", example = "RECYCLE")
    @Size(max = 50, message = "业务类型长度不能超过50个字符")
    private String businessType;
} 