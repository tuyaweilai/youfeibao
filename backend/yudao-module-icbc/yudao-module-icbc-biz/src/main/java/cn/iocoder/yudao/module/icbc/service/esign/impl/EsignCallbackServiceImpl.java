package cn.iocoder.yudao.module.icbc.service.esign.impl;

import cn.iocoder.yudao.module.icbc.service.esign.EsignCallbackService;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 电子签章签署状态通知 Service 实现（#92，ADR 0036）。
 *
 * <p>真正的验签与解析在 {@link EsignPort#parseCallback} 里（端口实现持有平台密钥）；
 * 这一层只把端口的**受控拒绝**（{@link EsignPort.EsignCallbackRejectedException}）翻译成平台统一的
 * 错误码：验签失败 / 伪造通知**明确失败**，绝不静默返回空。租户编号由端口从子客编号反查后带回，
 * 本层只校验它非空——查不到不猜、不乱写。
 *
 * <p><b>只捕获受控拒绝</b>：端口的 NPE / 数据库异常等不属于「拒绝」，原样向上抛，
 * 不让真 bug 被伪装成伪造攻击。
 */
@Service
@Validated
@Slf4j
public class EsignCallbackServiceImpl implements EsignCallbackService {

    @Resource
    private EsignPort esignPort;

    @Override
    public EsignPort.EsignCallback handle(String signature, String timestamp, String nonce, String body) {
        EsignPort.EsignCallback callback;
        try {
            callback = esignPort.parseCallback(signature, timestamp, nonce, body);
        } catch (EsignPort.EsignCallbackRejectedException e) {
            // 端口明确拒绝（验签失败 / 未开通 / 报文损坏）才翻译成「回调被拒绝」；
            // 其它运行时异常照实抛出，不把 NPE / DB 异常伪装成伪造攻击
            log.warn("[handle][电子签章回调被拒绝] reason={}", e.getMessage(), e);
            throw exception(ESIGN_CALLBACK_REJECTED, e.getMessage());
        }
        if (callback == null) {
            // 端口契约不允许返回 null（那是「空通知」，会把伪造通知静默吞掉）
            throw exception(ESIGN_CALLBACK_VERIFY_FAILED);
        }
        if (callback.getTenantId() == null) {
            throw exception(ESIGN_SUB_CUSTOMER_NOT_RESOLVED, "（空）");
        }
        return callback;
    }

}
