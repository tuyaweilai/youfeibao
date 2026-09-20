package cn.iocoder.yudao.module.logistics.dal.mysql.transporttask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运输任务 Mapper。
 */
@Mapper
public interface LogisticsTransportTaskMapper extends BaseMapperX<LogisticsTransportTaskDO> {

    /**
     * 按任务单号查询（租户内唯一；租户由框架自动加条件）。
     */
    default LogisticsTransportTaskDO selectByTaskNo(String taskNo) {
        return selectOne(LogisticsTransportTaskDO::getTaskNo, taskNo);
    }

    default PageResult<LogisticsTransportTaskDO> selectPage(LogisticsTransportTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LogisticsTransportTaskDO>()
                .likeIfPresent(LogisticsTransportTaskDO::getTaskNo, reqVO.getTaskNo())
                .likeIfPresent(LogisticsTransportTaskDO::getPlateNo, reqVO.getPlateNo())
                .eqIfPresent(LogisticsTransportTaskDO::getDriverId, reqVO.getDriverId())
                .eqIfPresent(LogisticsTransportTaskDO::getVehicleId, reqVO.getVehicleId())
                .eqIfPresent(LogisticsTransportTaskDO::getStatus, reqVO.getStatus())
                .likeIfPresent(LogisticsTransportTaskDO::getPickupAddress, reqVO.getPickupAddress())
                .betweenIfPresent(LogisticsTransportTaskDO::getExpectedStartTime,
                        reqVO.getExpectedStartTimeBegin(), reqVO.getExpectedStartTimeEnd())
                .orderByDesc(LogisticsTransportTaskDO::getId));
    }

}
