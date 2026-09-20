package cn.iocoder.yudao.module.logistics.controller.admin.transporttask;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskAssignReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 运输任务（V2b #78）。
 *
 * <p>调度在这里派车、代录节点（司机端归 V2c）、取消与确认完成。详情接口带运输时间线。
 */
@Tag(name = "管理后台 - 运输任务")
@RestController
@RequestMapping("/logistics/transport-task")
@Validated
public class LogisticsTransportTaskController {

    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;

    @PostMapping("/create")
    @Operation(summary = "创建运输任务", description = "带车与司机即等于派车（直接到已分配）；都不带则停在待分配")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_CREATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody LogisticsTransportTaskSaveReqVO createReqVO) {
        return success(logisticsTransportTaskService.createTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新运输任务", description = "只改地址 / 时间窗 / 联系人 / 备注；起运之后不接受改面单")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_UPDATE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody LogisticsTransportTaskSaveReqVO updateReqVO) {
        logisticsTransportTaskService.updateTask(updateReqVO);
        return success(true);
    }

    @PutMapping("/assign")
    @Operation(summary = "派车", description = "给待分配的任务安排车与司机")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_ASSIGN + "')")
    public CommonResult<Boolean> assign(@Valid @RequestBody LogisticsTransportTaskAssignReqVO assignReqVO) {
        logisticsTransportTaskService.assignTask(assignReqVO);
        return success(true);
    }

    @PutMapping("/accept")
    @Operation(summary = "接单", description = "V2c 由司机端点；本票由调度在 PC 上代记")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_UPDATE + "')")
    public CommonResult<Boolean> accept(@RequestParam("id") Long id) {
        logisticsTransportTaskService.acceptTask(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "确认完成", description = "一期由调度确认；完成或取消都会把车放回车队")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_UPDATE + "')")
    public CommonResult<Boolean> complete(@RequestParam("id") Long id) {
        logisticsTransportTaskService.completeTask(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消运输任务", description = "必填原因；已完成的不能取消")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_CANCEL + "')")
    public CommonResult<Boolean> cancel(@Valid @RequestBody LogisticsTransportTaskCancelReqVO cancelReqVO) {
        logisticsTransportTaskService.cancelTask(cancelReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得运输任务（含运输时间线与断点）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_QUERY + "')")
    public CommonResult<LogisticsTransportTaskRespVO> get(@RequestParam("id") Long id) {
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(id);
        LogisticsTransportTaskRespVO resp = BeanUtils.toBean(task, LogisticsTransportTaskRespVO.class);
        fillStatusName(resp);
        List<LogisticsTransportNodeDO> nodes = logisticsTransportNodeService.getNodeListByTaskId(task.getId());
        resp.setNodes(logisticsTransportNodeService.toRespList(nodes));
        resp.setMissingNodeNames(missingNodeNames(nodes));
        return success(resp);
    }

    @GetMapping("/page")
    @Operation(summary = "获得运输任务分页")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_QUERY + "')")
    public CommonResult<PageResult<LogisticsTransportTaskRespVO>> page(
            @Valid LogisticsTransportTaskPageReqVO pageReqVO) {
        PageResult<LogisticsTransportTaskDO> page = logisticsTransportTaskService.getTaskPage(pageReqVO);
        PageResult<LogisticsTransportTaskRespVO> result = BeanUtils.toBean(page, LogisticsTransportTaskRespVO.class);
        result.getList().forEach(this::fillStatusName);
        return success(result);
    }

    /**
     * 状态名：列表与详情都要给，页面直接显示，不让前端各自维护一份状态文案。
     */
    private void fillStatusName(LogisticsTransportTaskRespVO resp) {
        resp.setStatusName(LogisticsTransportTaskStatusEnum.nameOf(resp.getStatus()));
    }

    /**
     * 断点：流程里的五类节点，哪些还没上报。
     *
     * <p>本票只有「起运」能上报，所以这里会把其余四类都列成待补——这是**有意的**：
     * 页面要能显示「这趟活还有哪几步没留痕」，而不是假装流程已经完整。四类节点的上报与
     * 照片必填策略归 V4（#71）。
     */
    private List<String> missingNodeNames(List<LogisticsTransportNodeDO> nodes) {
        Set<Integer> reported = nodes.stream()
                .map(LogisticsTransportNodeDO::getNodeType)
                .collect(Collectors.toSet());
        return java.util.Arrays.stream(LogisticsTransportNodeTypeEnum.values())
                .filter(type -> !reported.contains(type.getType()))
                .map(LogisticsTransportNodeTypeEnum::getName)
                .collect(Collectors.toList());
    }

}
