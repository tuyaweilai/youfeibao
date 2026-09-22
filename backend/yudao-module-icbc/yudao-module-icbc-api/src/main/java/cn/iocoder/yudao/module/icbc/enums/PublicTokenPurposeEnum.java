package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 公开令牌的用途。每类用途绑定不同的业务对象、配不同的使用次数上限。
 *
 * <p>令牌给的是「没有账号的自然人」：它必须自己解析出租户与业务单，所以业务键类型
 * 在这里写死，mint 时按类型校验被绑定的业务是否存在。
 */
public enum PublicTokenPurposeEnum {

    /** 发票 PDF 下载：绑定一张票（合作方订单号），单次有效 */
    INVOICE_DOWNLOAD("INVOICE_DOWNLOAD", "发票下载", BusinessKeyType.ORDER, 1, false),
    /** 收方入驻失败后留联系方式：绑定一个收方，单次有效 */
    CONTACT_LEAD("CONTACT_LEAD", "失败留联系方式", BusinessKeyType.PAYEE, 1, false),
    /** 额度查询：绑定一个自然人收方，有效期内限次使用（页面可刷新） */
    QUOTA_QUERY("QUOTA_QUERY", "额度查询", BusinessKeyType.PAYEE, 20, false),
    /** 汇算清缴对账：绑定一个自然人收方，出售者查自己的开票与已缴税款（可刷新） */
    SETTLEMENT_STATEMENT("SETTLEMENT_STATEMENT", "汇算清缴对账", BusinessKeyType.PAYEE, 20, false),
    /** 出售者建档：绑定一个自然人收方，让自然人在自己手机上完成工行实名 / 收方入驻（可重开页面） */
    ONBOARDING("ONBOARDING", "出售者建档", BusinessKeyType.PAYEE, 20, true),
    /**
     * 触达通知（#36）：短信 / 收货员转达的链接，绑定一个自然人收方，让他打开就能看到
     * 「待确认的结算 / 付款异常 / 已开出的票」，**不需要先注册**（ADR 0023）。
     */
    SELLER_NOTICE("SELLER_NOTICE", "触达通知", BusinessKeyType.PAYEE, 20, false),
    /**
     * 本人自填建档（#94，ADR 0007 补充）：收货员把链接交给本人，让他在自己手机上走完
     * 同一套五步向导（拍证件 / 银行卡 → 确认 → 落库）。
     *
     * <p><b>两种绑定形态，按生成时这个人有没有档案来选</b>（#94 修票 ST-1 结构根因）：
     * <ul>
     *   <li><b>待建档</b>（{@code payeeId} 为空）：业务键类型是 {@link BusinessKeyType#ONBOARDING_INVITE}，
     *       绑定**这枚链接本身**——链接生成时还没有收方档案，绑 PAYEE 会强迫先建一个 stub（半成品）；</li>
     *   <li><b>已建档</b>（{@code payeeId} 非空）：业务键类型是 {@link BusinessKeyType#PAYEE}，
     *       把链接**锁到那个人身上**——持链接者就不能拿去改本租户里别的已知身份证的档案。</li>
     * </ul>
     * 有效期 24 小时；识别、重开页面与失败重试**不占次数**，只有成功落库那一次才占，所以限次就是 1。
     */
    ONBOARDING_WIZARD("ONBOARDING_WIZARD", "本人自填建档", BusinessKeyType.ONBOARDING_INVITE, 1, true),

    /**
     * 开票信息确认页（#106，ADR 0039）：绑定一张票（合作方订单号），让自然人在自己手机上打开
     * 工行的「自然人确认页面」。工行预下单是 UI 接口、返回一段自动提交表单，页面由**后端**输出，
     * 所以这枚令牌指回后端的公开页（见 {@code PublicPageLinkBuilder}）。
     *
     * <p>允许重开（限 20 次）：他在手机上关掉页面是常事，重开同一会话内的确认页是安全的。
     */
    INVOICE_CONFIRM_PAGE("INVOICE_CONFIRM_PAGE", "开票信息确认页", BusinessKeyType.ORDER, 20, false);

    /**
     * 令牌绑定的业务键类型。
     */
    public enum BusinessKeyType {
        /** 业务键是合作方订单号 */
        ORDER,
        /** 业务键是收方 ID */
        PAYEE,
        /**
         * 业务键是**自填建档链接本身**（尚未建档，没有收方 ID 可绑）。
         * 见 {@link #ONBOARDING_WIZARD}：链接生成时不校验收方是否存在，落库时才建档案。
         * 这个人已建档时改用 {@link #PAYEE}，不再走这一形态。
         */
        ONBOARDING_INVITE
    }

    private final String code;
    private final String name;
    private final BusinessKeyType businessKeyType;
    private final int maxUses;
    /**
     * 这枚用途的令牌是否允许被「作废」（{@code POST /icbc/public-token/revoke}）。
     *
     * <p>只有**收货员当场交给本人、需要在对方拿到后收回 / 重发的转达链接**才为 {@code true}
     * （#94：本人自填建档邀请、实名链接）。按业务事件由系统签发的自助链接（发票下载、额度查询、
     * 汇算清缴、触达通知）为 {@code false}：它们背后对应的是业务对象，要停用应当走底层业务动作，
     * 而不是让这个新端点在背后悄悄把别人的链接掐断。将来某个用途要支持作废，在这里显式打开。
     */
    private final boolean revocable;

    PublicTokenPurposeEnum(String code, String name, BusinessKeyType businessKeyType, int maxUses,
                           boolean revocable) {
        this.code = code;
        this.name = name;
        this.businessKeyType = businessKeyType;
        this.maxUses = maxUses;
        this.revocable = revocable;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BusinessKeyType getBusinessKeyType() {
        return businessKeyType;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public boolean isRevocable() {
        return revocable;
    }

    public static Optional<PublicTokenPurposeEnum> ofCode(String code) {
        return Arrays.stream(values())
                .filter(purpose -> purpose.code.equals(code))
                .findFirst();
    }

}
