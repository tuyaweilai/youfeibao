package cn.iocoder.yudao.module.icbc.service.cardrecognition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 默认的卡证识别实现：不识别，一律返回空结果。
 *
 * <p>一期没有接第三方卡证识别服务（真实接入见 #93），因此这里保持空实现：**不触网、不报错、
 * 不阻断建档**，向导退化为手工录入，落库结果与识别成功时同形（ADR 0037）。
 *
 * <p>接入真实服务时替换本 Bean（#93 的腾讯云 OCR 实现，{@link cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentCardRecognition}）：
 * 两个 Bean 不能同时在场，都按 {@code icbc.card-recognition.mode} 用 {@code @ConditionalOnProperty}
 * 二选一（照 {@code IcbcSdkGateway} / {@code FakeIcbcGateway} 的做法）。本类带
 * {@code matchIfMissing = true}，所以**未配置时生效的仍是它**、一律返回空结果。
 */
@Component
@ConditionalOnProperty(prefix = "icbc.card-recognition", name = "mode", havingValue = "stub", matchIfMissing = true)
public class StubCardRecognition implements CardRecognitionPort {

    @Override
    public IdCardFront recognizeIdCardFront(String imageBase64) {
        return IdCardFront.empty();
    }

    @Override
    public IdCardBack recognizeIdCardBack(String imageBase64) {
        return IdCardBack.empty();
    }

    @Override
    public BankCard recognizeBankCard(String imageBase64) {
        return BankCard.empty();
    }

}
