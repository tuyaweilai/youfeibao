package cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 自然人出售者 - 解绑自然人主体 Request VO。
 *
 * <p>解绑**只影响登录凭证**：自然人主体与交易记录永久保留（ADR 0017）。
 */
@Schema(description = "自然人出售者 - 解绑自然人主体 Request VO")
@Data
public class SellerSubjectUnbindReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

}
