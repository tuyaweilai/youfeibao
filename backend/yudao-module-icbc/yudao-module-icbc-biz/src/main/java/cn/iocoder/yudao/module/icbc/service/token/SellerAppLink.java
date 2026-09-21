package cn.iocoder.yudao.module.icbc.service.token;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自然人端（出售者端）一次性链接的签发结果。
 *
 * <p>{@code link} 形如 {@code https://<seller-app>/#/?token=xxx&purpose=ONBOARDING}：
 * 令牌负责在免登录端点里解析出租户与业务单，链接负责把人送到对应页面。
 */
@Data
@Builder
public class SellerAppLink {

    /** 一次性令牌 */
    private String token;
    /** 拼好的入口链接（令牌是 base64url，可直接进 query，无需再编码） */
    private String link;
    /** 令牌过期时间 */
    private LocalDateTime expiresTime;

}
