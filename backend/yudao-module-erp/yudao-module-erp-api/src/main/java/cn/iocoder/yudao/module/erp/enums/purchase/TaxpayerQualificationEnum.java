package cn.iocoder.yudao.module.erp.enums.purchase;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 纳税人资格枚举。
 *
 * <p>自然人恒为小规模纳税人（《增值税法实施条例》第七条），只有单位供货方能是一般纳税人，
 * 因此该字段只挂在 {@code erp_supplier} 上，不挂自然人出售者。
 */
@RequiredArgsConstructor
@Getter
public enum TaxpayerQualificationEnum implements ArrayValuable<Integer> {

    GENERAL(1, "一般纳税人"),
    SMALL_SCALE(2, "小规模纳税人"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(TaxpayerQualificationEnum::getQualification).toArray(Integer[]::new);

    /**
     * 纳税人资格
     */
    private final Integer qualification;
    /**
     * 纳税人资格名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TaxpayerQualificationEnum valueOf(Integer qualification) {
        return Arrays.stream(values()).filter(e -> e.getQualification().equals(qualification))
                .findFirst().orElse(null);
    }

    public static String nameOf(Integer qualification) {
        TaxpayerQualificationEnum taxpayerQualification = valueOf(qualification);
        return taxpayerQualification != null ? taxpayerQualification.getName() : null;
    }

}
