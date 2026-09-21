package cn.iocoder.yudao.module.icbc.service.cardrecognition;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 卡证识别端口：身份证（人像面 / 国徽面）与银行卡。
 *
 * <p>建档向导（#91）要在废品堆前读出示身份证与银行卡上的姓名、证件号、住址、证件有效期、
 * 卡号与开户行，收货员只做确认与修正。识别能力来自第三方卡证识别服务（目标供应商腾讯云 OCR），
 * 与工行的银税通道是两回事，因此**单列一个端口、不进 {@code IcbcGateway}**——后者是
 * 「平台对工行的唯一出站端口」（ADR 0009），这条边界见 ADR 0037。
 *
 * <p><b>也不扩 {@link cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort}</b>：
 * 那个端口的语义是「收购现场识别」，服务的是**货物流证据**（磅单、车牌）；本端口服务的是
 * **主体准入**。ADR 0037 给了三条理由：消费者不同（建档向导 vs 收购登记）；失败语义相反
 * （磅单识别失败人工补录即可，银行卡识别失败会挡住收方入驻）；供应商产品线不同。
 *
 * <p><b>契约要点</b>：
 * <ul>
 *   <li>实现只有一枚常驻 Bean（{@code TencentCardRecognition}），供应商（{@code stub} / {@code tencent}）
 *       由后台配置在**调用时**判定（#103）：{@code stub} 或配置不齐时一律返回空结果，向导退化为手工录入，
 *       仍然走得完，落库结果与识别成功时同形（ADR 0037）。识别失败、未配置、额度耗尽
 *       走的是同一条降级路径，都**不阻断建档**。</li>
 *   <li>入参是 <b>base64</b>，而 {@code AcquisitionRecognitionPort} 收 {@code imageUrl}：
 *       证件与银行卡的影像**不留存**，识别完即弃、不进文件服务、不落库（ADR 0037），
 *       所以没有可传的 URL。调用方负责在识别前压缩，base64 后不超过 10M（厂商限制，#93）。</li>
 *   <li>结果里的证件有效期等字段已经是**库内格式**（{@code yyyy-MM-dd}，长期为
 *       {@code 9999-12-30}）：厂商格式由实现负责转换，业务层不感知厂商差异（#93）。</li>
 *   <li>「只在空缺处回填、人工输入的值优先」是**业务层**的事，端口只负责「读出了什么」。</li>
 * </ul>
 */
public interface CardRecognitionPort {

    /**
     * 识别身份证人像面：姓名、证件号、住址。
     *
     * @param imageBase64 压缩后的身份证人像面照片（base64）
     * @return 识别结果；读不出来时返回 {@link IdCardFront#empty()}
     */
    IdCardFront recognizeIdCardFront(String imageBase64);

    /**
     * 识别身份证国徽面：证件签发日期与证件有效期。
     *
     * @param imageBase64 压缩后的身份证国徽面照片（base64）
     * @return 识别结果；读不出来时返回 {@link IdCardBack#empty()}
     */
    IdCardBack recognizeIdCardBack(String imageBase64);

    /**
     * 识别银行卡：卡号、开户行、是否我行卡。
     *
     * @param imageBase64 压缩后的银行卡照片（base64）
     * @return 识别结果；读不出来时返回 {@link BankCard#empty()}
     */
    BankCard recognizeBankCard(String imageBase64);

    /**
     * 身份证人像面的识别结果。
     */
    @Data
    @Builder
    class IdCardFront {

        private String name;

        private String idCardNo;

        private String address;

        /**
         * 图片质量分（0-100）；为空表示未识别
         */
        private Integer qualityScore;

        /**
         * 提示类告警（可读文案）：复印件 / 翻拍 / 边框不完整 / 反光……
         *
         * <p>展示给收货员当场判断，**不拦继续**；质量分偏低时提示重拍，也不硬拦（#93）。
         */
        private List<String> warnings;

        /**
         * 硬拦原因（可读文案）：非空即**不可继续**（如「临时身份证」）。
         *
         * <p>与 {@link #warnings} 的区别只有一条：这里非空就必须挡住，不能只做提示。
         */
        private List<String> blockReasons;

        public static IdCardFront empty() {
            return IdCardFront.builder().build();
        }

    }

    /**
     * 身份证国徽面的识别结果。
     */
    @Data
    @Builder
    class IdCardBack {

        /**
         * 证件签发日期 {@code yyyy-MM-dd}
         */
        private String idSignDate;

        /**
         * 证件有效期截止 {@code yyyy-MM-dd}；长期为 {@code 9999-12-30}
         *
         * <p>它是工行收方入驻的必输项，也是「抄错一个就开不出票」的高危字段。
         */
        private String idValidityPeriod;

        /**
         * 图片质量分（0-100）；为空表示未识别
         */
        private Integer qualityScore;

        /**
         * 提示类告警（可读文案），不拦继续
         */
        private List<String> warnings;

        /**
         * 硬拦原因（可读文案）：非空即不可继续（如「有效期不合法」）
         */
        private List<String> blockReasons;

        public static IdCardBack empty() {
            return IdCardBack.builder().build();
        }

    }

    /**
     * 银行卡的识别结果。
     */
    @Data
    @Builder
    class BankCard {

        private String bankCardNo;

        /**
         * 开户银行（厂商返回的行名可能带联行号后缀，由实现清掉；ADR 0035 已定不做联行号）
         */
        private String bankName;

        /**
         * 是否我行用户：{@code 1}-我行用户，{@code 0}-非我行用户。
         *
         * <p>由开户行名推断（开户行为工商银行即 1）。它取代了原先「没人确认过的缺省 1」
         * （{@code DEFAULT_ACCOUNT_CODE}），但**仍以本人在确认页的选择为准**（#93）。
         */
        private String accountCode;

        /**
         * 图片质量分（0-100）；为空表示未识别
         */
        private Integer qualityScore;

        /**
         * 提示类告警（可读文案），不拦继续
         */
        private List<String> warnings;

        /**
         * 硬拦原因（可读文案）：非空即不可继续（如「这是电子银行卡信息截图，不是实体卡照片」）
         */
        private List<String> blockReasons;

        public static BankCard empty() {
            return BankCard.builder().build();
        }

    }

}
