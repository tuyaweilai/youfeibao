package cn.iocoder.yudao.module.icbc.gateway.sdk;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.IcbcOutcome;
import cn.iocoder.yudao.module.icbc.gateway.IcbcReturnCodes;
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
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderGoods;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceGoods;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceRevokeReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceRevokeResult;
import com.icbc.api.IcbcApiException;
import com.icbc.api.IcbcRequest;
import com.icbc.api.IcbcResponse;import com.icbc.api.request.JftApiInvoiceDownloadRequestV1;
import com.icbc.api.request.JftApiInvoiceInfoQueryRequestV1;
import com.icbc.api.request.JftApiInvoiceRedOffsetRevokeRequestV1;
import com.icbc.api.request.JftApiInvoiceReversalRequestV1;
import com.icbc.api.request.JftApiUserEdpopenacctQueryRequestV1;
import com.icbc.api.request.JftApiUserEdpreceiveQueryRequestV1;
import com.icbc.api.request.JftUiUserFaceH5SubmitRequestV1;
import com.icbc.api.request.JftUiInvoicePayRequestV1;
import com.icbc.api.request.JftUiInvoicePreOrderRequestV1;
import com.icbc.api.request.JftUiRedInvoiceOffsetRequestV1;
import com.icbc.api.request.JftUiUserEdpopenacctSubmitRequestV1;
import com.icbc.api.request.JftUiVendorAuthRequestV1;
import com.icbc.api.response.JftApiInvoiceDownloadResponseV1;
import com.icbc.api.response.JftApiInvoiceInfoQueryResponseV1;
import com.icbc.api.response.JftApiInvoiceRedOffsetRevokeResponseV1;
import com.icbc.api.response.JftApiInvoiceReversalResponseV1;
import com.icbc.api.response.JftApiUserEdpopenacctQueryResponseV1;
import com.icbc.api.response.JftApiUserEdpreceiveQueryResponseV1;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * 工行适配层的生产实现：用官方 SDK 出站
 *
 * 所有签名、加密、网关地址、请求 / 响应类都只出现在本类与同包工具里。
 * 每个方法都把响应分类成 {@link IcbcOutcome}；代理异常 / 超时 / 未知返回码一律归为
 * {@link IcbcOutcome#UNKNOWN}，绝不重试，交由 {@code IcbcSubmitCoordinator} 决定先查询。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "icbc.gateway", name = "mode", havingValue = "sdk", matchIfMissing = true)
public class IcbcSdkGateway implements IcbcGateway {

    /**
     * 再生资源业务类型
     */
    private static final String BUSINESS_TYPE_RECYCLE = "0004";
    /**
     * 连通性探测用的外部用户编号
     */
    private static final String CONNECTIVITY_PROBE_OUT_USER_ID = "connectivity_check";
    private static final String CONNECTIVITY_PROBE_ACCOUNT = "6222020000000000000";

    @Resource
    private IcbcClientFactory clientFactory;

    // ==================== 实人认证 ====================

    @Override
    public IcbcGatewayResult<IcbcPage> submitFaceVerification(FaceVerifyPageReq req) {
        JftUiUserFaceH5SubmitRequestV1 request = new JftUiUserFaceH5SubmitRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.FACE_VERIFY_PAGE));
        JftUiUserFaceH5SubmitRequestV1.JftUiUserFaceH5SubmitRequestV1Biz biz =
                new JftUiUserFaceH5SubmitRequestV1.JftUiUserFaceH5SubmitRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        // 固定反向开票场景；证件目前仅支持身份证（见工行答复 §五）
        biz.setAuthScene("01");
        biz.setOutUserId(req.getOutUserId());
        biz.setCustName(req.getCustName());
        biz.setCertNo(req.getCertNo());
        biz.setMobile(req.getMobile());
        biz.setTransNo(req.getTransNo());
        biz.setCallbackUrl(req.getCallbackUrl());
        biz.setJumpUrl(req.getJumpUrl());
        biz.setFailJumpUrl(req.getFailJumpUrl());
        request.setBizContent(biz);
        return buildForm(request, null);
    }

    @Override
    public IcbcGatewayResult<FaceVerifyStatus> queryFaceVerification(String outUserId) {
        FaceH5QueryRequest request = new FaceH5QueryRequest();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.FACE_VERIFY_QUERY));
        FaceH5QueryRequest.Biz biz = request.getBizContent();
        biz.setAppId(clientFactory.appId());
        biz.setOutUserId(outUserId);

        return execute(request, IcbcApiPaths.RECEIVE_SUCCESS_CODE, response -> FaceVerifyStatus.builder()
                .outUserId(response.getOutUserId())
                .authResult(response.getAuthResult())
                .passed("1".equals(response.getAuthResult()))
                .failReason(response.getFailReason())
                .build());
    }

    // ==================== 收方入驻 ====================

    @Override
    public IcbcGatewayResult<IcbcPage> submitPayeeOnboarding(PayeeOnboardingPageReq req) {
        JftUiUserEdpopenacctSubmitRequestV1 request = new JftUiUserEdpopenacctSubmitRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.PAYEE_ONBOARDING_PAGE));
        JftUiUserEdpopenacctSubmitRequestV1.JftUiUserEdpopenacctSubmitRequestV1Biz biz =
                new JftUiUserEdpopenacctSubmitRequestV1.JftUiUserEdpopenacctSubmitRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        // 子商户 = 回收企业：优先用业务层给的（本租户付方档案），没给才回退全局配置（本地 / 联调）
        biz.setAppIdSub(StrUtil.isNotBlank(req.getOutVendorId()) ? req.getOutVendorId() : clientFactory.outVendorId());
        biz.setBusinessType(BUSINESS_TYPE_RECYCLE);
        biz.setOutUserId(req.getOutUserId());
        biz.setCorpSerno(req.getCorpSerno());
        biz.setTrxChannel(req.getTrxChannel());
        // 不主动开通电子钱包（见 ADR 0010）；工行若要求该字段必填，联调时再由适配层补默认值
        biz.setSkipImgUpload(req.getSkipImgUpload());
        biz.setSignDate(req.getSignDate());
        biz.setValidityPeriod(req.getValidityPeriod());
        biz.setJumpUrl(req.getJumpUrl());
        biz.setFailJumpUrl(req.getFailJumpUrl());
        biz.setCallbackUrl(req.getCallbackUrl());
        biz.setPreFillItems(buildPreFillItems(req));
        request.setBizContent(biz);
        return buildForm(request, null);
    }

    @Override
    public IcbcGatewayResult<PayeeOnboardingStatus> queryPayeeOnboarding(String outUserId, String outVendorId) {
        JftApiUserEdpopenacctQueryRequestV1 request = new JftApiUserEdpopenacctQueryRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.PAYEE_ONBOARDING_QUERY));
        JftApiUserEdpopenacctQueryRequestV1.JftApiUserEdpopenacctQueryRequestV1Biz biz =
                new JftApiUserEdpopenacctQueryRequestV1.JftApiUserEdpopenacctQueryRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setAppIdSub(StrUtil.isNotBlank(outVendorId) ? outVendorId : clientFactory.outVendorId());
        biz.setBusinessType(BUSINESS_TYPE_RECYCLE);
        biz.setOutUserId(outUserId);
        request.setBizContent(biz);

        return execute(request, IcbcApiPaths.RECEIVE_SUCCESS_CODE, response -> PayeeOnboardingStatus.builder()
                .outUserId(response.getOutUserId())
                .receiverStatus(response.getReceiverStatus())
                .auditStatus(response.getAuditStatus())
                // 数据接口不回传 result；审核通过时以 auditStatus=1 归一为 pass，其余留空等待异步通知
                .result("1".equals(response.getAuditStatus()) ? "pass" : null)
                .freezeStatus(response.getFreezeStatus())
                .openacctStatus(response.getOpenacctStatus())
                .mediumId(response.getMediumId())
                .custStatusDetail(response.getCustStatusDetail())
                .build());
    }

    // ==================== 企业授权 ====================

    @Override
    public IcbcGatewayResult<IcbcPage> submitEnterpriseAuthorization(EnterpriseAuthReq req) {
        JftUiVendorAuthRequestV1 request = new JftUiVendorAuthRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.ENTERPRISE_AUTHORIZATION));
        JftUiVendorAuthRequestV1.JftUiVendorAuthRequestV1Biz biz =
                new JftUiVendorAuthRequestV1.JftUiVendorAuthRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setOutVendorId(req.getOutVendorId() != null ? req.getOutVendorId() : clientFactory.outVendorId());
        biz.setOutUserId(req.getOutUserId());
        biz.setSiteType(req.getSiteType());
        biz.setUserType(req.getUserType());
        request.setBizContent(biz);
        return buildForm(request, null);
    }

    // ==================== 预下单 / 预查询 ====================

    @Override
    public IcbcGatewayResult<IcbcPage> submitPreOrder(PreOrderReq req) {
        JftUiInvoicePreOrderRequestV1 request = new JftUiInvoicePreOrderRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.PRE_ORDER));
        JftUiInvoicePreOrderRequestV1.JftUiInvoicePreOrderRequestV1Biz biz =
                new JftUiInvoicePreOrderRequestV1.JftUiInvoicePreOrderRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setOutVendorId(req.getOutVendorId() != null ? req.getOutVendorId() : clientFactory.outVendorId());
        biz.setOutOrderId(req.getOutOrderId());
        biz.setOutUserId(req.getOutUserId());
        biz.setInvoiceType(req.getInvoiceType());
        biz.setOrderAmount(req.getOrderAmount());
        biz.setSpecificElements(req.getSpecificElements());
        biz.setBuyerInvTypeCode(req.getBuyerInvTypeCode());
        biz.setTaxpayerNo(req.getTaxpayerNo());
        biz.setTaxpayerName(req.getTaxpayerName());
        biz.setDrawerName(req.getDrawerName());
        biz.setDrawerCardType(req.getDrawerCardType());
        biz.setDrawerCardNumber(req.getDrawerCardNumber());
        biz.setNaturalPersonName(req.getNaturalPersonName());
        biz.setCardType(req.getCardType());
        biz.setCardNumber(req.getCardNumber());
        biz.setSellerAddress(req.getSellerAddress());
        biz.setSellerTelephone(req.getSellerTelephone());
        biz.setAreaCode(req.getAreaCode());
        biz.setPayChannel(req.getPayChannel());
        biz.setIsSellerPersonProduct(req.getIsSellerPersonProduct());
        biz.setIitProject(req.getIitProject());
        biz.setNotes(req.getNotes());
        biz.setTaxRate(req.getTaxRate());
        biz.setSupplementaryTax(req.getSupplementaryTax());
        biz.setUnuseReduceTaxCode(req.getUnuseReduceTaxCode());
        biz.setTaxPayerAccountNo(req.getTaxPayerAccountNo());
        biz.setTaxPayerBankCode(req.getTaxPayerBankCode());
        biz.setTaxPayerOrgName(req.getTaxPayerOrgName());
        biz.setInvoiceNotifyUrl(req.getInvoiceNotifyUrl());
        biz.setPayJumpUrl(req.getPayJumpUrl());
        biz.setInvoiceJumpUrl(req.getInvoiceJumpUrl());
        biz.setPayRem(req.getPayRem());
        biz.setOrderRem(req.getOrderRem());
        biz.setGoodsInfo(toGoodsInfoList(req.getGoods()));
        request.setBizContent(biz);
        return buildForm(request, req.getOutOrderId());
    }

    @Override
    public IcbcGatewayResult<InvoiceInfo> queryInvoiceInfo(InvoiceQueryReq req) {
        JftApiInvoiceInfoQueryRequestV1 request = new JftApiInvoiceInfoQueryRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.INVOICE_QUERY));
        JftApiInvoiceInfoQueryRequestV1.JftApiPayInvoiceInfoQueryBiz biz =
                new JftApiInvoiceInfoQueryRequestV1.JftApiPayInvoiceInfoQueryBiz();
        biz.setAppId(clientFactory.appId());
        biz.setOutVendorId(req.getOutVendorId() != null ? req.getOutVendorId() : clientFactory.outVendorId());
        biz.setOutUserId(req.getOutUserId());
        biz.setOutOrderId(req.getOutOrderId());
        biz.setOutInvoiceId(req.getOutInvoiceId());
        biz.setOutRedOffsetId(req.getOutRedOffsetId());
        request.setBizContent(biz);

        return execute(request, IcbcApiPaths.INVOICE_SUCCESS_CODE, this::toInvoiceInfo);
    }

    // ==================== 付方支付 ====================

    @Override
    public IcbcGatewayResult<IcbcPage> submitPayment(PaymentReq req) {
        JftUiInvoicePayRequestV1 request = new JftUiInvoicePayRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.PAYMENT));
        JftUiInvoicePayRequestV1.JftUiInvoicePayRequestV1Biz biz =
                new JftUiInvoicePayRequestV1.JftUiInvoicePayRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setOutVendorId(req.getOutVendorId() != null ? req.getOutVendorId() : clientFactory.outVendorId());
        biz.setOutUserId(req.getOutUserId());
        biz.setOutOrderId(req.getOutOrderId());
        biz.setVerifiedCode(req.getVerifiedCode());
        biz.setUkeyId(req.getUkeyId());
        request.setBizContent(biz);
        return buildForm(request, req.getOutOrderId());
    }

    // ==================== 发票下载 / 取消 / 红冲 ====================

    @Override
    public IcbcGatewayResult<InvoiceFile> downloadInvoice(InvoiceDownloadReq req) {
        JftApiInvoiceDownloadRequestV1 request = new JftApiInvoiceDownloadRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.INVOICE_DOWNLOAD));
        JftApiInvoiceDownloadRequestV1.JftApiInvoiceDownloadRequestV1Biz biz =
                new JftApiInvoiceDownloadRequestV1.JftApiInvoiceDownloadRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setOutOrderId(req.getOutOrderId());
        biz.setOutInvoiceId(req.getOutInvoiceId());
        biz.setOutRedOffsetId(req.getOutRedOffsetId());
        biz.setIsRedOrblue(req.getIsRedOrblue());
        request.setBizContent(biz);

        return execute(request, IcbcApiPaths.INVOICE_SUCCESS_CODE, response -> InvoiceFile.builder()
                .fileName(buildInvoiceFileName(req))
                .content(response.getByteData())
                .build());
    }

    @Override
    public IcbcGatewayResult<InvoiceCancelResult> cancelInvoice(InvoiceCancelReq req) {
        JftApiInvoiceReversalRequestV1 request = new JftApiInvoiceReversalRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.INVOICE_CANCEL));
        JftApiInvoiceReversalRequestV1.JftApiInvoiceReversalRequestV1Biz biz =
                new JftApiInvoiceReversalRequestV1.JftApiInvoiceReversalRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setOutOrderId(req.getOutOrderId());
        request.setBizContent(biz);

        return execute(request, IcbcApiPaths.INVOICE_SUCCESS_CODE, response -> InvoiceCancelResult.builder()
                .reversalStatus(response.getReversalStatus())
                .reversalMsg(response.getReversalMsg())
                .build());
    }

    @Override
    public IcbcGatewayResult<IcbcPage> applyRedInvoice(RedInvoiceReq req) {
        JftUiRedInvoiceOffsetRequestV1 request = new JftUiRedInvoiceOffsetRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.RED_INVOICE_OFFSET));
        JftUiRedInvoiceOffsetRequestV1.JftUiRedInvoiceOffsetRequestV1Biz biz =
                new JftUiRedInvoiceOffsetRequestV1.JftUiRedInvoiceOffsetRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setOutRedOffsetId(req.getOutRedOffsetId());
        biz.setOutOrderId(req.getOutOrderId());
        biz.setRedOffsetReason(req.getRedOffsetReason());
        biz.setRedOffsetAmount(req.getRedOffsetAmount());
        biz.setIsRedo(req.getIsRedo());
        biz.setChannel(req.getChannel());
        biz.setJumpUrl(req.getJumpUrl());
        biz.setRedOffsetList(toRedOffsetGoodsList(req.getGoods()));
        request.setBizContent(biz);
        return buildForm(request, req.getOutOrderId());
    }

    @Override
    public IcbcGatewayResult<RedInvoiceRevokeResult> revokeRedInvoice(RedInvoiceRevokeReq req) {
        JftApiInvoiceRedOffsetRevokeRequestV1 request = new JftApiInvoiceRedOffsetRevokeRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.RED_INVOICE_REVOKE));
        JftApiInvoiceRedOffsetRevokeRequestV1.JftApiInvoiceRedOffsetRevokeRequestV1Biz biz =
                new JftApiInvoiceRedOffsetRevokeRequestV1.JftApiInvoiceRedOffsetRevokeRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setOutRedOffsetId(req.getOutRedOffsetId());
        biz.setOutOrderId(req.getOutOrderId());
        request.setBizContent(biz);

        return execute(request, IcbcApiPaths.INVOICE_SUCCESS_CODE, response -> RedInvoiceRevokeResult.builder()
                .outRedOffsetId(response.getOutRedOffsetId())
                .revokeStatus(response.getRevokeStatus())
                .build());
    }

    // ==================== 连通性 ====================

    @Override
    public IcbcGatewayResult<IcbcConnectivity> checkConnectivity() {
        JftApiUserEdpreceiveQueryRequestV1 request = new JftApiUserEdpreceiveQueryRequestV1();
        request.setServiceUrl(clientFactory.url(IcbcApiPaths.EDPRECEIVE_QUERY));
        JftApiUserEdpreceiveQueryRequestV1.JftApiUserEdpreceiveQueryRequestV1Biz biz =
                new JftApiUserEdpreceiveQueryRequestV1.JftApiUserEdpreceiveQueryRequestV1Biz();
        biz.setAppId(clientFactory.appId());
        biz.setAppIdSub(clientFactory.outVendorId());
        biz.setOutUserId(CONNECTIVITY_PROBE_OUT_USER_ID);
        biz.setReceiverAccount(CONNECTIVITY_PROBE_ACCOUNT);
        biz.setBusinessType(BUSINESS_TYPE_RECYCLE);
        request.setBizContent(biz);

        // 任何来自工行的业务响应（含业务错误码）都说明网关可达、签名有效；只有未知才算不可达
        IcbcGatewayResult<JftApiUserEdpreceiveQueryResponseV1> result =
                executeRaw(request, IcbcApiPaths.RECEIVE_SUCCESS_CODE);
        IcbcConnectivity connectivity = IcbcConnectivity.builder()
                .reachable(result.getOutcome() != IcbcOutcome.UNKNOWN)
                .returnCode(result.getReturnCode())
                .returnMsg(result.getReturnMsg())
                .build();
        if (result.isUnknown()) {
            return IcbcGatewayResult.unknown(result.getReturnCode(), result.getReturnMsg());
        }
        return IcbcGatewayResult.success(connectivity, result.getReturnCode(), result.getReturnMsg());
    }

    // ==================== 内部实现 ====================

    /**
     * 执行数据接口，并把响应映射成业务数据
     */
    private <T, R extends IcbcResponse> IcbcGatewayResult<T> execute(IcbcRequest<R> request, int successCode,
                                                                     Function<R, T> mapper) {
        IcbcGatewayResult<R> raw = executeRaw(request, successCode);
        if (raw.getOutcome() != IcbcOutcome.SUCCESS) {
            return IcbcGatewayResult.<T>builder()
                    .outcome(raw.getOutcome()).returnCode(raw.getReturnCode()).returnMsg(raw.getReturnMsg())
                    .build();
        }
        return IcbcGatewayResult.success(mapper.apply(raw.getData()), raw.getReturnCode(), raw.getReturnMsg());
    }

    /**
     * 执行数据接口，保留原始响应
     */
    private <R extends IcbcResponse> IcbcGatewayResult<R> executeRaw(IcbcRequest<R> request, int successCode) {
        try {
            R response = clientFactory.defaultClient().execute(request, generateMsgId());
            IcbcOutcome outcome = IcbcReturnCodes.classify(response.getReturnCode(), successCode);
            log.debug("[executeRaw] 工行返回 code={}, msg={}, outcome={}", response.getReturnCode(),
                    response.getReturnMsg(), outcome);
            if (outcome == IcbcOutcome.UNKNOWN) {
                return IcbcGatewayResult.unknown(response.getReturnCode(), response.getReturnMsg());
            }
            return IcbcGatewayResult.<R>builder()
                    .outcome(outcome).returnCode(response.getReturnCode()).returnMsg(response.getReturnMsg())
                    .data(response).build();
        } catch (IcbcApiException e) {
            // SDK 异常（含代理异常 / 超时 / 网络不可达）：指令是否到达未知，不得重复提交
            log.warn("[executeRaw] 工行 SDK 异常 code={}, msg={}", e.getErrCode(), e.getErrMsg());
            return IcbcGatewayResult.unknown(e.getErrCode(), e.getErrMsg());
        } catch (RuntimeException e) {
            log.error("[executeRaw] 工行调用运行时异常", e);
            return IcbcGatewayResult.unknown(0, e.getMessage());
        }
    }

    /**
     * 生成 UI 页面接口的自动提交表单
     */
    private IcbcGatewayResult<IcbcPage> buildForm(IcbcRequest<?> request, String outOrderId) {
        try {
            String formHtml = clientFactory.uiClient().buildPostForm(request);
            return IcbcGatewayResult.success(IcbcPage.builder().formHtml(formHtml).outOrderId(outOrderId).build(), 0, "成功");
        } catch (IcbcApiException e) {
            log.warn("[buildForm] 生成工行页面表单失败 code={}, msg={}", e.getErrCode(), e.getErrMsg());
            return IcbcGatewayResult.unknown(e.getErrCode(), e.getErrMsg());
        } catch (RuntimeException e) {
            log.error("[buildForm] 生成工行页面表单运行时异常", e);
            return IcbcGatewayResult.unknown(0, e.getMessage());
        }
    }

    private Map<String, Object> buildPreFillItems(PayeeOnboardingPageReq req) {
        Map<String, Object> items = new HashMap<>();
        items.put("receiverName", req.getReceiverName());
        items.put("receiverAccount", req.getReceiverAccount());
        items.put("mobile", req.getMobile());
        items.put("idNo", req.getIdNo());
        items.put("occupation", req.getOccupation());
        items.put("address", req.getAddress());
        return items;
    }

    private List<JftUiInvoicePreOrderRequestV1.GoodsInfo> toGoodsInfoList(List<PreOrderGoods> goods) {
        List<JftUiInvoicePreOrderRequestV1.GoodsInfo> list = new ArrayList<>();
        if (goods == null) {
            return list;
        }
        for (PreOrderGoods item : goods) {
            JftUiInvoicePreOrderRequestV1.GoodsInfo target = new JftUiInvoicePreOrderRequestV1.GoodsInfo();
            target.setGoodsSeqno(item.getGoodsSeqno());
            target.setProjectName(item.getProjectName());
            target.setGoodsNum(item.getGoodsNum());
            target.setGoodsAmt(item.getGoodsAmt());
            target.setWeight(item.getWeight());
            target.setPrice(item.getPrice());
            target.setUnits(item.getUnits());
            target.setTaxRate(item.getTaxRate());
            target.setMergedCode(item.getMergedCode());
            list.add(target);
        }
        return list;
    }

    private List<JftUiRedInvoiceOffsetRequestV1.RedOffsetGoodsInfo> toRedOffsetGoodsList(List<RedInvoiceGoods> goods) {
        List<JftUiRedInvoiceOffsetRequestV1.RedOffsetGoodsInfo> list = new ArrayList<>();
        if (goods == null) {
            return list;
        }
        for (RedInvoiceGoods item : goods) {
            JftUiRedInvoiceOffsetRequestV1.RedOffsetGoodsInfo target =
                    new JftUiRedInvoiceOffsetRequestV1.RedOffsetGoodsInfo();
            target.setGoodsSeqno(item.getGoodsSeqno());
            target.setBlueGoodsSeqno(item.getBlueGoodsSeqno());
            target.setProjectName(item.getProjectName());
            target.setGoodsNum(item.getGoodsNum());
            target.setGoodsAmt(item.getGoodsAmt());
            target.setWeight(item.getWeight());
            target.setPrice(item.getPrice());
            target.setUnits(item.getUnits());
            list.add(target);
        }
        return list;
    }

    private InvoiceInfo toInvoiceInfo(JftApiInvoiceInfoQueryResponseV1 response) {
        List<InvoiceInfo.InvoiceDetail> details = new ArrayList<>();
        if (response.getInvoiceDetailResponse() != null) {
            // 明细内部类是 SDK 的私有类型，用 JSON 中转取出字段
            JSONArray detailArray = (JSONArray) JSONArray.toJSON(response.getInvoiceDetailResponse());
            for (int i = 0; i < detailArray.size(); i++) {
                JSONObject item = detailArray.getJSONObject(i);
                details.add(InvoiceInfo.InvoiceDetail.builder()
                        .detailNumber(item.getString("detailNumber"))
                        .serviceName(item.getString("serviceName"))
                        .quantity(item.getString("quantity"))
                        .specsModel(item.getString("specsModel"))
                        .amount(item.getString("amount"))
                        .taxAmount(item.getString("taxAmount"))
                        .taxInclusiveAmount(item.getString("taxInclusiveAmount"))
                        .build());
            }
        }
        List<InvoiceInfo.RedOffsetDetail> redDetails = new ArrayList<>();
        if (response.getRedOffsetDetailList() != null) {
            JSONArray redArray = (JSONArray) JSONArray.toJSON(response.getRedOffsetDetailList());
            for (int i = 0; i < redArray.size(); i++) {
                JSONObject item = redArray.getJSONObject(i);
                redDetails.add(InvoiceInfo.RedOffsetDetail.builder()
                        .detailNumber(item.getString("detailNumber"))
                        .blueDetailNumber(item.getString("blueDetailNumber"))
                        .projectName(item.getString("projectName"))
                        .quantity(item.getString("quantity"))
                        .pricePerUnit(item.getString("pricePerUnit"))
                        .redOffsetAmount(item.getString("redOffsetAmount"))
                        .redOffsetTaxAmount(item.getString("redOffsetTaxAmount"))
                        .redOffsetTaxInclusiveAmount(item.getString("redOffsetTaxInclusiveAmount"))
                        .build());
            }
        }
        List<InvoiceInfo.LevyItem> levyItems = new ArrayList<>();
        if (response.getInvoiceLevyItemResponse() != null) {
            JSONArray levyArray = (JSONArray) JSONArray.toJSON(response.getInvoiceLevyItemResponse());
            for (int i = 0; i < levyArray.size(); i++) {
                JSONObject item = levyArray.getJSONObject(i);
                levyItems.add(InvoiceInfo.LevyItem.builder()
                        .levyItemCode(item.getString("levyItemCode"))
                        .levyItemName(item.getString("levyItemName"))
                        .levyGradeCode(item.getString("levyGradeCode"))
                        .levyGradeName(item.getString("levyGradeName"))
                        .taxBasis(item.getString("taxBasis"))
                        .taxRate(item.getString("taxRate"))
                        .taxPayable(item.getString("taxPayable"))
                        .voucherNum(item.getString("voucherNum"))
                        .taxStartDate(item.getString("taxStartDate"))
                        .taxEndDate(item.getString("taxEndDate"))
                        .build());
            }
        }
        return InvoiceInfo.builder()
                .outOrderId(response.getOutOrderId())
                .outUserId(response.getOutUserId())
                .outVendorId(response.getOutVendorId())
                .outInvoiceId(response.getOutInvoiceId())
                .outRedOffsetId(response.getOutRedOffsetId())
                .confirmStatus(response.getConfirmStatus())
                .payStatus(response.getPayStatus())
                .invoiceStatus(response.getInvoiceStatus())
                .uploadStatus(response.getUploadStatus())
                .taxStatus(response.getTaxStatus())
                .redOffsetStatus(response.getRedOffsetStatus())
                .redOffsetInvoiceCode(response.getRedOffsetInvoiceCode())
                .redOffsetReason(response.getRedOffsetReason())
                .redOffsetAmount(response.getRedOffsetAmount())
                .redOffsetTax(response.getRedOffsetTax())
                .redOffsetAmountTax(response.getRedOffsetAmountTax())
                .invoiceCode(response.getInvoiceCode())
                .invoiceNo(response.getInvoiceCode())
                .invoiceDate(response.getInvoiceDate())
                .taxAmount(response.getTaxAmount())
                .taxRealAmount(response.getTaxRealAmount())
                .tradeTime(response.getTradeTime())
                .supplementaryTax(response.getSupplementaryTax())
                .payAmount(response.getPayAmount())
                .actuallyReceivedAmount(response.getActuallyReceivedAmount())
                .icbcOrderId(response.getIcbcOrderId())
                .jOrderId(response.getjOrderId())
                .serialNo(response.getSerialNo())
                .invoiceDetail(details)
                .levyItems(levyItems)
                .redOffsetDetail(redDetails)
                .build();
    }

    private String buildInvoiceFileName(InvoiceDownloadReq req) {
        String key = req.getOutInvoiceId() != null ? req.getOutInvoiceId() : req.getOutOrderId();
        return "invoice_" + key + ".pdf";
    }

    private String generateMsgId() {
        return IdUtil.fastSimpleUUID();
    }

}
