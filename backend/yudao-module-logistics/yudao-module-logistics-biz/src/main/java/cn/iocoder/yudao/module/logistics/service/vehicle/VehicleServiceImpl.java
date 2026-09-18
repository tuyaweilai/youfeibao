package cn.iocoder.yudao.module.logistics.service.vehicle;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.*;
import cn.iocoder.yudao.module.logistics.convert.vehicle.VehicleConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.VehicleDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.vehicle.VehicleMapper;
import cn.iocoder.yudao.module.logistics.enums.VehicleStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 车辆信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    @Resource
    private VehicleMapper vehicleMapper;

    @Override
    public Long createVehicle(VehicleCreateReqVO createReqVO) {
        // 校验车牌号唯一性
        validatePlateNumberUnique(null, createReqVO.getPlateNumber());
        // 校验车辆状态
        validateVehicleStatus(createReqVO.getStatus());

        // 插入
        VehicleDO vehicle = VehicleConvert.INSTANCE.convert(createReqVO);
        vehicleMapper.insert(vehicle);
        // 返回
        return vehicle.getId();
    }

    @Override
    public void updateVehicle(VehicleUpdateReqVO updateReqVO) {
        // 校验存在
        validateVehicleExists(updateReqVO.getId());
        // 校验车牌号唯一性
        validatePlateNumberUnique(updateReqVO.getId(), updateReqVO.getPlateNumber());
        // 校验车辆状态
        validateVehicleStatus(updateReqVO.getStatus());

        // 更新
        VehicleDO updateObj = VehicleConvert.INSTANCE.convert(updateReqVO);
        vehicleMapper.updateById(updateObj);
    }

    @Override
    public void deleteVehicle(Long id) {
        // 校验存在
        VehicleDO vehicle = validateVehicleExists(id);
        // 校验车辆是否在使用中
        if (VehicleStatusEnum.IN_TRANSIT.getStatus().equals(vehicle.getStatus())) {
            throw ServiceExceptionUtil.exception(VEHICLE_IN_USE);
        }

        // 删除
        vehicleMapper.deleteById(id);
    }

    private void validatePlateNumberUnique(Long id, String plateNumber) {
        VehicleDO vehicle = vehicleMapper.selectByPlateNumber(plateNumber);
        if (vehicle == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的车辆
        if (id == null) {
            throw ServiceExceptionUtil.exception(VEHICLE_PLATE_NUMBER_EXISTS);
        }
        if (!vehicle.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(VEHICLE_PLATE_NUMBER_EXISTS);
        }
    }

    private void validateVehicleStatus(Integer status) {
        if (status == null) {
            return;
        }
        boolean valid = Arrays.stream(VehicleStatusEnum.values())
                .anyMatch(statusEnum -> statusEnum.getStatus().equals(status));
        if (!valid) {
            throw ServiceExceptionUtil.exception(VEHICLE_STATUS_INVALID);
        }
    }

    @Override
    public VehicleDO getVehicle(Long id) {
        return vehicleMapper.selectById(id);
    }

    @Override
    public VehicleRespVO getVehicleDetail(Long id) {
        VehicleDO vehicle = validateVehicleExists(id);
        VehicleRespVO respVO = VehicleConvert.INSTANCE.convert(vehicle);
        
        // TODO: 可以在这里添加企业名称等扩展信息
        // 例如：respVO.setEnterpriseName(enterpriseService.getEnterpriseName(vehicle.getEnterpriseId()));
        
        return respVO;
    }

    @Override
    public PageResult<VehicleRespVO> getVehiclePage(VehiclePageReqVO pageReqVO) {
        PageResult<VehicleDO> pageResult = vehicleMapper.selectPage(pageReqVO);
        PageResult<VehicleRespVO> result = VehicleConvert.INSTANCE.convertPage(pageResult);
        
        // TODO: 可以在这里批量设置企业名称等扩展信息
        
        return result;
    }

    @Override
    public List<VehicleDO> getVehicleList(VehiclePageReqVO exportReqVO) {
        return vehicleMapper.selectList(exportReqVO);
    }

    @Override
    public VehicleDO getVehicleByPlateNumber(String plateNumber) {
        return vehicleMapper.selectByPlateNumber(plateNumber);
    }

    @Override
    public List<VehicleDO> getVehicleListByEnterpriseId(Long enterpriseId) {
        return vehicleMapper.selectByEnterpriseId(enterpriseId);
    }

    @Override
    public List<VehicleDO> getVehicleListByStatus(Integer status) {
        return vehicleMapper.selectByStatus(status);
    }

    @Override
    public void updateVehicleStatus(Long id, Integer status) {
        // 校验存在
        validateVehicleExists(id);
        // 校验状态
        validateVehicleStatus(status);

        // 更新状态
        VehicleDO updateObj = new VehicleDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        vehicleMapper.updateById(updateObj);
    }

    @Override
    public VehicleDO validateVehicleExists(Long id) {
        VehicleDO vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw ServiceExceptionUtil.exception(VEHICLE_NOT_EXISTS);
        }
        return vehicle;
    }

} 