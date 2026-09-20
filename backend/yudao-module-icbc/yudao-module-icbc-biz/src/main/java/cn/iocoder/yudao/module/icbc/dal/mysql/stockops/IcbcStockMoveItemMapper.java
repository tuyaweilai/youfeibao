package cn.iocoder.yudao.module.icbc.dal.mysql.stockops;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockMoveItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 跨仓调拨单明细 Mapper（#54 T16）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockMoveItemMapper extends BaseMapperX<IcbcStockMoveItemDO> {

    default List<IcbcStockMoveItemDO> selectListByMoveId(Long moveId) {
        if (moveId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockMoveItemDO>()
                .eq(IcbcStockMoveItemDO::getMoveId, moveId)
                .orderByAsc(IcbcStockMoveItemDO::getId));
    }

    default List<IcbcStockMoveItemDO> selectListByMoveIds(Collection<Long> moveIds) {
        if (moveIds == null || moveIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockMoveItemDO>()
                .in(IcbcStockMoveItemDO::getMoveId, moveIds)
                .orderByAsc(IcbcStockMoveItemDO::getId));
    }

}
