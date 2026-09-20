package cn.iocoder.yudao.module.logistics.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehiclePageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆档案 Mapper。
 */
@Mapper
public interface LogisticsVehicleMapper extends BaseMapperX<LogisticsVehicleDO> {

    /**
     * 按车牌查询（租户内唯一；租户由框架自动加条件）。
     */
    default LogisticsVehicleDO selectByPlateNo(String plateNo) {
        return selectOne(LogisticsVehicleDO::getPlateNo, plateNo);
    }

    default PageResult<LogisticsVehicleDO> selectPage(LogisticsVehiclePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LogisticsVehicleDO>()
                .likeIfPresent(LogisticsVehicleDO::getPlateNo, reqVO.getPlateNo())
                .eqIfPresent(LogisticsVehicleDO::getVehicleType, reqVO.getVehicleType())
                .eqIfPresent(LogisticsVehicleDO::getStatus, reqVO.getStatus())
                .orderByDesc(LogisticsVehicleDO::getId));
    }

}
