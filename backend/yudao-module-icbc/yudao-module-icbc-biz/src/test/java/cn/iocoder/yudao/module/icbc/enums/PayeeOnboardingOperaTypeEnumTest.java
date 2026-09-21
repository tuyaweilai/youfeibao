package cn.iocoder.yudao.module.icbc.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PayeeOnboardingOperaTypeEnum} 的单元测试（#86）。
 *
 * <p>工行的两套表达要能对上：回调侧是 {@code operaType=02}（修改），查询侧是
 * {@code auditStatus=3}（修改审核中）。两者都指向「结果属于换卡单」，不能当新增结果去改写建档状态。
 */
public class PayeeOnboardingOperaTypeEnumTest {

    @Test
    public void testModifyCode() {
        assertEquals(PayeeOnboardingOperaTypeEnum.MODIFY, PayeeOnboardingOperaTypeEnum.ofCode("02"));
        assertTrue(PayeeOnboardingOperaTypeEnum.isModify("02"));
    }

    @Test
    public void testAddCallbackCarriesNoOperaType() {
        // 新增回调的报文本来就不带 operaType：缺省 / 未知都当新增
        assertFalse(PayeeOnboardingOperaTypeEnum.isModify(null));
        assertFalse(PayeeOnboardingOperaTypeEnum.isModify(""));
        assertFalse(PayeeOnboardingOperaTypeEnum.isModify("01"));
        assertNull(PayeeOnboardingOperaTypeEnum.ofCode("01"));
    }

    @Test
    public void testModifyAuditStatusFromQuery() {
        assertTrue(PayeeOnboardingOperaTypeEnum.isModifyAuditStatus("3"));
        assertFalse(PayeeOnboardingOperaTypeEnum.isModifyAuditStatus("1"));
        assertFalse(PayeeOnboardingOperaTypeEnum.isModifyAuditStatus("2"));
        assertFalse(PayeeOnboardingOperaTypeEnum.isModifyAuditStatus(null));
    }

}
