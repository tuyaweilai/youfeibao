package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * 卡证识别的腾讯云实现（#93，ADR 0037）。
 *
 * <p>三个 Action：{@code IDCardOCR}（{@code CardSide=FRONT} / {@code BACK}）与 {@code BankCardOCR}。
 * 厂商报文的映射（有效期转换、行名剥联行号、是否我行卡推断、电子卡截图拒收、告警分级）全在
 * {@link TencentOcrResultMapper}，这里只负责「把图片送出去、把结果收回来」。
 *
 * <p><b>降级路径只有一条</b>：未配置密钥、网络 / 超时、厂商报错（含额度耗尽）都返回**空结果**，
 * 向导退化为手工录入、不阻断建档（ADR 0037）。绝不把异常抛给向导。
 *
 * <p>与 {@link cn.iocoder.yudao.module.icbc.service.cardrecognition.StubCardRecognition} 二选一：
 * 由 {@code icbc.card-recognition.mode} 决定，{@code stub} 带 {@code matchIfMissing = true}
 * （未配置即 stub，本地 / 未接厂商时照常能跑）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "icbc.card-recognition", name = "mode", havingValue = "tencent")
public class TencentCardRecognition implements CardRecognitionPort {

    /** 身份证识别：启用告警与质量分，才会在 AdvancedInfo 里回这些字段（{@code Config} 是文档参数）。 */
    private static final String ID_CARD_CONFIG =
            "{\"CopyWarn\":true,\"BorderCheckWarn\":true,\"ReshootWarn\":true,"
                    + "\"DetectPsWarn\":true,\"TempIdWarn\":true,\"Quality\":true}";

    private final TencentCardRecognitionProperties properties;
    private final TencentOcrClient client;
    /** 未配置密钥只提示一次，避免每次拍照都刷日志 */
    private final AtomicBoolean missingConfigLogged = new AtomicBoolean();

    @Autowired
    public TencentCardRecognition(TencentCardRecognitionProperties properties) {
        this(properties, new TencentOcrClient(properties));
    }

    /** 测试用：注入一个假的客户端，不触网。 */
    TencentCardRecognition(TencentCardRecognitionProperties properties, TencentOcrClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public IdCardFront recognizeIdCardFront(String imageBase64) {
        JSONObject payload = new JSONObject();
        payload.put("ImageBase64", plainBase64(imageBase64));
        payload.put("CardSide", "FRONT");
        payload.put("Config", ID_CARD_CONFIG);
        return mapOrEmpty("IDCardOCR", payload, TencentOcrResultMapper::toIdCardFront, IdCardFront.empty());
    }

    @Override
    public IdCardBack recognizeIdCardBack(String imageBase64) {
        JSONObject payload = new JSONObject();
        payload.put("ImageBase64", plainBase64(imageBase64));
        payload.put("CardSide", "BACK");
        payload.put("Config", ID_CARD_CONFIG);
        return mapOrEmpty("IDCardOCR", payload, TencentOcrResultMapper::toIdCardBack, IdCardBack.empty());
    }

    @Override
    public BankCard recognizeBankCard(String imageBase64) {
        JSONObject payload = new JSONObject();
        payload.put("ImageBase64", plainBase64(imageBase64));
        // 不送 Config：BankCardOCR 默认就回 CardNo / BankInfo / CardType / Quality，
        // 而 Config 是否是它接受的参数没有离线可验证的依据（#93 报告里如实记）——
        // 宁可少要一项质量分，也不能送一个不确定的参数把整次识别打挂。
        return mapOrEmpty("BankCardOCR", payload, TencentOcrResultMapper::toBankCard, BankCard.empty());
    }

    private <T> T mapOrEmpty(String action, JSONObject payload, Function<JSONObject, T> mapper, T empty) {
        if (!configured()) {
            return empty;
        }
        try {
            JSONObject response = client.call(action, payload);
            return response == null ? empty : mapper.apply(response);
        } catch (RuntimeException e) {
            // 映射层出意外也算识别失败：手工录入那条路必须始终能走
            log.warn("[mapOrEmpty][腾讯云 OCR 结果处理失败：action={}, error={}]", action, e.getMessage());
            return empty;
        }
    }

    private boolean configured() {
        if (StrUtil.isNotBlank(properties.getSecretId()) && StrUtil.isNotBlank(properties.getSecretKey())) {
            return true;
        }
        if (missingConfigLogged.compareAndSet(false, true)) {
            log.warn("[configured][腾讯云卡证识别未配置密钥（icbc.card-recognition.secret-id/secret-key），"
                    + "识别一律返回空结果，向导退化为手工录入]");
        }
        return false;
    }

    /**
     * 去掉 dataURL 前缀：识别接口要的是纯 base64。前端已按注释不带前缀，这里再兜一道，
     * 免得把 {@code data:image/jpeg;base64,} 当成影像内容发给厂商。
     */
    static String plainBase64(String imageBase64) {
        if (imageBase64 == null) {
            return null;
        }
        int index = imageBase64.indexOf("base64,");
        return index >= 0 ? imageBase64.substring(index + "base64,".length()) : imageBase64;
    }

}
