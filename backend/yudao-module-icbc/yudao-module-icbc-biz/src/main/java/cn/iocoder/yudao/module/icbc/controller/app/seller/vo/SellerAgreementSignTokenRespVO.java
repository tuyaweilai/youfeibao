package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自然人端 - 取「去签署」用的公开令牌返回（#95）。
 *
 * <p>返回的令牌绑定收方、用途为 {@code ONBOARDING}；本人端拿它调
 * {@code POST /icbc/public/agreement/sign-url} 现取一枚第三方签署链接并跳转。
 * 令牌本身不是签署链接，签署链接只在点击那一刻生成、不缓存、不复用、不发短信。
 */
@Schema(description = "自然人端 - 取去签署用的公开令牌返回")
@Data
public class SellerAgreementSignTokenRespVO {

    @Schema(description = "一次性公开令牌（用途 ONBOARDING，绑定收方）")
    private String token;

    @Schema(description = "令牌过期时间")
    private LocalDateTime expiresTime;

}
