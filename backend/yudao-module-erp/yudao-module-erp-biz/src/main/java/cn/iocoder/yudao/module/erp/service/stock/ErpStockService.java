package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;

import java.math.BigDecimal;

/**
 * ERP 品类库存 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockService {

    /**
     * 获得品类库存
     *
     * @param id 编号
     * @return 库存
     */
    ErpStockDO getStock(Long id);

    /**
     * 基于品类 + 仓库，获得「未指定库位 / 批次」那一行库存（即 {@code location_id=0, batch_id=0}）。
     *
     * <p>要看仓库级合计用 {@link #getStockCount(Long, Long)}（跨库位 / 批次求和）。
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId 仓库编号
     * @return 品类库存
     */
    ErpStockDO getStock(Long goodsConfigId, Long warehouseId);

    /**
     * 基于品类 + 仓库 + 库位 + 批次，获得品类库存
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId   仓库编号
     * @param locationId    库位编号（null / 0 表示未指定）
     * @param batchId       批次编号（null / 0 表示未指定）
     * @return 品类库存
     */
    ErpStockDO getStock(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId);

    /**
     * 基于品类 + 仓库 + 库位 + 批次，**加行锁**获得品类库存（盘点调整用）。
     *
     * <p>余额行不存在时返回 null（此时没有行可锁）。盘点在同一维度上并发时，先抢到锁的那个会看到最新余额、
     * 算差额、写流水；后一个再读到的就是已调整后的余额。
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId   仓库编号
     * @param locationId    库位编号（null / 0 表示未指定）
     * @param batchId       批次编号（null / 0 表示未指定）
     * @return 品类库存（加锁读）
     */
    ErpStockDO getStockForUpdate(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId);

    /**
     * 获得品类库存数量
     *
     * 如果不存在库存记录，则返回 0
     *
     * @param goodsConfigId 品类编号
     * @return 品类库存数量
     */
    BigDecimal getStockCount(Long goodsConfigId);

    /**
     * 基于品类 + 仓库，获得库存数量（该仓库下全部库位 / 批次的合计）。不存在时返回 0。
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId   仓库编号
     * @return 库存数量
     */
    BigDecimal getStockCount(Long goodsConfigId, Long warehouseId);

    /**
     * 基于品类 + 仓库 + 库位 + 批次，获得库存数量。不存在时返回 0。
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId   仓库编号
     * @param locationId    库位编号（null / 0 表示未指定）
     * @param batchId       批次编号（null / 0 表示未指定）
     * @return 库存数量
     */
    BigDecimal getStockCount(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId);

    /**
     * 获得品类库存分页
     *
     * @param pageReqVO 分页查询
     * @return 库存分页
     */
    PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO);

    /**
     * 增量更新品类库存数量
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId 仓库编号
     * @param count 增量数量：正数，表示增加；负数，表示减少
     * @return 更新后的库存
     */
    BigDecimal updateStockCountIncrement(Long goodsConfigId, Long warehouseId, BigDecimal count);

    /**
     * 增量更新品类库存数量（按 4 个维度定位余额行）
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId   仓库编号
     * @param locationId    库位编号（null / 0 表示未指定）
     * @param batchId       批次编号（null / 0 表示未指定）
     * @param count         增量数量：正数，表示增加；负数，表示减少
     * @return 更新后的库存
     */
    BigDecimal updateStockCountIncrement(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId,
                                         BigDecimal count);

}