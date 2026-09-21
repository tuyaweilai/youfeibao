package cn.iocoder.yudao.module.icbc.service.cardrecognition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link StubCardRecognition} 的契约测试。
 *
 * <p>锁住的是 ADR 0037 的降级承诺：**未配置时不触网、不报错、不阻断建档**——返回可用的空结果
 * 而不是抛异常或返回 null，这样建档向导可以直接拿它走完手工录入那条路。
 */
public class StubCardRecognitionTest {

    private final CardRecognitionPort port = new StubCardRecognition();

    @Test
    public void testIdCardFrontIsEmptyAndNotNull() {
        CardRecognitionPort.IdCardFront result = port.recognizeIdCardFront("base64");

        assertNotNull(result);
        assertNull(result.getName());
        assertNull(result.getIdCardNo());
        assertNull(result.getAddress());
    }

    @Test
    public void testIdCardBackIsEmptyAndNotNull() {
        CardRecognitionPort.IdCardBack result = port.recognizeIdCardBack("base64");

        assertNotNull(result);
        assertNull(result.getIdSignDate());
        assertNull(result.getIdValidityPeriod());
    }

    @Test
    public void testBankCardIsEmptyAndNotNull() {
        CardRecognitionPort.BankCard result = port.recognizeBankCard("base64");

        assertNotNull(result);
        assertNull(result.getBankCardNo());
        assertNull(result.getBankName());
        assertNull(result.getAccountCode());
    }

}
