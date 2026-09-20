package cn.iocoder.yudao.module.icbc.service.admission;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import cn.iocoder.yudao.module.icbc.service.admission.impl.SellerAdmissionServiceImpl;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SELLER_SUBJECT_TYPE_NOT_NATURAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SellerAdmissionService} 的单元测试（issue #48，ADR 0029）。
 *
 * <p>反向开票的准入是「主体 = 自然人」的硬约束：六类主体逐一断言——自然人放行，
 * 其余五类（个体工商户 / 个人独资企业 / 合伙企业 / 企业法人 / 农民专业合作社）一律拒绝，
 * 且错误信息指向「由对方开票 + 进项收票」。
 */
public class SellerAdmissionServiceTest {

    private final SellerAdmissionService sellerAdmissionService = new SellerAdmissionServiceImpl();

    @Test
    public void testNaturalSellerAllowed() {
        assertTrue(sellerAdmissionService.isReverseInvoiceAllowed(SellerSubjectTypeEnum.NATURAL.getType()));
        // 断言不抛异常
        assertDoesNotThrow(() -> sellerAdmissionService.assertReverseInvoiceAllowed(
                SellerSubjectTypeEnum.NATURAL.getType()));
    }

    @Test
    public void testEveryNonNaturalSellerRejected() {
        int rejected = 0;
        for (SellerSubjectTypeEnum subjectType : SellerSubjectTypeEnum.values()) {
            if (subjectType.isNatural()) {
                continue;
            }
            rejected++;
            assertFalse(sellerAdmissionService.isReverseInvoiceAllowed(subjectType.getType()),
                    subjectType.getName() + " 不属于自然人，不能反向开票");

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> sellerAdmissionService.assertReverseInvoiceAllowed(subjectType.getType()));
            assertEquals(SELLER_SUBJECT_TYPE_NOT_NATURAL.getCode(), exception.getCode());
            // 错误信息要能说清「是谁」以及「为什么」——不能只给一个笼统的参数错误
            assertTrue(exception.getMessage().contains(subjectType.getName()), "实际：" + exception.getMessage());
            assertTrue(exception.getMessage().contains("不是自然人"), "实际：" + exception.getMessage());
        }
        // 六态里恰好五类非自然人
        assertEquals(5, rejected);
    }

    @Test
    public void testRejectionPointsToReceiptPath() {
        String message = sellerAdmissionService.reverseInvoiceRejectionMessage(
                SellerSubjectTypeEnum.INDIVIDUAL_BUSINESS.getType());
        assertTrue(message.contains("个体工商户"), "实际：" + message);
        // 指向非自然人的取票路径：由对方开票 + 我们收票
        assertTrue(message.contains("自行开具增值税发票"), "实际：" + message);
        assertTrue(message.contains("进项收票"), "实际：" + message);
    }

    @Test
    public void testNullSubjectTypeAllowedForHistoricalData() {
        // 反向开票通道此前只对自然人开放，历史单据没有主体类型字段，按自然人放行才能开出票
        assertTrue(sellerAdmissionService.isReverseInvoiceAllowed(null));
        assertDoesNotThrow(() -> sellerAdmissionService.assertReverseInvoiceAllowed(null));
    }

    @Test
    public void testUnknownSubjectTypeRejected() {
        // 未知类型宁可不认，也不误放进反向开票
        assertFalse(sellerAdmissionService.isReverseInvoiceAllowed(99));
        ServiceException exception = assertThrows(ServiceException.class,
                () -> sellerAdmissionService.assertReverseInvoiceAllowed(99));
        assertTrue(exception.getMessage().contains("未知主体类型"), "实际：" + exception.getMessage());
    }

}
