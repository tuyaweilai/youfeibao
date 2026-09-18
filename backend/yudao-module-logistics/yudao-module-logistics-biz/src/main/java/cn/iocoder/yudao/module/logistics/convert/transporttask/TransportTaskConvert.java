package cn.iocoder.yudao.module.logistics.convert.transporttask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.TransportTaskDO;
import cn.iocoder.yudao.module.logistics.enums.TransportTaskAbnormalTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.TransportTaskStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 物流运输任务 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface TransportTaskConvert {

    TransportTaskConvert INSTANCE = Mappers.getMapper(TransportTaskConvert.class);

    TransportTaskDO convert(TransportTaskCreateReqVO bean);

    TransportTaskDO convert(TransportTaskUpdateReqVO bean);

    @Mapping(target = "taskStatusName", source = "taskStatus", qualifiedByName = "taskStatusToName")
    @Mapping(target = "abnormalTypeName", source = "abnormalType", qualifiedByName = "abnormalTypeToName")
    TransportTaskRespVO convert(TransportTaskDO bean);

    @Mapping(target = "taskStatusName", source = "taskStatus", qualifiedByName = "taskStatusToName")
    @Mapping(target = "abnormalTypeName", source = "abnormalType", qualifiedByName = "abnormalTypeToName")
    TransportTaskExcelVO convertExcel(TransportTaskDO bean);

    List<TransportTaskRespVO> convertList(List<TransportTaskDO> list);

    List<TransportTaskExcelVO> convertExcelList(List<TransportTaskDO> list);

    PageResult<TransportTaskRespVO> convertPage(PageResult<TransportTaskDO> page);

    @Named("taskStatusToName")
    default String taskStatusToName(Integer taskStatus) {
        if (taskStatus == null) {
            return null;
        }
        for (TransportTaskStatusEnum statusEnum : TransportTaskStatusEnum.values()) {
            if (statusEnum.getStatus().equals(taskStatus)) {
                return statusEnum.getName();
            }
        }
        return "未知状态";
    }

    @Named("abnormalTypeToName")
    default String abnormalTypeToName(Integer abnormalType) {
        if (abnormalType == null) {
            return null;
        }
        for (TransportTaskAbnormalTypeEnum typeEnum : TransportTaskAbnormalTypeEnum.values()) {
            if (typeEnum.getType().equals(abnormalType)) {
                return typeEnum.getName();
            }
        }
        return "未知异常类型";
    }

} 