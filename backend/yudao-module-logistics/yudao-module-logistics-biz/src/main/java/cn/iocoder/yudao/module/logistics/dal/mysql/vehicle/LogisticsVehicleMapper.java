package cn.iocoder.yudao.module.logistics.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehiclePageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 车辆档案 Mapper。
 *
 * <p>分页与导出**共用同一个条件构造函数**：各写一套的话，「导出和列表看到的不是一回事」
 * 迟早会发生，而这种错在导出文件里几乎不会被发现。
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
        return selectPage(reqVO, buildQuery(reqVO));
    }

    /**
     * 导出用：同条件的全量列表（不分页）。
     */
    default List<LogisticsVehicleDO> selectList(LogisticsVehiclePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<LogisticsVehicleDO> buildQuery(LogisticsVehiclePageReqVO reqVO) {
        return new LambdaQueryWrapperX<LogisticsVehicleDO>()
                .likeIfPresent(LogisticsVehicleDO::getPlateNo, reqVO.getPlateNo())
                .eqIfPresent(LogisticsVehicleDO::getVehicleType, reqVO.getVehicleType())
                .eqIfPresent(LogisticsVehicleDO::getStatus, reqVO.getStatus())
                .orderByDesc(LogisticsVehicleDO::getId);
    }

}
