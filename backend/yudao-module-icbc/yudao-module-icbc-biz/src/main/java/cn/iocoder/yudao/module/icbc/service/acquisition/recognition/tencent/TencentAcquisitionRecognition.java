package cn.iocoder.yudao.module.icbc.service.acquisition.recognition.tencent;

import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionConfigService;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionEffectiveConfig;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrClient;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * 收购现场识别端口的**唯一常驻实现**（#112）：车牌识别走腾讯云 OCR 的 {@code LicensePlateOCR}。
 *
 * <p><b>为什么与卡证识别共用一份配置</b>：腾讯云的车牌识别与身份证 / 银行卡识别是**同一个产品**
 * （{@code ocr} / {@code 2018-11-19}）下的不同 action，同一套 SecretId / SecretKey 与 endpoint，
 * 额度也在同一个账号里。ADR 0037 原先那句「不是同一个产品」是错的，已在 #112 更正；端口分离的
 * 结论不变，因为**消费者与失败语义仍然不同**（收购登记的货物流证据 vs 建档的主体准入）。
 *
 * <p><b>供应商运行期判定</b>：照 {@code TencentCardRecognition} 的先例，每次调用向
 * {@link CardRecognitionConfigService#resolveEffectiveConfig()} 取一份生效参数——后台改完保存即生效，
 * 无需重启。
 *
 * <p><b>降级路径只有一条</b>：{@code provider=stub}、未配密钥、网络 / 超时、厂商报错（含额度耗尽、
 * 识别接口未开通）都返回**空结果**，现场退化为手工录入、不阻断登记（ADR 0013 / 0037）。
 * 绝不把异常抛给现场端。两个 action 走同一条公共路径 {@code call}：
 * <ul>
 *   <li>车牌 {@code LicensePlateOCR}（#112）；</li>
 *   <li>磅单 {@code GeneralBasicOCR} 通用印刷体 + 平台侧解析（#113）。</li>
 * </ul>
 */
@Slf4j
@Component
public class TencentAcquisitionRecognition implements AcquisitionRecognitionPort {

    private final CardRecognitionConfigService configService;
    private final TencentOcrClient client;
    /** provider=tencent 却没配密钥时只提示一次，避免现场每拍一张照片就刷一条日志 */
    private final AtomicBoolean missingConfigLogged = new AtomicBoolean();

    /**
     * 构造注入：HTTP 客户端是 Spring Bean，测试可换成假的，「厂商报错也走同一条降级路径」因此能被钉住。
     */
    @Autowired
    public TencentAcquisitionRecognition(CardRecognitionConfigService configService, TencentOcrClient client) {
        this.configService = configService;
        this.client = client;
    }

    /**
     * 磅单字段识别（#113）：通用印刷体识别（{@code GeneralBasicOCR}）+ 平台侧解析。
     *
     * <p>选通用印刷体而不是厂商的结构化抽取（{@code SmartStructuralOCR}），是拿真实磅单
     * {@code 参考/过磅模版.jpg} 实测后的结论：结构化抽取把车号与总重丢了、把净重读成单位「Kg」、
     * 把 19130 挂到了「备注」上；而通用印刷体把这 33 行连坐标读得干干净净，
     * 解析后 32220 − 13090 = 19130 与单子上的净重完全自洽。详见 ADR 0013 的修订节。
     *
     * <p>解析不出来的字段一律留空，原始文字行随结果一起回（现场端可展开核对），不落库。
     */
    @Override
    public WeightTicketRecognition recognizeWeightTicket(String imageBase64) {
        return call("GeneralBasicOCR", imageBase64,
                response -> TencentWeightTicketParser.parse(TencentWeightTicketParser.toLines(response)),
                WeightTicketRecognition.empty());
    }

    @Override
    public PlateRecognition recognizePlate(String imageBase64) {
        return call("LicensePlateOCR", imageBase64, TencentPlateResultMapper::toPlate, PlateRecognition.empty());
    }

    /**
     * 一次识别的公共路径：读生效配置（失败 / 未启用 / 未配齐一律空结果，不触网）→ 打厂商 → 交给映射。
     *
     * <p>降级路径只有这一条（ADR 0013 / 0037 的安静降级）：现场端看到的是空结果，退化为手工录入。
     */
    private <T> T call(String action, String imageBase64, Function<JSONObject, T> mapper, T empty) {
        CardRecognitionEffectiveConfig effective;
        try {
            effective = configService.resolveEffectiveConfig();
        } catch (RuntimeException e) {
            // 每次识别都读一次库：配置读不出来（表未迁移 / DB 抖动）也必须安静降级，不能把收货员卡在识别上
            log.warn("[call][读取 OCR 平台配置失败，识别返回空结果，不阻断登记：action={}, error={}]",
                    action, e.getMessage());
            return empty;
        }
        if (!effective.isTencent()) {
            // 未启用（stub）是有意的配置：安静降级，不刷日志、不触网
            return empty;
        }
        if (!effective.hasCredentials()) {
            if (missingConfigLogged.compareAndSet(false, true)) {
                log.warn("[call][OCR 选的是 tencent 但未配密钥（后台「平台运营 / 卡证识别」"
                        + "或 icbc.card-recognition.secret-id/secret-key），识别一律返回空结果，现场手工录入]");
            }
            return empty;
        }
        JSONObject payload = new JSONObject();
        // 识别接口要的是纯 base64：前端已不带 dataURL 前缀，这里再兜一道
        payload.put("ImageBase64", TencentOcrClient.plainBase64(imageBase64));
        try {
            JSONObject response = client.call(effective.toOcrSettings(), action, payload);
            return response == null ? empty : mapper.apply(response);
        } catch (RuntimeException e) {
            // 映射 / 解析层出意外也算识别失败：手工录入那条路必须始终能走
            log.warn("[call][腾讯云 OCR 结果处理失败：action={}, error={}]", action, e.getMessage());
            return empty;
        }
    }

}
