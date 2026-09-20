package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMovePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveSaveReqVO;

import javax.validation.Valid;

/**
 * 跨仓调拨 Service（#54 T16，ADR 0025）。
 *
 * <p>把同一品类的货从源维度搬到目标维度：过账时经 {@code StockApi#move} 一次写两条流水
 * （{@code MOVE_OUT} / {@code MOVE_IN}），余额与流水始终一致；作废已过账的单按相反方向调回。
 */
public interface StockMoveService {

    /**
     * 登记调拨单（待过账，不动库存）。
     *
     * @return 调拨单编号
     */
    Long createStockMove(@Valid StockMoveSaveReqVO reqVO);

    /**
     * 过账：经 {@code StockApi#move} 源减目标加。重复过账幂等。
     */
    void postStockMove(Long id);

    /**
     * 登记并过账（仓管一次成型）。
     *
     * @return 调拨单编号
     */
    Long confirmStockMove(@Valid StockMoveSaveReqVO reqVO);

    /**
     * 作废调拨单：待过账的直接作废；已过账的先按相反方向调回。
     */
    void cancelStockMove(@Valid StockMoveCancelReqVO reqVO);

    /**
     * 调拨单详情（含明细）
     */
    StockMoveRespVO getStockMove(Long id);

    /**
     * 调拨单分页
     */
    PageResult<StockMoveRespVO> getStockMovePage(@Valid StockMovePageReqVO reqVO);

}
