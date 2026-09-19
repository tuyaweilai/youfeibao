package cn.iocoder.yudao.module.icbc.enums;

import java.math.BigDecimal;

/**
 * 代办税费（issue #13）的税率与期限口径。
 *
 * <p>税总 5 号公告下回收企业为出售者代办增值税及附加税费、个人所得税。金额口径
 * <strong>只有这一处</strong>：申报清单、补缴、汇算清缴都从这里取。
 *
 * <ul>
 *   <li>增值税：小规模月销售额 10 万元以下免征；超过则全额计税，3% 征收率减按 1%（
 *       {@link #VAT_RATE_ONE_PERCENT}）或放弃减按按 3%（{@link #VAT_RATE_THREE_PERCENT}）；</li>
 *   <li>附加税费：以增值税为基数，城建 7% + 教育费附加 3% + 地方教育附加 2% = 12%，
 *       自然人（小规模纳税人）减半征收，综合 {@link #SURCHARGE_RATE}；</li>
 *   <li>个人所得税：按销售额 0.5% 预缴经营所得个税（{@link #IIT_RATE}），不受 10 万免征线影响。</li>
 * </ul>
 *
 * <p>这些比率是政策常量而非工行接口字段：工行只负责逐票缴税，申报与补缴的口径由平台自持。
 * 若以后要按地区 / 政策调整，改这里一处即可。
 */
public final class IcbcTaxConstants {

    /** 月销售额增值税免征线（元）：10 万。与额度台账的 {@code MONTHLY_EXEMPT_AMOUNT} 同一口径 */
    public static final BigDecimal MONTHLY_EXEMPT_AMOUNT = new BigDecimal("100000.00");

    /** 3% 征收率减按 1% */
    public static final BigDecimal VAT_RATE_ONE_PERCENT = new BigDecimal("0.01");

    /** 放弃减按，按 3% 征收率 */
    public static final BigDecimal VAT_RATE_THREE_PERCENT = new BigDecimal("0.03");

    /** 经营所得个人所得税预缴率：0.5% */
    public static final BigDecimal IIT_RATE = new BigDecimal("0.005");

    /** 附加税费综合征收率：12% × 减半 = 6% */
    public static final BigDecimal SURCHARGE_RATE = new BigDecimal("0.06");

    /** 次月申报期截止日（日），简化处理：固定为次月 15 日 */
    public static final int DECLARATION_DEADLINE_DAY = 15;

    /** 申报期临近预警阈值（天）：截止日前 5 天起提醒 */
    public static final int DEADLINE_WARNING_DAYS = 5;

    /** 汇算清缴截止：次年 3 月 31 日 */
    public static final int ANNUAL_SETTLEMENT_DEADLINE_MONTH = 3;
    public static final int ANNUAL_SETTLEMENT_DEADLINE_DAY = 31;

    private IcbcTaxConstants() {
    }

}
