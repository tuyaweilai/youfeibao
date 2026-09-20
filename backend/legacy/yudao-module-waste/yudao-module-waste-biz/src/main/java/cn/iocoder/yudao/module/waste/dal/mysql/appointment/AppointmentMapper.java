package cn.iocoder.yudao.module.waste.dal.mysql.appointment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.AppointmentPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 危废转移预约 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface AppointmentMapper extends BaseMapperX<AppointmentDO> {

    default PageResult<AppointmentDO> selectPage(AppointmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AppointmentDO>()
                .eqIfPresent(AppointmentDO::getAppointmentNo, reqVO.getAppointmentNo())
                .eqIfPresent(AppointmentDO::getProducerEnterpriseId, reqVO.getProducerEnterpriseId())
                .likeIfPresent(AppointmentDO::getProducerEnterpriseName, reqVO.getProducerEnterpriseName())
                .eqIfPresent(AppointmentDO::getRecyclerEnterpriseId, reqVO.getRecyclerEnterpriseId())
                .likeIfPresent(AppointmentDO::getRecyclerEnterpriseName, reqVO.getRecyclerEnterpriseName())
                .eqIfPresent(AppointmentDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(AppointmentDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(AppointmentDO::getWasteCategory, reqVO.getWasteCategory())
                .eqIfPresent(AppointmentDO::getAppointmentStatus, reqVO.getAppointmentStatus())
                .eqIfPresent(AppointmentDO::getAssignmentType, reqVO.getAssignmentType())
                .eqIfPresent(AppointmentDO::getBusinessMode, reqVO.getBusinessMode())
                .eqIfPresent(AppointmentDO::getIsUrgent, reqVO.getIsUrgent())
                .betweenIfPresent(AppointmentDO::getExpectedPickupTime, reqVO.getExpectedPickupTime())
                .betweenIfPresent(AppointmentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AppointmentDO::getId));
    }

    default List<AppointmentDO> selectList(AppointmentPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<AppointmentDO>()
                .eqIfPresent(AppointmentDO::getAppointmentNo, reqVO.getAppointmentNo())
                .eqIfPresent(AppointmentDO::getProducerEnterpriseId, reqVO.getProducerEnterpriseId())
                .likeIfPresent(AppointmentDO::getProducerEnterpriseName, reqVO.getProducerEnterpriseName())
                .eqIfPresent(AppointmentDO::getRecyclerEnterpriseId, reqVO.getRecyclerEnterpriseId())
                .likeIfPresent(AppointmentDO::getRecyclerEnterpriseName, reqVO.getRecyclerEnterpriseName())
                .eqIfPresent(AppointmentDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(AppointmentDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(AppointmentDO::getWasteCategory, reqVO.getWasteCategory())
                .eqIfPresent(AppointmentDO::getAppointmentStatus, reqVO.getAppointmentStatus())
                .eqIfPresent(AppointmentDO::getAssignmentType, reqVO.getAssignmentType())
                .eqIfPresent(AppointmentDO::getBusinessMode, reqVO.getBusinessMode())
                .eqIfPresent(AppointmentDO::getIsUrgent, reqVO.getIsUrgent())
                .betweenIfPresent(AppointmentDO::getExpectedPickupTime, reqVO.getExpectedPickupTime())
                .betweenIfPresent(AppointmentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AppointmentDO::getId));
    }

    default AppointmentDO selectByAppointmentNo(String appointmentNo) {
        return selectOne(AppointmentDO::getAppointmentNo, appointmentNo);
    }

    default List<AppointmentDO> selectListByProducerEnterpriseId(Long producerEnterpriseId) {
        return selectList(AppointmentDO::getProducerEnterpriseId, producerEnterpriseId);
    }

    default List<AppointmentDO> selectListByRecyclerEnterpriseId(Long recyclerEnterpriseId) {
        return selectList(AppointmentDO::getRecyclerEnterpriseId, recyclerEnterpriseId);
    }

    default List<AppointmentDO> selectListByStatus(Integer status) {
        return selectList(AppointmentDO::getAppointmentStatus, status);
    }

    default List<AppointmentDO> selectListByStatusAndRecyclerEnterpriseId(Integer status, Long recyclerEnterpriseId) {
        return selectList(new LambdaQueryWrapperX<AppointmentDO>()
                .eq(AppointmentDO::getAppointmentStatus, status)
                .eq(AppointmentDO::getRecyclerEnterpriseId, recyclerEnterpriseId)
                .orderByDesc(AppointmentDO::getId));
    }

    default List<AppointmentDO> selectListByExpectedPickupTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<AppointmentDO>()
                .between(AppointmentDO::getExpectedPickupTime, startTime, endTime)
                .orderByAsc(AppointmentDO::getExpectedPickupTime));
    }

    default Long selectCountByRecyclerEnterpriseIdAndStatus(Long recyclerEnterpriseId, Integer status) {
        return selectCount(new LambdaQueryWrapperX<AppointmentDO>()
                .eq(AppointmentDO::getRecyclerEnterpriseId, recyclerEnterpriseId)
                .eq(AppointmentDO::getAppointmentStatus, status));
    }

} 