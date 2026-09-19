package cn.iocoder.yudao.module.icbc.service.acquisition;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * 收购登记 Service 接口。
 *
 * <p>对应 issue #7：收货员在现场完整登记一笔收购——谁卖的、卖的什么、多少量、什么价、
 * 车牌、货物照片、什么时候在哪儿。登记完，这一笔的合同流、货物流、信息流骨架就成形了。
 */
public interface AcquisitionService {

    /**
     * 登记一笔收购。
     *
     * <p>按 {@code clientRequestId} 幂等：同一笔重复登记只落一条，返回既有收购单编号。
     * 必须要件（出售者、品类、数量、金额、磅单）缺一即拒，并说明缺什么。
     *
     * <p>响应里带回该出售者的额度余量提示（issue #12）：额度是自然人跨租户累计的，
     * 现场不看就没人看得到。这里只提示不断拦，硬校验在开票申请。
     *
     * @return 收购单编号与该出售者的额度余量提示
     */
    AcquisitionCreateRespVO createAcquisition(@Valid AcquisitionCreateReqVO reqVO);

    /**
     * 离线补传：逐条登记，重复补传不产生重复单据，单条失败不影响其他条。
     */
    List<AcquisitionSyncResultVO> syncOffline(@Valid AcquisitionOfflineSyncReqVO reqVO);

    /**
     * 人工修正磅单 / 车牌的识别结果，并重新做车牌比对。
     */
    void correctRecognition(@Valid AcquisitionCorrectionReqVO reqVO);

    /**
     * 按编号取收购单
     */
    IcbcAcquisitionDO getAcquisition(Long id);

    /**
     * 分页查询收购单
     */
    PageResult<IcbcAcquisitionDO> getAcquisitionPage(AcquisitionPageReqVO reqVO);

    /**
     * 某出售者的全部收购单（倒序）
     */
    List<IcbcAcquisitionDO> getAcquisitionsByPayeeId(Long payeeId);

    // ==================== 与开票 / 付款链路的关联 ====================

    /**
     * 把收购单挂到开票合作方订单号上，状态推进为「待付款」。
     */
    void linkInvoice(Long acquisitionId, String partnerOrderId);

    /**
     * 按开票合作方订单号把收购单标记为「已付款」。
     */
    void markPaidByInvoicePartnerOrderId(String partnerOrderId);

    /**
     * 按开票合作方订单号把收购单标记为「已开票」。
     */
    void markInvoicedByInvoicePartnerOrderId(String partnerOrderId);

    // ==================== 确认书导出 ====================

    /**
     * 导出单笔收购确认书（可打印）
     */
    void exportConfirmation(Long id, HttpServletResponse response);

}
