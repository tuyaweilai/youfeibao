package cn.iocoder.yudao.module.icbc.service.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionReviewReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionSaveReqVO;

/**
 * 采购订单履约异常授权单 Service（#47 T09）。
 *
 * <p>超量 / 过期 / 跨场站交货被企业配置成「提交授权审核」时的唯一入口：提交一张授权单 → 审核 →
 * 已通过的授权单成为交货门禁（{@code PurchaseOrderService#assertDeliveryAllowed}）的放行依据。
 *
 * <p>授权单只追加：待审核不能重复提交同一件事；已审核的不再改（要改就重新提交一张）。
 * 它只放宽被授权的那一件事（追加量 / 有效期 / 场站），不改订单状态、不改任何已发生的业务。
 */
public interface PurchaseOrderExceptionService {

    /**
     * 提交一张履约异常授权单（落为待审核）。
     *
     * @param reqVO 授权申请
     * @return 授权单编号
     */
    Long requestException(PurchaseOrderExceptionSaveReqVO reqVO);

    /**
     * 审核授权单（通过 / 拒绝）。只有待审核的可以审。
     *
     * @param reqVO 审核信息
     */
    void reviewException(PurchaseOrderExceptionReviewReqVO reqVO);

    /**
     * 获得履约异常授权单分页。
     *
     * @param reqVO 分页条件
     * @return 分页
     */
    PageResult<PurchaseOrderExceptionRespVO> getExceptionPage(PurchaseOrderExceptionPageReqVO reqVO);

    /**
     * 获得履约异常授权单。
     *
     * @param id 授权单编号
     * @return 授权单
     */
    PurchaseOrderExceptionRespVO getException(Long id);

}
