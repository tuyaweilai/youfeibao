package cn.iocoder.yudao.module.logistics.service.transporttask.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskAssignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskOverrideAssignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskReassignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskReassignDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskReassignMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 运输任务 Service 实现（V2b #78）。
 *
 * <p>职责边界：状态机在枚举里，**状态推进只经 {@link #transitStatus} 一条路**；
 * 派车与释放车辆是「运输中」这个状态的唯一出入口（车辆档案的 CRUD 拒绝手工设它）。
 */
@Service
@Validated
public class LogisticsTransportTaskServiceImpl implements LogisticsTransportTaskService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;
    @Resource
    private LogisticsTransportTaskReassignMapper logisticsTransportTaskReassignMapper;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(LogisticsTransportTaskSaveReqVO createReqVO) {
        boolean hasVehicle = createReqVO.getVehicleId() != null;
        boolean hasDriver = createReqVO.getDriverId() != null;
        // 可以都不给（待分配），但不能只给一个：半套派车没有意义，只会让页面显示一辆没人的车
        if (hasVehicle != hasDriver) {
            throw exception(TRANSPORT_TASK_ASSIGN_REQUIRED);
        }

        LogisticsTransportTaskDO task = BeanUtils.toBean(createReqVO, LogisticsTransportTaskDO.class);
        task.setTaskNo(generateTaskNo());
        task.setStatus(LogisticsTransportTaskStatusEnum.PENDING.getStatus());
        if (hasVehicle) {
            // 带车带人创建 = 直接完成派车，落到「已分配」。创建路径不做授权放行：
            // 要带过期证件出车，先建任务再走 assignTaskWithOverride，让授权那一步显式发生。
            fillAssignment(task, createReqVO.getVehicleId(), createReqVO.getDriverId(), false);
            task.setStatus(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus());
        }
        logisticsTransportTaskMapper.insert(task);
        return task.getId();
    }

    @Override
    public void updateTask(LogisticsTransportTaskSaveReqVO updateReqVO) {
        LogisticsTransportTaskDO exists = getTask(updateReqVO.getId());
        // 起运之后不再改面单：地址、联系人、时间窗都是发给司机与出售者的信息，改了就与手上的不一致。
        // 要换车换人走派车（assignTask），要停走取消（cancelTask）。
        if (!canEdit(exists.getStatus())) {
            throw exception(TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE);
        }
        LogisticsTransportTaskDO update = BeanUtils.toBean(updateReqVO, LogisticsTransportTaskDO.class);
        update.setStatus(null); // 状态不在这里改
        update.setVehicleId(null);
        update.setDriverId(null);
        update.setPlateNo(null);
        update.setDriverName(null);
        update.setDriverMobile(null);
        logisticsTransportTaskMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(LogisticsTransportTaskAssignReqVO assignReqVO) {
        LogisticsTransportTaskDO task = getTask(assignReqVO.getId());
        if (!Objects.equals(task.getStatus(), LogisticsTransportTaskStatusEnum.PENDING.getStatus())) {
            throw exception(TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE);
        }
        LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
        update.setId(task.getId());
        fillAssignment(update, assignReqVO.getVehicleId(), assignReqVO.getDriverId(), false);
        update.setStatus(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus());
        update.setAssignTime(LocalDateTime.now());
        logisticsTransportTaskMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTaskWithOverride(LogisticsTransportTaskOverrideAssignReqVO overrideReqVO) {
        if (StrUtil.isBlank(overrideReqVO.getOverrideReason())) {
            throw exception(TRANSPORT_TASK_OVERRIDE_REASON_REQUIRED);
        }
        LogisticsTransportTaskDO task = getTask(overrideReqVO.getId());
        if (!Objects.equals(task.getStatus(), LogisticsTransportTaskStatusEnum.PENDING.getStatus())) {
            throw exception(TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE);
        }
        LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
        update.setId(task.getId());
        try {
            fillAssignment(update, overrideReqVO.getVehicleId(), overrideReqVO.getDriverId(), true);
        } catch (ServiceException ex) {
            // 硬门禁走这条路也拦：换个说法告诉调用者「这不是能授权的那一类」
            if (TRANSPORT_TASK_VEHICLE_NOT_AVAILABLE.getCode().equals(ex.getCode())
                    || TRANSPORT_TASK_DRIVER_NOT_ACTIVE.getCode().equals(ex.getCode())) {
                throw exception(TRANSPORT_TASK_OVERRIDE_NOT_APPLICABLE);
            }
            throw ex;
        }
        update.setStatus(LogisticsTransportTaskStatusEnum.ASSIGNED.getStatus());
        update.setAssignTime(LocalDateTime.now());
        update.setOverrideReason(overrideReqVO.getOverrideReason());
        update.setOverrideBy(SecurityFrameworkUtils.getLoginUserId());
        update.setOverrideTime(LocalDateTime.now());
        logisticsTransportTaskMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignTask(LogisticsTransportTaskReassignReqVO reassignReqVO) {
        if (StrUtil.isBlank(reassignReqVO.getReason())) {
            throw exception(TRANSPORT_TASK_REASSIGN_REASON_REQUIRED);
        }
        LogisticsTransportTaskDO task = getTask(reassignReqVO.getId());
        LogisticsTransportTaskStatusEnum current = LogisticsTransportTaskStatusEnum.ofStatus(task.getStatus())
                .orElseThrow(() -> exception(TRANSPORT_TASK_REASSIGN_NOT_ALLOWED));
        // 待分配还没有车与人可换；终态已经了结。中途（已分配 / 已接单 / 执行中）都能换车换人。
        if (current != LogisticsTransportTaskStatusEnum.ASSIGNED
                && current != LogisticsTransportTaskStatusEnum.ACCEPTED
                && current != LogisticsTransportTaskStatusEnum.IN_TRANSIT) {
            throw exception(TRANSPORT_TASK_REASSIGN_NOT_ALLOWED);
        }

        Long prevVehicleId = task.getVehicleId();
        LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
        update.setId(task.getId());
        // 门禁与首次派车一致（证件过期 / 车辆维修中 / 司机离职都会拦），并占用新车
        fillAssignment(update, reassignReqVO.getVehicleId(), reassignReqVO.getDriverId(), false);
        // 原车放回车队（换的是同一辆车就不动它；维修中的车 releaseByTask 会自己跳过）
        if (!Objects.equals(prevVehicleId, reassignReqVO.getVehicleId())) {
            logisticsVehicleService.releaseByTask(prevVehicleId);
        }
        // 不覆盖任务上的原派车时间：改派时间属于承接记录，不是「第一次派车是什么时候」
        update.setAssignTime(null);
        logisticsTransportTaskMapper.updateById(update);

        // 承接记录：原车原人 → 新车新人 + 原因 + 谁改的。只追加，不覆盖。
        LogisticsTransportTaskReassignDO record = new LogisticsTransportTaskReassignDO();
        record.setTaskId(task.getId());
        record.setTaskNo(task.getTaskNo());
        record.setPrevVehicleId(task.getVehicleId());
        record.setPrevPlateNo(task.getPlateNo());
        record.setPrevDriverId(task.getDriverId());
        record.setPrevDriverName(task.getDriverName());
        record.setPrevDriverMobile(task.getDriverMobile());
        record.setVehicleId(update.getVehicleId());
        record.setPlateNo(update.getPlateNo());
        record.setDriverId(update.getDriverId());
        record.setDriverName(update.getDriverName());
        record.setDriverMobile(update.getDriverMobile());
        record.setReason(reassignReqVO.getReason());
        record.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        record.setOperatorName(SecurityFrameworkUtils.getLoginUserNickname());
        record.setReassignTime(LocalDateTime.now());
        logisticsTransportTaskReassignMapper.insert(record);
    }

    @Override
    public List<LogisticsTransportTaskReassignDO> getReassignListByTaskId(Long taskId) {
        if (taskId == null) {
            return Collections.emptyList();
        }
        return logisticsTransportTaskReassignMapper.selectListByTaskId(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTask(Long id) {
        LogisticsTransportTaskDO task = getTask(id);
        LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
        update.setId(id);
        update.setAcceptTime(LocalDateTime.now());
        transitStatusAndFill(task, LogisticsTransportTaskStatusEnum.ACCEPTED, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long id) {
        LogisticsTransportTaskDO task = getTask(id);
        LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
        update.setId(id);
        update.setCompleteTime(LocalDateTime.now());
        transitStatusAndFill(task, LogisticsTransportTaskStatusEnum.COMPLETED, update);
        logisticsVehicleService.releaseByTask(task.getVehicleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(LogisticsTransportTaskCancelReqVO cancelReqVO) {
        if (StrUtil.isBlank(cancelReqVO.getCancelReason())) {
            throw exception(TRANSPORT_TASK_CANCEL_REASON_REQUIRED);
        }
        LogisticsTransportTaskDO task = getTask(cancelReqVO.getId());
        LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
        update.setId(task.getId());
        update.setCancelTime(LocalDateTime.now());
        update.setCancelReason(cancelReqVO.getCancelReason());
        transitStatusAndFill(task, LogisticsTransportTaskStatusEnum.CANCELLED, update);
        logisticsVehicleService.releaseByTask(task.getVehicleId());
    }

    @Override
    public void transitStatus(LogisticsTransportTaskDO task, LogisticsTransportTaskStatusEnum target) {
        transitStatusAndFill(task, target, null);
    }

    /**
     * 状态推进的唯一实现：状态机判定 + 落状态与本次动作附带的字段。
     *
     * <p>同状态视为允许（幂等重放不报错）：重复点「接单」、节点补传都会走到这里。
     */
    @Override
    public void transitStatusAndFill(LogisticsTransportTaskDO task, LogisticsTransportTaskStatusEnum target,
                                     LogisticsTransportTaskDO extraUpdate) {
        LogisticsTransportTaskStatusEnum current = LogisticsTransportTaskStatusEnum.ofStatus(task.getStatus())
                .orElseThrow(() -> exception(TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE));
        if (!current.canTransitTo(target)) {
            throw exception(TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE);
        }
        if (current == target) {
            return; // 已经在这个状态：不写库，也不改动作时间
        }
        LogisticsTransportTaskDO update = extraUpdate != null ? extraUpdate : new LogisticsTransportTaskDO();
        update.setId(task.getId());
        update.setStatus(target.getStatus());
        logisticsTransportTaskMapper.updateById(update);
        task.setStatus(target.getStatus());
    }

    @Override
    public LogisticsTransportTaskDO getTask(Long id) {
        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(id);
        if (task == null) {
            throw exception(TRANSPORT_TASK_NOT_EXISTS);
        }
        return task;
    }

    @Override
    public LogisticsTransportTaskDO getTaskByTaskNo(String taskNo) {
        if (StrUtil.isBlank(taskNo)) {
            return null;
        }
        return logisticsTransportTaskMapper.selectByTaskNo(taskNo);
    }

    @Override
    public PageResult<LogisticsTransportTaskDO> getTaskPage(LogisticsTransportTaskPageReqVO pageReqVO) {
        return logisticsTransportTaskMapper.selectPage(pageReqVO);
    }

    /**
     * 填车与司机的**引用 + 快照**，并占用车辆。
     *
     * <p>快照是给一票一档用的：档案改名或删档都不该让历史单据变样（ADR 0032 第 7 条）。
     */
    private void fillAssignment(LogisticsTransportTaskDO target, Long vehicleId, Long driverId,
                                boolean allowExpiredDocuments) {
        LogisticsVehicleDO vehicle = allowExpiredDocuments
                ? logisticsVehicleService.getAssignableVehicleAllowingExpiredDocuments(vehicleId)
                : logisticsVehicleService.getAssignableVehicle(vehicleId);
        LogisticsDriverDO driver = allowExpiredDocuments
                ? logisticsDriverService.getAssignableDriverAllowingExpiredDocuments(driverId)
                : logisticsDriverService.getAssignableDriver(driverId);
        target.setVehicleId(vehicle.getId());
        target.setPlateNo(vehicle.getPlateNo());
        target.setDriverId(driver.getId());
        target.setDriverName(driver.getName());
        target.setDriverMobile(driver.getMobile());
        target.setAssignTime(LocalDateTime.now());
        logisticsVehicleService.occupyByTask(vehicle.getId());
    }

    /**
     * 起运之后不接受改面单；终态更不接受。
     */
    private boolean canEdit(Integer status) {
        LogisticsTransportTaskStatusEnum current = LogisticsTransportTaskStatusEnum.ofStatus(status).orElse(null);
        if (current == null) {
            return false;
        }
        return current == LogisticsTransportTaskStatusEnum.PENDING
                || current == LogisticsTransportTaskStatusEnum.ASSIGNED
                || current == LogisticsTransportTaskStatusEnum.ACCEPTED;
    }

    private String generateTaskNo() {
        return "TT" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}
