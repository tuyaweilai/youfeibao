package cn.iocoder.yudao.module.icbc.controller.admin.payee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 工行收方信息新增/修改 Request VO")
@Data
public class PayeeInfoSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "收方编号（工行返回）", example = "ICBC001")
    private String payeeNo;

    @Schema(description = "合作方收方编号（我方生成）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PARTNER001")
    @NotEmpty(message = "合作方收方编号不能为空")
    private String partnerPayeeId;

    @Schema(description = "收方姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "收方姓名不能为空")
    private String name;

    @Schema(description = "身份证号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @NotEmpty(message = "身份证号码不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", 
             message = "身份证号码格式不正确")
    private String idCardNo;

    @Schema(description = "手机号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotEmpty(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String mobile;

    @Schema(description = "银行卡号", example = "6222021234567890123")
    private String bankCardNo;

    @Schema(description = "开户银行", example = "中国工商银行")
    private String bankName;

    @Schema(description = "开户支行", example = "北京分行营业部")
    private String bankBranch;

    @Schema(description = "地址", example = "北京市朝阳区xxx街道")
    private String address;

    @Schema(description = "业务类型", example = "RECYCLE")
    private String businessType;

    @Schema(description = "职业", example = "001")
    private String occupation;

    @Schema(description = "关联企业名称", example = "某某回收公司")
    private String companyName;

} 