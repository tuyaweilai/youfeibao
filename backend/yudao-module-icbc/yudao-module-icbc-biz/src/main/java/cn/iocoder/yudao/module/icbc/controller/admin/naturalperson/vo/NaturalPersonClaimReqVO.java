package cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 平台运营 - 身份认领（把某个登录凭证绑到已有自然人主体上）Request VO。
 *
 * <p>这是**人工**入口：自动化流程只负责拒绝（同一身份证绑不同手机号时不覆盖、不自动合并），
 * 认领与裁决由平台运营核实后手工完成（见 ADR 0017）。
 */
@Schema(description = "平台运营 - 身份认领 Request VO")
@Data
public class NaturalPersonClaimReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "登录凭证：会员用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "登录凭证（会员用户编号）不能为空")
    private Long memberUserId;

    @Schema(description = "认领依据与核实过程", example = "电话核实：本人报出身份证后四位与开户行")
    private String remark;

}
