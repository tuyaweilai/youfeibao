package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购订单「完成比例」采用的履约口径（#47 T09）。
 *
 * <p>需求原文是「合同明确履约重量口径；订单完成比例按该口径计算」。本平台一期把这条口径落在
 * **租户级配置**（{@code icbc_purchase_setting.performance_basis}）上，因为采购合同（#45）没有
 * 承载该字段，而订单本身逐单配置会让「同一份合同下的订单口径不一致」。口径只允许在**能取到数**的
 * 口径里选（见 {@link PurchaseProgressMeasureEnum#isAvailable()}）：入库口径（#52 未落地）不可选。
 *
 * <p>口径只是「用哪个数字算完成比例 / 未履行量」，不改变五口径各自的取数。
 */
public enum PurchasePerformanceBasisEnum {

    /** 按验收口径：以到场验收的量算完成比例（默认）。 */
    ACCEPTED("ACCEPTED", "验收口径", PurchaseProgressMeasureEnum.ACCEPTED),

    /** 按结算口径：以已被结算单归集的量算完成比例。 */
    SETTLED("SETTLED", "结算口径", PurchaseProgressMeasureEnum.SETTLED);

    private final String code;
    private final String name;
    private final PurchaseProgressMeasureEnum measure;

    PurchasePerformanceBasisEnum(String code, String name, PurchaseProgressMeasureEnum measure) {
        this.code = code;
        this.name = name;
        this.measure = measure;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 对应的履约口径。
     */
    public PurchaseProgressMeasureEnum getMeasure() {
        return measure;
    }

    public static Optional<PurchasePerformanceBasisEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(PurchasePerformanceBasisEnum::getName).orElse(null);
    }

}
