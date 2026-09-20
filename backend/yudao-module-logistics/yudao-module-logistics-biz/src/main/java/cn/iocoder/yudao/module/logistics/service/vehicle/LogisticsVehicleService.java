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

    /**
     * 运输任务占用车辆：置为「运输中」。
     *
     * <p><b>这是「运输中」唯一的合法来源</b>：档案 CRUD（{@link #createVehicle} / {@link #updateVehicle}）
     * 一律拒绝手工设置该状态，因为手工置成「运输中」会让派车看到一辆在跑却实际没跑的车。
     *
     * @param vehicleId 车辆编号
     */
    void occupyByTask(Long vehicleId);

    /**
     * 任务结束（完成或取消）释放车辆：放回「可用」。
     *
     * <p>车辆若已被置为「维护中」则保持不动——维修中的车不该因为跑完一趟就变回可用。
     *
     * @param vehicleId 车辆编号，可为 null（还没派车就取消的任务）
     */
    void releaseByTask(Long vehicleId);

    /**
     * 获得可派的车（状态不是维护中）。派车时用。
     *
     * @param vehicleId 车辆编号
     * @return 车辆
     */
    LogisticsVehicleDO getAssignableVehicle(Long vehicleId);

    PageResult<LogisticsVehicleDO> getVehiclePage(LogisticsVehiclePageReqVO pageReqVO);

}
