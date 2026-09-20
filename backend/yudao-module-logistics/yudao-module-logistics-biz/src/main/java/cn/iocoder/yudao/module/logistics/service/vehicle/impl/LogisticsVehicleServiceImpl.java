package cn.iocoder.yudao.module.logistics.service.vehicle.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehiclePageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.vehicle.LogisticsVehicleMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 车辆档案 Service 实现（V2a #77）。
 */
@Service
@Validated
public class LogisticsVehicleServiceImpl implements LogisticsVehicleService {

    @Resource
    private LogisticsVehicleMapper logisticsVehicleMapper;

    @Override
    public Long createVehicle(LogisticsVehicleSaveReqVO createReqVO) {
        assertPlateNoAvailable(createReqVO.getPlateNo(), null);
        assertStatusNotDrivenByTask(createReqVO.getStatus());
        LogisticsVehicleDO vehicle = BeanUtils.toBean(createReqVO, LogisticsVehicleDO.class);
        logisticsVehicleMapper.insert(vehicle);
        return vehicle.getId();
    }

    @Override
    public void updateVehicle(LogisticsVehicleSaveReqVO updateReqVO) {
        LogisticsVehicleDO exists = getVehicle(updateReqVO.getId());
        assertPlateNoAvailable(updateReqVO.getPlateNo(), exists.getId());
        assertStatusNotDrivenByTask(updateReqVO.getStatus());
        LogisticsVehicleDO update = BeanUtils.toBean(updateReqVO, LogisticsVehicleDO.class);
        logisticsVehicleMapper.updateById(update);
    }

    @Override
    public void deleteVehicle(Long id) {
        getVehicle(id);
        logisticsVehicleMapper.deleteById(id);
    }

    @Override
    public LogisticsVehicleDO getVehicle(Long id) {
        LogisticsVehicleDO vehicle = logisticsVehicleMapper.selectById(id);
        if (vehicle == null) {
            throw exception(VEHICLE_NOT_EXISTS);
        }
        return vehicle;
    }

    @Override
    public PageResult<LogisticsVehicleDO> getVehiclePage(LogisticsVehiclePageReqVO pageReqVO) {
        return logisticsVehicleMapper.selectPage(pageReqVO);
    }

    private void assertPlateNoAvailable(String plateNo, Long id) {
        LogisticsVehicleDO exists = logisticsVehicleMapper.selectByPlateNo(plateNo);
        if (exists == null) {
            return;
        }
        if (!Objects.equals(exists.getId(), id)) {
            throw exception(VEHICLE_PLATE_NO_DUPLICATE);
        }
    }

    /**
     * 「运输中」是运输任务驱动的状态（任务执行时车辆被占用），不接受在档案上手工设置——
     * 否则派车会看到一辆「运输中」但其实没跑的车，或者一辆正在跑却被改成「可用」的车。
     */
    private void assertStatusNotDrivenByTask(Integer status) {
        if (LogisticsVehicleStatusEnum.IN_TRANSIT.getStatus().equals(status)) {
            throw exception(VEHICLE_STATUS_NOT_ALLOW_UPDATE);
        }
    }

}
