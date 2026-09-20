package cn.iocoder.yudao.module.logistics.dal.mysql.transportnode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 运输节点 Mapper。
 */
@Mapper
public interface LogisticsTransportNodeMapper extends BaseMapperX<LogisticsTransportNodeDO> {

    /**
     * 按客户端请求号查询（幂等预检）。
     */
    default LogisticsTransportNodeDO selectByClientRequestId(String clientRequestId) {
        return selectOne(LogisticsTransportNodeDO::getClientRequestId, clientRequestId);
    }

    /**
     * 按任务编号取节点，**按发生时间正序**（时间线要按事情发生的顺序讲，不是按上报顺序）。
     */
    default List<LogisticsTransportNodeDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<LogisticsTransportNodeDO>()
                .eq(LogisticsTransportNodeDO::getTaskId, taskId)
                .orderByAsc(LogisticsTransportNodeDO::getNodeTime)
                .orderByAsc(LogisticsTransportNodeDO::getId));
    }

    /**
     * 按任务单号取节点，按发生时间正序。
     */
    default List<LogisticsTransportNodeDO> selectListByTaskNo(String taskNo) {
        return selectList(new LambdaQueryWrapperX<LogisticsTransportNodeDO>()
                .eq(LogisticsTransportNodeDO::getTaskNo, taskNo)
                .orderByAsc(LogisticsTransportNodeDO::getNodeTime)
                .orderByAsc(LogisticsTransportNodeDO::getId));
    }

}
