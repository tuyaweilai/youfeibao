package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 租户级电子签章的开通状态（#92，ADR 0036）。
 *
 * <p>电子签章的章是**租户级**的：回收企业才是发起方（第三方接口约束，也与 ADR 0001 一致），
 * 平台不代盖。所以「这家企业能不能用电子签」是本租户自己的一个状态，而不是平台级开关。
 *
 * <p>三态只讲**开通走到哪一步**，不讲协议签没签完：协议状态（待签署 / 生效 / 作废）在
 * {@code icbc_framework_agreement} 上，两者互不包含（CONTEXT.md「待签署」词条）。
 *
 * <ul>
 *   <li>{@link #NOT_OPENED}：还没点过「开通电子签」；</li>
 *   <li>{@link #AUTHENTICATING}：已经拿到一次性控制台链接，企业认证 / 制章在第三方侧进行中；</li>
 *   <li>{@link #ACTIVATED}：企业认证通过且企业印章已创建，可以发起电子签署。</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum EsignActivationStatusEnum {

    /** 未开通：从没点过开通入口 */
    NOT_OPENED(0, "未开通", "点「开通电子签」拿到控制台链接，完成企业认证与创建企业印章"),
    /** 认证中：控制台链接已给出，等第三方侧完成企业认证与制章 */
    AUTHENTICATING(1, "认证中", "在第三方控制台完成企业认证并创建企业印章，回来确认激活"),
    /** 已激活：企业认证通过且印章就位 */
    ACTIVATED(2, "已激活", "可以发起电子签署（框架收购协议 + 反向发票合规告知函一次签完）");

    /** 持久化用的编码 */
    private final Integer status;
    /** 展示名 */
    private final String name;
    /** 下一步该做什么（可直接展示给管理员） */
    private final String nextStep;

    public static EsignActivationStatusEnum ofStatus(Integer status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .findFirst()
                .orElse(null);
    }

    public static String nameOf(Integer status) {
        EsignActivationStatusEnum item = ofStatus(status);
        return item != null ? item.getName() : null;
    }

}
