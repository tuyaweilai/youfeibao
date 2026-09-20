package cn.iocoder.yudao.module.logistics.controller.admin.driverapp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driverapp.LogisticsDriverAppService;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transportnode.TransportNodeGaps;
import cn.iocoder.yudao.module.logistics.service.transportstop.LogisticsTransportStopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 司机端（V2c #79）。
 *
 * <p>与收货员现场端同一套鉴权：走 `/admin-api` + token + tenant-id（ADR 0016 的做法），
 * 不新建一套登录体系；司机账号由回收企业建档（ADR 0032）。
 *
 * <p>**只回答「派给我的活」**：列表强制按登录账号对应的司机编号过滤，详情 / 接单 / 上报节点
 * 都先校验归属（见 {@code LogisticsDriverAppService}）。司机在这里看不到任何金额，
 * 也拿不到收购定稿、结算确认、付款开票的权限——现场不产生金额（ADR 0031）。
 */
@Tag(name = "司机端 - 运输任务与节点")
@RestController
@RequestMapping("/logistics/driver-app")
@Validated
public class LogisticsDriverAppController {

    @Resource
    private LogisticsDriverAppService logisticsDriverAppService;
    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;
    @Resource
    private LogisticsTransportStopService logisticsTransportStopService;

    /**
     * 口径说明：**不得暗示「整车复磅可以合并结算」**（ADR 0031）。司机端也要看到这句话。
     */
    private static final String SCOPE_NOTE =
            "一次集货不构成把几个出售者合并结算的依据：每个停靠点各自交接、各自复磅、各自结算，整车复磅只核对总运输量。";

    @GetMapping("/profile")
    @Operation(summary = "我是谁", description = "当前登录账号对应的司机档案；没建档会明确提示找管理员")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_APP_TASK_QUERY + "')")
    public CommonResult<Map<String, Object>> profile() {
        LogisticsDriverDO driver = logisticsDriverAppService.getCurrentDriver();
        return success(Map.of(
                "driverId", driver.getId(),
                "name", driver.getName(),
                "mobile", driver.getMobile() == null ? "" : driver.getMobile(),
                "source", driver.getSource(),
                "sourceName", LogisticsDriverSourceEnum.ofSource(driver.getSource())
                        .map(LogisticsDriverSourceEnum::getName).orElse("")));
    }

    @GetMapping("/task/page")
    @Operation(summary = "派给我的任务分页", description = "司机编号由登录账号决定，忽略入参里的 driverId")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_APP_TASK_QUERY + "')")
    public CommonResult<PageResult<LogisticsTransportTaskRespVO>> taskPage(
            @Valid LogisticsTransportTaskPageReqVO pageReqVO) {
        PageResult<LogisticsTransportTaskDO> page = logisticsDriverAppService.getMyTaskPage(pageReqVO);
        PageResult<LogisticsTransportTaskRespVO> result = PageResult.empty();
        result.setTotal(page.getTotal());
        Map<Long, Integer> pendingStopCounts = logisticsTransportStopService.getPendingStopCounts(
                page.getList().stream().map(LogisticsTransportTaskDO::getId).collect(Collectors.toList()));
        result.setList(page.getList().stream().map(task -> {
            LogisticsTransportTaskRespVO resp = new LogisticsTransportTaskRespVO();
            copyForDriver(task, resp);
            // 司机在列表上就要看到「这趟还剩几家没提」
            resp.setPendingStopCount(pendingStopCounts.getOrDefault(task.getId(), 0));
            resp.setScopeNote(SCOPE_NOTE);
            return resp;
        }).toList());
        return success(result);
    }

    @GetMapping("/task/get")
    @Operation(summary = "我的一趟任务（含时间线与断点）")
    @Parameter(name = "id", description = "任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_APP_TASK_QUERY + "')")
    public CommonResult<LogisticsTransportTaskRespVO> taskGet(@RequestParam("id") Long id) {
        LogisticsTransportTaskDO task = logisticsDriverAppService.getMyTask(id);
        LogisticsTransportTaskRespVO resp = new LogisticsTransportTaskRespVO();
        copyForDriver(task, resp);
        List<LogisticsTransportNodeDO> nodes = logisticsTransportNodeService.getNodeListByTaskId(task.getId());
        resp.setNodes(logisticsTransportNodeService.toRespList(nodes));
        resp.setMissingNodeNames(TransportNodeGaps.missingNodeNames(nodes));
        resp.setMissingEvidenceNames(TransportNodeGaps.missingEvidenceNames(nodes));
        // 停靠点：司机按点逐个处理，每点带自己的节点与进度
        resp.setStops(logisticsTransportStopService.getStopRespListByTaskId(task.getId()));
        resp.setPendingStopCount(logisticsTransportStopService
                .getPendingStopCounts(Collections.singletonList(task.getId()))
                .getOrDefault(task.getId(), 0));
        resp.setScopeNote(SCOPE_NOTE);
        return success(resp);
    }

    @PutMapping("/task/accept")
    @Operation(summary = "接单", description = "只能接自己的任务")
    @Parameter(name = "id", description = "任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_APP_TASK_ACCEPT + "')")
    public CommonResult<Boolean> taskAccept(@RequestParam("id") Long id) {
        logisticsDriverAppService.acceptMyTask(id);
        return success(true);
    }

    @PostMapping("/node/report")
    @Operation(summary = "上报运输节点", description = "只能报自己任务上的；同一 clientRequestId 重复提交返回既有节点编号")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_APP_NODE_REPORT + "')")
    public CommonResult<Long> nodeReport(@Valid @RequestBody LogisticsTransportNodeReportReqVO reportReqVO) {
        return success(logisticsDriverAppService.reportMyNode(reportReqVO));
    }

    @PostMapping("/node/abnormal/report")
    @Operation(summary = "上报运输异常", description = "只能报自己任务上的；异常是独立标记，不改任务状态；同一 clientRequestId 重复提交返回既有记录编号")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_APP_NODE_REPORT + "')")
    public CommonResult<Long> nodeAbnormalReport(
            @Valid @RequestBody LogisticsTransportAbnormalReportReqVO reportReqVO) {
        return success(logisticsDriverAppService.reportMyAbnormal(reportReqVO));
    }

    @PostMapping("/handover/create")
    @Operation(summary = "登记交接（现场谈好的事）",
            description = "品类、参考量、参考单价与凭证照片；**现场不产生金额**（ADR 0031），收购单在回场复磅后生成；同一 clientRequestId 重复提交返回既有登记编号")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_APP_HANDOVER_REPORT + "')")
    public CommonResult<Long> handoverCreate(
            @Valid @RequestBody LogisticsTransportHandoverCreateReqVO reqVO) {
        return success(logisticsDriverAppService.createMyHandover(reqVO));
    }

    @GetMapping("/handover/list")
    @Operation(summary = "我登记的交接", description = "按任务取（集货时一家一条）；只能看自己任务上的")
    @Parameter(name = "taskId", description = "运输任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_APP_HANDOVER_QUERY + "')")
    public CommonResult<List<LogisticsTransportHandoverRespVO>> handoverList(@RequestParam("taskId") Long taskId) {
        return success(logisticsDriverAppService.getMyHandoverList(taskId));
    }

    /**
     * 司机看到的字段：**没有金额、没有开票、没有付款**，也不给调度侧的备注与内部编号。
     *
     * <p>刻意手写而不是 {@code BeanUtils.toBean}：司机端可见面应当是一份明确的清单，
     * 将来任务对象加字段时，不该自动泄漏到司机手机上。
     */
    private void copyForDriver(LogisticsTransportTaskDO task, LogisticsTransportTaskRespVO resp) {
        resp.setId(task.getId());
        resp.setTaskNo(task.getTaskNo());
        resp.setStatus(task.getStatus());
        resp.setStatusName(LogisticsTransportTaskStatusEnum.nameOf(task.getStatus()));
        resp.setPlateNo(task.getPlateNo());
        resp.setDriverName(task.getDriverName());
        resp.setDriverMobile(task.getDriverMobile());
        resp.setDepartureAddress(task.getDepartureAddress());
        resp.setPickupAddress(task.getPickupAddress());
        resp.setPickupContactName(task.getPickupContactName());
        resp.setPickupContactPhone(task.getPickupContactPhone());
        resp.setExpectedStartTime(task.getExpectedStartTime());
        resp.setExpectedEndTime(task.getExpectedEndTime());
        resp.setCargoName(task.getCargoName());
        resp.setEstimatedQuantity(task.getEstimatedQuantity());
        resp.setQuantityUnit(task.getQuantityUnit());
        resp.setAssignTime(task.getAssignTime());
        resp.setAcceptTime(task.getAcceptTime());
        resp.setStartTime(task.getStartTime());
        resp.setCompleteTime(task.getCompleteTime());
        resp.setCancelTime(task.getCancelTime());
        resp.setCancelReason(task.getCancelReason());
        resp.setRemark(task.getRemark());
    }

}
