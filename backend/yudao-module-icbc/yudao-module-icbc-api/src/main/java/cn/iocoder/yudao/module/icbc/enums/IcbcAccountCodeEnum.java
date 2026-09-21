package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 「是否我行用户」（工行报文的 {@code accountCode}）。
 *
 * <p>这个取值只有两处会写字：建档向导的银行卡确认页（本人确认 / 卡证识别），以及换卡发起侧
 * （#86）。收方档案是**权威副本**：确认过的值落在 {@code icbc_payee_info.account_code} 上，
 * 收方入驻发起与换卡审核通过时都从档案取。
 *
 * <p>{@link #ICBC} 同时是「没人确认过」时的缺省（#81 盘问结论）：现场拿到的多是工行卡，
 * 猜错的代价是工行驳回入驻，而不是默默写错一笔钱。这里放一处，免得各服务各写一份字面量 `"1"`。
 */
@Getter
@AllArgsConstructor
public enum IcbcAccountCodeEnum {

    NON_ICBC("0", "非我行用户"),
    ICBC("1", "我行用户");

    /**
     * 持久化用的编码
     */
    private final String code;

    /**
     * 展示名
     */
    private final String name;

    public static IcbcAccountCodeEnum ofCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

}
