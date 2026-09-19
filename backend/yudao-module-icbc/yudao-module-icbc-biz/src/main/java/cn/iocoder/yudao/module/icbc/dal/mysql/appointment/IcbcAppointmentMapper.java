package cn.iocoder.yudao.module.icbc.dal.mysql.appointment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;
import cn.iocoder.yudao.module.icbc.enums.AppointmentStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 到站预约 Mapper（#35）。
 *
 * <p>本表是租户表：现场侧（收货员）看到的是本企业场站的预约；自然人侧的跨企业读取集中在
 * Service 里用 {@code TenantUtils.executeIgnore} 显式表达。
 */
@Mapper
public interface IcbcAppointmentMapper extends BaseMapperX<IcbcAppointmentDO> {

    default IcbcAppointmentDO selectByAppointmentNo(String appointmentNo) {
        return selectOne(IcbcAppointmentDO::getAppointmentNo, appointmentNo);
    }

    /**
     * 某个自然人主体在本租户的预约（倒序）。
     */
    default List<IcbcAppointmentDO> selectListByNaturalPersonId(Long naturalPersonId) {
        if (naturalPersonId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcAppointmentDO>()
                .eq(IcbcAppointmentDO::getNaturalPersonId, naturalPersonId)
                .orderByDesc(IcbcAppointmentDO::getId));
    }

    /**
     * 某个自然人主体在本租户「待到站」的预约，按预计到站时间升序（排队口径）。
     */
    default List<IcbcAppointmentDO> selectPendingByNaturalPersonId(Long naturalPersonId) {
        if (naturalPersonId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcAppointmentDO>()
                .eq(IcbcAppointmentDO::getNaturalPersonId, naturalPersonId)
                .eq(IcbcAppointmentDO::getStatus, AppointmentStatusEnum.PENDING.getStatus())
                .orderByAsc(IcbcAppointmentDO::getExpectedArrivalTime)
                .orderByAsc(IcbcAppointmentDO::getId));
    }

    default List<IcbcAppointmentDO> selectListByPayeeId(Long payeeId) {
        if (payeeId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcAppointmentDO>()
                .eq(IcbcAppointmentDO::getPayeeId, payeeId)
                .orderByDesc(IcbcAppointmentDO::getId));
    }

    default PageResult<IcbcAppointmentDO> selectPage(AppointmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcAppointmentDO>()
                .eqIfPresent(IcbcAppointmentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(IcbcAppointmentDO::getStationId, reqVO.getStationId())
                .eqIfPresent(IcbcAppointmentDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(IcbcAppointmentDO::getNaturalPersonId, reqVO.getNaturalPersonId())
                .likeIfPresent(IcbcAppointmentDO::getPlateNo, reqVO.getPlateNo())
                .geIfPresent(IcbcAppointmentDO::getExpectedArrivalTime, reqVO.getArrivalTimeStart())
                .leIfPresent(IcbcAppointmentDO::getExpectedArrivalTime, reqVO.getArrivalTimeEnd())
                .orderByDesc(IcbcAppointmentDO::getId));
    }

}
