package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;

import java.math.BigDecimal;

/**
 * ERP 库存 API 接口。
 *
 * <p>回收业务模块（如 {@code icbc}）通过本接口把库存写入 ERP 的库存域，维度是「品类 + 仓库」：
 * 入库 / 出库各写一条 {@code erp_stock_record} 流水，并增量更新 {@code erp_stock} 余额。
 * 所有写入按「业务类型 + 业务编号 + 业务项编号」幂等，重复提交只形成一次业务事实。
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

}
