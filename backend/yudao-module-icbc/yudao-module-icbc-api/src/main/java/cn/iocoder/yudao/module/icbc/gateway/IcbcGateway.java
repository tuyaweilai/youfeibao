package cn.iocoder.yudao.module.icbc.gateway;

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

/**
 * 工行适配层：平台对工行的<strong>唯一</strong>出站端口
 *
 * 设计约束（见 issue #3）：
 * <ul>
 *   <li>平台其余部分不得出现工行的网关地址、签名与加解密逻辑。它们全部收在本端口之后的
 *       {@code gateway.sdk} 包内。</li>
 *   <li>端口覆盖收方入驻页面与结果查询、企业授权、预下单、预查询、付方支付、发票下载、
 *       发票取消、红字冲销与撤销。</li>
 *   <li>每个方法返回 {@link IcbcGatewayResult}，调用方据 {@link IcbcOutcome} 分支，
 *       不解析工行返回码字典。</li>
 *   <li>工行推来的异步通知走 {@code IcbcNotifyService} 唯一入口，不经过本端口。</li>
 * </ul>
 *
 * 实现可替换：生产用 {@code IcbcSdkGateway}，测试注入 {@code FakeIcbcGateway}，测试不触网。
 */
public interface IcbcGateway {

    /**
     * 实人认证 H5 页面：出售者建档的实名核验前置环节
     */
    IcbcGatewayResult<IcbcPage> submitFaceVerification(FaceVerifyPageReq req);

    /**
     * 实人认证结果查询
     */
    IcbcGatewayResult<FaceVerifyStatus> queryFaceVerification(String outUserId);

    /**
     * 收方入驻页面（实名 + 绑定银行卡）
     */
    IcbcGatewayResult<IcbcPage> submitPayeeOnboarding(PayeeOnboardingPageReq req);

    /**
     * 收方入驻结果查询：查开户与智慧清分入驻结果。
     *
     * @param outUserId   平台级外部用户编号（自然人主体）
     * @param outVendorId 子商户编号（回收企业）；收方是「自然人 × 子商户」的，所以查询也要带它
     */
    IcbcGatewayResult<PayeeOnboardingStatus> queryPayeeOnboarding(String outUserId, String outVendorId);

    /**
     * 企业授权：返回税务可信二维码页面，供法代 / 财务负责人扫码实人认证
     */
    IcbcGatewayResult<IcbcPage> submitEnterpriseAuthorization(EnterpriseAuthReq req);

    /**
     * 开票信息预下单：返回自然人确认页面
     */
    IcbcGatewayResult<IcbcPage> submitPreOrder(PreOrderReq req);

    /**
     * 开票信息预查询：取回自然人确认状态与五条状态线
     */
    IcbcGatewayResult<InvoiceInfo> queryInvoiceInfo(InvoiceQueryReq req);

    /**
     * 付方支付：返回企业支付页面
     */
    IcbcGatewayResult<IcbcPage> submitPayment(PaymentReq req);

    /**
     * 发票下载：取回 PDF 字节流
     */
    IcbcGatewayResult<InvoiceFile> downloadInvoice(InvoiceDownloadReq req);

    /**
     * 发票取消：仅限预开票成功但未支付的发票
     */
    IcbcGatewayResult<InvoiceCancelResult> cancelInvoice(InvoiceCancelReq req);

    /**
     * 红字冲销开票：返回红字确认单页面
     */
    IcbcGatewayResult<IcbcPage> applyRedInvoice(RedInvoiceReq req);

    /**
     * 红字冲销确认单撤销
     */
    IcbcGatewayResult<RedInvoiceRevokeResult> revokeRedInvoice(RedInvoiceRevokeReq req);

    /**
     * 真实连通性校验：用一条数据接口打满工行网关，验证网络与签名配置
     */
    IcbcGatewayResult<IcbcConnectivity> checkConnectivity();

}
