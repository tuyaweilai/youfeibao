package cn.iocoder.yudao.module.waste.convert.appointment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.*;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.AppointmentCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.AppointmentRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.AppointmentUpdateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.AppointmentExcelVO;
import cn.iocoder.yudao.module.waste.controller.app.appointment.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.enums.AppointmentStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 危废转移预约 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface AppointmentConvert {

    AppointmentConvert INSTANCE = Mappers.getMapper(AppointmentConvert.class);

    AppointmentDO convert(AppointmentCreateReqVO bean);

    AppointmentDO convert(AppointmentUpdateReqVO bean);

    AppointmentRespVO convert(AppointmentDO bean);

    List<AppointmentRespVO> convertList(List<AppointmentDO> list);

    PageResult<AppointmentRespVO> convertPage(PageResult<AppointmentDO> page);

    List<AppointmentExcelVO> convertExcelList(List<AppointmentDO> list);

    // ========== APP端转换方法 ==========

    AppointmentDO convert(AppAppointmentCreateReqVO bean);

    @Mapping(target = "appointmentStatusName", expression = "java(getAppointmentStatusName(bean.getAppointmentStatus()))")
    AppAppointmentRespVO convertApp(AppointmentDO bean);

    List<AppAppointmentRespVO> convertAppList(List<AppointmentDO> list);

    PageResult<AppAppointmentRespVO> convertAppPage(PageResult<AppointmentDO> page);

    /**
     * 获取预约状态名称
     */
    default String getAppointmentStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(status);
        return statusEnum != null ? statusEnum.getName() : null;
    }

} 