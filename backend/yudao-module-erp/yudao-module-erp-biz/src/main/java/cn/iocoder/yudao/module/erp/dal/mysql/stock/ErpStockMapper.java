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
                .orderByDesc(ErpStockDO::getId));
    }

    default ErpStockDO selectByGoodsConfigIdAndWarehouseId(Long goodsConfigId, Long warehouseId) {
        return selectOne(ErpStockDO::getGoodsConfigId, goodsConfigId,
                ErpStockDO::getWarehouseId, warehouseId);
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
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("SUM(count) AS sumCount")
                .eq("goods_config_id", goodsConfigId));
        // 获得数量。不按列别名取值：不同数据库（H2 / MySQL）返回的 key 大小写不一致，取唯一一列更稳。
        if (CollUtil.isEmpty(result) || CollUtil.isEmpty(result.get(0))) {
            return BigDecimal.ZERO;
        }
        Object value = result.get(0).values().iterator().next();
        return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
    }

}