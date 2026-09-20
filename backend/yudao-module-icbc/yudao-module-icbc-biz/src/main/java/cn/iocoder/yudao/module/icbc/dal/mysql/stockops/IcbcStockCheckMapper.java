package cn.iocoder.yudao.module.icbc.dal.mysql.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockCheckDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盘点单 Mapper（#54 T16）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockCheckMapper extends BaseMapperX<IcbcStockCheckDO> {

    /** 锁住盘点单行：过账 / 作废的并发从第一条语句起串行化。 */
    default IcbcStockCheckDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<IcbcStockCheckDO>()
                .eq(IcbcStockCheckDO::getId, id)
                .last("FOR UPDATE"));
    }

    default PageResult<IcbcStockCheckDO> selectPage(StockCheckPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcStockCheckDO>()
                .likeIfPresent(IcbcStockCheckDO::getCheckNo, reqVO.getCheckNo())
                .eqIfPresent(IcbcStockCheckDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcStockCheckDO::getId));
    }

}
