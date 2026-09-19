package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 自然人主体状态。
 *
 * <p>停用是平台运营的人工处置（如身份证被冒用核实），**不删除任何数据**：身份与交易记录都要按
 * 税务追溯期保留（ADR 0017）。
 */
@Getter
@AllArgsConstructor
public enum NaturalPersonStatusEnum {

    NORMAL(0, "正常"),
    DISABLED(1, "已停用");

    private final Integer status;
    private final String name;

    public static NaturalPersonStatusEnum of(Integer status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .findFirst()
                .orElse(null);
    }

}
