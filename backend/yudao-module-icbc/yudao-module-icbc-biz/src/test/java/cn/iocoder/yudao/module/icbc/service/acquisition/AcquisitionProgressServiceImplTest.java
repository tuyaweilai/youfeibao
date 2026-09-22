package cn.iocoder.yudao.module.icbc.service.acquisition;

import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.acquisition.impl.AcquisitionProgressServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 收购进度派生规则表（ADR 0038）。
 *
 * <p>派生是**纯函数**（不进库），所以这里直接把 DO 摆出来验规则：档位怎么走、异常怎么叠、
 * 乱序为什么不会把档位推回去。
 */
public class AcquisitionProgressServiceImplTest {

    private final AcquisitionProgressServiceImpl service = new AcquisitionProgressServiceImpl();

    // ==================== 档位 ====================

    @Test
    public void testStage_noInvoiceOrder_isRegistered() {
        AcquisitionProgress progress = service.derive(acquisition(), null);

        assertEquals(AcquisitionStatusEnum.REGISTERED, progress.getStage());
        assertFalse(progress.isAbnormal());
    }

    @Test
    public void testStage_preOrderInProgress_isWaitingSellerConfirm() {
        // 预下单已返回、出售者还没在工行页面上确认：这一档不能说成「待付款」，
        // 否则现场催单会催错人（催企业付款，而其实卡在对方手机上）
        AcquisitionProgress progress = service.derive(acquisition(), order(
                PreInvoiceStatusEnum.IN_PROGRESS.getStatus(), PaymentStatusEnum.PENDING.getStatus(), 0));

        assertEquals(AcquisitionStatusEnum.WAITING_SELLER_CONFIRM, progress.getStage());
    }

    @Test
    public void testStage_preInvoiceSuccess_isPendingPayment() {
        AcquisitionProgress progress = service.derive(acquisition(), order(
                PreInvoiceStatusEnum.SUCCESS.getStatus(), PaymentStatusEnum.PENDING.getStatus(), 0));

        assertEquals(AcquisitionStatusEnum.PENDING_PAYMENT, progress.getStage());
    }

    @Test
    public void testStage_paymentSuccess_isPaid() {
        AcquisitionProgress progress = service.derive(acquisition(), order(
                PreInvoiceStatusEnum.SUCCESS.getStatus(), PaymentStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.ISSUING.getStatus()));

        assertEquals(AcquisitionStatusEnum.PAID, progress.getStage());
    }

    @Test
    public void testStage_invoiceIssued_isInvoiced() {
        // 「已开票」过去没有任何调用方、永远不会出现（#105 修的就是这个）
        AcquisitionProgress progress = service.derive(acquisition(), order(
                PreInvoiceStatusEnum.SUCCESS.getStatus(), PaymentStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.ISSUED.getStatus()));

        assertEquals(AcquisitionStatusEnum.INVOICED, progress.getStage());
    }

    @Test
    public void testStage_enterpriseCancelled_winsOverIssuedInvoice() {
        // 企业作废是敏感动作（货已经收了），带原因、对自然人可见，压过一切
        IcbcAcquisitionDO acquisition = acquisition();
        acquisition.setCancelReason("现场登记有误，重复登记");

        AcquisitionProgress progress = service.derive(acquisition, order(
                PreInvoiceStatusEnum.SUCCESS.getStatus(), PaymentStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.ISSUED.getStatus()));

        assertEquals(AcquisitionStatusEnum.CANCELLED, progress.getStage());
    }

    @Test
    public void testStage_preInvoiceCancelled_isCancelledWithReason() {
        // 工行侧预开票被取消：档位是「已作废」，但原因是外部的，必须写出来，
        // 否则这条作废在收货员眼里没有来由
        AcquisitionProgress progress = service.derive(acquisition(), order(
                PreInvoiceStatusEnum.CANCELLED.getStatus(), PaymentStatusEnum.PENDING.getStatus(), 0));

        assertEquals(AcquisitionStatusEnum.CANCELLED, progress.getStage());
        assertTrue(progress.isAbnormal());
        assertTrue(progress.getAbnormalReasons().stream().anyMatch(reason -> reason.contains("预开票已取消")));
    }

    // ==================== 异常不占档位（ADR 0021） ====================

    @Test
    public void testAbnormal_invoiceFailed_doesNotReplacePaidStage() {
        // 钱已经付出去了、票没开出来：档位是「已付款」，异常必须露头
        AcquisitionProgress progress = service.derive(acquisition(), order(
                PreInvoiceStatusEnum.SUCCESS.getStatus(), PaymentStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.FAILED.getStatus()));

        assertEquals(AcquisitionStatusEnum.PAID, progress.getStage());
        assertTrue(progress.isAbnormal());
        assertEquals("开票：开票失败", progress.getAbnormalReasons().get(0));
    }

    @Test
    public void testAbnormal_afterIssued_taxAndUploadStillVisible() {
        // 票开出来了，缴税与上传仍可能失败——四条线独立，谁都不许被好看的档位盖住
        InvoiceOrderDO order = order(PreInvoiceStatusEnum.SUCCESS.getStatus(),
                PaymentStatusEnum.SUCCESS.getStatus(), InvoiceIssueStatusEnum.ISSUED.getStatus());
        order.setTaxStatus(TaxStatusEnum.ABNORMAL_AMOUNT.getStatus());
        order.setUploadStatus(UploadStatusEnum.FAILED.getStatus());

        AcquisitionProgress progress = service.derive(acquisition(), order);

        assertEquals(AcquisitionStatusEnum.INVOICED, progress.getStage());
        assertTrue(progress.isAbnormal());
        assertEquals(2, progress.getAbnormalReasons().size());
        assertTrue(progress.getAbnormalReasons().stream().anyMatch(reason -> reason.startsWith("缴税：")));
        assertTrue(progress.getAbnormalReasons().stream().anyMatch(reason -> reason.startsWith("上传：")));
    }

    @Test
    public void testAbnormal_paymentPartialSuccess_isVisible() {
        AcquisitionProgress progress = service.derive(acquisition(), order(
                PreInvoiceStatusEnum.SUCCESS.getStatus(), PaymentStatusEnum.PARTIAL_SUCCESS.getStatus(), 0));

        assertTrue(progress.isAbnormal());
        assertEquals("付款：部分成功", progress.getAbnormalReasons().get(0));
    }

    // ==================== 红冲：不回退档位，但要露头 ====================

    @Test
    public void testAbnormal_redOffset_keepsInvoicedStageWorthSaying() {
        // 红票开出后蓝票在税务上已作废，这件事不能吃在「已开票」下面；但货款真的付了、
        // 蓝票真的开过，所以档位不退（退成「待付款」是撒谎）
        InvoiceOrderDO order = order(PreInvoiceStatusEnum.SUCCESS.getStatus(),
                PaymentStatusEnum.SUCCESS.getStatus(), InvoiceIssueStatusEnum.ISSUED.getStatus());
        RedInvoiceDO redInvoice = RedInvoiceDO.builder()
                .partnerOrderId("ORDER_TEST_1")
                .redOffsetStatus(RedOffsetStatusEnum.SUCCESS.getStatus())
                .reason("开票有误")
                .build();

        AcquisitionProgress progress = service.derive(acquisition(), order, redInvoice);

        assertEquals(AcquisitionStatusEnum.INVOICED, progress.getStage());
        assertTrue(progress.isAbnormal());
        assertEquals("发票流：已红冲（开票有误）", progress.getAbnormalReasons().get(0));
    }

    @Test
    public void testAbnormal_redOffsetNotYetSucceeded_isNotAnnotated() {
        // 红冲还在路上（申请中 / 撤销过）不算「已红冲」，不打扰收货员
        InvoiceOrderDO order = order(PreInvoiceStatusEnum.SUCCESS.getStatus(),
                PaymentStatusEnum.SUCCESS.getStatus(), InvoiceIssueStatusEnum.ISSUED.getStatus());
        RedInvoiceDO redInvoice = RedInvoiceDO.builder()
                .partnerOrderId("ORDER_TEST_1")
                .redOffsetStatus(RedOffsetStatusEnum.APPLYING.getStatus())
                .build();

        AcquisitionProgress progress = service.derive(acquisition(), order, redInvoice);

        assertFalse(progress.isAbnormal());
    }

    // ==================== 乱序 / 重复 ====================

    @Test
    public void testOutOfOrder_stalePreInvoiceStatus_doesNotRollBackIssuedStage() {
        // 通知乱序：票已开出，又收到一条旧的「预开票在途」。档位看的是当前真值，
        // 「已开票」是最高优先级的正终态，不会被推回「待自然人确认」
        AcquisitionProgress progress = service.derive(acquisition(), order(
                PreInvoiceStatusEnum.IN_PROGRESS.getStatus(), PaymentStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.ISSUED.getStatus()));

        assertEquals(AcquisitionStatusEnum.INVOICED, progress.getStage());
    }

    @Test
    public void testRepeat_sameInput_sameResult() {
        InvoiceOrderDO order = order(PreInvoiceStatusEnum.SUCCESS.getStatus(),
                PaymentStatusEnum.SUCCESS.getStatus(), InvoiceIssueStatusEnum.ISSUED.getStatus());

        AcquisitionProgress first = service.derive(acquisition(), order);
        AcquisitionProgress second = service.derive(acquisition(), order);

        assertEquals(first.getStage(), second.getStage());
        assertEquals(first.getAbnormalReasons(), second.getAbnormalReasons());
    }

    // ==================== 造数 ====================

    private IcbcAcquisitionDO acquisition() {
        IcbcAcquisitionDO acquisition = new IcbcAcquisitionDO();
        acquisition.setId(1L);
        acquisition.setAcquisitionNo("ACQ_TEST_1");
        return acquisition;
    }

    private InvoiceOrderDO order(Integer preInvoiceStatus, Integer paymentStatus, Integer invoiceStatus) {
        return InvoiceOrderDO.builder()
                .acquisitionId(1L)
                .partnerOrderId("ORDER_TEST_1")
                .preInvoiceStatus(preInvoiceStatus)
                .paymentStatus(paymentStatus)
                .invoiceStatus(invoiceStatus)
                .build();
    }

}
