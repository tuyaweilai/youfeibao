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
 * <p>喂的是**腾讯云官方文档 / 官方 SDK 示例里逐字抄下来的报文**，不是手搓的形状——手搓的
 * 字段名可能厂商根本不返回，于是「单测全绿」与「真机什么都不做」同时成立（独立评审 SP-1/SP-2/SP-3）。
 *
 * <p>两个 Action 的返回形状不同，测试里分开钉：
 * <ul>
 *   <li>{@code IDCardOCR}：告警在 {@code AdvancedInfo.WarnInfos} 的**告警码**里。</li>
 *   <li>{@code BankCardOCR}：**没有 AdvancedInfo**，告警在顶层 {@code WarningCode}、
 *       质量分在顶层 {@code QualityValue}、卡类别在顶层 {@code CardCategory}。</li>
 * </ul>
 */
public class TencentOcrResultMapperTest {

    // ==================== 官方示例（原文抄录，见各常量注释） ====================

    /**
     * 腾讯云 {@code BankCardOCR} 官方输出示例（document/product/866/36216 与 tccli
     * {@code tccli/examples/ocr/v20181119/BankCardOCR.md} 一字不差）。
     *
     * <p>注意：卡类别是 {@code CardCategory}（示例值「标准实体银行卡」），{@code CardType}
     * 是「卡类型」（示例值「贷记卡」）——AC4 要读的是前者。
     */
    private static final String OFFICIAL_BANK_CARD_RESPONSE = "{\"Response\":{"
            + "\"BankInfo\":\"招商银行(03080000)\","
            + "\"BorderCutImage\":null,"
            + "\"CardCategory\":\"标准实体银行卡\","
            + "\"CardName\":\"招商银行信用卡\","
            + "\"CardNo\":\"6225768888888888\","
            + "\"CardNoImage\":null,"
            + "\"CardType\":\"贷记卡\","
            + "\"QualityValue\":null,"
            + "\"RequestId\":\"4eeb7002-ef18-46b1-92dc-21e836f27d7f\","
            + "\"ValidDate\":\"07/2023\","
            + "\"WarningCode\":null}}";

    /** 腾讯云 {@code IDCardOCR} 官方「身份证识别（人像面）」输出示例（文档示例3）。 */
    private static final String OFFICIAL_ID_CARD_FRONT_RESPONSE = "{\"Response\":{"
            + "\"Address\":\"广东省深圳市南山区腾讯大厦\","
            + "\"AdvancedInfo\":\"{\\\"WarnInfos\\\":[]}\","
            + "\"Authority\":\"\","
            + "\"Birth\":\"1995/5/13\","
            + "\"IdNum\":\"440305199505132561\","
            + "\"Name\":\"刘洋\","
            + "\"Nation\":\"汉\","
            + "\"ReflectDetailInfos\":[],"
            + "\"RequestId\":\"c762a670-c622-408a-865a-da27a9ffa53b\","
            + "\"Sex\":\"女\","
            + "\"ValidDate\":\"\"}}";

    /** 腾讯云 {@code IDCardOCR} 官方「身份证识别（国徽面）」输出示例（文档示例4）。 */
    private static final String OFFICIAL_ID_CARD_BACK_RESPONSE = "{\"Response\":{"
            + "\"Address\":\"\","
            + "\"AdvancedInfo\":\"{\\\"WarnInfos\\\":[]}\","
            + "\"Authority\":\"上海市公安局南山分局\","
            + "\"Birth\":\"\","
            + "\"IdNum\":\"\","
            + "\"Name\":\"\","
            + "\"Nation\":\"\","
            + "\"ReflectDetailInfos\":[],"
            + "\"RequestId\":\"c058efd9-a469-4256-a18d-bf539fd2231b\","
            + "\"Sex\":\"\","
            + "\"ValidDate\":\"2018.08.12-2038.08.12\"}}";

    // ==================== 身份证人像面 ====================

    @Test
    public void testIdCardFront_officialExample_mapsNameIdAddress() {
        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(responseNode(
                OFFICIAL_ID_CARD_FRONT_RESPONSE));

        assertEquals("刘洋", result.getName());
        assertEquals("440305199505132561", result.getIdCardNo());
        assertEquals("广东省深圳市南山区腾讯大厦", result.getAddress());
        assertNull(result.getQualityScore(), "官方示例没请求 Quality，不该凭空有分");
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.getBlockReasons().isEmpty());
    }

    @Test
    public void testIdCardFront_qualityComesFromAdvancedInfo() {
        JSONObject response = obj("Name", "张三",
                "AdvancedInfo", advanced("{\"Quality\":\"88\",\"WarnInfos\":[]}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertEquals(88, result.getQualityScore());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testIdCardFront_warnInfoCodesBecomeReadableWarningsNotBlocks() {
        // WarnInfos 是「Code 告警码列表」：-9102 复印件、-9107 反光
        JSONObject response = obj("Name", "张三",
                "AdvancedInfo", advanced("{\"WarnInfos\":[-9102,-9107]}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertTrue(result.getWarnings().contains("身份证复印件"), "码要翻成可读文案，不能把 -9102 原样透传");
        assertTrue(result.getWarnings().contains("身份证反光"));
        assertFalse(result.getWarnings().contains("-9102"));
        assertTrue(result.getBlockReasons().isEmpty(), "复印件 / 反光是提示不拦");
    }

    @Test
    public void testIdCardFront_tempIdCodeBlocks() {
        // -9104 临时身份证告警
        JSONObject response = obj("AdvancedInfo", advanced("{\"WarnInfos\":[-9104]}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertTrue(result.getBlockReasons().contains("临时身份证"));
        assertFalse(result.getWarnings().contains("临时身份证"), "硬拦原因不进提示列表");
    }

    @Test
    public void testIdCardFront_invalidDateCodeBlocks() {
        // -9109 身份证有效日期不合法告警（官方文档 WarnInfos 释义）
        JSONObject response = obj("AdvancedInfo", advanced("{\"WarnInfos\":[-9109]}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertTrue(result.getBlockReasons().contains("身份证有效期不合法"));
    }

    @Test
    public void testIdCardFront_invalidDateLegacyCodeAlsoBlocks() {
        // #93 修票书写的 -9100；官方文档最新版是 -9109。两个都当「有效期不合法」硬拦。
        JSONObject response = obj("AdvancedInfo", advanced("{\"WarnInfos\":[-9100]}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertTrue(result.getBlockReasons().contains("身份证有效期不合法"));
    }

    @Test
    public void testIdCardFront_unknownCodeIsNotSilent_butDoesNotBlock() {
        JSONObject response = obj("AdvancedInfo", advanced("{\"WarnInfos\":[-9999]}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        // 未知码：记 warn（日志）+ 回一句可读文案；不据此硬拦，因为无法确证其严重性
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("未识别的告警"));
        assertTrue(result.getBlockReasons().isEmpty());
    }

    @Test
    public void testIdCardFront_requestConfigKeysAreNotReturnKeys_reverseAssertion() {
        // CopyWarn / TempIdWarn 是 Config 的**请求**开关，不在文档列出的返回键里：
        // 即便它们出现在报文里，也不该被当成告警（独立评审 SP-3 的反向钉）
        JSONObject response = obj("Name", "张三",
                "AdvancedInfo", advanced("{\"CopyWarn\":\"true\",\"TempIdWarn\":\"true\","
                        + "\"BorderCheckWarn\":\"true\",\"ReshootWarn\":\"true\",\"ReflectWarn\":\"true\"}"));

        CardRecognitionPort.IdCardFront result = TencentOcrResultMapper.toIdCardFront(response);

        assertEquals("张三", result.getName());
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.getBlockReasons().isEmpty());
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
    public void testIdCardBack_officialExample_convertsDotSeparatedValidity() {
        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(responseNode(
                OFFICIAL_ID_CARD_BACK_RESPONSE));

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
    public void testIdCardBack_invalidDateWarnCodeIsABlock() {
        JSONObject response = obj("ValidDate", "",
                "AdvancedInfo", advanced("{\"WarnInfos\":[-9109]}"));

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertTrue(result.getBlockReasons().contains("身份证有效期不合法"),
                "厂商明确回 -9109 时必须硬拦，哪怕 ValidDate 是空串");
    }

    @Test
    public void testIdCardBack_unparseableValidityIsABlock() {
        JSONObject response = obj("ValidDate", "有效期见原件");

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertNull(result.getIdValidityPeriod());
        assertTrue(result.getBlockReasons().contains("身份证有效期不合法"));
    }

    @Test
    public void testIdCardBack_illegalMonthDayIsABlock() {
        JSONObject response = obj("ValidDate", "2018.13.40-2038.02.30");

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertNull(result.getIdSignDate());
        assertTrue(result.getBlockReasons().contains("身份证有效期不合法"));
    }

    @Test
    public void testIdCardBack_blankValidityIsNotABlock_manualEntryFallback() {
        // 官方示例1（临时身份证告警示例，用了一张人像面图）里国徽面字段本就为空
        JSONObject response = new JSONObject();

        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(response);

        assertNull(result.getIdSignDate());
        assertNull(result.getIdValidityPeriod());
        assertTrue(result.getBlockReasons().isEmpty(), "厂商没给有效期不是拒收，收货员手工补录即可（ADR 0037）");
    }

    @Test
    public void testIdCardBack_authorityIsNotMappedAnywhere() {
        CardRecognitionPort.IdCardBack result = TencentOcrResultMapper.toIdCardBack(responseNode(
                OFFICIAL_ID_CARD_BACK_RESPONSE));

        // 端口里没有签发机关字段：这里只断言结果形状，签发机关无处可落（ADR 0037）
        assertEquals("2018-08-12", result.getIdSignDate());
        assertEquals("2038-08-12", result.getIdValidityPeriod());
    }

    // ==================== 银行卡 ====================

    @Test
    public void testBankCard_officialExample_mapsCardNoBankNameAccountCode() {
        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(responseNode(
                OFFICIAL_BANK_CARD_RESPONSE));

        assertEquals("6225768888888888", result.getBankCardNo());
        assertEquals("招商银行", result.getBankName(), "尾部联行号要剥掉（ADR 0035 不做联行号）");
        assertEquals("0", result.getAccountCode(), "招商银行不是我行");
        assertNull(result.getQualityScore(), "官方示例没请求 EnableQualityValue，QualityValue 为 null");
        assertTrue(result.getWarnings().isEmpty(), "WarningCode 为 null");
        assertTrue(result.getBlockReasons().isEmpty(), "标准实体银行卡不是截图");
    }

    @Test
    public void testBankCard_creditCardTypeIsNotAnElectronicScreenshot() {
        // 官方示例里 CardType=贷记卡：卡类型不是卡类别，不能被误判成截图（SP-1 反向断言）
        JSONObject response = obj(
                "CardNo", "6225768888888888",
                "BankInfo", "招商银行(03080000)",
                "CardCategory", "标准实体银行卡",
                "CardType", "贷记卡");

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertTrue(result.getBlockReasons().isEmpty(), "贷记卡是卡类型，与「电子银行卡信息截图」无关");
    }

    @Test
    public void testBankCard_electronicScreenshotCategoryIsABlock() {
        // 卡类别（CardCategory）为「电子银行卡信息截图」→ 硬拦（AC4）
        JSONObject response = obj(
                "CardNo", "6222021234567890123",
                "BankInfo", "中国工商银行(03080000)",
                "CardCategory", "电子银行卡信息截图",
                "CardType", "借记卡");

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertEquals(1, result.getBlockReasons().size());
        assertTrue(result.getBlockReasons().get(0).contains("电子银行卡信息截图"));
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testBankCard_warningCodesBecomeReadableWarnings() {
        // 顶层 WarningCode：-9113 复印件、-9114 翻拍件
        JSONObject response = obj("CardNo", "6222021234567890123", "WarningCode",
                JSON.parseArray("[-9113,-9114]"));

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertTrue(result.getWarnings().contains("银行卡复印件"));
        assertTrue(result.getWarnings().contains("银行卡翻拍"));
        assertTrue(result.getBlockReasons().isEmpty(), "银行卡告警是提示不拦，硬拦只有电子卡截图");
    }

    @Test
    public void testBankCard_qualityValueFromTopLevel() {
        JSONObject response = obj("CardNo", "6222021234567890123", "QualityValue", 95);

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertEquals(95, result.getQualityScore());
    }

    @Test
    public void testBankCard_advancedInfoIsNotAReturnKey_reverseAssertion() {
        // BankCardOCR 没有 AdvancedInfo：即便报文里出现身份证那套开关，也不该被当成银行卡告警
        JSONObject response = obj("CardNo", "6222021234567890123",
                "AdvancedInfo", advanced("{\"CopyWarn\":\"true\",\"TempIdWarn\":\"true\"}"));

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.getBlockReasons().isEmpty(), "银行卡不该被判「临时身份证」");
    }

    @Test
    public void testBankCard_icbcNameInfersAccountCodeOne() {
        JSONObject response = obj("CardNo", "6222021234567890123", "BankInfo", "中国工商银行(03080000)");

        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(response);

        assertEquals("中国工商银行", result.getBankName());
        assertEquals("1", result.getAccountCode());
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
    public void testBankCard_emptyResponseIsEmptyAndNotNull() {
        CardRecognitionPort.BankCard result = TencentOcrResultMapper.toBankCard(new JSONObject());

        assertNotNull(result);
        assertNull(result.getBankCardNo());
        assertNull(result.getAccountCode());
        assertTrue(result.getBlockReasons().isEmpty());
    }

    // ==================== 助手 ====================

    /** 从官方示例的完整响应体里取出 {@code Response} 节点（映射层吃的是节点）。 */
    private static JSONObject responseNode(String officialBody) {
        return JSON.parseObject(officialBody).getJSONObject("Response");
    }

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
