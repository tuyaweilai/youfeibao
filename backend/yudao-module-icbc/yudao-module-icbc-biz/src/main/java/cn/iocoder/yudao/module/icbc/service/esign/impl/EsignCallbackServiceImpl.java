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
 * 这一层负责把「端口怎么失败」翻译成平台统一的错误码：验签失败 / 伪造通知**明确失败**，
 * 绝不静默返回空。租户编号由端口从子客编号反查后带回，本层只校验它非空——查不到不猜、不乱写。
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
        } catch (RuntimeException e) {
            // 端口把验签失败 / 未开通 / 报文损坏都抛了出来，这里统一落成可读的拒绝原因
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
