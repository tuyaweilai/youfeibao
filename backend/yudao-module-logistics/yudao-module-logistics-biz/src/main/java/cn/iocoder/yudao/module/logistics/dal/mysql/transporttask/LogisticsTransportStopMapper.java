package cn.iocoder.yudao.module.logistics.dal.mysql.transporttask;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportStopDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 运输停靠点 Mapper（V5 #72）。
 */
@Mapper
public interface LogisticsTransportStopMapper extends BaseMapperX<LogisticsTransportStopDO> {

    /**
     * 按任务取停靠点，按停靠顺序正序。
     */
    default List<LogisticsTransportStopDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<LogisticsTransportStopDO>()
                .eq(LogisticsTransportStopDO::getTaskId, taskId)
                .orderByAsc(LogisticsTransportStopDO::getStopNo)
                .orderByAsc(LogisticsTransportStopDO::getId));
    }

    /**
     * 按任务编号批量取停靠点（列表页算「还剩几家没提」用，避免 N+1）。
     */
    default List<LogisticsTransportStopDO> selectListByTaskIds(Collection<Long> taskIds) {
        return selectList(new LambdaQueryWrapperX<LogisticsTransportStopDO>()
                .in(LogisticsTransportStopDO::getTaskId, taskIds)
                .orderByAsc(LogisticsTransportStopDO::getStopNo));
    }

}
