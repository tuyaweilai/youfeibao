package cn.iocoder.yudao.module.icbc.dal.mysql.stockin;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 入库单明细 Mapper（#52 T14）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockInItemMapper extends BaseMapperX<IcbcStockInItemDO> {

    default List<IcbcStockInItemDO> selectListByStockInId(Long stockInId) {
        if (stockInId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockInItemDO>()
                .eq(IcbcStockInItemDO::getStockInId, stockInId)
                .orderByAsc(IcbcStockInItemDO::getId));
    }

    default List<IcbcStockInItemDO> selectListByStockInIds(Collection<Long> stockInIds) {
        if (stockInIds == null || stockInIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockInItemDO>()
                .in(IcbcStockInItemDO::getStockInId, stockInIds)
                .orderByAsc(IcbcStockInItemDO::getId));
    }

}
