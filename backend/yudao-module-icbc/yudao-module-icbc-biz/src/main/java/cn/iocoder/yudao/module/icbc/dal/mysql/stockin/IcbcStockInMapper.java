package cn.iocoder.yudao.module.icbc.dal.mysql.stockin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 入库单 Mapper（#52 T14）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcStockInMapper extends BaseMapperX<IcbcStockInDO> {

    default IcbcStockInDO selectByStockInNo(String stockInNo) {
        return selectOne(IcbcStockInDO::getStockInNo, stockInNo);
    }

    /**
     * 锁住入库单行（{@code SELECT ... FOR UPDATE}）。
     *
     * <p>必须作为过账 / 作废事务的**第一条语句**：InnoDB 的可重复读快照建立在事务里
     * 第一条普通读上，先做普通读再做加锁读，后续的累计入库校验会用旧快照。
     */
    default IcbcStockInDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<IcbcStockInDO>()
                .eq(IcbcStockInDO::getId, id)
                .last("FOR UPDATE"));
    }

    default PageResult<IcbcStockInDO> selectPage(StockInPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcStockInDO>()
                .likeIfPresent(IcbcStockInDO::getStockInNo, reqVO.getStockInNo())
                .likeIfPresent(IcbcStockInDO::getAcquisitionNo, reqVO.getAcquisitionNo())
                .likeIfPresent(IcbcStockInDO::getSellerName, reqVO.getSellerName())
                .eqIfPresent(IcbcStockInDO::getGoodsConfigId, reqVO.getGoodsConfigId())
                .eqIfPresent(IcbcStockInDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcStockInDO::getId));
    }

    /**
     * 某收购单的全部入库单（含待过账与已作废），按 id 升序。
     *
     * <p>服务层据此算「累计入库 = 已过账合计 − 已作废（已冲销）合计」。
     */
    default List<IcbcStockInDO> selectListByAcquisitionId(Long acquisitionId) {
        if (acquisitionId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockInDO>()
                .eq(IcbcStockInDO::getAcquisitionId, acquisitionId)
                .orderByAsc(IcbcStockInDO::getId));
    }

    /**
     * 批量取多张收购单的入库单（待入库列表一次性算累计入库，避免逐行查询）。
     */
    default List<IcbcStockInDO> selectListByAcquisitionIds(Collection<Long> acquisitionIds) {
        if (acquisitionIds == null || acquisitionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcStockInDO>()
                .in(IcbcStockInDO::getAcquisitionId, acquisitionIds)
                .orderByAsc(IcbcStockInDO::getId));
    }

}
