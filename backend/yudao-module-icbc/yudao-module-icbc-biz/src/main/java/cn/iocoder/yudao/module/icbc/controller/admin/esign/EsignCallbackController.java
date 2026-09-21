package cn.iocoder.yudao.module.icbc.controller.admin.esign;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignCallbackRespVO;
import cn.iocoder.yudao.module.icbc.service.esign.EsignCallbackService;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 电子签章签署状态通知入口（#92，ADR 0036）。
 *
 * <p>第三方推来的签署状态**从这个唯一入口进来**：验签 → 归一化 → 反查租户。报文里带的是
 * 子客编号而不是租户号，所以实现里由 {@link EsignPort#parseCallback} 反查出 {@code tenantId}
 * 后交回；本票不改协议状态（属 #95）。
 *
 * <p><b>验签失败必须明确失败</b>：伪造的通知不能被静默吞掉，接口返回错误码而不是「成功但什么都没做」。
 *
 * <p>回调不带我们的登录态、也不带租户请求头，因此本接口 {@link PermitAll} 免登录，租户由子客编号反查。
 */
@Tag(name = "管理后台 - 电子签章签署状态通知入口")
@RestController
@RequestMapping("/icbc/esign/callback")
@Validated
@Slf4j
public class EsignCallbackController {

    /** 第三方回调签名相关请求头 */
    static final String HEADER_SIGNATURE = "X-TC-Signature";
    static final String HEADER_TIMESTAMP = "X-TC-Timestamp";
    static final String HEADER_NONCE = "X-TC-Nonce";

    @Resource
    private EsignCallbackService esignCallbackService;

    @PostMapping("/notify")
    @PermitAll
    @Operation(summary = "接收电子签章签署状态通知",
            description = "验签失败会明确返回错误码，不静默吞掉；成功时回带反查出的租户编号供后续路由")
    public CommonResult<EsignCallbackRespVO> receiveNotify(HttpServletRequest request) throws IOException {
        String body = request.getParameter("biz_content");
        if (body == null) {
            body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        }
        String signature = firstNonBlank(request.getHeader(HEADER_SIGNATURE), request.getParameter("signature"));
        String timestamp = firstNonBlank(request.getHeader(HEADER_TIMESTAMP), request.getParameter("timestamp"));
        String nonce = firstNonBlank(request.getHeader(HEADER_NONCE), request.getParameter("nonce"));
        log.info("接收电子签章签署状态通知，报文字节数: {}", body == null ? 0 : body.length());
        EsignPort.EsignCallback callback = esignCallbackService.handle(signature, timestamp, nonce, body);
        return success(toRespVO(callback));
    }

    private static EsignCallbackRespVO toRespVO(EsignPort.EsignCallback callback) {
        EsignCallbackRespVO resp = new EsignCallbackRespVO();
        resp.setTenantId(callback.getTenantId());
        resp.setSignTaskId(callback.getSignTaskId());
        resp.setFinished(callback.isFinished());
        resp.setUnfinishedReason(callback.getUnfinishedReason());
        resp.setSignedAt(callback.getSignedAt());
        return resp;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isEmpty() ? first : second;
    }

}
