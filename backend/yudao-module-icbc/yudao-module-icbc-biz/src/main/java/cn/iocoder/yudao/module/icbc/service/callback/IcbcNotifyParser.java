package cn.iocoder.yudao.module.icbc.service.callback;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.CALLBACK_DATA_FORMAT_ERROR;

/**
 * 工行异步通知报文解析器
 *
 * 报文外层为 {@code { "notifyData": "<base64 JSON>", "signData": "..." }}，
 * 接收变量名为 {@code biz_content}。兼容直接传明文 JSON 的情况。
 */
@Component
public class IcbcNotifyParser {

    /**
     * 解析报文
     */
    public IcbcNotifyMessage parse(String body) {
        if (StrUtil.isBlank(body)) {
            throw exception(CALLBACK_DATA_FORMAT_ERROR);
        }
        String trimmed = body.trim();
        JSONObject outer = tryParseJson(trimmed);
        String sign = outer != null ? outer.getString("signData") : null;
        String decoded = decodeNotifyData(trimmed, outer);
        JSONObject payload = parseJson(decoded);
        String notifyTypeCode = payload.getString("notifyType");
        CallbackNotifyTypeEnum notifyType = CallbackNotifyTypeEnum.of(notifyTypeCode);
        if (notifyType == null) {
            throw exception(CALLBACK_DATA_FORMAT_ERROR);
        }
        String businessId = firstNonBlank(payload, "outOrderId", "outRedOffsetId", "outUserId", "transNo");
        String notifyId = firstNonBlank(payload, "notifyId", "transNo");
        if (StrUtil.isBlank(notifyId)) {
            // 工行不保证返回通知唯一号：用类型 + 业务号 + 内容摘要构造稳定幂等键，
            // 既能让「重复到达」去重，又能让「同单不同状态的两次通知」各自成行
            notifyId = notifyType.getType() + ":" + StrUtil.blankToDefault(businessId, "unknown") + ":"
                    + DigestUtil.sha1Hex(decoded);
        }
        return IcbcNotifyMessage.builder()
                .notifyId(notifyId)
                .notifyType(notifyType)
                .businessId(businessId)
                .notifyData(decoded)
                .sign(sign)
                .build();
    }

    /**
     * 取出并 base64 解码 notifyData；无外层信封时按明文处理
     */
    private String decodeNotifyData(String body, JSONObject outer) {
        String notifyData = outer != null ? outer.getString("notifyData") : null;
        if (StrUtil.isBlank(notifyData)) {
            return body;
        }
        try {
            return new String(Base64.getDecoder().decode(notifyData), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            try {
                return new String(Base64.getUrlDecoder().decode(notifyData), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                throw exception(CALLBACK_DATA_FORMAT_ERROR);
            }
        }
    }

    private JSONObject parseJson(String text) {
        JSONObject json = tryParseJson(text);
        if (json == null) {
            throw exception(CALLBACK_DATA_FORMAT_ERROR);
        }
        return json;
    }

    private JSONObject tryParseJson(String text) {
        try {
            return JSONObject.parseObject(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String firstNonBlank(JSONObject json, String... keys) {
        for (String key : keys) {
            String value = json.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

}
