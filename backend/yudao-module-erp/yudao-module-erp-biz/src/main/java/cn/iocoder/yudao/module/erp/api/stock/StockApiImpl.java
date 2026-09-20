package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_EXCEED_AVAILABLE;

/**
 * ERP 库存 API 实现类。
 *
 * <p>同一业务项（业务类型 + 业务编号 + 业务项编号）只写一次流水，重复调用不再加 / 减库存。
 *
 * <p>同一品类的货允许拆到多个库位：每个库位一条明细（业务项编号不同），
 * 传了 {@link StockChangeReqDTO#getMaxCount()} 时累计入库不得超过可入库量。
 *
 * @author 芋道源码
 */
@Service
public class StockApiImpl implements StockApi {

    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockRecordService stockRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal in(StockChangeReqDTO reqDTO) {
        return changeStock(reqDTO, reqDTO.getCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal out(StockChangeReqDTO reqDTO) {
        return changeStock(reqDTO, reqDTO.getCount().negate());
    }

    @Override
    public BigDecimal getStockCount(Long goodsConfigId, Long warehouseId) {
        return stockService.getStockCount(goodsConfigId, warehouseId);
    }

    @Override
    public BigDecimal getStockCount(Long goodsConfigId, Long warehouseId, Long locationId, Long batchId) {
        return stockService.getStockCount(goodsConfigId, warehouseId, locationId, batchId);
    }

    @Override
    public BigDecimal getStockSum(Long goodsConfigId) {
        return stockService.getStockCount(goodsConfigId);
    }

    /**
     * 变更库存：幂等（同一业务项只写一次流水），返回变更后该品类在该仓库 / 库位 / 批次的库存数量。
     */
    private BigDecimal changeStock(StockChangeReqDTO reqDTO, BigDecimal count) {
        if (stockRecordService.existsStockRecord(reqDTO.getBizType(), reqDTO.getBizId(), reqDTO.getBizItemId())) {
            return stockService.getStockCount(reqDTO.getGoodsConfigId(), reqDTO.getWarehouseId(),
                    reqDTO.getLocationId(), reqDTO.getBatchId());
        }
        // 累计入库不得超过可入库量（只对入库方向生效）
        if (count.signum() > 0 && reqDTO.getMaxCount() != null) {
            BigDecimal alreadyIn = stockRecordService.getStockRecordSum(
                    reqDTO.getBizType(), reqDTO.getBizId(), reqDTO.getGoodsConfigId());
            if (alreadyIn.add(count).compareTo(reqDTO.getMaxCount()) > 0) {
                throw exception(STOCK_IN_EXCEED_AVAILABLE, reqDTO.getMaxCount(), alreadyIn, count);
            }
        }
        stockRecordService.createStockRecord(ErpStockRecordCreateReqBO.builder()
                .goodsConfigId(reqDTO.getGoodsConfigId()).warehouseId(reqDTO.getWarehouseId())
                .locationId(reqDTO.getLocationId()).batchId(reqDTO.getBatchId())
                .count(count).bizType(reqDTO.getBizType()).bizId(reqDTO.getBizId())
                .bizItemId(reqDTO.getBizItemId()).bizNo(reqDTO.getBizNo())
                .build());
        return stockService.getStockCount(reqDTO.getGoodsConfigId(), reqDTO.getWarehouseId(),
                reqDTO.getLocationId(), reqDTO.getBatchId());
    }

}
