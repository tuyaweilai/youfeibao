package cn.iocoder.yudao.module.icbc.service.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公开令牌的签名载荷。它是令牌里明文携带的部分，签名保证不被篡改。
 *
 * <p>不含任何敏感信息，只够解析出「租户 + 业务单 + 用途 + 有效期」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicTokenPayload {

    /** 令牌唯一编号，用于在库里定位记录、做单用途/限次 */
    private String jti;

    /** 用途 */
    private String purpose;

    /** 租户编号 */
    private Long tenantId;

    /** 绑定业务键：订单号或收方 ID */
    private String businessKey;

    /**
     * 业务键类型（{@code PublicTokenPurposeEnum.BusinessKeyType} 名）。
     *
     * <p>用途本身通常就决定了业务键类型，但 {@code ONBOARDING_WIZARD} 例外：待建档时绑定链接本身、
     * 已建档时绑定收方 ID（#94 修票 ST-1）。免登录端点据此才知道这枚链接有没有锁到某个人身上。
     */
    private String businessKeyType;

    /** 过期时间（epoch 秒） */
    private Long expiresAt;

}
