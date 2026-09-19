package cn.iocoder.yudao.module.icbc.service.notify;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.notify.vo.*;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicNoticeRespVO;

import javax.validation.Valid;

/**
 * 出售者触达 Service（#36，ADR 0023）。
 *
 * <p>一期触达只有两条路：**短信**（平台 / 租户开关，默认关闭）与**收货员一键把确认链接转达给他**。
 * 短信只发三条：结算单待确认 / 付款异常 / 发票已开出；每条都带一次性令牌链接，收短信的人不需要注册。
 * 不做 App 推送、不做公众号。
 *
 * <p>触达幂等：同一业务事件（同一结算单版本 / 同一笔付款的同一异常状态 / 同一张票开出）只发一次，
 * 不重复轰炸；发不出去也要留记录并说明原因。
 */
public interface SellerNotifyService {

    /** 结算单待确认：生成结算单、企业对异议改版后触发（幂等） */
    void onSettlementPending(Long settlementId);

    /** 付款异常：支付状态收敛为失败 / 冲正 / 退汇 / 部分成功等异常态时触发（幂等） */
    void onPaymentException(String partnerOrderId);

    /** 发票已开出：开票状态首次收敛为「已开票」时触发（幂等） */
    void onInvoiceIssued(String partnerOrderId);

    /**
     * 收货员一键把结算确认链接转达给出售者：返回一次性令牌链接与可直接复制的短信文案；
     * {@code sendSms=true} 时顺带发一条短信（由人显式触发，不受自动开关影响）。
     */
    NotifyForwardLinkRespVO forwardSettlementLink(@Valid NotifyForwardLinkReqVO reqVO);

    /** 触达记录分页（本租户）。 */
    PageResult<NotifyRespVO> getNotifyPage(NotifyPageReqVO reqVO);

    /** 当前触达设置（平台开关 + 本租户开关 + 入口是否配置）。 */
    NotifySettingRespVO getSetting();

    /** 保存本租户触达设置。 */
    void saveSetting(@Valid NotifySettingSaveReqVO reqVO);

    /**
     * 免登录通知：按收方档案组装「这个人现在有什么事」。调用方必须已切到该收方所属租户。
     */
    PublicNoticeRespVO getNoticeForPayee(Long payeeId);

}
