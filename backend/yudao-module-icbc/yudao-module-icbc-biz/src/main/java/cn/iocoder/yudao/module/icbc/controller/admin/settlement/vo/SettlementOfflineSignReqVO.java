package cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 线下签字逃生门：上传带签字的纸质确认书 + 标注办理人，等价于确认（ADR 0018）。
 */
@Schema(description = "管理后台 - 结算单线下签字确认 Request VO")
@Data
public class SettlementOfflineSignReqVO {

    @Schema(description = "结算单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "结算单编号不能为空")
    private Long settlementId;

    @Schema(description = "带签字的纸质确认书附件地址", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://cdn.example.com/sign/settlement-1024.jpg")
    @NotEmpty(message = "确认书附件不能为空")
    private String fileUrl;

    @Schema(description = "办理人", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "办理人不能为空")
    private String handler;

    @Schema(description = "备注", example = "现场签字并拍照留存")
    private String remark;

}
