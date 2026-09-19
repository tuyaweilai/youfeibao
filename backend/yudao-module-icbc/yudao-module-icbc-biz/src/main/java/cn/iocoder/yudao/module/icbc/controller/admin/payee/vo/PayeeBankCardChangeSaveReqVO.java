package cn.iocoder.yudao.module.icbc.controller.admin.payee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 发起变更收款账户（换银行卡）请求（服务层入参）。
 *
 * <p>收方档案只保留生效中的那一张卡；本请求提供的新卡先落在变更单上，审核通过才换过去（#37）。
 */
@Schema(description = "管理后台 - 发起变更收款账户请求")
@Data
public class PayeeBankCardChangeSaveReqVO {

    @Schema(description = "收方（出售者）档案编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "收方编号不能为空")
    private Long payeeId;

    @Schema(description = "新银行卡号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新银行卡号不能为空")
    @Size(max = 64, message = "银行卡号长度不能超过 64 个字符")
    private String newBankCardNo;

    @Schema(description = "新卡开户银行", example = "中国工商银行")
    @Size(max = 100, message = "开户银行长度不能超过 100 个字符")
    private String newBankName;

    @Schema(description = "新卡开户支行", example = "北京分行营业部")
    @Size(max = 100, message = "开户支行长度不能超过 100 个字符")
    private String newBankBranch;

    @Schema(description = "证件签发日期 yyyy-MM-dd（为空沿用档案）", example = "2020-01-01")
    private String idSignDate;

    @Schema(description = "证件截止日期 yyyy-MM-dd（为空沿用档案）", example = "2030-01-01")
    private String idValidityPeriod;

    @Schema(description = "发起来源：SELLER_PORTAL / FIELD / ADMIN", example = "SELLER_PORTAL")
    private String requestSource;

    @Schema(description = "发起 IP（留痕）")
    private String requestIp;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
