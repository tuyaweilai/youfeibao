package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 腾讯云 OCR 的客户端（#93）：拼请求、签名、把厂商响应里的 {@code Response} 节点交出去。
 *
 * <p>HTTP 走构造注入的 {@link TencentOcrTransport}（Spring 里是 {@link HutoolTencentOcrTransport}），
 * 所以「厂商报错」这条降级判据能被测试直接钉住，而不是只能靠断言（独立评审 ST-2）。
 *
 * <p><b>#103 起它是一枚常驻 Bean</b>：不再靠启动期的 {@code @ConditionalOnProperty} 决定在不在场，
 * 每次调用由调用方传入一份 {@link TencentOcrSettings}（DB 优先、空则回落 yaml / env）。连通性自检
 * 也走同一枚客户端，不另起一套 HTTP。
 *
 * <p><b>不记请求体、不记响应体</b>：请求体是证件 / 银行卡影像的 base64（ADR 0037 要求识别完即弃），
 * 响应体是姓名 / 身份证号 / 住址 / 银行卡号。日志里只留厂商错误码与 {@code RequestId}，
 * 这两样都不带 PII，却是排查额度耗尽与联调问题的关键。
 *
 * <p>返回 {@code null} 的情况（网络 / 非 2xx / 厂商报错 / 响应不可解析）在调用方走同一条降级路径：
 * 向导退化为手工录入（ADR 0037）。
 */
@Slf4j
@Component
public class TencentOcrClient {

    /** 服务名（签名用）：腾讯云 OCR 的固定值，不是可配项 */
    private static final String SERVICE = "ocr";
    /** API 版本（签名与请求头用）：腾讯云 OCR 的固定值，不是可配项 */
    private static final String VERSION = "2018-11-19";

    private final TencentOcrTransport transport;

    @Autowired
    public TencentOcrClient(TencentOcrTransport transport) {
        this.transport = transport;
    }

    /**
     * 调用一个 OCR Action，厂商报错（含额度耗尽）也收成 {@code null}。
     *
     * @param settings 这一次调用用的生效参数（密钥 / 地域 / endpoint / 超时）
     * @param action   Action 名（{@code IDCardOCR} / {@code BankCardOCR}）
     * @param payload  请求体（不含公共参数，公共参数以请求头与签名承载）
     * @return 厂商响应里的 {@code Response} 节点；调用失败或厂商报错时返回 {@code null}
     */
    public JSONObject call(TencentOcrSettings settings, String action, JSONObject payload) {
        JSONObject response = callRaw(settings, action, payload);
        if (response == null) {
            return null;
        }
        JSONObject error = response.getJSONObject("Error");
        if (error != null) {
            // 额度耗尽（LimitExceeded / FailedOperation.NoEnoughQuota 等）与未配置走同一条降级路径
            log.warn("[call][腾讯云 OCR 返回错误：action={}, code={}, requestId={}]",
                    action, error.getString("Code"), response.getString("RequestId"));
            return null;
        }
        return response;
    }

    /**
     * 发一次请求并返回 {@code Response} 节点（**保留厂商错误**，供连通性自检判断签名是否被接受）。
     * 网络异常 / 非 2xx / 响应不是 JSON 时返回 {@code null}。
     */
    public JSONObject callRaw(TencentOcrSettings settings, String action, JSONObject payload) {
        long timestamp = System.currentTimeMillis() / 1000;
        String body = payload.toJSONString();
        String authorization = TencentOcrSigner.buildAuthorization(
                settings.getSecretId(), settings.getSecretKey(), settings.getEndpoint(),
                SERVICE, body, timestamp);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Authorization", authorization);
        headers.put("X-TC-Action", action);
        headers.put("X-TC-Timestamp", String.valueOf(timestamp));
        headers.put("X-TC-Version", VERSION);
        headers.put("X-TC-Region", settings.getRegion());
        TencentOcrTransport.Request request = new TencentOcrTransport.Request(
                "https://" + settings.getEndpoint(), headers, body, settings.getTimeout());
        try {
            TencentOcrTransport.Result result = transport.post(request);
            if (result.status() < 200 || result.status() >= 300) {
                log.warn("[callRaw][腾讯云 OCR 返回非 2xx：action={}, status={}]", action, result.status());
                return null;
            }
            return parseResponse(action, result.body());
        } catch (RuntimeException e) {
            // 弱网 / 超时 / DNS：识别失败不阻断建档，但要把原因留在日志里
            log.warn("[callRaw][腾讯云 OCR 调用异常：action={}, error={}]", action, e.getMessage());
            return null;
        }
    }

    private JSONObject parseResponse(String action, String body) {
        if (StrUtil.isBlank(body)) {
            log.warn("[parseResponse][腾讯云 OCR 响应为空：action={}]", action);
            return null;
        }
        JSONObject root;
        try {
            root = JSON.parseObject(body);
        } catch (RuntimeException e) {
            log.warn("[parseResponse][腾讯云 OCR 响应不是合法 JSON：action={}]", action);
            return null;
        }
        JSONObject result = root.getJSONObject("Response");
        if (result == null) {
            log.warn("[parseResponse][腾讯云 OCR 响应缺少 Response 节点：action={}]", action);
            return null;
        }
        return result;
    }

    /**
     * 去掉 dataURL 前缀：识别接口要的是纯 base64。
     *
     * <p>两个识别端口（卡证、收购现场的磅单与车牌）都要做这一步，所以判据只留这一处，
     * 不各自再写一遍（{@code IDCardOCR} / {@code BankCardOCR} / {@code LicensePlateOCR} 同理）。
     */
    public static String plainBase64(String imageBase64) {
        if (imageBase64 == null) {
            return null;
        }
        int index = imageBase64.indexOf("base64,");
        return index >= 0 ? imageBase64.substring(index + "base64,".length()) : imageBase64;
    }

}
