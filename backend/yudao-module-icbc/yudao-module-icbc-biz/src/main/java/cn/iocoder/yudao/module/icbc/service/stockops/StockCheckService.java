package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckSaveReqVO;

import javax.validation.Valid;

/**
 * 盘点调整 Service（#54 T16，ADR 0025）。
 *
 * <p>登记时录实盘数；过账时经 {@code StockApi#adjustTo} 由 ERP 在同一个事务里算差额并对齐余额
 * （盘盈写 {@code CHECK_MORE_IN}、盘亏写 {@code CHECK_LESS_OUT}，账实相符不写流水）。
 * 差额由过账时算出并落明细，登记与过账之间发生的出入库不会被错误地抹平。
 */
public interface StockCheckService {

    /**
     * 登记盘点单（待过账，不动库存）。明细此时只记实盘数。
     *
     * @return 盘点单编号
     */
    Long createStockCheck(@Valid StockCheckSaveReqVO reqVO);

    /**
     * 过账：经 {@code StockApi#adjustTo} 对齐余额，并把账面数 / 差额落明细。重复过账幂等。
     */
    void postStockCheck(Long id);

    /**
     * 登记并过账。
     *
     * @return 盘点单编号
     */
    Long confirmStockCheck(@Valid StockCheckSaveReqVO reqVO);

    /**
     * 作废盘点单：待过账的直接作废；已过账的按记录的差额冲销。
     */
    void cancelStockCheck(@Valid StockCheckCancelReqVO reqVO);

    /**
     * 盘点单详情（含账面 / 实盘 / 差额）
     */
    StockCheckRespVO getStockCheck(Long id);

    /**
     * 盘点单分页
     */
    PageResult<StockCheckRespVO> getStockCheckPage(@Valid StockCheckPageReqVO reqVO);

}
