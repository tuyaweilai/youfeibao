package cn.iocoder.yudao.module.icbc.dal.mysql.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMovePageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockMoveDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跨仓调拨单 Mapper（#54 T16）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockMoveMapper extends BaseMapperX<IcbcStockMoveDO> {

    /** 锁住调拨单行：过账 / 作废的并发从第一条语句起串行化。 */
    default IcbcStockMoveDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<IcbcStockMoveDO>()
                .eq(IcbcStockMoveDO::getId, id)
                .last("FOR UPDATE"));
    }

    default PageResult<IcbcStockMoveDO> selectPage(StockMovePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcStockMoveDO>()
                .likeIfPresent(IcbcStockMoveDO::getMoveNo, reqVO.getMoveNo())
                .eqIfPresent(IcbcStockMoveDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcStockMoveDO::getId));
    }

}
