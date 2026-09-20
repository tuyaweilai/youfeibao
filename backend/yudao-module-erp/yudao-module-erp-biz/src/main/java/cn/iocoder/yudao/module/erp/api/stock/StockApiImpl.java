package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * ERP 库存 API 实现类。
 *
 * <p>同一业务项（业务类型 + 业务编号 + 业务项编号）只写一次流水，重复调用不再加 / 减库存。
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
    public BigDecimal getStockSum(Long goodsConfigId) {
        return stockService.getStockCount(goodsConfigId);
    }

    /**
     * 变更库存：幂等（同一业务项只写一次流水），返回变更后该品类在该仓库的库存数量。
     */
    private BigDecimal changeStock(StockChangeReqDTO reqDTO, BigDecimal count) {
        if (stockRecordService.existsStockRecord(reqDTO.getBizType(), reqDTO.getBizId(), reqDTO.getBizItemId())) {
            return stockService.getStockCount(reqDTO.getGoodsConfigId(), reqDTO.getWarehouseId());
        }
        stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                reqDTO.getGoodsConfigId(), reqDTO.getWarehouseId(), count,
                reqDTO.getBizType(), reqDTO.getBizId(), reqDTO.getBizItemId(), reqDTO.getBizNo()));
        return stockService.getStockCount(reqDTO.getGoodsConfigId(), reqDTO.getWarehouseId());
    }

}
