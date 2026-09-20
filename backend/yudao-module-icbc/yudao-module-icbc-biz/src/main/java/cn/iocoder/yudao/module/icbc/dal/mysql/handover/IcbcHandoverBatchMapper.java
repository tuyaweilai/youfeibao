package cn.iocoder.yudao.module.icbc.dal.mysql.handover;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcHandoverBatchDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcWeighingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 交接批次 Mapper（#50 T12）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcHandoverBatchMapper extends BaseMapperX<IcbcHandoverBatchDO> {

    default IcbcHandoverBatchDO selectByBatchNo(String batchNo) {
        return selectOne(IcbcHandoverBatchDO::getBatchNo, batchNo);
    }

    /**
     * 按物流侧交接登记编号反查批次（V6 #73）：同一个现场交接登记只会建出一个批次（回场复磅幂等）。
     */
    default IcbcHandoverBatchDO selectByLogisticsHandoverId(Long logisticsHandoverId) {
        if (logisticsHandoverId == null) {
            return null;
        }
        return selectOne(IcbcHandoverBatchDO::getLogisticsHandoverId, logisticsHandoverId);
    }

    /**
     * 按一批物流侧交接登记编号取已有批次的登记编号集合（待复磅清单过滤用，一次查批）。
     */
    default List<IcbcHandoverBatchDO> selectListByLogisticsHandoverIds(Collection<Long> logisticsHandoverIds) {
        if (logisticsHandoverIds == null || logisticsHandoverIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcHandoverBatchDO>()
                .in(IcbcHandoverBatchDO::getLogisticsHandoverId, logisticsHandoverIds));
    }

    default List<IcbcHandoverBatchDO> selectListByPayeeId(Long payeeId) {
        return selectList(new LambdaQueryWrapperX<IcbcHandoverBatchDO>()
                .eq(IcbcHandoverBatchDO::getPayeeId, payeeId)
                .orderByDesc(IcbcHandoverBatchDO::getId));
    }

    default PageResult<IcbcHandoverBatchDO> selectPage(HandoverBatchPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcHandoverBatchDO>()
                .eqIfPresent(IcbcHandoverBatchDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(IcbcHandoverBatchDO::getStationId, reqVO.getStationId())
                .eqIfPresent(IcbcHandoverBatchDO::getSourceType, reqVO.getSourceType())
                .likeIfPresent(IcbcHandoverBatchDO::getPlateNo, reqVO.getPlateNo())
                .geIfPresent(IcbcHandoverBatchDO::getOccurTime, reqVO.getOccurTimeStart())
                .leIfPresent(IcbcHandoverBatchDO::getOccurTime, reqVO.getOccurTimeEnd())
                .orderByDesc(IcbcHandoverBatchDO::getId));
    }

    /**
     * 按车牌反查交接批次（#55 T17 关联单据查询用）：车牌可能只登记在批次上，
     * 收购单侧没有；服务层再把批次下的收购单一起取回。
     */
    default List<IcbcHandoverBatchDO> selectListByPlateNo(String plateNo) {
        return selectList(new LambdaQueryWrapperX<IcbcHandoverBatchDO>()
                .eq(IcbcHandoverBatchDO::getPlateNo, plateNo)
                .orderByDesc(IcbcHandoverBatchDO::getId));
    }

}
