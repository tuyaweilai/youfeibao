package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockBalanceRespDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockRecordRespDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockReportQueryDTO;

/**
 * ERP 库存只读查询 API（#57 T19：经营报表的「库存表」）。
 *
 * <p>{@link StockApi} 只提供写入与「单点取数」；经营报表要按品类 / 仓库 / 库位 / 批次分页读
 * **在库余额**与**入出流水**，并带上仓库 / 库位 / 批次名称与批次入库时间（算库龄），
 * 这些都在 ERP 侧。所以另开一条**只读**端口，避免与写入端口互相干扰。
 *
 * <p>依赖方向恒为「回收业务模块 → erp」，ERP 不知道 icbc 的存在（ADR 0025）。
 *
 * @author 芋道源码
 */
public interface StockReportApi {

    /**
     * 在库余额分页：一行 = 一个「品类 + 仓库 + 库位 + 批次」的库存数。
     *
     * <p>只给数量口径，不带任何金额（ADR 0027：本期库存只做数量台账）。
     *
     * @param query 查询条件（含分页）
     * @return 在库余额分页
     */
    PageResult<StockBalanceRespDTO> getStockBalancePage(StockReportQueryDTO query);

    /**
     * 入出流水分页：一行 = 一条库存变更流水（正数入库、负数出库）。
     *
     * @param query 查询条件（含分页）
     * @return 入出流水分页
     */
    PageResult<StockRecordRespDTO> getStockRecordPage(StockReportQueryDTO query);

}
