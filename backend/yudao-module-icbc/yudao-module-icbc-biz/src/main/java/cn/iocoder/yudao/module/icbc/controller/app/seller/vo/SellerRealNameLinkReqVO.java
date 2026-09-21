package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 自然人端 - 取实名认证入口（#89）。
 *
 * <p>实名只能本人做，而实人认证是工行的页面接口（只在微信环境能唤起）。本人端拿到一枚
 * {@code ONBOARDING} 一次性令牌后，在自己的微信里打开工行页面完成人脸；
 * 入驻由平台在实名通过后自动发起（ADR 0035），本人不需要再点任何东西。
 */
@Schema(description = "自然人端 - 取实名认证入口请求")
@Data
public class SellerRealNameLinkReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "收方（出售者）档案编号：本人可能在多家企业都有档案，任取其一即可（实名是平台级的）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "收方档案编号不能为空")
    private Long payeeId;

}
