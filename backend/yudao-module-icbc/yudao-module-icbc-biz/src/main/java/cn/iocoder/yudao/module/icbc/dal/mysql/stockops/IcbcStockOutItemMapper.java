package cn.iocoder.yudao.module.icbc.dal.mysql.stockops;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockOutItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 非销售出库单明细 Mapper（#54 T16）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockOutItemMapper extends BaseMapperX<IcbcStockOutItemDO> {

    default List<IcbcStockOutItemDO> selectListByStockOutId(Long stockOutId) {
        if (stockOutId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockOutItemDO>()
                .eq(IcbcStockOutItemDO::getStockOutId, stockOutId)
                .orderByAsc(IcbcStockOutItemDO::getId));
    }

    default List<IcbcStockOutItemDO> selectListByStockOutIds(Collection<Long> stockOutIds) {
        if (stockOutIds == null || stockOutIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockOutItemDO>()
                .in(IcbcStockOutItemDO::getStockOutId, stockOutIds)
                .orderByAsc(IcbcStockOutItemDO::getId));
    }

}
