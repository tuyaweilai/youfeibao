package cn.iocoder.yudao.module.logistics.dal.mysql.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsTransportCostDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 运输费用（内部成本）Mapper（V8 #75）。
 */
@Mapper
public interface LogisticsTransportCostMapper extends BaseMapperX<LogisticsTransportCostDO> {

    default List<LogisticsTransportCostDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<LogisticsTransportCostDO>()
                .eq(LogisticsTransportCostDO::getTaskId, taskId)
                .orderByDesc(LogisticsTransportCostDO::getId));
    }

    default PageResult<LogisticsTransportCostDO> selectPage(LogisticsTransportCostPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<LogisticsTransportCostDO> selectList(LogisticsTransportCostPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<LogisticsTransportCostDO> buildQuery(LogisticsTransportCostPageReqVO reqVO) {
        return new LambdaQueryWrapperX<LogisticsTransportCostDO>()
                .eqIfPresent(LogisticsTransportCostDO::getTaskId, reqVO.getTaskId())
                .likeIfPresent(LogisticsTransportCostDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(LogisticsTransportCostDO::getCostType, reqVO.getCostType())
                .eqIfPresent(LogisticsTransportCostDO::getBearer, reqVO.getBearer())
                .orderByDesc(LogisticsTransportCostDO::getId);
    }

}
