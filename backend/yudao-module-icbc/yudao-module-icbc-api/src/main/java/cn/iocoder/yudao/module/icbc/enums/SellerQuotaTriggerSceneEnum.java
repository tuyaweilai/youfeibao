package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 触发「办理经营主体登记」引导的场景：超 500 万这件事是在哪一步被发现的。
 *
 * <p>它不影响引导本身的处理，但影响责任归属：开票申请被拒是开票员发现的，收购登记时
 * 就已经超是收货员在发现的——两者要联系的人、要说的话不一样。
 */
public enum SellerQuotaTriggerSceneEnum {

    /** 开票申请被拒：开票员发起时被硬校验拦下 */
    INVOICE_APPLICATION("INVOICE_APPLICATION", "开票申请被拒"),
    /** 收购登记时已超：收货员在现场就看到了余量提示 */
    ACQUISITION("ACQUISITION", "收购登记时已超");

    private final String code;
    private final String name;

    SellerQuotaTriggerSceneEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static Optional<SellerQuotaTriggerSceneEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(SellerQuotaTriggerSceneEnum::getName).orElse(null);
    }

}
