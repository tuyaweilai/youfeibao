package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自然人端 - 取实名认证入口的返回（#89）。
 *
 * <p>返回一枚 {@code ONBOARDING} 一次性令牌（绑定收方）：本人端用它在自己微信里打开工行
 * 实人认证页面；办完之后落点页会自动刷新状态。**没有「替本人发起入驻」的东西**——
 * 入驻是平台在实名通过后自动做的。
 */
@Schema(description = "自然人端 - 取实名认证入口返回")
@Data
public class SellerRealNameLinkRespVO {

    @Schema(description = "实名认证一次性令牌（用途 ONBOARDING）")
    private String token;

    @Schema(description = "令牌过期时间")
    private LocalDateTime expiresTime;

    @Schema(description = "给自然人看的一句话说明")
    private String message;

}
