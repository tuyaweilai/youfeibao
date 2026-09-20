package cn.iocoder.yudao.module.logistics.dal.mysql.transporthandover;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporthandover.LogisticsTransportHandoverDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 交接登记 Mapper（V6 #73）。租户表，不做跨租户读取。
 */
@Mapper
public interface LogisticsTransportHandoverMapper extends BaseMapperX<LogisticsTransportHandoverDO> {

    default LogisticsTransportHandoverDO selectByClientRequestId(String clientRequestId) {
        return selectOne(LogisticsTransportHandoverDO::getClientRequestId, clientRequestId);
    }

    default LogisticsTransportHandoverDO selectByStopId(Long stopId) {
        return selectOne(LogisticsTransportHandoverDO::getStopId, stopId);
    }

    default List<LogisticsTransportHandoverDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<LogisticsTransportHandoverDO>()
                .eq(LogisticsTransportHandoverDO::getTaskId, taskId)
                .orderByAsc(LogisticsTransportHandoverDO::getStopId));
    }

    default PageResult<LogisticsTransportHandoverDO> selectPage(LogisticsTransportHandoverPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LogisticsTransportHandoverDO>()
                .eqIfPresent(LogisticsTransportHandoverDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(LogisticsTransportHandoverDO::getStopId, reqVO.getStopId())
                .eqIfPresent(LogisticsTransportHandoverDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(LogisticsTransportHandoverDO::getDocumentStatus, reqVO.getDocumentStatus())
                .likeIfPresent(LogisticsTransportHandoverDO::getPlateNo, reqVO.getPlateNo())
                .geIfPresent(LogisticsTransportHandoverDO::getOccurTime, reqVO.getOccurTimeStart())
                .leIfPresent(LogisticsTransportHandoverDO::getOccurTime, reqVO.getOccurTimeEnd())
                .orderByDesc(LogisticsTransportHandoverDO::getId));
    }

    /**
     * 最近登记的若干条（供 icbc 侧「按现场交接登记回场复磅」的候选队列）。
     *
     * <p>用 {@code selectPage} 而不是全量：这是一份待处理队列，不是查询接口。排序与分页都交给
     * MyBatis-Plus，避免自己拼 limit 的方言差异。
     */
    default List<LogisticsTransportHandoverDO> selectRecentList(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        LogisticsTransportHandoverPageReqVO reqVO = new LogisticsTransportHandoverPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(limit);
        return selectPage(reqVO, new LambdaQueryWrapperX<LogisticsTransportHandoverDO>()
                .orderByDesc(LogisticsTransportHandoverDO::getId)).getList();
    }

    default Long selectCountByStopId(Long stopId) {
        return selectCount(LogisticsTransportHandoverDO::getStopId, stopId);
    }

}
