package cn.iocoder.yudao.module.icbc.service.callback;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.CALLBACK_DATA_FORMAT_ERROR;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IcbcNotifyParser} 的单元测试类
 */
public class IcbcNotifyParserTest {

    private final IcbcNotifyParser parser = new IcbcNotifyParser();

    @Test
    public void testParse_base64Envelope() {
        String payload = "{\"notifyType\":\"02\",\"outOrderId\":\"ORDER_1\",\"transNo\":\"T_1\"}";
        String body = "{\"notifyData\":\"" + base64(payload) + "\",\"signData\":\"SIGN\"}";

        IcbcNotifyMessage message = parser.parse(body);

        assertEquals(CallbackNotifyTypeEnum.PAYMENT, message.getNotifyType());
        assertEquals("ORDER_1", message.getBusinessId());
        assertEquals("T_1", message.getNotifyId());
        assertEquals(payload, message.getNotifyData());
        assertEquals("SIGN", message.getSign());
    }

    @Test
    public void testParse_plainJson() {
        String body = "{\"notifyType\":\"09\",\"outRedOffsetId\":\"RED_1\",\"transNo\":\"T_RED\"}";

        IcbcNotifyMessage message = parser.parse(body);

        assertEquals(CallbackNotifyTypeEnum.RED_REVOKE, message.getNotifyType());
        assertEquals("RED_1", message.getBusinessId());
        assertEquals("T_RED", message.getNotifyId());
    }

    @Test
    public void testParse_derivedNotifyIdIsStableAndContentSensitive() {
        String first = "{\"notifyType\":\"03\",\"outOrderId\":\"ORDER_2\",\"invoiceStatus\":\"02\"}";
        String second = "{\"notifyType\":\"03\",\"outOrderId\":\"ORDER_2\",\"invoiceStatus\":\"04\"}";

        IcbcNotifyMessage firstMessage = parser.parse(first);
        IcbcNotifyMessage repeatMessage = parser.parse(first);
        IcbcNotifyMessage secondMessage = parser.parse(second);

        // 同一内容重复到达 → 同一幂等键（可去重）
        assertEquals(firstMessage.getNotifyId(), repeatMessage.getNotifyId());
        // 同单不同状态 → 不同幂等键（各自成行）
        assertNotEquals(firstMessage.getNotifyId(), secondMessage.getNotifyId());
    }

    @Test
    public void testParse_infersFaceVerifyWithoutNotifyType() {
        String body = "{\"appId\":\"A\",\"transNode\":\"N1\",\"outUserId\":\"USER_1\",\"verifyResult\":\"1\"}";

        IcbcNotifyMessage message = parser.parse(body);

        assertEquals(CallbackNotifyTypeEnum.FACE_VERIFY, message.getNotifyType());
        assertEquals("USER_1", message.getBusinessId());
    }

    @Test
    public void testParse_infersPayeeOnboardingWithoutNotifyType() {
        String body = "{\"appId\":\"A\",\"outUserId\":\"USER_2\",\"result\":\"pass\",\"openacctStatus\":\"02\"}";

        IcbcNotifyMessage message = parser.parse(body);

        assertEquals(CallbackNotifyTypeEnum.PAYEE_ONBOARDING, message.getNotifyType());
        assertEquals("USER_2", message.getBusinessId());
    }

    @Test
    public void testParse_invalidFormat() {
        assertServiceException(() -> parser.parse("not-json"), CALLBACK_DATA_FORMAT_ERROR);
        assertServiceException(() -> parser.parse("{\"notifyType\":\"99\"}"), CALLBACK_DATA_FORMAT_ERROR);
        assertServiceException(() -> parser.parse(""), CALLBACK_DATA_FORMAT_ERROR);
    }

    private String base64(String payload) {
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

}
