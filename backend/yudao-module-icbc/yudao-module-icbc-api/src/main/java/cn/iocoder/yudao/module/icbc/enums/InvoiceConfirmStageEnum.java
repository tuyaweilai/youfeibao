package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 自然人在结算单里看到的「开票信息确认」进度（#106，ADR 0039）。
 *
 * <p>结算确认之后，同一张结算单下的每张收购单都要在**工行的页面**上由本人确认开票信息。
 * 一个批次可能有多张收购单，于是自然人在手机上一次看到好几步；本枚举是这几步的档位，
 * 与 {@link AcquisitionStatusEnum} 不同：那个是收货员看的「这一笔卡在哪一步」，
 * 这个是自然人看的「我还要在工行页面上确认几张」。
 */
public enum InvoiceConfirmStageEnum {

    /** 还不能发起开票：前置校验没过（资质 / 额度 / 要件…）或本企业开票参数没配全，原因在 items 里 */
    BLOCKED("BLOCKED", "还不能发起开票"),
    /** 待本人在工行页面确认开票信息 */
    WAITING_CONFIRM("WAITING_CONFIRM", "待你在工行页面确认"),
    /** 本人已确认（预开票成功），等回收企业付款，付款成功即开票 */
    CONFIRMED("CONFIRMED", "已确认，等企业付款");
    // 说明：发起失败 / 预开票失败这类「要企业处理」的情况，档位仍停在 BLOCKED，
    // 具体原因放在 message 与 failures 里——不新增一个自然人看不懂也处理不了的档位。

    private final String code;
    private final String name;

    InvoiceConfirmStageEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static Optional<InvoiceConfirmStageEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

}
