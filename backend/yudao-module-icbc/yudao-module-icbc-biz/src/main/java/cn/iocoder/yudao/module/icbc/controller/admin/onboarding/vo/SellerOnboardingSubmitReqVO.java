package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 发起收方入驻请求
 *
 * <p>姓名、身份证、手机号、银行卡号取自出售者档案；这里补上证件签发 / 截止日期与交易渠道。
 * 银行卡识别结果（开户行 / 支行）由前端 OCR 后一并写回档案。
 */
@Schema(description = "管理后台 - 发起出售者收方入驻请求")
@Data
public class SellerOnboardingSubmitReqVO {

    @Schema(description = "出售者（收方）编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出售者编号不能为空")
    private Long payeeId;

    @Schema(description = "证件签发日期 yyyy-MM-dd", example = "2020-01-01")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "证件签发日期格式应为 yyyy-MM-dd")
    private String idSignDate;

    @Schema(description = "证件截止日期 yyyy-MM-dd，永久有效传 9999-12-30", example = "2030-01-01")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "证件截止日期格式应为 yyyy-MM-dd")
    private String idValidityPeriod;

    @Schema(description = "交易渠道：01 安卓 APP、02 iOS APP、03 H5、04 微信公众号、05 微信小程序、06 支付宝场景号",
            example = "05")
    @Size(max = 2, message = "交易渠道长度不能超过2个字符")
    private String trxChannel;

    @Schema(description = "银行卡识别结果：开户银行", example = "中国工商银行")
    private String bankName;

    @Schema(description = "银行卡识别结果：开户支行", example = "北京分行营业部")
    private String bankBranch;

}
