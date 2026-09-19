package cn.iocoder.yudao.module.icbc.service.payment.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentApplyRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReceiptRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceQueryReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PaymentReq;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionService;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.service.notify.SellerNotifyService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeBankCardChangeService;
import cn.iocoder.yudao.module.icbc.service.payment.PaymentService;
import cn.iocoder.yudao.module.icbc.util.AmountUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 付方支付 Service 实现（issue #9）。
 *
 * <p>付款的起点是一笔「已预开票成功」的收购：从收购单解析出开票合作方订单号，校验金额一致，
 * 经 {@link IcbcGateway#submitPayment} 生成企业支付页面。工行返回的结果分三种：已受理（SUCCESS）、
 * 明确拒绝（BUSINESS_FAILED）、结果未知（UNKNOWN）。结果未知时<strong>绝不重复提交</strong>，
 * 只把状态留在「待支付」等查询收敛。
 *
 * <p>状态收敛只有一条路径 {@link #applyPaymentStatus}：通知（{@code PaymentNotifyHandler}）
 * 与主动查询都调它，所以两侧结果天然一致。成功时归档转账回单并把收购单推进为「已付款」。
 *
 * @author 芋道源码
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private IcbcGateway icbcGateway;

    @Resource
    private AcquisitionService acquisitionService;

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Resource
    private SellerNotifyService sellerNotifyService;

    @Resource
    private PayeeBankCardChangeService payeeBankCardChangeService;

    // ==================== 发起付款 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentApplyRespVO applyPayment(PaymentApplyReqVO reqVO) {
        // 1. 解析业务单：开票合作方订单号 = 收购单号
        InvoiceOrderDO invoiceOrder = resolveInvoiceOrder(reqVO);
        IcbcAcquisitionDO acquisition = resolveAcquisition(invoiceOrder);

        // 2. 只有预开票成功才能付款（AC #1）
        assertPreInvoiceSuccess(invoiceOrder);

        // 3. 付款金额必须与收购单金额一致（AC #6）
        BigDecimal amount = resolveAmount(acquisition, reqVO.getAmount());

        // 4. 同一业务单号已有支付单：成功 / 在途不重复提交，异常状态可重新发起（AC #5）
        PaymentOrderDO existing = paymentOrderMapper.selectByPartnerOrderId(invoiceOrder.getPartnerOrderId());
        if (existing != null && !PaymentStatusEnum.isReInitiable(existing.getPaymentStatus())) {
            return reuseOrReInitiate(existing, invoiceOrder, amount, reqVO);
        }

        // 5. 收款账户变更（#37）：审核期间新交易的付款挂起，别把钱打到废卡（退汇）
        payeeBankCardChangeService.assertPaymentNotSuspended(acquisition.getPayeeId());

        if (existing != null) {
            return reuseOrReInitiate(existing, invoiceOrder, amount, reqVO);
        }

        // 6. 首次发起：先落库再提交，保证「结果未知」时也有据可查
        PaymentOrderDO order = createOrder(invoiceOrder, acquisition, amount, reqVO);
        paymentOrderMapper.insert(order);
        return submitPaymentPage(order, invoiceOrder, reqVO);
    }

    /**
     * 从请求解析出开票订单：收购单编号与开票合作方订单号二选一。
     */
    private InvoiceOrderDO resolveInvoiceOrder(PaymentApplyReqVO reqVO) {
        String partnerOrderId = reqVO.getPartnerOrderId();
        if (reqVO.getAcquisitionId() != null) {
            IcbcAcquisitionDO acquisition = acquisitionService.getAcquisition(reqVO.getAcquisitionId());
            if (StrUtil.isBlank(acquisition.getInvoicePartnerOrderId())) {
                throw exception(PAYMENT_ACQUISITION_NOT_LINKED);
            }
            partnerOrderId = acquisition.getInvoicePartnerOrderId();
        }
        if (StrUtil.isBlank(partnerOrderId)) {
            throw exception(PAYMENT_PARAM_ERROR);
        }
        InvoiceOrderDO invoiceOrder = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (invoiceOrder == null) {
            throw exception(PAYMENT_INVOICE_ORDER_NOT_EXISTS);
        }
        return invoiceOrder;
    }

    private IcbcAcquisitionDO resolveAcquisition(InvoiceOrderDO invoiceOrder) {
        if (invoiceOrder.getAcquisitionId() == null) {
            throw exception(PAYMENT_ACQUISITION_NOT_LINKED);
        }
        return acquisitionService.getAcquisition(invoiceOrder.getAcquisitionId());
    }

    private void assertPreInvoiceSuccess(InvoiceOrderDO invoiceOrder) {
        if (!PreInvoiceStatusEnum.SUCCESS.getStatus().equals(invoiceOrder.getPreInvoiceStatus())) {
            throw exception(PAYMENT_PRE_INVOICE_NOT_SUCCESS);
        }
    }

    private BigDecimal resolveAmount(IcbcAcquisitionDO acquisition, BigDecimal declaredAmount) {
        BigDecimal amount = acquisition.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw exception(PAYMENT_AMOUNT_ERROR);
        }
        if (declaredAmount != null && declaredAmount.compareTo(amount) != 0) {
            throw exception(PAYMENT_AMOUNT_MISMATCH, amount.toPlainString(), declaredAmount.toPlainString());
        }
        return amount;
    }

    /**
     * 同一业务单号已有支付单时的处理：成功与在途不重复提交；只有失败 / 冲正 / 退汇 / 部分成功
     * 这类终态异常才允许重新发起，且保留重试次数。
     */
    private PaymentApplyRespVO reuseOrReInitiate(PaymentOrderDO existing, InvoiceOrderDO invoiceOrder,
                                                 BigDecimal amount, PaymentApplyReqVO reqVO) {
        if (!PaymentStatusEnum.isReInitiable(existing.getPaymentStatus())) {
            PaymentApplyRespVO resp = buildApplyResponse(existing, true);
            resp.setPayPageHtml(existing.getRedirectUrl());
            resp.setMessage(PaymentStatusEnum.isSuccess(existing.getPaymentStatus())
                    ? "该笔已支付成功，未重复提交"
                    : "该笔支付正在处理中，未重复提交");
            return resp;
        }

        // 异常状态：重置为待支付后重新生成支付页面，清掉上一笔的错误信息并累计重试次数
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderDO>()
                .eq(PaymentOrderDO::getId, existing.getId())
                .set(PaymentOrderDO::getPaymentStatus, PaymentStatusEnum.PENDING.getStatus())
                .set(PaymentOrderDO::getPaymentAmount, amount)
                .set(PaymentOrderDO::getPayStatus, null)
                .set(PaymentOrderDO::getErrorCode, null)
                .set(PaymentOrderDO::getErrorMsg, null)
                .set(PaymentOrderDO::getRetryCount,
                        (existing.getRetryCount() == null ? 0 : existing.getRetryCount()) + 1));

        PaymentOrderDO fresh = paymentOrderMapper.selectById(existing.getId());
        return submitPaymentPage(fresh, invoiceOrder, reqVO);
    }

    private PaymentOrderDO createOrder(InvoiceOrderDO invoiceOrder, IcbcAcquisitionDO acquisition,
                                       BigDecimal amount, PaymentApplyReqVO reqVO) {
        return PaymentOrderDO.builder()
                .orderNo(generateOrderNo())
                .partnerOrderId(invoiceOrder.getPartnerOrderId())
                .acquisitionId(acquisition.getId())
                .invoiceOrderId(invoiceOrder.getId())
                .payeeNo(invoiceOrder.getPayeeNo())
                .payerNo(invoiceOrder.getPayerNo())
                .paymentAmount(amount)
                .paymentStatus(PaymentStatusEnum.PENDING.getStatus())
                .verifiedCode(reqVO.getVerifiedCode())
                .ukeyId(reqVO.getUkeyId())
                .retryCount(0)
                .build();
    }

    /**
     * 经适配层生成企业支付页面。平台只发指令，资金从回收企业账户经公对私结算直达出售者银行卡，
     * 不经平台任何自有账户（AC #4）。
     */
    private PaymentApplyRespVO submitPaymentPage(PaymentOrderDO order, InvoiceOrderDO invoiceOrder,
                                                 PaymentApplyReqVO reqVO) {
        IcbcGatewayResult<IcbcPage> result = icbcGateway.submitPayment(PaymentReq.builder()
                .outOrderId(order.getPartnerOrderId())
                .outVendorId(invoiceOrder.getPayerNo())
                .outUserId(invoiceOrder.getPayeeNo())
                .verifiedCode(reqVO.getVerifiedCode())
                .ukeyId(reqVO.getUkeyId())
                .build());
        if (result.isSuccess() && result.getData() != null) {
            updateAfterSubmit(order, PaymentStatusEnum.PENDING, result.getData().getFormHtml(), null, null);
            PaymentApplyRespVO resp = buildApplyResponse(paymentOrderMapper.selectById(order.getId()), false);
            resp.setPayPageHtml(result.getData().getFormHtml());
            resp.setMessage("已生成企业支付页面");
            return resp;
        }
        if (result.isUnknown()) {
            // 工行明确要求：结果未知时不得重复提交，先查询。这里保持「待支付」，由查询 / 通知收敛
            updateAfterSubmit(order, PaymentStatusEnum.PENDING, null,
                    String.valueOf(result.getReturnCode()), result.getReturnMsg());
            PaymentApplyRespVO resp = buildApplyResponse(paymentOrderMapper.selectById(order.getId()), false);
            resp.setSuccess(false);
            resp.setMessage(ErrorCodeConstants.PAYMENT_RESULT_UNKNOWN.getMsg());
            return resp;
        }
        // 工行明确拒绝：落为支付失败，允许重新发起
        updateAfterSubmit(order, PaymentStatusEnum.FAILED, null,
                String.valueOf(result.getReturnCode()), result.getReturnMsg());
        PaymentApplyRespVO resp = buildApplyResponse(paymentOrderMapper.selectById(order.getId()), false);
        resp.setSuccess(false);
        resp.setMessage(StrUtil.blankToDefault(result.getReturnMsg(), ErrorCodeConstants.PAYMENT_FAILED.getMsg()));
        return resp;
    }

    private void updateAfterSubmit(PaymentOrderDO order, PaymentStatusEnum status, String pageHtml,
                                   String errorCode, String errorMsg) {
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderDO>()
                .eq(PaymentOrderDO::getId, order.getId())
                .set(PaymentOrderDO::getPaymentStatus, status.getStatus())
                .set(PaymentOrderDO::getRedirectUrl, pageHtml)
                .set(PaymentOrderDO::getErrorCode, errorCode)
                .set(PaymentOrderDO::getErrorMsg, errorMsg));
    }

    private PaymentApplyRespVO buildApplyResponse(PaymentOrderDO order, boolean duplicate) {
        PaymentApplyRespVO resp = new PaymentApplyRespVO();
        resp.setSuccess(true);
        resp.setDuplicate(duplicate);
        resp.setPartnerOrderId(order.getPartnerOrderId());
        resp.setOrderNo(order.getOrderNo());
        resp.setAcquisitionId(order.getAcquisitionId());
        resp.setPayAmount(order.getPaymentAmount());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setPaymentStatusName(PaymentStatusEnum.nameOf(order.getPaymentStatus()));
        resp.setReInitiable(PaymentStatusEnum.isReInitiable(order.getPaymentStatus()));
        resp.setErrorCode(order.getErrorCode());
        resp.setErrorMsg(order.getErrorMsg());
        return resp;
    }

    // ==================== 状态查询与收敛 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentStatusRespVO queryPaymentStatus(PaymentQueryReqVO reqVO) {
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(reqVO.getPartnerOrderId());
        if (order == null) {
            throw exception(PAYMENT_ORDER_NOT_EXISTS);
        }
        // 主动查询：取回工行最新 payStatus 并收敛，与通知路径共用 applyPaymentStatus
        IcbcGatewayResult<InvoiceInfo> result = icbcGateway.queryInvoiceInfo(InvoiceQueryReq.builder()
                .outOrderId(order.getPartnerOrderId())
                .outUserId(order.getPayeeNo())
                .outVendorId(order.getPayerNo())
                .build());
        if (result.isSuccess() && result.getData() != null
                && StrUtil.isNotBlank(result.getData().getPayStatus())) {
            InvoiceInfo info = result.getData();
            applyPaymentStatus(order.getPartnerOrderId(), info.getPayStatus(), info.getPayAmount(),
                    info.getActuallyReceivedAmount(), info.getSerialNo(), info.getIcbcOrderId());
            order = paymentOrderMapper.selectById(order.getId());
        }
        return buildStatusResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyPaymentStatus(String partnerOrderId, String payStatus, String payAmount,
                                   String actuallyReceivedAmount, String serialNo, String icbcOrderId) {
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            // 通知早于平台数据落库：抛业务异常，通知落为失败，数据落库后可重放
            throw exception(CALLBACK_BUSINESS_NOT_EXISTS);
        }
        Integer platformStatus = PaymentStatusEnum.toStatus(payStatus);
        boolean firstSuccess = PaymentStatusEnum.isSuccess(platformStatus)
                && !PaymentStatusEnum.isSuccess(order.getPaymentStatus());

        LambdaUpdateWrapper<PaymentOrderDO> update = new LambdaUpdateWrapper<PaymentOrderDO>()
                .eq(PaymentOrderDO::getId, order.getId())
                .set(StrUtil.isNotBlank(payStatus), PaymentOrderDO::getPayStatus, payStatus)
                .set(PaymentOrderDO::getPaymentStatus, platformStatus);
        if (StrUtil.isNotBlank(serialNo)) {
            update.set(PaymentOrderDO::getPaymentSerialNo, serialNo);
        }
        if (StrUtil.isNotBlank(icbcOrderId)) {
            update.set(PaymentOrderDO::getIcbcOrderNo, icbcOrderId);
        }
        BigDecimal received = AmountUtils.parse(actuallyReceivedAmount);
        if (received == null) {
            received = AmountUtils.parse(payAmount);
        }
        if (received != null) {
            update.set(PaymentOrderDO::getActuallyReceivedAmount, received);
        }
        if (firstSuccess) {
            // 支付成功：归档转账回单。回单号优先取工行流水，其次工行订单号，最后退回平台订单号
            LocalDateTime now = LocalDateTime.now();
            update.set(PaymentOrderDO::getPaymentTime, now);
            update.set(PaymentOrderDO::getReceiptTime, now);
            update.set(PaymentOrderDO::getReceiptNo,
                    StrUtil.blankToDefault(serialNo, StrUtil.blankToDefault(icbcOrderId, order.getOrderNo())));
            if (received == null) {
                update.set(PaymentOrderDO::getActuallyReceivedAmount, order.getPaymentAmount());
            }
        }
        paymentOrderMapper.update(null, update);

        // 回写开票单支付状态；支付成功再把收购单推进为「已付款」，让资金流挂到该笔收购上
        if (order.getInvoiceOrderId() != null) {
            invoiceOrderService.updateOrderStatus(order.getInvoiceOrderId(), null, null, platformStatus, null);
        }
        if (PaymentStatusEnum.isSuccess(platformStatus)) {
            acquisitionService.markPaidByInvoicePartnerOrderId(partnerOrderId);
        }
        if (firstSuccess) {
            // 付款成功是「真正开票」的触发点：推进为开票中，并尽力向工行确认一次开票状态
            invoiceOrderService.onPaymentSucceeded(partnerOrderId);
        }
        if (PaymentStatusEnum.isException(platformStatus)) {
            // 触达（#36）：失败 / 冲正 / 退汇 / 部分成功他要知道下一步，且知道钱没走通
            sellerNotifyService.onPaymentException(partnerOrderId);
        }
        log.info("支付状态收敛 - partnerOrderId: {}, payStatus: {}, platformStatus: {}",
                partnerOrderId, payStatus, platformStatus);
    }

    private PaymentStatusRespVO buildStatusResponse(PaymentOrderDO order) {
        PaymentStatusRespVO resp = new PaymentStatusRespVO();
        resp.setPartnerOrderId(order.getPartnerOrderId());
        resp.setOrderNo(order.getOrderNo());
        resp.setAcquisitionId(order.getAcquisitionId());
        resp.setPaymentAmount(order.getPaymentAmount());
        resp.setActuallyReceivedAmount(order.getActuallyReceivedAmount());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setPaymentStatusName(PaymentStatusEnum.nameOf(order.getPaymentStatus()));
        resp.setPayStatus(order.getPayStatus());
        resp.setReInitiable(PaymentStatusEnum.isReInitiable(order.getPaymentStatus()));
        resp.setPaymentTime(order.getPaymentTime());
        resp.setPaymentSerialNo(order.getPaymentSerialNo());
        resp.setIcbcOrderNo(order.getIcbcOrderNo());
        resp.setReceiptNo(order.getReceiptNo());
        resp.setReceiptTime(order.getReceiptTime());
        resp.setErrorCode(order.getErrorCode());
        resp.setErrorMsg(order.getErrorMsg());
        return resp;
    }

    // ==================== 转账回单 ====================

    @Override
    public PaymentReceiptRespVO getReceipt(String partnerOrderId) {
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            throw exception(PAYMENT_ORDER_NOT_EXISTS);
        }
        if (!PaymentStatusEnum.isSuccess(order.getPaymentStatus())) {
            throw exception(PAYMENT_RECEIPT_NOT_AVAILABLE);
        }
        PaymentReceiptRespVO resp = new PaymentReceiptRespVO();
        resp.setPartnerOrderId(order.getPartnerOrderId());
        resp.setOrderNo(order.getOrderNo());
        resp.setAcquisitionId(order.getAcquisitionId());
        resp.setReceiptNo(order.getReceiptNo());
        resp.setReceiptTime(order.getReceiptTime());
        resp.setReceiptFileUrl(order.getReceiptFileUrl());
        resp.setPaymentSerialNo(order.getPaymentSerialNo());
        resp.setPaymentAmount(order.getPaymentAmount());
        resp.setActuallyReceivedAmount(order.getActuallyReceivedAmount());
        resp.setPaymentStatusName(PaymentStatusEnum.nameOf(order.getPaymentStatus()));
        return resp;
    }

    // ==================== 辅助 ====================

    private String generateOrderNo() {
        return "PAY" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
    }

}
