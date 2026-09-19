package cn.iocoder.yudao.module.icbc.gateway.fake;

import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.EnterpriseAuthReq;
import cn.iocoder.yudao.module.icbc.gateway.model.FaceVerifyPageReq;
import cn.iocoder.yudao.module.icbc.gateway.model.FaceVerifyStatus;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcConnectivity;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceCancelReq;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceCancelResult;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceDownloadReq;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceFile;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceQueryReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PayeeOnboardingPageReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PayeeOnboardingStatus;
import cn.iocoder.yudao.module.icbc.gateway.model.PaymentReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceRevokeReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceRevokeResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 假适配层
 *
 * 通过 {@code icbc.gateway.mode=fake} 注入。它不触网、只记录调用序列并返回可配置的预设结果，
 * 让测试可以断言「平台发出了什么指令」而不是「平台调用了某个方法」。
 *
 * 默认各操作返回成功；测试可用 {@code setXxxResult} 覆盖为业务失败或
 * {@link cn.iocoder.yudao.module.icbc.gateway.IcbcOutcome#UNKNOWN}，模拟代理异常 / 超时。
 */
@Component
@ConditionalOnProperty(prefix = "icbc.gateway", name = "mode", havingValue = "fake")
public class FakeIcbcGateway implements IcbcGateway {

    /**
     * 操作名常量，供断言使用
     */
    public static final String OP_SUBMIT_FACE_VERIFICATION = "submitFaceVerification";
    public static final String OP_QUERY_FACE_VERIFICATION = "queryFaceVerification";
    public static final String OP_SUBMIT_PAYEE_ONBOARDING = "submitPayeeOnboarding";
    public static final String OP_QUERY_PAYEE_ONBOARDING = "queryPayeeOnboarding";
    public static final String OP_SUBMIT_ENTERPRISE_AUTHORIZATION = "submitEnterpriseAuthorization";
    public static final String OP_SUBMIT_PRE_ORDER = "submitPreOrder";
    public static final String OP_QUERY_INVOICE_INFO = "queryInvoiceInfo";
    public static final String OP_SUBMIT_PAYMENT = "submitPayment";
    public static final String OP_DOWNLOAD_INVOICE = "downloadInvoice";
    public static final String OP_CANCEL_INVOICE = "cancelInvoice";
    public static final String OP_APPLY_RED_INVOICE = "applyRedInvoice";
    public static final String OP_REVOKE_RED_INVOICE = "revokeRedInvoice";
    public static final String OP_CHECK_CONNECTIVITY = "checkConnectivity";

    private final List<IcbcInvocation> invocations = new CopyOnWriteArrayList<>();

    private IcbcGatewayResult<IcbcPage> faceVerifyPageResult =
            IcbcGatewayResult.success(IcbcPage.builder().formHtml("<form id=\"face-verify\"></form>").build(), 0, "成功");
    private IcbcGatewayResult<FaceVerifyStatus> faceVerifyStatusResult =
            IcbcGatewayResult.success(FaceVerifyStatus.builder().outUserId("x").authResult("02").passed(true).build(), 0, "成功");
    private IcbcGatewayResult<IcbcPage> payeeOnboardingResult =
            IcbcGatewayResult.success(IcbcPage.builder().formHtml("<form id=\"payee-onboarding\"></form>").build(), 0, "成功");
    private IcbcGatewayResult<PayeeOnboardingStatus> payeeOnboardingStatusResult =
            IcbcGatewayResult.success(PayeeOnboardingStatus.builder().receiverStatus("1").auditStatus("1").build(), 0, "成功");
    private IcbcGatewayResult<IcbcPage> enterpriseAuthResult =
            IcbcGatewayResult.success(IcbcPage.builder().formHtml("<form id=\"enterprise-auth\"></form>").build(), 0, "成功");
    private IcbcGatewayResult<IcbcPage> preOrderResult =
            IcbcGatewayResult.success(IcbcPage.builder().formHtml("<form id=\"pre-order\"></form>").build(), 0, "成功");
    private IcbcGatewayResult<InvoiceInfo> invoiceInfoResult =
            IcbcGatewayResult.success(InvoiceInfo.builder().build(), 10100000, "接口访问成功");
    private IcbcGatewayResult<IcbcPage> paymentResult =
            IcbcGatewayResult.success(IcbcPage.builder().formHtml("<form id=\"payment\"></form>").build(), 0, "成功");
    private IcbcGatewayResult<InvoiceFile> invoiceFileResult =
            IcbcGatewayResult.success(InvoiceFile.builder().fileName("invoice.pdf")
                    .content("PDF".getBytes(StandardCharsets.UTF_8)).build(), 10100000, "接口访问成功");
    private IcbcGatewayResult<InvoiceCancelResult> invoiceCancelResult =
            IcbcGatewayResult.success(InvoiceCancelResult.builder().reversalStatus("00").build(), 10100000, "接口访问成功");
    private IcbcGatewayResult<IcbcPage> redInvoiceResult =
            IcbcGatewayResult.success(IcbcPage.builder().formHtml("<form id=\"red-invoice\"></form>").build(), 0, "成功");
    private IcbcGatewayResult<RedInvoiceRevokeResult> redInvoiceRevokeResult =
            IcbcGatewayResult.success(RedInvoiceRevokeResult.builder().revokeStatus("10").build(), 10100000, "接口访问成功");
    private IcbcGatewayResult<IcbcConnectivity> connectivityResult =
            IcbcGatewayResult.success(IcbcConnectivity.builder().reachable(true).returnCode(30601006)
                    .returnMsg("未查询到收方信息").build(), 30601006, "未查询到收方信息");

    // ==================== 端口实现 ====================

    @Override
    public IcbcGatewayResult<IcbcPage> submitFaceVerification(FaceVerifyPageReq req) {
        record(OP_SUBMIT_FACE_VERIFICATION, req);
        return faceVerifyPageResult;
    }

    @Override
    public IcbcGatewayResult<FaceVerifyStatus> queryFaceVerification(String outUserId) {
        record(OP_QUERY_FACE_VERIFICATION, outUserId);
        return faceVerifyStatusResult;
    }

    @Override
    public IcbcGatewayResult<IcbcPage> submitPayeeOnboarding(PayeeOnboardingPageReq req) {
        record(OP_SUBMIT_PAYEE_ONBOARDING, req);
        return payeeOnboardingResult;
    }

    @Override
    public IcbcGatewayResult<PayeeOnboardingStatus> queryPayeeOnboarding(String outUserId, String outVendorId) {
        record(OP_QUERY_PAYEE_ONBOARDING, outUserId);
        return payeeOnboardingStatusResult;
    }

    @Override
    public IcbcGatewayResult<IcbcPage> submitEnterpriseAuthorization(EnterpriseAuthReq req) {
        record(OP_SUBMIT_ENTERPRISE_AUTHORIZATION, req);
        return enterpriseAuthResult;
    }

    @Override
    public IcbcGatewayResult<IcbcPage> submitPreOrder(PreOrderReq req) {
        record(OP_SUBMIT_PRE_ORDER, req);
        return preOrderResult;
    }

    @Override
    public IcbcGatewayResult<InvoiceInfo> queryInvoiceInfo(InvoiceQueryReq req) {
        record(OP_QUERY_INVOICE_INFO, req);
        return invoiceInfoResult;
    }

    @Override
    public IcbcGatewayResult<IcbcPage> submitPayment(PaymentReq req) {
        record(OP_SUBMIT_PAYMENT, req);
        return paymentResult;
    }

    @Override
    public IcbcGatewayResult<InvoiceFile> downloadInvoice(InvoiceDownloadReq req) {
        record(OP_DOWNLOAD_INVOICE, req);
        return invoiceFileResult;
    }

    @Override
    public IcbcGatewayResult<InvoiceCancelResult> cancelInvoice(InvoiceCancelReq req) {
        record(OP_CANCEL_INVOICE, req);
        return invoiceCancelResult;
    }

    @Override
    public IcbcGatewayResult<IcbcPage> applyRedInvoice(RedInvoiceReq req) {
        record(OP_APPLY_RED_INVOICE, req);
        return redInvoiceResult;
    }

    @Override
    public IcbcGatewayResult<RedInvoiceRevokeResult> revokeRedInvoice(RedInvoiceRevokeReq req) {
        record(OP_REVOKE_RED_INVOICE, req);
        return redInvoiceRevokeResult;
    }

    @Override
    public IcbcGatewayResult<IcbcConnectivity> checkConnectivity() {
        record(OP_CHECK_CONNECTIVITY, null);
        return connectivityResult;
    }

    // ==================== 断言与配置助手 ====================

    private void record(String operation, Object payload) {
        invocations.add(new IcbcInvocation(operation, payload));
    }

    /**
     * 全部调用序列（按发生顺序）
     */
    public List<IcbcInvocation> getInvocations() {
        return invocations;
    }

    /**
     * 已发生的操作名序列
     */
    public List<String> getInvokedOperations() {
        return invocations.stream().map(IcbcInvocation::getOperation).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 某操作实际发生的次数
     */
    public long countOperation(String operation) {
        return invocations.stream().filter(item -> item.getOperation().equals(operation)).count();
    }

    /**
     * 最后一次某操作的入参
     */
    @SuppressWarnings("unchecked")
    public <T> T lastPayload(String operation) {
        return invocations.stream()
                .filter(item -> item.getOperation().equals(operation))
                .reduce((first, second) -> second)
                .map(item -> (T) item.getPayload())
                .orElse(null);
    }

    public void reset() {
        invocations.clear();
    }

    /**
     * 清空调用序列并把可覆盖的结果恢复为默认值，供测试之间互不影响。
     */
    public void resetAll() {
        invocations.clear();
        faceVerifyPageResult = IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id=\"face-verify\"></form>").build(), 0, "成功");
        faceVerifyStatusResult = IcbcGatewayResult.success(
                FaceVerifyStatus.builder().outUserId("x").authResult("02").passed(true).build(), 0, "成功");
        payeeOnboardingResult = IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id=\"payee-onboarding\"></form>").build(), 0, "成功");
        payeeOnboardingStatusResult = IcbcGatewayResult.success(
                PayeeOnboardingStatus.builder().receiverStatus("1").auditStatus("1").build(), 0, "成功");
        enterpriseAuthResult = IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id=\"enterprise-auth\"></form>").build(), 0, "成功");
        preOrderResult = IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id=\"pre-order\"></form>").build(), 0, "成功");
        invoiceInfoResult = IcbcGatewayResult.success(InvoiceInfo.builder().build(), 10100000, "接口访问成功");
        paymentResult = IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id=\"payment\"></form>").build(), 0, "成功");
        invoiceFileResult = IcbcGatewayResult.success(InvoiceFile.builder().fileName("invoice.pdf")
                .content("PDF".getBytes(StandardCharsets.UTF_8)).build(), 10100000, "接口访问成功");
        invoiceCancelResult = IcbcGatewayResult.success(
                InvoiceCancelResult.builder().reversalStatus("00").build(), 10100000, "接口访问成功");
        redInvoiceResult = IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id=\"red-invoice\"></form>").build(), 0, "成功");
        redInvoiceRevokeResult = IcbcGatewayResult.success(
                RedInvoiceRevokeResult.builder().revokeStatus("10").build(), 10100000, "接口访问成功");
        connectivityResult = IcbcGatewayResult.success(IcbcConnectivity.builder().reachable(true).returnCode(30601006)
                .returnMsg("未查询到收方信息").build(), 30601006, "未查询到收方信息");
    }

    @Data
    @AllArgsConstructor
    public static class IcbcInvocation {

        private String operation;
        private Object payload;

    }

    // ==================== 结果覆盖 ====================

    public void setFaceVerifyPageResult(IcbcGatewayResult<IcbcPage> result) {
        this.faceVerifyPageResult = result;
    }

    public void setFaceVerifyStatusResult(IcbcGatewayResult<FaceVerifyStatus> result) {
        this.faceVerifyStatusResult = result;
    }

    public void setPayeeOnboardingResult(IcbcGatewayResult<IcbcPage> result) {
        this.payeeOnboardingResult = result;
    }

    public void setPayeeOnboardingStatusResult(IcbcGatewayResult<PayeeOnboardingStatus> result) {
        this.payeeOnboardingStatusResult = result;
    }

    public void setEnterpriseAuthResult(IcbcGatewayResult<IcbcPage> result) {
        this.enterpriseAuthResult = result;
    }

    public void setPreOrderResult(IcbcGatewayResult<IcbcPage> result) {
        this.preOrderResult = result;
    }

    public void setInvoiceInfoResult(IcbcGatewayResult<InvoiceInfo> result) {
        this.invoiceInfoResult = result;
    }

    public void setPaymentResult(IcbcGatewayResult<IcbcPage> result) {
        this.paymentResult = result;
    }

    public void setInvoiceFileResult(IcbcGatewayResult<InvoiceFile> result) {
        this.invoiceFileResult = result;
    }

    public void setInvoiceCancelResult(IcbcGatewayResult<InvoiceCancelResult> result) {
        this.invoiceCancelResult = result;
    }

    public void setRedInvoiceResult(IcbcGatewayResult<IcbcPage> result) {
        this.redInvoiceResult = result;
    }

    public void setRedInvoiceRevokeResult(IcbcGatewayResult<RedInvoiceRevokeResult> result) {
        this.redInvoiceRevokeResult = result;
    }

    public void setConnectivityResult(IcbcGatewayResult<IcbcConnectivity> result) {
        this.connectivityResult = result;
    }

}
