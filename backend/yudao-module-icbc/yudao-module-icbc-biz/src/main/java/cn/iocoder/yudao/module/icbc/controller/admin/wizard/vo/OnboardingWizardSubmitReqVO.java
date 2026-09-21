package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 建档向导 - 第五步「签署框架收购协议」提交请求。
 *
 * <p>第 4 步确认后由它一次性落库：姓名 / 证件号 / 证件有效期登记并关联**自然人主体**（有则复用，
 * 已填的值不覆盖）；卡号 / 开户行 / 住址与是否我行卡写进**收方档案**；同一时刻把框架收购协议
 * 落成纸质签法（电子签章未开通时，见 ADR 0036）。
 *
 * <p>协议要素（名称 / 数量 / 规格 / 回收期次 / 结算方式）是税总 5 号公告第十七条点名的合同流
 * 证据：这里给了与现场纸质件一致的缺省值，前端可改，但**不能留空**。
 */
@Schema(description = "管理后台 - 建档向导：提交（落库 + 落纸质协议）")
@Data
public class OnboardingWizardSubmitReqVO {

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

    @Schema(description = "常住住址", example = "北京市朝阳区xxx街道")
    private String address;

    @Schema(description = "证件签发日期 yyyy-MM-dd", example = "2020-01-01")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "证件签发日期格式应为 yyyy-MM-dd")
    private String idSignDate;

    @Schema(description = "证件截止日期 yyyy-MM-dd，永久有效传 9999-12-30", example = "2030-01-01")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "证件截止日期格式应为 yyyy-MM-dd")
    private String idValidityPeriod;

    @Schema(description = "银行卡号（本人）", example = "6222021234567890123")
    @Pattern(regexp = "^\\d{16,19}$", message = "银行卡号应为 16-19 位数字")
    private String bankCardNo;

    @Schema(description = "开户银行", example = "中国工商银行")
    private String bankName;

    @Schema(description = "开户支行", example = "北京分行营业部")
    private String bankBranch;

    @Schema(description = "是否我行用户：0-非我行用户，1-我行用户；缺省视为 1", example = "1")
    @Pattern(regexp = "^[01]$", message = "是否我行用户只能填 0 或 1")
    private String accountCode;

    // ==================== 协议要素（税总 5 号公告第十七条） ====================

    @Schema(description = "货物名称", example = "报废产品")
    @Size(max = 200, message = "货物名称长度不能超过200个字符")
    private String productName;

    @Schema(description = "数量", example = "以实际交货为准")
    @Size(max = 100, message = "数量长度不能超过100个字符")
    private String quantity;

    @Schema(description = "规格", example = "以实际交货为准")
    @Size(max = 100, message = "规格长度不能超过100个字符")
    private String specification;

    @Schema(description = "回收期次", example = "长期")
    @Size(max = 100, message = "回收期次长度不能超过100个字符")
    private String recyclePeriod;

    @Schema(description = "结算方式", example = "银行转账")
    @Size(max = 200, message = "结算方式长度不能超过200个字符")
    private String settlementMethod;

}
