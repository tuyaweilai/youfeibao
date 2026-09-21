package cn.iocoder.yudao.module.icbc.service.onboarding;

import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.authorization.IcbcSellerAuthorizationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 出售者建档 Service 接口
 *
 * <p>对应 issue #6：收货员在现场为一个自然人出售者办完所有一次性手续——实名核验、
 * 银行卡绑定、收方入驻、签署框架收购协议、授权反向开票与代办税费。办完之后，此人
 * 以后每次来卖货都不需要再办任何手续。
 */
public interface SellerOnboardingService {

    /**
     * 回头客带档：按身份证或手机号查出既有档案，免于重新登记。
     *
     * @param idCardNo 身份证号（可空）
     * @param mobile   手机号（可空）
     * @return 既有档案；查不到返回 {@code null}
     */
    PayeeInfoDO findReturningCustomer(String idCardNo, String mobile);

    // ==================== 实人认证 ====================

    /**
     * 发起实人认证，返回工行 H5 页面表单
     */
    SellerStepRespVO startRealName(@Valid SellerRealNameReqVO reqVO);

    /**
     * 主动查询实人认证结果并回写档案
     */
    PayeeInfoDO syncRealName(Long payeeId);

    // ==================== 收方入驻 ====================

    /**
     * 发起收方入驻（实名通过后才可发起）。
     *
     * <p>走的是工行数据接口，**不生成任何页面**（ADR 0035）：调用成功只代表受理，状态进入「审核中」，
     * 结论等审核通知或 {@link #syncOnboarding}。已在途或已通过的重复发起会被忽略（不重复提交）。
     *
     * @return 受理后的收方档案
     */
    PayeeInfoDO submitOnboarding(@Valid SellerOnboardingSubmitReqVO reqVO);

    /**
     * 主动查询收方入驻结果并回写档案
     */
    PayeeInfoDO syncOnboarding(Long payeeId);

    /**
     * 把审核结果收敛到建档状态机上（只有审核一条线）。
     *
     * @param payeeId     出售者编号
     * @param auditStatus 查询接口给的审核状态：1-审核通过，2-新增审核中，3-修改审核中，4-删除审核中；回调路径传空
     * @param result      审核结果：pass / reject；查询接口不带这个字段，传空
     * @param rejectReason 审核拒绝原因
     * @return 更新后的档案
     */
    PayeeInfoDO reconcileOnboardingStatus(Long payeeId, String auditStatus, String result, String rejectReason);

    /**
     * 收方入驻失败时留下联系方式等待联系
     */
    void leaveContactFallback(@Valid SellerContactFallbackReqVO reqVO);

    // ==================== 建档总览与开票门禁 ====================

    /**
     * 建档总览：状态机、协议、授权与是否可开票
     */
    SellerOnboardingRespVO getOnboarding(Long payeeId);

    /**
     * 开票门禁：未完成建档（含审核拒绝 / 开户失败）的出售者不能用于开票
     *
     * @param payeeId 出售者编号
     */
    void assertReadyForInvoice(Long payeeId);

    /**
     * 开票门禁（按外部用户编号）：查不到档案时放行，保持对既有数据的兼容；
     * 查得到则要求已完成建档。
     *
     * @param outUserId 平台级外部用户编号（自然人主体的工行 outUserId）
     */
    void assertReadyForInvoiceByOutUserId(String outUserId);

    /**
     * 某收方是否有在途的收款账户变更（换卡，#37）。
     */
    boolean hasPendingBankCardChange(Long payeeId);

    // ==================== 框架收购协议 ====================

    /**
     * 签署 / 更新框架收购协议。同一出售者只保留一份生效协议，旧协议作废并留痕。
     *
     * @return 协议编号
     */
    Long saveFrameworkAgreement(@Valid FrameworkAgreementSaveReqVO reqVO);

    /**
     * 取生效中的框架收购协议
     */
    IcbcFrameworkAgreementDO getActiveFrameworkAgreement(Long payeeId);

    /**
     * 取某出售者的全部协议（倒序，含已作废）
     */
    List<IcbcFrameworkAgreementDO> getFrameworkAgreements(Long payeeId);

    // ==================== 首次授权 ====================

    /**
     * 记录出售者首次反向开票与代办税费授权（留痕）
     *
     * @return 授权记录编号
     */
    Long authorizeSeller(@Valid SellerAuthorizationSaveReqVO reqVO);

    /**
     * 取某出售者最近一次授权
     */
    IcbcSellerAuthorizationDO getSellerAuthorization(Long payeeId);

    // ==================== 异步通知处理 ====================

    /**
     * 处理实人认证结果通知
     */
    void handleFaceVerifyNotify(String outUserId, boolean passed, String failReason);

    /**
     * 处理收方入驻结果通知。
     *
     * <p>{@code outUserId} 是平台级外部用户编号（自然人主体），而入驻是「自然人 × 子商户」的动作，
     * 所以还要靠 {@code outVendorId}（报文里的 {@code appIdSub}）定位是哪家回收企业。
     * 定位不了时抛业务异常，让通知落失败、在平台运营的通知监控里人工处理——不猜、不跨企业乱写。
     *
     * @param outUserId    平台级外部用户编号
     * @param outVendorId  子商户编号（回收企业），即报文里的 appIdSub
     * @param result       审核结果（pass / reject）
     * @param rejectReason 拒绝原因
     */
    void handleOnboardingNotify(String outUserId, String outVendorId, String result, String rejectReason);

}
