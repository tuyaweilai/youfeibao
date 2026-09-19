package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 自然人端 - 发起变更收款账户（换银行卡）请求。
 *
 * <p>换的是**本人在某一家回收企业**绑定的那张卡：卡号由他本人在自己手机上填，随后走工行收方入驻 H5
 * 完成新卡绑定与审核（见 ADR 0010）。我们只收卡号与开户行，不新造流程、不允许多张卡。
 */
@Schema(description = "自然人端 - 变更收款账户请求")
@Data
public class SellerBankCardChangeReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "租户编号（要变更哪一家回收企业登记的收款账户）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "租户编号不能为空")
    private Long tenantId;

    @Schema(description = "新银行卡号（本人卡，工行收方入驻只绑一张）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新银行卡号不能为空")
    @Size(max = 64, message = "银行卡号长度不能超过 64 个字符")
    private String bankCardNo;

    @Schema(description = "新卡开户银行（选填）", example = "中国工商银行")
    @Size(max = 100, message = "开户银行长度不能超过 100 个字符")
    private String bankName;

    @Schema(description = "新卡开户支行（选填）", example = "北京分行营业部")
    @Size(max = 100, message = "开户支行长度不能超过 100 个字符")
    private String bankBranch;

    @Schema(description = "证件签发日期 yyyy-MM-dd（选填，默认沿用档案）", example = "2020-01-01")
    private String idSignDate;

    @Schema(description = "证件截止日期 yyyy-MM-dd（选填，默认沿用档案）", example = "2030-01-01")
    private String idValidityPeriod;

}
