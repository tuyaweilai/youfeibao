package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ERP 品类库存 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockMapper extends BaseMapperX<ErpStockDO> {

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockDO>()
                .eqIfPresent(ErpStockDO::getGoodsConfigId, reqVO.getGoodsConfigId())
                .eqIfPresent(ErpStockDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(ErpStockDO::getLocationId, reqVO.getLocationId())
                .eqIfPresent(ErpStockDO::getBatchId, reqVO.getBatchId())
                .orderByDesc(ErpStockDO::getId));
    }

    default ErpStockDO selectByGoodsConfigIdAndWarehouseIdAndLocationIdAndBatchId(
            Long goodsConfigId, Long warehouseId, Long locationId, Long batchId) {
        return selectOne(new LambdaQueryWrapperX<ErpStockDO>()
                .eq(ErpStockDO::getGoodsConfigId, goodsConfigId)
                .eq(ErpStockDO::getWarehouseId, warehouseId)
                .eq(ErpStockDO::getLocationId, locationId)
                .eq(ErpStockDO::getBatchId, batchId));
    }

    default Long selectCountByLocationId(Long locationId) {
        return selectCount(ErpStockDO::getLocationId, locationId);
    }

    default Long selectCountByBatchId(Long batchId) {
        return selectCount(ErpStockDO::getBatchId, batchId);
    }

    default int updateCountIncrement(Long id, BigDecimal count, boolean negativeEnable) {
        LambdaUpdateWrapper<ErpStockDO> updateWrapper = new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id);
        if (count.compareTo(BigDecimal.ZERO) > 0) {
            updateWrapper.setSql("count = count + " + count);
        } else if (count.compareTo(BigDecimal.ZERO) < 0) {
            if (!negativeEnable) {
                updateWrapper.ge(ErpStockDO::getCount, count.abs());
            }
            updateWrapper.setSql("count = count - " + count.abs());
        }
        return update(null, updateWrapper);
    }

    default BigDecimal selectSumByGoodsConfigId(Long goodsConfigId) {
        // SQL sum 查询：同一品类可能散在多个仓库 / 库位 / 批次的余额行里
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("SUM(count) AS sumCount")
                .eq("goods_config_id", goodsConfigId));
        return extractSum(result);
    }

    default BigDecimal selectSumByGoodsConfigIdAndWarehouseId(Long goodsConfigId, Long warehouseId) {
        // SQL sum 查询：同一仓库下可能有多个库位 / 批次的余额行，求和才是仓库级库存
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("SUM(count) AS sumCount")
                .eq("goods_config_id", goodsConfigId)
                .eq("warehouse_id", warehouseId));
        return extractSum(result);
    }

    /**
     * 不按列别名取值：不同数据库（H2 / MySQL）返回的 key 大小写不一致，取唯一一列更稳。
     */
    default BigDecimal extractSum(List<Map<String, Object>> result) {
        if (CollUtil.isEmpty(result) || CollUtil.isEmpty(result.get(0))) {
            return BigDecimal.ZERO;
        }
        Object value = result.get(0).values().iterator().next();
        return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
    }

}