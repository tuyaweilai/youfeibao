package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.module.erp.api.stock.dto.StockAdjustReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockMoveReqDTO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal move(StockMoveReqDTO reqDTO) {
        Long fromLocationId = normalizeZero(reqDTO.getFromLocationId());
        Long fromBatchId = normalizeZero(reqDTO.getFromBatchId());
        Long toLocationId = normalizeZero(reqDTO.getToLocationId());
        Long toBatchId = normalizeZero(reqDTO.getToBatchId());
        // 幂等：目标侧已有调拨入库流水 → 不再搬一次（同一业务项只移动一次）
        if (stockRecordService.existsStockRecord(ErpStockRecordBizTypeEnum.MOVE_IN.getType(),
                reqDTO.getBizId(), reqDTO.getBizItemId())) {
            return stockService.getStockCount(reqDTO.getGoodsConfigId(), reqDTO.getToWarehouseId(),
                    toLocationId, toBatchId);
        }
        // 1. 源减：库存不足时 updateStockCountIncrement 会拦住，整个事务回滚，目标也不会加
        out(buildChangeReq(reqDTO, reqDTO.getFromWarehouseId(), fromLocationId, fromBatchId,
                ErpStockRecordBizTypeEnum.MOVE_OUT.getType()));
        // 2. 目标加
        in(buildChangeReq(reqDTO, reqDTO.getToWarehouseId(), toLocationId, toBatchId,
                ErpStockRecordBizTypeEnum.MOVE_IN.getType()));
        return stockService.getStockCount(reqDTO.getGoodsConfigId(), reqDTO.getToWarehouseId(),
                toLocationId, toBatchId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal adjustTo(StockAdjustReqDTO reqDTO) {
        Long locationId = normalizeZero(reqDTO.getLocationId());
        Long batchId = normalizeZero(reqDTO.getBatchId());
        // 幂等：该业务项已写过盘盈或盘亏流水 → 视为已调整，不再改余额
        if (stockRecordService.existsStockRecord(ErpStockRecordBizTypeEnum.CHECK_MORE_IN.getType(),
                reqDTO.getBizId(), reqDTO.getBizItemId())
                || stockRecordService.existsStockRecord(ErpStockRecordBizTypeEnum.CHECK_LESS_OUT.getType(),
                reqDTO.getBizId(), reqDTO.getBizItemId())) {
            return BigDecimal.ZERO;
        }
        // 1. 加行锁读当前余额（余额行不存在时无可锁，按 0 起算）
        ErpStockDO stock = stockService.getStockForUpdate(
                reqDTO.getGoodsConfigId(), reqDTO.getWarehouseId(), locationId, batchId);
        BigDecimal bookCount = stock != null ? stock.getCount() : BigDecimal.ZERO;
        // 2. 差额 = 实盘 − 账面；账实相符不写流水
        BigDecimal diff = reqDTO.getTargetCount().subtract(bookCount);
        if (diff.signum() == 0) {
            return BigDecimal.ZERO;
        }
        Integer bizType = diff.signum() > 0 ? ErpStockRecordBizTypeEnum.CHECK_MORE_IN.getType()
                : ErpStockRecordBizTypeEnum.CHECK_LESS_OUT.getType();
        stockRecordService.createStockRecord(ErpStockRecordCreateReqBO.builder()
                .goodsConfigId(reqDTO.getGoodsConfigId()).warehouseId(reqDTO.getWarehouseId())
                .locationId(locationId).batchId(batchId)
                .count(diff).bizType(bizType).bizId(reqDTO.getBizId())
                .bizItemId(reqDTO.getBizItemId()).bizNo(reqDTO.getBizNo())
                .build());
        return diff;
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

    /**
     * 按调拨请求构造单侧库存变更请求（源用 MOVE_OUT、目标用 MOVE_IN）。
     */
    private StockChangeReqDTO buildChangeReq(StockMoveReqDTO moveReqDTO, Long warehouseId, Long locationId,
                                             Long batchId, Integer bizType) {
        StockChangeReqDTO reqDTO = new StockChangeReqDTO();
        reqDTO.setGoodsConfigId(moveReqDTO.getGoodsConfigId());
        reqDTO.setWarehouseId(warehouseId);
        reqDTO.setLocationId(locationId);
        reqDTO.setBatchId(batchId);
        reqDTO.setCount(moveReqDTO.getCount());
        reqDTO.setBizType(bizType);
        reqDTO.setBizId(moveReqDTO.getBizId());
        reqDTO.setBizItemId(moveReqDTO.getBizItemId());
        reqDTO.setBizNo(moveReqDTO.getBizNo());
        return reqDTO;
    }

    private static Long normalizeZero(Long id) {
        return id == null ? 0L : id;
    }

}
