package cn.iocoder.yudao.module.icbc.service.stockin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 待入库 → 入库单 → 库存流水 Service（#52 T14，ADR 0027）。
 *
 * <p>入库是收购单派生的**单向动作**：验收后的货进待入库，仓管选仓库 / 库位 / 批次确认实际入库量，
 * 过账后经 {@code StockApi} 写库存流水、增量余额。icbc 不直接碰 {@code erp_stock*}。
 */
public interface StockInService {

    /**
     * 可入库实物量（**唯一取数点**）。
     *
     * <p>接收量优先，无接收结论时退回净重（实物口径，毛重 − 皮重；ADR 0028：结算重量只作计价基准，
     * 不影响库存）。接收量由 #53（T15）的验收结论给出：拒收 / 退回 / 余货出场的部分不可入库。
     */
    BigDecimal resolveAvailableQuantity(IcbcAcquisitionDO acquisition);

    /**
     * 某收购单的累计入库 = 当前已过账入库单合计（作废的已不在其中）。
     */
    BigDecimal getStockedQuantity(Long acquisitionId);

    /**
     * 某采购订单下**已过账入库的数量**，按订单明细汇总（#47 履约的「入库」口径用）。
     *
     * <p>入库单挂收购单、收购单再挂采购订单明细：这层关系收在入库模块内，采购模块不用知道入库单的表结构。
     * 返回的 Map 只含「有入库量」的明细；待过账与已作废的不计。
     */
    Map<Long, BigDecimal> getStockedQuantityByOrderItems(Long purchaseOrderId);

    /**
     * 待入库分页：已验收（已归入结算单）、未作废、且剩余可入库大于 0 的收购单。
     */
    PageResult<StockInPendingRespVO> getPendingPage(@Valid StockInPendingPageReqVO reqVO);

    /**
     * 建入库单（**待过账**，不动库存）：只记下选的仓库 / 库位 / 批次与实际入库量。
     *
     * @return 入库单编号
     */
    Long createStockIn(@Valid StockInSaveReqVO reqVO);

    /**
     * 过账：经 {@code StockApi} 写库存流水并增量余额。已过账时幂等返回（重复确认不重复加库存）。
     */
    void postStockIn(Long id);

    /**
     * 作废入库单：待过账的直接作废；已过账的先按相反方向冲销库存。
     */
    void cancelStockIn(@Valid StockInCancelReqVO reqVO);

    /**
     * 确认入库 = 建单 + 过账（仓管一次成型）。
     *
     * @return 入库单编号
     */
    Long confirmStockIn(@Valid StockInSaveReqVO reqVO);

    /**
     * 入库单详情（含明细）
     */
    StockInRespVO getStockIn(Long id);

    /**
     * 入库单分页
     */
    PageResult<StockInRespVO> getStockInPage(@Valid StockInPageReqVO reqVO);

}
