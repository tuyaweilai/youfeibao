package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutSaveReqVO;

import javax.validation.Valid;

/**
 * 非销售出库 Service（#54 T16，ADR 0025）。
 *
 * <p>报损 / 退货出库 / 内部领用：**不挂客户**的减库存动作。登记落待过账（不动库存），过账才经
 * {@code StockApi#out} 写流水；作废已过账的单按相反方向冲销。
 */
public interface StockOutService {

    /**
     * 登记出库单（待过账，不动库存）。
     *
     * @return 出库单编号
     */
    Long createStockOut(@Valid StockOutSaveReqVO reqVO);

    /**
     * 过账：经 {@code StockApi} 写库存流水并减余额。重复过账幂等。
     */
    void postStockOut(Long id);

    /**
     * 登记并过账（仓管一次成型）。
     *
     * @return 出库单编号
     */
    Long confirmStockOut(@Valid StockOutSaveReqVO reqVO);

    /**
     * 作废出库单：待过账的直接作废；已过账的先按相反方向冲销库存。
     */
    void cancelStockOut(@Valid StockOutCancelReqVO reqVO);

    /**
     * 出库单详情（含明细）
     */
    StockOutRespVO getStockOut(Long id);

    /**
     * 出库单分页
     */
    PageResult<StockOutRespVO> getStockOutPage(@Valid StockOutPageReqVO reqVO);

}
