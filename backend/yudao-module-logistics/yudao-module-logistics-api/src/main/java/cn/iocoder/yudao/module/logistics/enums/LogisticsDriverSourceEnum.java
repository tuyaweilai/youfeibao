package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 司机的来源：自有还是承运商的人。
 *
 * <p>这条区分关系到「运费该付给谁、出了事责任归谁」，所以从建档起就要记。
 * 承运商档案本身（合同、运价）归 V3 / V8，本票只存来源标记。
 */
@AllArgsConstructor
@Getter
public enum LogisticsDriverSourceEnum implements IntArrayValuable {

    SELF(1, "自有"),
    CARRIER(2, "承运商");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsDriverSourceEnum::getSource).toArray();

    private final Integer source;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsDriverSourceEnum> ofSource(Integer source) {
        return Arrays.stream(values()).filter(item -> item.source.equals(source)).findFirst();
    }

}
