package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TencentOcrResultMapper} 的单元测试（#93 的主战场）。
 *
 * <p>喂的是按腾讯云 OCR 响应形状构造的**厂商报文样本**，不触网、不依赖 Spring。锁住的是
 * 「厂商格式 → 库内格式 / 可读文案」的映射与拒收判定，也就是本票验收里最容易出错的那几项：
 * 有效期两种写法、行名带联行号、是否我行卡推断、电子银行卡截图拒收、临时身份证与
 * 复印件 / 翻拍 / 边框 / 反光的分级。
 */
public class TencentOcrResultMapperTest {

    // ==================== 身份证人像面 ====================

    @Test
    public void testIdCardFront_mapsNameIdAddressAndQuality() {
        JSONObject response = obj(
                "Name", "张三",
                "Sex", "男",
                "Nation", "汉",
                "Birth", "1990/01/01",
                "Address", "北京市朝阳区某街道 1 号",
                "IdNum", "110101199001011234",
                "AdvancedInfo", advanced("{\"Quality\":\"88\",\"CopyWarn\":\"false\"}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertEquals("张三", result.getName());
        assertEquals("110101199001011234", result.getIdCardNo());
        assertEquals("北京市朝阳区某街道 1 号", result.getAddress());
        assertEquals(88, result.getQualityScore());
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.getBlockReasons().isEmpty());
    }

    @Test
    public void testIdCardFront_copyReshootBorderAndReflectAreWarningsNotBlocks() {
        JSONObject response = obj("Name", "张三",
                "AdvancedInfo", advanced("{\"CopyWarn\":\"true\",\"BorderCheckWarn\":true,"
                        + "\"ReshootWarn\":\"true\",\"WarnInfos\":[\"反光严重\"]}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertTrue(result.getWarnings().contains("身份证复印件"));
        assertTrue(result.getWarnings().contains("身份证边框不完整"));
        assertTrue(result.getWarnings().contains("身份证翻拍"));
        assertTrue(result.getWarnings().contains("反光严重"));
        assertTrue(result.getBlockReasons().isEmpty(), "提示类告警不拦继续");
    }

    @Test
    public void testIdCardFront_tempIdWarnIsABlock() {
        JSONObject response = obj("AdvancedInfo", advanced("{\"TempIdWarn\":\"true\"}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertEquals(1, result.getBlockReasons().size());
        assertTrue(result.getBlockReasons().get(0).contains("临时身份证"));
        assertFalse(result.getWarnings().contains("临时身份证"), "硬拦原因不进提示列表");
    }

    @Test
    public void testIdCardFront_emptyResponseIsEmptyAndNotNull() {
        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(new JSONObject());

        assertNotNull(result);
        assertNull(result.getName());
        assertNull(result.getQualityScore());
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.getBlockReasons().isEmpty());
    }

    @Test
    public void testIdCardFront_advancedInfoMissingIsTolerated() {
        JSONObject response = obj("Name", "李四", "IdNum", "110101199001011111");

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertEquals("李四", result.getName());
        assertNull(result.getQualityScore());
        assertTrue(result.getWarnings().isEmpty());
    }

    // ==================== 身份证国徽面：有效期 ====================

    @Test
    public void testIdCardBack_dotSeparatedValidityConvertedToDashFormat() {
        JSONObject response = obj(
                "Authority", "北京市公安局朝阳分局",
                "ValidDate", "2018.08.12-2038.08.12");

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertEquals("2018-08-12", result.getIdSignDate(), "点号分隔的签发日期要转成 yyyy-MM-dd");
        assertEquals("2038-08-12", result.getIdValidityPeriod(), "点号分隔的截止日期要转成 yyyy-MM-dd");
        assertTrue(result.getBlockReasons().isEmpty());
    }

    @Test
    public void testIdCardBack_longTermValidityMapsTo99991230() {
        JSONObject response = obj("ValidDate", "2018.08.12-长期");

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertEquals("2018-08-12", result.getIdSignDate());
        assertEquals("9999-12-30", result.getIdValidityPeriod());
    }

    @Test
    public void testIdCardBack_dashSeparatedValidityAlsoAccepted() {
        JSONObject response = obj("ValidDate", "2018-8-1-2038-8-9");

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertEquals("2018-08-01", result.getIdSignDate());
        assertEquals("2038-08-09", result.getIdValidityPeriod());
    }

    @Test
    public void testIdCardBack_unparseableValidityIsABlock() {
        JSONObject response = obj("ValidDate", "有效期见原件");

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertNull(result.getIdValidityPeriod());
        assertEquals(1, result.getBlockReasons().size());
        assertTrue(result.getBlockReasons().get(0).contains("有效期不合法"));
    }

    @Test
    public void testIdCardBack_illegalMonthDayIsABlock() {
        JSONObject response = obj("ValidDate", "2018.13.40-2038.02.30");

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertNull(result.getIdSignDate());
        assertTrue(result.getBlockReasons().contains("有效期不合法"));
    }

    @Test
    public void testIdCardBack_blankValidityIsNotABlock_manualEntryFallback() {
        JSONObject response = new JSONObject();

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertNull(result.getIdSignDate());
        assertNull(result.getIdValidityPeriod());
        assertTrue(result.getBlockReasons().isEmpty(), "厂商没给有效期不是拒收，收货员手工补录即可（ADR 0037）");
    }

    @Test
    public void testIdCardBack_authorityIsNotMappedAnywhere() {
        JSONObject response = obj(
                "Authority", "北京市公安局朝阳分局",
                "ValidDate", "2018.08.12-2038.08.12");

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        // 端口里没有签发机关字段：这里只断言结果形状，签发机关无处可落（ADR 0037）
        assertEquals("2018-08-12", result.getIdSignDate());
        assertEquals("2038-08-12", result.getIdValidityPeriod());
    }

    // ==================== 银行卡 ====================

    @Test
    public void testBankCard_icbcNameInfersAccountCodeOne() {
        JSONObject response = obj(
                "CardNo", "6222021234567890123",
                "BankInfo", "中国工商银行(03080000)",
                "Quality", 95);

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertEquals("6222021234567890123", result.getBankCardNo());
        assertEquals("中国工商银行", result.getBankName(), "尾部联行号要剥掉（ADR 0035 不做联行号）");
        assertEquals("1", result.getAccountCode());
        assertEquals(95, result.getQualityScore());
    }

    @Test
    public void testBankCard_nonIcbcNameInfersAccountCodeZero() {
        JSONObject response = obj("CardNo", "6225881234567890", "BankInfo", "招商银行(03080000)");

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertEquals("招商银行", result.getBankName());
        assertEquals("0", result.getAccountCode());
    }

    @Test
    public void testBankCard_emptyBankNameLeavesAccountCodeNull() {
        // 行名读不出来时不硬猜：accountCode 留空，让确认页按「本人确认 → 无」走缺省
        JSONObject response = obj("CardNo", "6222021234567890123");

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertNull(result.getBankName());
        assertNull(result.getAccountCode());
    }

    @Test
    public void testBankCard_electronicScreenshotIsABlock() {
        JSONObject response = obj(
                "CardNo", "6222021234567890123",
                "BankInfo", "中国工商银行(03080000)",
                "CardType", "电子银行卡信息截图");

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertEquals(1, result.getBlockReasons().size());
        assertTrue(result.getBlockReasons().get(0).contains("电子银行卡信息截图"));
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testBankCard_borderAndReflectAreWarnings() {
        JSONObject response = obj("CardNo", "6222021234567890123",
                "AdvancedInfo", advanced("{\"BorderCheckWarn\":\"true\",\"WarnInfos\":[\"反光\"]}"));

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertTrue(result.getWarnings().contains("银行卡边框不完整"));
        assertTrue(result.getWarnings().contains("反光"));
        assertTrue(result.getBlockReasons().isEmpty());
    }

    @Test
    public void testBankCard_emptyResponseIsEmptyAndNotNull() {
        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(new JSONObject());

        assertNotNull(result);
        assertNull(result.getBankCardNo());
        assertNull(result.getAccountCode());
        assertTrue(result.getBlockReasons().isEmpty());
    }

    // ==================== 助手 ====================

    private static JSONObject obj(Object... keyValues) {
        JSONObject json = new JSONObject();
        for (int i = 0; i < keyValues.length; i += 2) {
            json.put((String) keyValues[i], keyValues[i + 1]);
        }
        return json;
    }

    /** 把一段 JSON 编成厂商 {@code AdvancedInfo} 里那种「JSON 字符串」。 */
    private static String advanced(String json) {
        return JSON.parseObject(json).toJSONString();
    }

}
