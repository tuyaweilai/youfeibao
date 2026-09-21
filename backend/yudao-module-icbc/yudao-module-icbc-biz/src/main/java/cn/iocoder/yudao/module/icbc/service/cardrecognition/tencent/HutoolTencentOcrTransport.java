package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 用 Hutool 发 HTTP 的传输层实现（#93）：整个模块里唯一 new {@code HttpRequest} 的地方。
 *
 * <p>不记请求体（影像 base64）、不记响应体（PII）：日志只在 {@link TencentOcrClient} 里记
 * 厂商错误码与 {@code RequestId}（ADR 0037「识别完即弃」）。
 */
@Component
public class HutoolTencentOcrTransport implements TencentOcrTransport {

    @Override
    public Result post(Request request) {
        HttpRequest httpRequest = HttpRequest.post(request.url());
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            httpRequest.header(header.getKey(), header.getValue());
        }
        HttpResponse response = httpRequest
                .body(request.body())
                .timeout(request.timeoutMillis())
                .execute();
        return new Result(response.getStatus(), response.body());
    }

}
