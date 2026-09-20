package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE2;

/**
 * ERP 品类库存 Service 实现类
 *
 * <p>余额行按「品类 + 仓库 + 库位 + 批次」四个维度唯一；库位 / 批次用 0 表示未指定。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockServiceImpl implements ErpStockService {

    /**
     * 允许库存为负数
     *
     * TODO 芋艿：后续做成 db 配置
     */
    private static final Boolean NEGATIVE_STOCK_COUNT_ENABLE = false;

    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private ErpStockMapper stockMapper;

    @Override
    public ErpStockDO getStock(Long id) {
        return stockMapper.selectById(id);
    }

    @Override
    public ErpStockDO getStock(Long goodsConfigId, Long warehouseId) {
        return getStock(goodsConfigId, warehouseId, 0L, 0L);
    }

    @Override
    public ErpStockDO getStock(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId) {
        return stockMapper.selectByGoodsConfigIdAndWarehouseIdAndLocationIdAndBatchId(
                goodsConfigId, warehouseId, normalizeZero(locationId), normalizeZero(batchId));
    }

    @Override
    public BigDecimal getStockCount(Long goodsConfigId) {
        BigDecimal count = stockMapper.selectSumByGoodsConfigId(goodsConfigId);
        return count != null ? count : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getStockCount(Long goodsConfigId, Long warehouseId) {
        // 仓库级库存 = 该仓库下全部库位 / 批次余额行之和
        BigDecimal count = stockMapper.selectSumByGoodsConfigIdAndWarehouseId(goodsConfigId, warehouseId);
        return count != null ? count : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getStockCount(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId) {
        ErpStockDO stock = getStock(goodsConfigId, warehouseId, locationId, batchId);
        return stock != null ? stock.getCount() : BigDecimal.ZERO;
    }

    @Override
    public PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO) {
        return stockMapper.selectPage(pageReqVO);
    }

    @Override
    public BigDecimal updateStockCountIncrement(Long goodsConfigId, Long warehouseId, BigDecimal count) {
        return updateStockCountIncrement(goodsConfigId, warehouseId, 0L, 0L, count);
    }

    @Override
    public BigDecimal updateStockCountIncrement(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId,
                                                BigDecimal count) {
        long location = normalizeZero(locationId);
        long batch = normalizeZero(batchId);
        // 1.1 查询当前库存
        ErpStockDO stock = stockMapper.selectByGoodsConfigIdAndWarehouseIdAndLocationIdAndBatchId(
                goodsConfigId, warehouseId, location, batch);
        if (stock == null) {
            stock = new ErpStockDO().setGoodsConfigId(goodsConfigId).setWarehouseId(warehouseId)
                    .setLocationId(location).setBatchId(batch).setCount(BigDecimal.ZERO);
            try {
                stockMapper.insert(stock);
            } catch (DuplicateKeyException ex) {
                // 并发下另一个请求已经插入：唯一约束兜底，回读既有余额行
                stock = stockMapper.selectByGoodsConfigIdAndWarehouseIdAndLocationIdAndBatchId(
                        goodsConfigId, warehouseId, location, batch);
            }
        }
        // 1.2 校验库存是否充足
        if (!NEGATIVE_STOCK_COUNT_ENABLE && stock.getCount().add(count).compareTo(BigDecimal.ZERO) < 0) {
            throw exception(STOCK_COUNT_NEGATIVE, String.valueOf(goodsConfigId),
                    warehouseService.getWarehouse(warehouseId).getName(), stock.getCount(), count);
        }

        // 2. 库存变更
        int updateCount = stockMapper.updateCountIncrement(stock.getId(), count, NEGATIVE_STOCK_COUNT_ENABLE);
        if (updateCount == 0) {
            // 此时不好去查询最新库存，所以直接抛出该提示，不提供具体库存数字
            throw exception(STOCK_COUNT_NEGATIVE2, String.valueOf(goodsConfigId),
                    warehouseService.getWarehouse(warehouseId).getName());
        }

        // 3. 返回最新库存
        return stock.getCount().add(count);
    }

    /**
     * 库位 / 批次编号用 0 表示未指定；null 归一为 0，保证唯一约束下不会出现多行「未指定」余额。
     */
    private static long normalizeZero(Long id) {
        return id != null ? id : 0L;
    }

}
