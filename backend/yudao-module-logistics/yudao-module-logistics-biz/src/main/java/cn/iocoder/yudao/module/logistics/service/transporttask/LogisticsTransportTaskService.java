package cn.iocoder.yudao.module.logistics.service.transporttask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskAssignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskOverrideAssignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskReassignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskReassignDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;

import javax.validation.Valid;
import java.util.List;

/**
 * 运输任务 Service（V2b #78）。
 *
 * <p>落地 ADR 0032 的取舍：**一任务 = 一车 + 一司机 + 一次执行**（含一个出发地、一个提货点、
 * 一个时间窗），一期不建独立「车次」；任务可挂采购安排，也可什么都不挂。
 *
 * <p>状态推进统一走 {@link #transitStatus}：状态机在 {@link LogisticsTransportTaskStatusEnum#canTransitTo}
 * 一处定义，非法推进一律拒绝（不是静默忽略）。派车会把车辆置为「运输中」，完成或取消时放回车队。
 */
public interface LogisticsTransportTaskService {

    /**
     * 建任务。带车与司机即等于派车（直接到「已分配」）；都不带则停在「待分配」。
     */
    Long createTask(@Valid LogisticsTransportTaskSaveReqVO createReqVO);

    /**
     * 改任务的基本信息（地址、时间窗、联系人、备注）。**不改状态、不改车与人**——
     * 改派走 {@link #assignTask}，状态推进走各自的动作。
     */
    void updateTask(@Valid LogisticsTransportTaskSaveReqVO updateReqVO);

    /**
     * 派车：给待分配的任务安排车与司机。**门禁**：车辆不得维修中、司机必须在职，
     * 且证件都未过期；任何一条不过就拒绝。
     */
    void assignTask(@Valid LogisticsTransportTaskAssignReqVO assignReqVO);

    /**
     * 授权放行派车：证件过期时由管理员带着原因放行，**并把原因 / 授权人 / 时间落到任务上**。
     *
     * <p>只放行软门禁（证件过期）。车辆维修中、司机离职这类硬门禁即便走这条路也拦——
     * 那不是在办手续，是在无证运营。
     */
    void assignTaskWithOverride(@Valid LogisticsTransportTaskOverrideAssignReqVO overrideReqVO);

    /**
     * 改派：换车换人。**保留承接关系、不覆盖原记录**——原车原人、新车新人、原因、谁改的
     * 都写进 {@code logistics_transport_task_reassign}，任务行上的快照只是「当前是谁」。
     *
     * <p>只有「已分配 / 已接单 / 执行中」能改派；改派**不改状态机**（异常与改派都不是状态）。
     * 门禁与 {@link #assignTask} 一致：车辆不得维修中、司机必须在职、证件都不过期。
     */
    void reassignTask(@Valid LogisticsTransportTaskReassignReqVO reassignReqVO);

    /**
     * 按任务取改派承接记录，按改派时间正序。没有改派过返回空列表。
     */
    List<LogisticsTransportTaskReassignDO> getReassignListByTaskId(Long taskId);

    /**
     * 接单（V2c 由司机端点；本票由调度在 PC 上代记）。
     */
    void acceptTask(Long id);

    /**
     * 调度确认完成。一期「一次执行」的完成由调度确认，而不是由最后一个节点自动推出——
     * 多停靠点场景下「哪个节点算整趟完成」要等 V5（#72）定义。
     */
    void completeTask(Long id);

    /**
     * 取消（必填原因）。
     */
    void cancelTask(@Valid LogisticsTransportTaskCancelReqVO cancelReqVO);

    /**
     * 把任务推进到目标状态（内部动作与节点上报都走这里）。非法推进抛业务异常。
     */
    void transitStatus(LogisticsTransportTaskDO task, LogisticsTransportTaskStatusEnum target);

    /**
     * 同上，并顺带写入本次动作附带的字段（如起运时间）。
     *
     * <p>节点上报用：上报「起运」既要推状态，又要把 {@code startTime} 落在**发生时间**上
     *（不是上报时间——补录时两者相差可能很久）。
     */
    void transitStatusAndFill(LogisticsTransportTaskDO task, LogisticsTransportTaskStatusEnum target,
                              LogisticsTransportTaskDO extraUpdate);

    /**
     * 获得任务；不存在时抛业务异常。
     */
    LogisticsTransportTaskDO getTask(Long id);

    /**
     * 按任务单号获得任务；不存在返回 {@code null}（读取面用）。
     */
    LogisticsTransportTaskDO getTaskByTaskNo(String taskNo);

    PageResult<LogisticsTransportTaskDO> getTaskPage(LogisticsTransportTaskPageReqVO pageReqVO);

}
