package cn.iocoder.yudao.module.logistics.service.transporttask;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.*;
import cn.iocoder.yudao.module.logistics.convert.transporttask.TransportTaskConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.TransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.TransportTaskMapper;
import cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.logistics.enums.TransportTaskStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 物流运输任务 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TransportTaskServiceImpl implements TransportTaskService {

    @Resource
    private TransportTaskMapper transportTaskMapper;

    @Override
    public Long createTransportTask(TransportTaskCreateReqVO createReqVO) {
        // 校验任务编号唯一性
        validateTaskNoUnique(null, createReqVO.getTaskNo());
        
        // 插入
        TransportTaskDO transportTask = TransportTaskConvert.INSTANCE.convert(createReqVO);
        transportTaskMapper.insert(transportTask);
        
        // 返回
        return transportTask.getId();
    }

    @Override
    public void updateTransportTask(TransportTaskUpdateReqVO updateReqVO) {
        // 校验存在
        validateTransportTaskExists(updateReqVO.getId());
        // 校验任务编号唯一性
        validateTaskNoUnique(updateReqVO.getId(), updateReqVO.getTaskNo());
        
        // 更新
        TransportTaskDO updateObj = TransportTaskConvert.INSTANCE.convert(updateReqVO);
        transportTaskMapper.updateById(updateObj);
    }

    @Override
    public void deleteTransportTask(Long id) {
        // 校验存在
        validateTransportTaskExists(id);
        // 删除
        transportTaskMapper.deleteById(id);
    }

    private void validateTaskNoUnique(Long id, String taskNo) {
        TransportTaskDO transportTask = transportTaskMapper.selectByTaskNo(taskNo);
        if (transportTask == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的任务
        if (id == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_NO_DUPLICATE);
        }
        if (!transportTask.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_NO_DUPLICATE);
        }
    }

    @Override
    public TransportTaskDO getTransportTask(Long id) {
        return transportTaskMapper.selectById(id);
    }

    @Override
    public TransportTaskRespVO getTransportTaskDetail(Long id) {
        TransportTaskDO transportTask = getTransportTask(id);
        return TransportTaskConvert.INSTANCE.convert(transportTask);
    }

    @Override
    public PageResult<TransportTaskRespVO> getTransportTaskPage(TransportTaskPageReqVO pageReqVO) {
        PageResult<TransportTaskDO> pageResult = transportTaskMapper.selectPage(pageReqVO);
        return TransportTaskConvert.INSTANCE.convertPage(pageResult);
    }

    @Override
    public List<TransportTaskDO> getTransportTaskList(TransportTaskPageReqVO exportReqVO) {
        return transportTaskMapper.selectList(exportReqVO);
    }

    @Override
    public TransportTaskDO getTransportTaskByTaskNo(String taskNo) {
        return transportTaskMapper.selectByTaskNo(taskNo);
    }

    @Override
    public List<TransportTaskDO> getTransportTaskListByOrderId(Long orderId) {
        return transportTaskMapper.selectByOrderId(orderId);
    }

    @Override
    public List<TransportTaskDO> getTransportTaskListByVehicleId(Long vehicleId) {
        return transportTaskMapper.selectByVehicleId(vehicleId);
    }

    @Override
    public List<TransportTaskDO> getTransportTaskListByDriverId(Long driverId) {
        return transportTaskMapper.selectByDriverId(driverId);
    }

    @Override
    public List<TransportTaskDO> getTransportTaskListByTaskStatus(Integer taskStatus) {
        return transportTaskMapper.selectByTaskStatus(taskStatus);
    }

    @Override
    public List<TransportTaskDO> getTransportTaskListByEnterpriseId(Long enterpriseId) {
        return transportTaskMapper.selectByEnterpriseId(enterpriseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTransportTask(Long id, Long vehicleId, Long driverId) {
        // 校验任务存在
        TransportTaskDO transportTask = validateTransportTaskExists(id);
        
        // 校验任务状态
        if (!TransportTaskStatusEnum.PENDING.getStatus().equals(transportTask.getTaskStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_STATUS_NOT_PENDING);
        }
        
        // 更新任务状态和分配信息
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setVehicleId(vehicleId);
        updateObj.setDriverId(driverId);
        updateObj.setTaskStatus(TransportTaskStatusEnum.ASSIGNED.getStatus());
        updateObj.setAssignTime(LocalDateTime.now());
        transportTaskMapper.updateById(updateObj);
        
        log.info("[assignTransportTask][任务({})分配给车辆({})和司机({})]", id, vehicleId, driverId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTransportTask(Long id) {
        // 校验任务存在
        TransportTaskDO transportTask = validateTransportTaskExists(id);
        
        // 校验任务状态
        if (!TransportTaskStatusEnum.ASSIGNED.getStatus().equals(transportTask.getTaskStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_STATUS_NOT_ASSIGNED);
        }
        
        // 更新任务状态
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setTaskStatus(TransportTaskStatusEnum.ACCEPTED.getStatus());
        updateObj.setAcceptTime(LocalDateTime.now());
        transportTaskMapper.updateById(updateObj);
        
        log.info("[acceptTransportTask][任务({})已被接受]", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTransport(Long id) {
        // 校验任务存在
        TransportTaskDO transportTask = validateTransportTaskExists(id);
        
        // 校验任务状态
        if (!TransportTaskStatusEnum.ACCEPTED.getStatus().equals(transportTask.getTaskStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_STATUS_NOT_ACCEPTED);
        }
        
        // 更新任务状态
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setTaskStatus(TransportTaskStatusEnum.IN_TRANSIT.getStatus());
        transportTaskMapper.updateById(updateObj);
        
        log.info("[startTransport][任务({})开始运输]", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPickup(Long id, BigDecimal actualQuantity) {
        // 校验任务存在
        TransportTaskDO transportTask = validateTransportTaskExists(id);
        
        // 校验任务状态
        if (!TransportTaskStatusEnum.IN_TRANSIT.getStatus().equals(transportTask.getTaskStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_STATUS_NOT_IN_TRANSIT);
        }
        
        // 更新任务状态和实际数量
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setTaskStatus(TransportTaskStatusEnum.PICKED_UP.getStatus());
        updateObj.setActualPickupTime(LocalDateTime.now());
        updateObj.setActualQuantity(actualQuantity);
        transportTaskMapper.updateById(updateObj);
        
        log.info("[confirmPickup][任务({})确认取货，实际数量：{}]", id, actualQuantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDelivery(Long id) {
        // 校验任务存在
        TransportTaskDO transportTask = validateTransportTaskExists(id);
        
        // 校验任务状态
        if (!TransportTaskStatusEnum.PICKED_UP.getStatus().equals(transportTask.getTaskStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_STATUS_NOT_PICKED_UP);
        }
        
        // 更新任务状态
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setTaskStatus(TransportTaskStatusEnum.DELIVERED.getStatus());
        updateObj.setActualDeliveryTime(LocalDateTime.now());
        transportTaskMapper.updateById(updateObj);
        
        log.info("[confirmDelivery][任务({})确认送达]", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long id) {
        // 校验任务存在
        TransportTaskDO transportTask = validateTransportTaskExists(id);
        
        // 校验任务状态
        if (!TransportTaskStatusEnum.DELIVERED.getStatus().equals(transportTask.getTaskStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_STATUS_NOT_DELIVERED);
        }
        
        // 更新任务状态
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setTaskStatus(TransportTaskStatusEnum.COMPLETED.getStatus());
        transportTaskMapper.updateById(updateObj);
        
        log.info("[completeTask][任务({})已完成]", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long id, String reason) {
        // 校验任务存在
        TransportTaskDO transportTask = validateTransportTaskExists(id);
        
        // 校验任务状态（已完成的任务不能取消）
        if (TransportTaskStatusEnum.COMPLETED.getStatus().equals(transportTask.getTaskStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_STATUS_COMPLETED_CANNOT_CANCEL);
        }
        
        // 更新任务状态
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setTaskStatus(TransportTaskStatusEnum.CANCELLED.getStatus());
        updateObj.setRemark(reason);
        transportTaskMapper.updateById(updateObj);
        
        log.info("[cancelTask][任务({})已取消，原因：{}]", id, reason);
    }

    @Override
    public void updateLocation(Long id, String location, BigDecimal latitude, BigDecimal longitude) {
        // 校验任务存在
        validateTransportTaskExists(id);
        
        // 更新位置信息
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setCurrentLocation(location);
        updateObj.setCurrentLatitude(latitude);
        updateObj.setCurrentLongitude(longitude);
        transportTaskMapper.updateById(updateObj);
        
        log.debug("[updateLocation][任务({})更新位置：{}]", id, location);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportAbnormal(Long id, Integer abnormalType, String abnormalReason) {
        // 校验任务存在
        validateTransportTaskExists(id);
        
        // 更新异常信息
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setIsAbnormal(true);
        updateObj.setAbnormalType(abnormalType);
        updateObj.setAbnormalReason(abnormalReason);
        transportTaskMapper.updateById(updateObj);
        
        log.warn("[reportAbnormal][任务({})报告异常，类型：{}，原因：{}]", id, abnormalType, abnormalReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveAbnormal(Long id) {
        // 校验任务存在
        TransportTaskDO transportTask = validateTransportTaskExists(id);
        
        // 校验是否有异常
        if (!Boolean.TRUE.equals(transportTask.getIsAbnormal())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_NO_ABNORMAL);
        }
        
        // 清除异常信息
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(id);
        updateObj.setIsAbnormal(false);
        updateObj.setAbnormalType(null);
        updateObj.setAbnormalReason(null);
        transportTaskMapper.updateById(updateObj);
        
        log.info("[resolveAbnormal][任务({})异常已解决]", id);
    }

    @Override
    public TransportTaskDO validateTransportTaskExists(Long id) {
        if (id == null) {
            return null;
        }
        TransportTaskDO transportTask = transportTaskMapper.selectById(id);
        if (transportTask == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.TRANSPORT_TASK_NOT_EXISTS);
        }
        return transportTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchAssignResultVO batchAssignTasks(@Valid BatchAssignReqVO batchAssignReqVO) {
        BatchAssignResultVO result = new BatchAssignResultVO();
        result.setTotalCount(0); // 暂时设为0，等VO类完善后再实现
        result.setSuccessCount(0);
        result.setFailureCount(0);
        
        // TODO: 实现批量分配逻辑
        log.info("[batchAssignTasks][批量分配任务]");
        
        return result;
    }

    @Override
    public List<AssignmentRecommendationVO> getAssignmentRecommendations(Long taskId) {
        // TODO: 实现分配推荐逻辑
        log.info("[getAssignmentRecommendations][获取任务分配推荐，任务ID：{}]", taskId);
        return new ArrayList<>();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignTask(Long taskId, Long newDriverId, Long newVehicleId, String reason) {
        // 校验任务存在
        validateTransportTaskExists(taskId);
        
        // 更新分配信息
        TransportTaskDO updateObj = new TransportTaskDO();
        updateObj.setId(taskId);
        updateObj.setDriverId(newDriverId);
        updateObj.setVehicleId(newVehicleId);
        updateObj.setRemark(reason);
        transportTaskMapper.updateById(updateObj);
        
        log.info("[reassignTask][重新分配任务，任务ID：{}，新司机：{}，新车辆：{}，原因：{}]", 
                taskId, newDriverId, newVehicleId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchAssignResultVO batchAssignTransportTask(BatchAssignReqVO batchAssignReqVO) {
        return batchAssignTasks(batchAssignReqVO);
    }

    @Override
    public List<AssignmentRecommendationVO> recommendAssignment(Long taskId) {
        return getAssignmentRecommendations(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignTransportTask(Long id, Long vehicleId, Long driverId, String reason) {
        reassignTask(id, driverId, vehicleId, reason);
    }

} 