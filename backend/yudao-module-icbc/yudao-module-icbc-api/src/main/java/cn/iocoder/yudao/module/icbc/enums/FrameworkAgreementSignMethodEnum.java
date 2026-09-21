package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 框架收购协议的签署方式。
 *
 * <p>哪个值由**电子签章端口的可用性**决定，不靠配置开关、也不由前端写死（ADR 0036 / 0037）：
 * 租户电子签章可用就 {@link #ELECTRONIC}，不可用就降级为 {@link #PAPER}，两条路都不阻断建档。
 *
 * <p>取值与 {@code IcbcFrameworkAgreementDO#signMethod} 的注释一致；从前这个字段一路为
 * {@code null}（没人签过却被盖上 {@code signedAt}），建档向导（#91）起第一次真正写值。
 */
@Getter
@AllArgsConstructor
public enum FrameworkAgreementSignMethodEnum {

    /**
     * 电子签章：由第三方电子合同平台承载（腾讯电子签，目标供应商）。
     */
    ELECTRONIC("ELECTRONIC", "电子签章"),

    /**
     * 纸质签署：现场打印、本人签字，平台只留记录。
     *
     * <p>电子签章未开通（无企业主体或印章）时的唯一降级路径（ADR 0036）。
     */
    PAPER("PAPER", "纸质签署");

    /**
     * 持久化用的编码
     */
    private final String code;

    /**
     * 展示名
     */
    private final String name;

    public static FrameworkAgreementSignMethodEnum ofCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

}
