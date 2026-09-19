package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 出售者额度超限后的「办理经营主体登记」引导状态。
 *
 * <p>自然人出售者连续 12 个月反向开票累计销售额超过 500 万元后，就不能再由回收企业
 * 反向开票（税总 5 号公告）——他必须办理经营主体登记、以主体身份开票。这一步没有接口可
 * 代替：工行只做事后补缴，事前拦截是平台自己的责任。所以平台除了拒绝开票，还要留下一条
 * 可跟进、可结案的引导记录，否则「拒绝」就只是一次没有下文的报错。
 */
public enum SellerQuotaGuidanceStatusEnum {

    /** 待引导：超限已发生，还没联系上出售者 */
    PENDING(0, "待引导", "联系该出售者，说明已超 500 万上限，引导其办理经营主体登记"),
    /** 已引导：已告知出售者，等其办理 */
    INFORMED(1, "已引导", "出售者已被告知；其办理经营主体登记前不能再反向开票"),
    /** 已办结：出售者已办理经营主体登记，后续由其主体开票 */
    RESOLVED(2, "已办结", null);

    private final Integer status;
    private final String name;
    private final String nextAction;

    SellerQuotaGuidanceStatusEnum(Integer status, String name, String nextAction) {
        this.status = status;
        this.name = name;
        this.nextAction = nextAction;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getNextAction() {
        return nextAction;
    }

    public static Optional<SellerQuotaGuidanceStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(SellerQuotaGuidanceStatusEnum::getName).orElse("未知");
    }

    public static String nextActionOf(Integer status) {
        return ofStatus(status).map(SellerQuotaGuidanceStatusEnum::getNextAction).orElse(null);
    }

}
