package cn.iocoder.yudao.module.logistics.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.VehiclePageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.VehicleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 车辆信息 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface VehicleMapper extends BaseMapperX<VehicleDO> {

    default PageResult<VehicleDO> selectPage(VehiclePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<VehicleDO>()
                .eqIfPresent(VehicleDO::getEnterpriseId, reqVO.getEnterpriseId())
                .likeIfPresent(VehicleDO::getPlateNumber, reqVO.getPlateNumber())
                .likeIfPresent(VehicleDO::getVehicleType, reqVO.getVehicleType())
                .eqIfPresent(VehicleDO::getStatus, reqVO.getStatus())
                .likeIfPresent(VehicleDO::getGpsDeviceId, reqVO.getGpsDeviceId())
                .betweenIfPresent(VehicleDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(VehicleDO::getId));
    }

    default List<VehicleDO> selectList(VehiclePageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<VehicleDO>()
                .eqIfPresent(VehicleDO::getEnterpriseId, reqVO.getEnterpriseId())
                .likeIfPresent(VehicleDO::getPlateNumber, reqVO.getPlateNumber())
                .likeIfPresent(VehicleDO::getVehicleType, reqVO.getVehicleType())
                .eqIfPresent(VehicleDO::getStatus, reqVO.getStatus())
                .likeIfPresent(VehicleDO::getGpsDeviceId, reqVO.getGpsDeviceId())
                .betweenIfPresent(VehicleDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(VehicleDO::getId));
    }

    default VehicleDO selectByPlateNumber(String plateNumber) {
        return selectOne(VehicleDO::getPlateNumber, plateNumber);
    }

    default List<VehicleDO> selectByEnterpriseId(Long enterpriseId) {
        return selectList(VehicleDO::getEnterpriseId, enterpriseId);
    }

    default List<VehicleDO> selectByStatus(Integer status) {
        return selectList(VehicleDO::getStatus, status);
    }

    default Long selectCountByEnterpriseId(Long enterpriseId) {
        return selectCount(VehicleDO::getEnterpriseId, enterpriseId);
    }

} 