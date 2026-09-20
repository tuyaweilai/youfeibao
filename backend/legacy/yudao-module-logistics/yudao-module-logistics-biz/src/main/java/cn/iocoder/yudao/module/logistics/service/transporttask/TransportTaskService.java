package cn.iocoder.yudao.module.logistics.service.transporttask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.TransportTaskDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流运输任务 Service 接口
 *
 * @author 芋道源码
 */
public interface TransportTaskService {

    /**
     * 创建物流运输任务
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTransportTask(@Valid TransportTaskCreateReqVO createReqVO);

    /**
     * 更新物流运输任务
     *
     * @param updateReqVO 更新信息
     */
    void updateTransportTask(@Valid TransportTaskUpdateReqVO updateReqVO);

    /**
     * 删除物流运输任务
     *
     * @param id 编号
     */
    void deleteTransportTask(Long id);

    /**
     * 获得物流运输任务
     *
     * @param id 编号
     * @return 物流运输任务
     */
    TransportTaskDO getTransportTask(Long id);

    /**
     * 获得物流运输任务详情
     *
     * @param id 编号
     * @return 物流运输任务详情
     */
    TransportTaskRespVO getTransportTaskDetail(Long id);

    /**
     * 获得物流运输任务分页
     *
     * @param pageReqVO 分页查询
     * @return 物流运输任务分页
     */
    PageResult<TransportTaskRespVO> getTransportTaskPage(TransportTaskPageReqVO pageReqVO);

    /**
     * 获得物流运输任务列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 物流运输任务列表
     */
    List<TransportTaskDO> getTransportTaskList(TransportTaskPageReqVO exportReqVO);

    /**
     * 根据任务编号获得物流运输任务
     *
     * @param taskNo 任务编号
     * @return 物流运输任务
     */
    TransportTaskDO getTransportTaskByTaskNo(String taskNo);

    /**
     * 根据订单ID获得物流运输任务列表
     *
     * @param orderId 订单ID
     * @return 物流运输任务列表
     */
    List<TransportTaskDO> getTransportTaskListByOrderId(Long orderId);

    /**
     * 根据车辆ID获得物流运输任务列表
     *
     * @param vehicleId 车辆ID
     * @return 物流运输任务列表
     */
    List<TransportTaskDO> getTransportTaskListByVehicleId(Long vehicleId);

    /**
     * 根据司机ID获得物流运输任务列表
     *
     * @param driverId 司机ID
     * @return 物流运输任务列表
     */
    List<TransportTaskDO> getTransportTaskListByDriverId(Long driverId);

    /**
     * 根据任务状态获得物流运输任务列表
     *
     * @param taskStatus 任务状态
     * @return 物流运输任务列表
     */
    List<TransportTaskDO> getTransportTaskListByTaskStatus(Integer taskStatus);

    /**
     * 根据企业ID获得物流运输任务列表
     *
     * @param enterpriseId 企业ID
     * @return 物流运输任务列表
     */
    List<TransportTaskDO> getTransportTaskListByEnterpriseId(Long enterpriseId);

    /**
     * 分配运输任务
     *
     * @param id 任务ID
     * @param vehicleId 车辆ID
     * @param driverId 司机ID
     */
    void assignTransportTask(Long id, Long vehicleId, Long driverId);

    /**
     * 批量分配运输任务
     *
     * @param batchAssignReqVO 批量分配请求
     * @return 分配结果
     */
    BatchAssignResultVO batchAssignTransportTask(BatchAssignReqVO batchAssignReqVO);

    /**
     * 智能推荐任务分配
     *
     * @param taskId 任务ID
     * @return 推荐方案列表
     */
    List<AssignmentRecommendationVO> recommendAssignment(Long taskId);

    /**
     * 重新分配运输任务
     *
     * @param id 任务编号
     * @param vehicleId 车辆编号
     * @param driverId 司机编号
     * @param reason 重新分配原因
     */
    void reassignTransportTask(Long id, Long vehicleId, Long driverId, String reason);

    /**
     * 接受运输任务
     *
     * @param id 任务ID
     */
    void acceptTransportTask(Long id);

    /**
     * 开始运输
     *
     * @param id 任务ID
     */
    void startTransport(Long id);

    /**
     * 确认取货
     *
     * @param id 任务ID
     * @param actualQuantity 实际数量
     */
    void confirmPickup(Long id, BigDecimal actualQuantity);

    /**
     * 确认送达
     *
     * @param id 任务ID
     */
    void confirmDelivery(Long id);

    /**
     * 完成任务
     *
     * @param id 任务ID
     */
    void completeTask(Long id);

    /**
     * 取消任务
     *
     * @param id 任务ID
     * @param reason 取消原因
     */
    void cancelTask(Long id, String reason);

    /**
     * 更新位置信息
     *
     * @param id 任务ID
     * @param location 当前位置
     * @param latitude 纬度
     * @param longitude 经度
     */
    void updateLocation(Long id, String location, BigDecimal latitude, BigDecimal longitude);

    /**
     * 报告异常
     *
     * @param id 任务ID
     * @param abnormalType 异常类型
     * @param abnormalReason 异常原因
     */
    void reportAbnormal(Long id, Integer abnormalType, String abnormalReason);

    /**
     * 解决异常
     *
     * @param id 任务ID
     */
    void resolveAbnormal(Long id);

    /**
     * 校验运输任务是否存在
     *
     * @param id 任务ID
     * @return 运输任务信息
     */
    TransportTaskDO validateTransportTaskExists(Long id);

    /**
     * 批量分配任务
     *
     * @param batchAssignReqVO 批量分配请求
     * @return 批量分配结果
     */
    BatchAssignResultVO batchAssignTasks(@Valid BatchAssignReqVO batchAssignReqVO);

    /**
     * 获取分配推荐
     *
     * @param taskId 任务ID
     * @return 分配推荐列表
     */
    List<AssignmentRecommendationVO> getAssignmentRecommendations(Long taskId);

    /**
     * 重新分配任务
     *
     * @param taskId 任务ID
     * @param newDriverId 新司机ID
     * @param newVehicleId 新车辆ID
     * @param reason 重新分配原因
     */
    void reassignTask(Long taskId, Long newDriverId, Long newVehicleId, String reason);

} 