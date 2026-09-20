package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.module.erp.api.stock.dto.StockAdjustReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockMoveReqDTO;

import java.math.BigDecimal;

/**
 * ERP 库存 API 接口。
 *
 * <p>回收业务模块（如 {@code icbc}）通过本接口把库存写入 ERP 的库存域，维度是「品类 + 仓库 + 库位 + 批次」：
 * 入库 / 出库各写一条 {@code erp_stock_record} 流水，并增量更新 {@code erp_stock} 余额；
 * 调拨写一出一进两条，盘点把余额对齐到实盘数。
 * 所有写入按「业务类型 + 业务编号 + 业务项编号」幂等，重复提交只形成一次业务事实。
 *
 * <p>写入不直接改余额：每一笔余额变更都有对应的流水，冲销是反向流水而不是改余额（余额与流水始终一致）。
 *
 * <p>依赖方向恒为「回收业务模块 → erp」，ERP 不知道 icbc 的存在。
 *
 * @author 芋道源码
 */
public interface StockApi {

    /**
     * 入库：增加库存。
     *
     * @param reqDTO 库存变更请求（数量为正数）
     * @return 变更后的库存数量
     */
    BigDecimal in(StockChangeReqDTO reqDTO);

    /**
     * 出库：减少库存。
     *
     * @param reqDTO 库存变更请求（数量为正数，内部按负数处理）
     * @return 变更后的库存数量
     */
    BigDecimal out(StockChangeReqDTO reqDTO);

    /**
     * 查询某品类在某仓库的库存数量。不存在时返回 0。
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId   仓库编号
     * @return 库存数量
     */
    BigDecimal getStockCount(Long goodsConfigId, Long warehouseId);

    /**
     * 查询某品类在某仓库某库位某批次的库存数量。不存在时返回 0。
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId   仓库编号
     * @param locationId    库位编号（null / 0 表示未指定）
     * @param batchId       批次编号（null / 0 表示未指定）
     * @return 库存数量
     */
    BigDecimal getStockCount(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId);

    /**
     * 查询某品类在所有仓库的库存数量合计。不存在时返回 0。
     *
     * @param goodsConfigId 品类编号
     * @return 库存数量合计
     */
    BigDecimal getStockSum(Long goodsConfigId);

    /**
     * 跨仓调拨：源维度减少、目标维度增加，一次写两条流水（{@code MOVE_OUT} / {@code MOVE_IN}）。
     *
     * <p>「搬数量」是库存域的操作，调用方只说要搬多少、从哪到哪，不用自己配对两条流水；
     * 同一业务项重复调用不再移动。
     *
     * @param reqDTO 调拨请求（数量为正数）
     * @return 调拨后目标维度的库存数量
     */
    BigDecimal move(StockMoveReqDTO reqDTO);

    /**
     * 盘点调整：把某维度的余额调整到盘点实盘数，盘盈 / 盘亏各写一条流水。
     *
     * <p>差额由本接口在同一个事务里算（调用方不用「读余额再写」，避免并发窗口）；
     * 实盘数与当前余额相等时不写流水。同一业务项重复调用不再调整。
     *
     * @param reqDTO 盘点请求（实盘数不小于 0）
     * @return 实际调整量：正数表示盘盈，负数表示盘亏，0 表示账实相符
     */
    BigDecimal adjustTo(StockAdjustReqDTO reqDTO);

}
