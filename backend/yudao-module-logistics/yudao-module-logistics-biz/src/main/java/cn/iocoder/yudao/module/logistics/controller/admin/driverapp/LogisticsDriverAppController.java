package cn.iocoder.yudao.module.logistics.controller.admin.driverapp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driverapp.LogisticsDriverAppService;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

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
        result.setList(page.getList().stream().map(task -> {
            LogisticsTransportTaskRespVO resp = new LogisticsTransportTaskRespVO();
            copyForDriver(task, resp);
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
