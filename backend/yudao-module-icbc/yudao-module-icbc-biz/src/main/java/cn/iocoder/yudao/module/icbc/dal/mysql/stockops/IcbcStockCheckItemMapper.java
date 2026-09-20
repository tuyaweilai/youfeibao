package cn.iocoder.yudao.module.icbc.dal.mysql.stockops;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockCheckItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 盘点单明细 Mapper（#54 T16）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockCheckItemMapper extends BaseMapperX<IcbcStockCheckItemDO> {

    default List<IcbcStockCheckItemDO> selectListByCheckId(Long checkId) {
        if (checkId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockCheckItemDO>()
                .eq(IcbcStockCheckItemDO::getCheckId, checkId)
                .orderByAsc(IcbcStockCheckItemDO::getId));
    }

    default List<IcbcStockCheckItemDO> selectListByCheckIds(Collection<Long> checkIds) {
        if (checkIds == null || checkIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockCheckItemDO>()
                .in(IcbcStockCheckItemDO::getCheckId, checkIds)
                .orderByAsc(IcbcStockCheckItemDO::getId));
    }

}
