package cn.iocoder.yudao.module.icbc.dal.mysql.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockOpeningDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 期初记录 Mapper（#54 T16）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockOpeningMapper extends BaseMapperX<IcbcStockOpeningDO> {

    /** 锁住期初行：作废的并发从第一条语句起串行化。 */
    default IcbcStockOpeningDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<IcbcStockOpeningDO>()
                .eq(IcbcStockOpeningDO::getId, id)
                .last("FOR UPDATE"));
    }

    /**
     * 查某维度**生效中**的期初（status != 已作废）。同一维度只允许一条生效期初。
     */
    default IcbcStockOpeningDO selectActiveByDimension(Long goodsConfigId, Long warehouseId,
                                                      Long locationId, Long batchId) {
        return selectOne(new LambdaQueryWrapperX<IcbcStockOpeningDO>()
                .eq(IcbcStockOpeningDO::getGoodsConfigId, goodsConfigId)
                .eq(IcbcStockOpeningDO::getWarehouseId, warehouseId)
                .eq(IcbcStockOpeningDO::getLocationId, locationId)
                .eq(IcbcStockOpeningDO::getBatchId, batchId)
                .ne(IcbcStockOpeningDO::getStatus,
                        cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum.CANCELLED.getStatus()));
    }

    default PageResult<IcbcStockOpeningDO> selectPage(StockOpeningPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcStockOpeningDO>()
                .likeIfPresent(IcbcStockOpeningDO::getOpeningNo, reqVO.getOpeningNo())
                .eqIfPresent(IcbcStockOpeningDO::getGoodsConfigId, reqVO.getGoodsConfigId())
                .eqIfPresent(IcbcStockOpeningDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(IcbcStockOpeningDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcStockOpeningDO::getId));
    }

    /**
     * 生效中的期初条数（status != 已作废）。0 表示还没导过期初，余额不能当「当前库存」读。
     */
    default Long selectActiveCount() {
        return selectCount(new LambdaQueryWrapperX<IcbcStockOpeningDO>()
                .ne(IcbcStockOpeningDO::getStatus,
                        cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum.CANCELLED.getStatus()));
    }

}
