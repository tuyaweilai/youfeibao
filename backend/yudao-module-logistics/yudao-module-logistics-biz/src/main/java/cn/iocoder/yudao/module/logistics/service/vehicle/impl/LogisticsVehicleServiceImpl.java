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
    public void occupyByTask(Long vehicleId) {
        LogisticsVehicleDO vehicle = logisticsVehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw exception(VEHICLE_NOT_EXISTS);
        }
        LogisticsVehicleDO update = new LogisticsVehicleDO();
        update.setId(vehicleId);
        update.setStatus(LogisticsVehicleStatusEnum.IN_TRANSIT.getStatus());
        logisticsVehicleMapper.updateById(update);
    }

    @Override
    public void releaseByTask(Long vehicleId) {
        if (vehicleId == null) {
            return;
        }
        LogisticsVehicleDO vehicle = logisticsVehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            return; // 车被删了就不管：任务已经结束，不该因此报错
        }
        if (LogisticsVehicleStatusEnum.MAINTENANCE.getStatus().equals(vehicle.getStatus())) {
            return; // 维修中的车不因为跑完一趟就变回可用
        }
        LogisticsVehicleDO update = new LogisticsVehicleDO();
        update.setId(vehicleId);
        update.setStatus(LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        logisticsVehicleMapper.updateById(update);
    }

    @Override
    public LogisticsVehicleDO getAssignableVehicle(Long vehicleId) {
        LogisticsVehicleDO vehicle = getAssignableVehicleAllowingExpiredDocuments(vehicleId);
        if (isDocumentExpired(vehicle)) {
            throw exception(VEHICLE_DOCUMENT_EXPIRED);
        }
        return vehicle;
    }

    @Override
    public LogisticsVehicleDO getAssignableVehicleAllowingExpiredDocuments(Long vehicleId) {
        LogisticsVehicleDO vehicle = getVehicle(vehicleId);
        if (LogisticsVehicleStatusEnum.MAINTENANCE.getStatus().equals(vehicle.getStatus())) {
            throw exception(TRANSPORT_TASK_VEHICLE_NOT_AVAILABLE);
        }
        return vehicle;
    }

    @Override
    public boolean isDocumentExpired(LogisticsVehicleDO vehicle) {
        java.time.LocalDate today = java.time.LocalDate.now();
        return isBeforeToday(vehicle.getDrivingLicenseExpiryDate(), today)
                || isBeforeToday(vehicle.getInsuranceExpiryDate(), today);
    }

    @Override
    public java.util.List<LogisticsVehicleDO> getVehicleList(LogisticsVehiclePageReqVO exportReqVO) {
        return logisticsVehicleMapper.selectList(exportReqVO);
    }

    /** 到期日早于今天即过期；没填到期日视为「未登记」，不拦（一期允许先建档后补证） */
    private boolean isBeforeToday(java.time.LocalDate expiryDate, java.time.LocalDate today) {
        return expiryDate != null && expiryDate.isBefore(today);
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
