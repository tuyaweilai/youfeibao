package cn.iocoder.yudao.module.logistics.dal.mysql.transporttask;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskReassignDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 运输任务改派承接记录 Mapper（V4 #71）。
 */
@Mapper
public interface LogisticsTransportTaskReassignMapper extends BaseMapperX<LogisticsTransportTaskReassignDO> {

    /**
     * 按任务取改派记录，按改派时间正序（承接关系要按发生顺序讲）。
     */
    default List<LogisticsTransportTaskReassignDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<LogisticsTransportTaskReassignDO>()
                .eq(LogisticsTransportTaskReassignDO::getTaskId, taskId)
                .orderByAsc(LogisticsTransportTaskReassignDO::getReassignTime)
                .orderByAsc(LogisticsTransportTaskReassignDO::getId));
    }

}
