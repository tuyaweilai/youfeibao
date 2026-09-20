package cn.iocoder.yudao.module.icbc.service.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseArrangementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDeliveryCheckReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDeliveryCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderProgressRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSettingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSettingSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderStatusUpdateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDealDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderItemDO;

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
     * 获得采购订单执行进度（#47 T09：**计划 / 验收 / 入库 / 结算 / 未履行**五口径分列，不混口径）。
     *
     * <p>口径的唯一来源是 {@code PurchaseProgressMeasureEnum}；完成比例按企业配置的履约口径计算，
     * 随响应一起返回口径名与口径说明。「入库」在入库单（#52）落地前取不到数，返回
     * {@code available=false} + {@code unavailableReason}，不给一个会被误读的 0。
     *
     * @param id 订单编号
     * @return 执行进度
     */
    PurchaseOrderProgressRespVO getProgress(Long id);

    /**
     * 校验一次交货是否被允许（#47 T09 AC2：超量 / 过期 / 跨场站交货按企业配置拦截或提交授权审核）。
     *
     * <p>只读，不抛业务异常：调用方（现场 UI）拿它去提示「为什么不能收」与「下一步做什么」。
     *
     * @param reqVO 交货校验信息
     * @return 校验结果
     */
    PurchaseOrderDeliveryCheckRespVO checkDelivery(PurchaseOrderDeliveryCheckReqVO reqVO);

    /**
     * 交货门禁：不允许则抛业务异常。收购登记（#51）在把收购单挂到订单上之前调用它。
     *
     * @param reqVO 交货校验信息
     */
    void assertDeliveryAllowed(PurchaseOrderDeliveryCheckReqVO reqVO);

    /**
     * 获得采购履约配置（未配置过时返回默认值：完成比例按验收口径、三类异常都拦截）。
     *
     * @return 配置
     */
    PurchaseOrderSettingRespVO getSetting();

    /**
     * 修改采购履约配置（租户级，单行）。
     *
     * @param reqVO 配置
     */
    void updateSetting(PurchaseOrderSettingSaveReqVO reqVO);

    /**
     * 校验订单可作为采购依据：必须「执行中」且未过期。
     *
     * @param id 订单编号
     * @return 执行中的订单
     */
    IcbcPurchaseOrderDO assertUsableAsPurchaseBasis(Long id);

    /**
     * 登记一次成交：留价格快照与调整原因，数量汇总为明细的**验收口径**。
     *
     * <p>成交价通常来自收购单（#51）；服务端按明细定价方式算出参考价并比对，
     * 不一致时 {@code adjustReason} 必填。
     *
     * <p>#47 T09 起，这里也是**交货门禁**的落点：正数（收货）先过
     * {@link #assertDeliveryAllowed}——超量 / 过期 / 跨场站按企业配置拦截或要求授权审核；
     * 负数表示**退货**，按同一口径自动扣回（已暂停 / 完成 / 关闭的订单仍可登记退货，只有草稿不行）。
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
     * 获得某交易对方的**可选采购安排**（#51 T13）：执行中且未过期的采购订单，连同其品类明细。
     *
     * <p>收购登记现场据此选出一个有效采购安排，也可以什么都不选（不选即「直接收购」）。
     * 「有效」的判定与 {@link #assertUsableAsPurchaseBasis(Long)} 共用同一套规则（执行中 + 未过期）。
     *
     * @param payeeId 自然人出售者档案编号；为空返回空列表（现场先带出售者再选安排）
     * @return 可选采购安排
     */
    List<PurchaseArrangementRespVO> getUsableArrangements(Long payeeId);

    /**
     * 取订单明细，并校验它确实属于该订单（收购单关联采购安排时调用）。
     *
     * @param orderId 订单编号
     * @param itemId  明细编号
     * @return 采购订单明细
     */
    IcbcPurchaseOrderItemDO getOrderItem(Long orderId, Long itemId);

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

    /**
     * 按来源单据查成交记录（#58）：收购单作废时用它判断是否已经反冲过，避免重复扣回。
     */
    List<IcbcPurchaseOrderDealDO> selectDealsBySource(String sourceType, Long sourceId);

    /**
     * 修正由收购单产生的成交的数量（#58）：接收结论（#53 拒收 / 部分接收）变更时，
     * 同一笔收购的成交就地改数量；未关联订单 / 没有对应成交时什么也不做。
     */
    void correctAcquisitionDeal(Long sourceId, BigDecimal quantity, BigDecimal acceptedQuantity);

}
