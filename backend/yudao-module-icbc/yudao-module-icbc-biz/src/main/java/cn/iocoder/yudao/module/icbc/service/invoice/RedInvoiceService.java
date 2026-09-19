package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceApplyResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceRevokeReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;

/**
 * 红字冲销与发票取消 Service 接口（issue #14）。
 *
 * <p>三件事彼此独立，不能混：
 * <ul>
 *   <li><strong>红字冲销开票</strong>：对已开出的蓝票另开一张红票冲销，红蓝一一对应；</li>
 *   <li><strong>红字确认单撤销</strong>：红票尚未上传成功时，把红字确认单收回；</li>
 *   <li><strong>发票取消</strong>：仅限「预开票成功但未支付」的发票，此时票尚未开出，无需红冲。</li>
 * </ul>
 */
public interface RedInvoiceService {

    /**
     * 发起红字冲销：校验通过后经适配层取得红字确认单页面。
     *
     * <p>同一蓝票重复发起不再下第二次单，直接返回既有红冲记录（{@code duplicate=true}）；
     * 失败 / 撤销后的红冲可以重新发起。
     */
    RedInvoiceApplyResultVO apply(RedInvoiceApplyReqVO reqVO);

    /**
     * 撤销尚未生效的红字确认单。
     */
    RedInvoiceQueryRespVO revoke(RedInvoiceRevokeReqVO reqVO);

    /**
     * 按红冲流水号查询。
     */
    RedInvoiceQueryRespVO getByRedOffsetNo(String redOffsetNo);

    /**
     * 按蓝票合作方订单号查询最近一次红冲记录（可能为空）。
     */
    RedInvoiceQueryRespVO getByPartnerOrderId(String partnerOrderId);

    /**
     * 主动向工行查询红冲最新状态并收敛（重复提交 / 超时后只查状态不重下单）。
     */
    RedInvoiceQueryRespVO refresh(String redOffsetNo);

    /**
     * 用工行通知 / 查询结果收敛红冲状态线（{@code notifyType=07/08/09}）。
     *
     * <p>查不到红冲记录时抛 {@code CALLBACK_BUSINESS_NOT_EXISTS}，通知落失败可重放；
     * 已红冲成功 / 已撤销的终态不被旧的进行中状态回退。
     */
    void applyRedInvoiceInfo(String outRedOffsetId, InvoiceInfo info);

    /**
     * 取消「预开票成功但未支付」的发票。
     */
    void cancelPreInvoice(String partnerOrderId);

    /**
     * 最近一次红冲记录（供证据链装配），无则返回 {@code null}。
     */
    RedInvoiceDO getLatestRedInvoice(String partnerOrderId);
}
