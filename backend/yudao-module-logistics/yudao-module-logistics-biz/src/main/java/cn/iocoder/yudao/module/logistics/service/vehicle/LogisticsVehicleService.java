package cn.iocoder.yudao.module.logistics.service.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehiclePageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;

import javax.validation.Valid;

/**
 * 车辆档案 Service（V2a #77）。
 *
 * <p>车辆是派车的可选对象：车牌在**租户内唯一**，状态里只有「可用 / 维护中」是人工维护的——
 * 「运输中」由运输任务驱动（V2b 起），所以不接受手工设置。
 */
public interface LogisticsVehicleService {

    Long createVehicle(@Valid LogisticsVehicleSaveReqVO createReqVO);

    void updateVehicle(@Valid LogisticsVehicleSaveReqVO updateReqVO);

    void deleteVehicle(Long id);

    /**
     * 获得车辆；不存在时抛业务异常。
     *
     * @param id 车辆编号
     * @return 车辆
     */
    LogisticsVehicleDO getVehicle(Long id);

    PageResult<LogisticsVehicleDO> getVehiclePage(LogisticsVehiclePageReqVO pageReqVO);

}
