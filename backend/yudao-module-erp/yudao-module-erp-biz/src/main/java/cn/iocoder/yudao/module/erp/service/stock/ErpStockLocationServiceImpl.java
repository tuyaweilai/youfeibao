package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLocationDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLocationMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 库位 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockLocationServiceImpl implements ErpStockLocationService {

    @Resource
    private ErpStockLocationMapper locationMapper;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockMapper stockMapper;

    @Override
    public Long createStockLocation(ErpStockLocationSaveReqVO createReqVO) {
        // 1.1 校验仓库存在
        validateWarehouseExists(createReqVO.getWarehouseId());
        // 1.2 校验同仓库下名称不重复
        validateNameUnique(null, createReqVO.getWarehouseId(), createReqVO.getName());
        // 2. 插入
        ErpStockLocationDO location = BeanUtils.toBean(createReqVO, ErpStockLocationDO.class);
        locationMapper.insert(location);
        return location.getId();
    }

    @Override
    public void updateStockLocation(ErpStockLocationSaveReqVO updateReqVO) {
        // 1.1 校验存在
        validateStockLocationExists(updateReqVO.getId());
        // 1.2 校验仓库存在
        validateWarehouseExists(updateReqVO.getWarehouseId());
        // 1.3 校验同仓库下名称不重复
        validateNameUnique(updateReqVO.getId(), updateReqVO.getWarehouseId(), updateReqVO.getName());
        // 2. 更新
        locationMapper.updateById(BeanUtils.toBean(updateReqVO, ErpStockLocationDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockLocation(Long id) {
        // 1.1 校验存在
        ErpStockLocationDO location = validateStockLocationExists(id);
        // 1.2 还有库存余额的库位不允许删除，避免库存变成找不到的孤儿
        if (stockMapper.selectCountByLocationId(id) > 0) {
            throw exception(STOCK_LOCATION_HAS_STOCK, location.getName());
        }
        // 2. 删除
        locationMapper.deleteById(id);
    }

    private void validateWarehouseExists(Long warehouseId) {
        if (warehouseService.getWarehouse(warehouseId) == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
    }

    private void validateNameUnique(Long id, Long warehouseId, String name) {
        ErpStockLocationDO location = locationMapper.selectByWarehouseIdAndName(warehouseId, name);
        if (location == null) {
            return;
        }
        if (id == null || !location.getId().equals(id)) {
            throw exception(STOCK_LOCATION_NAME_DUPLICATE, name);
        }
    }

    private ErpStockLocationDO validateStockLocationExists(Long id) {
        ErpStockLocationDO location = locationMapper.selectById(id);
        if (location == null) {
            throw exception(STOCK_LOCATION_NOT_EXISTS);
        }
        return location;
    }

    @Override
    public ErpStockLocationDO getStockLocation(Long id) {
        return locationMapper.selectById(id);
    }

    @Override
    public List<ErpStockLocationDO> getStockLocationList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return locationMapper.selectBatchIds(ids);
    }

    @Override
    public List<ErpStockLocationDO> getStockLocationListByWarehouseId(Long warehouseId) {
        return locationMapper.selectListByWarehouseId(warehouseId);
    }

    @Override
    public List<ErpStockLocationDO> getStockLocationListByStatus(Integer status) {
        return locationMapper.selectListByStatus(status);
    }

    @Override
    public PageResult<ErpStockLocationDO> getStockLocationPage(ErpStockLocationPageReqVO pageReqVO) {
        return locationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpStockLocationDO> validStockLocationList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpStockLocationDO> list = locationMapper.selectBatchIds(ids);
        Map<Long, ErpStockLocationDO> locationMap = convertMap(list, ErpStockLocationDO::getId);
        for (Long id : ids) {
            ErpStockLocationDO location = locationMap.get(id);
            if (location == null) {
                throw exception(STOCK_LOCATION_NOT_EXISTS);
            }
            if (CommonStatusEnum.isDisable(location.getStatus())) {
                throw exception(STOCK_LOCATION_NOT_ENABLE, location.getName());
            }
        }
        return list;
    }

}
