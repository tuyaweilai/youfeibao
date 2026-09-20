package cn.iocoder.yudao.module.logistics.dal.mysql.transportnode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.TransportNodePageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.TransportNodeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 物流运输节点记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface TransportNodeMapper extends BaseMapperX<TransportNodeDO> {

    default PageResult<TransportNodeDO> selectPage(TransportNodePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TransportNodeDO>()
                .eqIfPresent(TransportNodeDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(TransportNodeDO::getNodeType, reqVO.getNodeType())
                .eqIfPresent(TransportNodeDO::getOperatorId, reqVO.getOperatorId())
                .likeIfPresent(TransportNodeDO::getOperatorName, reqVO.getOperatorName())
                .likeIfPresent(TransportNodeDO::getNodeLocation, reqVO.getNodeLocation())
                .betweenIfPresent(TransportNodeDO::getNodeTime, reqVO.getBeginNodeTime(), reqVO.getEndNodeTime())
                .betweenIfPresent(TransportNodeDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(TransportNodeDO::getNodeTime));
    }

    default List<TransportNodeDO> selectList(TransportNodePageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<TransportNodeDO>()
                .eqIfPresent(TransportNodeDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(TransportNodeDO::getNodeType, reqVO.getNodeType())
                .eqIfPresent(TransportNodeDO::getOperatorId, reqVO.getOperatorId())
                .likeIfPresent(TransportNodeDO::getOperatorName, reqVO.getOperatorName())
                .likeIfPresent(TransportNodeDO::getNodeLocation, reqVO.getNodeLocation())
                .betweenIfPresent(TransportNodeDO::getNodeTime, reqVO.getBeginNodeTime(), reqVO.getEndNodeTime())
                .betweenIfPresent(TransportNodeDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(TransportNodeDO::getNodeTime));
    }

    default List<TransportNodeDO> selectByTaskId(Long taskId) {
        return selectList(TransportNodeDO::getTaskId, taskId);
    }

    default List<TransportNodeDO> selectByTaskIdOrderByNodeTime(Long taskId) {
        return selectList(new LambdaQueryWrapperX<TransportNodeDO>()
                .eq(TransportNodeDO::getTaskId, taskId)
                .orderByAsc(TransportNodeDO::getNodeTime));
    }

    default List<TransportNodeDO> selectByNodeType(Integer nodeType) {
        return selectList(TransportNodeDO::getNodeType, nodeType);
    }

    default List<TransportNodeDO> selectByOperatorId(Long operatorId) {
        return selectList(TransportNodeDO::getOperatorId, operatorId);
    }

    default TransportNodeDO selectLatestByTaskId(Long taskId) {
        return selectOne(new LambdaQueryWrapperX<TransportNodeDO>()
                .eq(TransportNodeDO::getTaskId, taskId)
                .orderByDesc(TransportNodeDO::getNodeTime)
                .last("LIMIT 1"));
    }

    default Long selectCountByTaskId(Long taskId) {
        return selectCount(TransportNodeDO::getTaskId, taskId);
    }

} 