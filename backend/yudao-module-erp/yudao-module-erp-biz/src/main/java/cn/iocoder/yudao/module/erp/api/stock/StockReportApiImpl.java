package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockBalanceRespDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockRecordRespDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockReportQueryDTO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLocationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockBatchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLocationMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ERP 库存只读查询 API 实现类（#57 T19）。
 *
 * <p>只读：直接复用 {@code ErpStockService} / {@code ErpStockRecordService} 的分页，再把
 * 仓库 / 库位 / 批次名称与批次入库时间补上，供经营报表展示「货在哪、多久了」。不写任何库存。
 *
 * @author 芋道源码
 */
@Service
public class StockReportApiImpl implements StockReportApi {

    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpWarehouseMapper warehouseMapper;
    @Resource
    private ErpStockLocationMapper locationMapper;
    @Resource
    private ErpStockBatchMapper batchMapper;

    @Override
    public PageResult<StockBalanceRespDTO> getStockBalancePage(StockReportQueryDTO query) {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setGoodsConfigId(query.getGoodsConfigId());
        reqVO.setWarehouseId(query.getWarehouseId());
        reqVO.setLocationId(query.getLocationId());
        reqVO.setBatchId(query.getBatchId());
        reqVO.setPageNo(query.getPageNo());
        reqVO.setPageSize(query.getPageSize());

        PageResult<ErpStockDO> page = stockService.getStockPage(reqVO);
        if (page.getList().isEmpty()) {
            return PageResult.empty(page.getTotal());
        }
        // 批量取名称，避免逐行查库（N+1）
        Map<Long, ErpWarehouseDO> warehouses = warehouses(stocksField(page.getList(), ErpStockDO::getWarehouseId));
        Map<Long, ErpStockLocationDO> locations = locations(stocksField(page.getList(), ErpStockDO::getLocationId));
        Map<Long, ErpStockBatchDO> batches = batches(stocksField(page.getList(), ErpStockDO::getBatchId));
        List<StockBalanceRespDTO> list = page.getList().stream()
                .map(stock -> toBalance(stock, warehouses, locations, batches))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public PageResult<StockRecordRespDTO> getStockRecordPage(StockReportQueryDTO query) {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setGoodsConfigId(query.getGoodsConfigId());
        reqVO.setWarehouseId(query.getWarehouseId());
        reqVO.setLocationId(query.getLocationId());
        reqVO.setBatchId(query.getBatchId());
        reqVO.setBizType(query.getBizType());
        reqVO.setBizNo(query.getBizNo());
        reqVO.setPageNo(query.getPageNo());
        reqVO.setPageSize(query.getPageSize());

        PageResult<ErpStockRecordDO> page = stockRecordService.getStockRecordPage(reqVO);
        if (page.getList().isEmpty()) {
            return PageResult.empty(page.getTotal());
        }
        Map<Long, ErpWarehouseDO> warehouses =
                warehouses(recordsField(page.getList(), ErpStockRecordDO::getWarehouseId));
        Map<Long, ErpStockLocationDO> locations =
                locations(recordsField(page.getList(), ErpStockRecordDO::getLocationId));
        Map<Long, ErpStockBatchDO> batches = batches(recordsField(page.getList(), ErpStockRecordDO::getBatchId));
        List<StockRecordRespDTO> list = page.getList().stream()
                .map(record -> toRecord(record, warehouses, locations, batches))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    // ==================== 名称批量加载 ====================

    private static List<Long> stocksField(List<ErpStockDO> list, Function<ErpStockDO, Long> getter) {
        return distinctIds(list.stream().map(getter).collect(Collectors.toList()));
    }

    private static List<Long> recordsField(List<ErpStockRecordDO> list, Function<ErpStockRecordDO, Long> getter) {
        return distinctIds(list.stream().map(getter).collect(Collectors.toList()));
    }

    /** 去掉空值与 {@code 0}（0 表示未指定库位 / 批次，查名称没有意义） */
    private static List<Long> distinctIds(Collection<Long> ids) {
        return ids.stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());
    }

    private Map<Long, ErpWarehouseDO> warehouses(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return warehouseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ErpWarehouseDO::getId, w -> w, (a, b) -> a));
    }

    private Map<Long, ErpStockLocationDO> locations(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return locationMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ErpStockLocationDO::getId, l -> l, (a, b) -> a));
    }

    private Map<Long, ErpStockBatchDO> batches(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ErpStockBatchDO::getId, b -> b, (a, b) -> a));
    }

    // ==================== DO → DTO ====================

    private static StockBalanceRespDTO toBalance(ErpStockDO stock, Map<Long, ErpWarehouseDO> warehouses,
                                                 Map<Long, ErpStockLocationDO> locations,
                                                 Map<Long, ErpStockBatchDO> batches) {
        StockBalanceRespDTO dto = new StockBalanceRespDTO();
        dto.setId(stock.getId());
        dto.setGoodsConfigId(stock.getGoodsConfigId());
        dto.setWarehouseId(stock.getWarehouseId());
        ErpWarehouseDO warehouse = warehouses.get(stock.getWarehouseId());
        dto.setWarehouseName(warehouse != null ? warehouse.getName() : null);
        dto.setLocationId(stock.getLocationId());
        ErpStockLocationDO location = locations.get(stock.getLocationId());
        dto.setLocationName(location != null ? location.getName() : null);
        dto.setBatchId(stock.getBatchId());
        ErpStockBatchDO batch = batches.get(stock.getBatchId());
        dto.setBatchNo(batch != null ? batch.getBatchNo() : null);
        dto.setBatchInTime(batch != null ? batch.getInTime() : null);
        dto.setCount(stock.getCount());
        return dto;
    }

    private static StockRecordRespDTO toRecord(ErpStockRecordDO record, Map<Long, ErpWarehouseDO> warehouses,
                                               Map<Long, ErpStockLocationDO> locations,
                                               Map<Long, ErpStockBatchDO> batches) {
        StockRecordRespDTO dto = new StockRecordRespDTO();
        dto.setId(record.getId());
        dto.setGoodsConfigId(record.getGoodsConfigId());
        dto.setWarehouseId(record.getWarehouseId());
        ErpWarehouseDO warehouse = warehouses.get(record.getWarehouseId());
        dto.setWarehouseName(warehouse != null ? warehouse.getName() : null);
        dto.setLocationId(record.getLocationId());
        ErpStockLocationDO location = locations.get(record.getLocationId());
        dto.setLocationName(location != null ? location.getName() : null);
        dto.setBatchId(record.getBatchId());
        ErpStockBatchDO batch = batches.get(record.getBatchId());
        dto.setBatchNo(batch != null ? batch.getBatchNo() : null);
        dto.setCount(record.getCount());
        dto.setTotalCount(record.getTotalCount());
        dto.setBizType(record.getBizType());
        dto.setBizTypeName(bizTypeName(record.getBizType()));
        dto.setBizId(record.getBizId());
        dto.setBizItemId(record.getBizItemId());
        dto.setBizNo(record.getBizNo());
        dto.setCreateTime(record.getCreateTime());
        return dto;
    }

    private static String bizTypeName(Integer bizType) {
        if (bizType == null) {
            return null;
        }
        for (ErpStockRecordBizTypeEnum type : ErpStockRecordBizTypeEnum.values()) {
            if (Objects.equals(type.getType(), bizType)) {
                return type.getName();
            }
        }
        return String.valueOf(bizType);
    }

}
