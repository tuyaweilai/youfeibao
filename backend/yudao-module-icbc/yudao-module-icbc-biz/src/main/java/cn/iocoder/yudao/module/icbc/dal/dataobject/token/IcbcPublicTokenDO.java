package cn.iocoder.yudao.module.icbc.dal.dataobject.token;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 公开令牌 DO。
 *
 * <p>给没有账号的自然人用的短期令牌。它必须先于租户上下文被读到，才知道把请求放到哪个
 * 租户下执行，所以这张表<b>不带租户维度</b>（继承 {@link BaseDO}），已登记进
 * {@code yudao.tenant.ignore-tables}；租户编号以普通列 {@code tenant_id} 显式存储。
 *
 * <p>令牌本身是「payload + HMAC 签名」的紧凑串，签名防篡改，本表负责单用途 / 限次与有效期。
 */
@TableName("icbc_public_token")
@KeySequence("icbc_public_token_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPublicTokenDO extends BaseDO {

    @TableId
    private Long id;

    /** 令牌唯一编号，签名 payload 里的 jti */
    private String jti;

    /** 用途，枚举 {@link cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum} */
    private String purpose;

    /** 令牌解析出的租户编号（显式存储，不参与多租户拦截） */
    private Long tenantId;

    /** 绑定的业务键：订单号或收方 ID */
    private String businessKey;

    /** 允许使用次数 */
    private Integer maxUses;

    /** 已使用次数 */
    private Integer usedCount;

    /** 过期时间 */
    private LocalDateTime expiresTime;

    /** 最后使用时间 */
    private LocalDateTime lastUsedTime;

    /** 备注 */
    private String remark;

}
