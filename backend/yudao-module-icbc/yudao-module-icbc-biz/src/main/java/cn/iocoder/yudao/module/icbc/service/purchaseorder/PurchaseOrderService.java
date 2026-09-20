package cn.iocoder.yudao.module.icbc.service.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderProgressRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderStatusUpdateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单 Service 接口（#46 T08，ADR 0027）。
 *
 * <p>采购订单是回收企业内部的**采购执行依据**：向谁买哪些品类、多少量、什么价、在哪段时间、
 * 哪个场站，一单多条品类明细、一条明细可分多次收货。它不是交易对方下的单（那是预约到站），
 * 零散收购也可以不挂订单（报表标「直接收购」，见 #51）。
 *
 * <p>{@link #assertUsableAsPurchaseBasis(Long)} 是「执行中且未过期」的唯一门禁；收购登记（#51）
 * 选采购安排时调用它，不要各自复制判断。
 */
public interface PurchaseOrderService {

    /**
     * 新建采购订单（落为草稿，还没有采购效力）。
     *
     * <p>关联合同时校验合同已审核生效（{@code PurchaseContractService#assertUsableAsPurchaseBasis}）。
     *
     * @param createReqVO 新建信息
     * @return 订单编号
     */
    Long createOrder(PurchaseOrderSaveReqVO createReqVO);

    /**
     * 修改采购订单（只允许草稿；执行中的订单不再改明细，避免与已发生的收货对不上）。
     *
     * @param updateReqVO 修改信息
     */
    void updateOrder(PurchaseOrderSaveReqVO updateReqVO);

    /**
     * 状态流转：草稿 → 执行中 ⇄ 暂停 → 完成 → 关闭。非法流转与暂停缺原因在这里拦下。
     *
     * @param reqVO 目标状态与原因
     */
    void updateStatus(PurchaseOrderStatusUpdateReqVO reqVO);

    /**
     * 删除采购订单（只允许草稿；已发生的业务不因删除而消失，#47）。
     *
     * @param id 订单编号
     */
    void deleteOrder(Long id);

    /**
     * 获得采购订单。
     *
     * @param id 订单编号
     * @return 采购订单
     */
    IcbcPurchaseOrderDO getOrder(Long id);

    /**
     * 获得采购订单详情（含品类明细、已收量推导与交货日价格表）。
     *
     * @param id 订单编号
     * @return 详情
     */
    PurchaseOrderRespVO getDetail(Long id);

    /**
     * 获得采购订单分页。
     *
     * @param pageReqVO 分页条件
     * @return 分页
     */
    PageResult<PurchaseOrderRespVO> getOrderPage(PurchaseOrderPageReqVO pageReqVO);

    /**
     * 获得采购订单执行进度（本票：计划 / 已收；五口径分列见 #47）。
     *
     * @param id 订单编号
     * @return 执行进度
     */
    PurchaseOrderProgressRespVO getProgress(Long id);

    /**
     * 校验订单可作为采购依据：必须「执行中」且未过期。
     *
     * @param id 订单编号
     * @return 执行中的订单
     */
    IcbcPurchaseOrderDO assertUsableAsPurchaseBasis(Long id);

    /**
     * 登记一次成交：留价格快照与调整原因，数量汇总为明细的已收量。
     *
     * <p>成交价通常来自收购单（#51）；服务端按明细定价方式算出参考价并比对，
     * 不一致时 {@code adjustReason} 必填。
     *
     * @param reqVO 成交信息
     * @return 成交记录编号
     */
    Long recordDeal(PurchaseOrderDealReqVO reqVO);

    /**
     * 获得某订单的全部成交记录（价格快照只追加）。
     *
     * @param orderId 订单编号
     * @return 成交记录
     */
    List<PurchaseOrderDealRespVO> getDealList(Long orderId);

    /**
     * 按明细与交货日算参考价：固定单价直接用明细单价；按交货日价格表取「不晚于交货日的最新一条」，
     * 没覆盖到则回退明细单价。
     *
     * @param itemId       明细编号
     * @param deliveryDate 交货日（可空，按明细单价）
     * @return 参考单价
     */
    BigDecimal resolveUnitPrice(Long itemId, LocalDate deliveryDate);

    /**
     * 只读：按订单编号取「单号 + 单据金额」，供进项收票勾稽（#49）使用。
     *
     * <p>本票自身不调用；#49 合并后把 {@code PURCHASE_ORDER} 类型的 {@code biz_amount} 接到这里。
     *
     * @param id 订单编号
     * @return 单号与金额
     */
    PurchaseOrderAmountDTO getOrderAmount(Long id);

}
