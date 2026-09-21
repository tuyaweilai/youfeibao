package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import java.util.Map;

/**
 * 腾讯云 OCR 的 HTTP 传输层（#93）。
 *
 * <p>单独抽出这一层是为了让「厂商报错也走同一条降级路径」这件事**可测**：真正 new
 * {@code HttpRequest} 的代码只剩 {@link HutoolTencentOcrTransport}，测试注入一个返回固定
 * 状态码 / 响应体的假传输层，就能钉住 {@link TencentOcrClient} 对 Error 节点、非 2xx、
 * 非 JSON、缺 {@code Response} 的处理（独立评审 ST-2）。
 *
 * <p>传输层只做「发出去、把原始状态与响应体拿回来」：分类（厂商错误 / 网络异常）留给
 * {@link TencentOcrClient}。实现遇到网络 / 超时异常时**抛 RuntimeException**，由客户端统一收成 null。
 */
public interface TencentOcrTransport {

    /**
     * 发一次 POST。
     *
     * @param request 已经拼好的请求（URL、请求头、请求体、超时）
     * @return 原始 HTTP 状态码与响应体
     * @throws RuntimeException 网络 / 超时 / DNS 等传输失败
     */
    Result post(Request request);

    /**
     * 一次出站请求的完整形状。
     */
    final class Request {

        private final String url;
        private final Map<String, String> headers;
        private final String body;
        private final int timeoutMillis;

        public Request(String url, Map<String, String> headers, String body, int timeoutMillis) {
            this.url = url;
            this.headers = headers;
            this.body = body;
            this.timeoutMillis = timeoutMillis;
        }

        public String url() {
            return url;
        }

        public Map<String, String> headers() {
            return headers;
        }

        public String body() {
            return body;
        }

        public int timeoutMillis() {
            return timeoutMillis;
        }
    }

    /**
     * 厂商的原始回应。
     */
    final class Result {

        private final int status;
        private final String body;

        public Result(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int status() {
            return status;
        }

        public String body() {
            return body;
        }
    }

}
