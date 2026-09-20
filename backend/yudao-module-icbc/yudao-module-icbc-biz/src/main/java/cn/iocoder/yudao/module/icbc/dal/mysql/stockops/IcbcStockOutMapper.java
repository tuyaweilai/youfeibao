package cn.iocoder.yudao.module.icbc.dal.mysql.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockOutDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 非销售出库单 Mapper（#54 T16）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockOutMapper extends BaseMapperX<IcbcStockOutDO> {

    /** 锁住出库单行：过账 / 作废的并发从第一条语句起串行化。 */
    default IcbcStockOutDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<IcbcStockOutDO>()
                .eq(IcbcStockOutDO::getId, id)
                .last("FOR UPDATE"));
    }

    default PageResult<IcbcStockOutDO> selectPage(StockOutPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcStockOutDO>()
                .likeIfPresent(IcbcStockOutDO::getStockOutNo, reqVO.getStockOutNo())
                .eqIfPresent(IcbcStockOutDO::getOutType, reqVO.getOutType())
                .eqIfPresent(IcbcStockOutDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcStockOutDO::getId));
    }

}
