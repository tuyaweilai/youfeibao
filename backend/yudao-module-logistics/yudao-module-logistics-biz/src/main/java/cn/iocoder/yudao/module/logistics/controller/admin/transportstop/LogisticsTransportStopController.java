package cn.iocoder.yudao.module.logistics.controller.admin.transportstop;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopSaveReqVO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.transportstop.LogisticsTransportStopService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 运输停靠点（V5 #72）。
 *
 * <p>停靠点是任务里的一个提货 / 送货地点，每个**独立推进**：各自的节点、进度与断点；
 * 取消一个点不影响同一任务里的其它点。**一次集货不构成把几个出售者合并结算的依据**（ADR 0031）。
 */
@Tag(name = "管理后台 - 运输停靠点")
@RestController
@RequestMapping("/logistics/transport-stop")
@Validated
public class LogisticsTransportStopController {

    @Resource
    private LogisticsTransportStopService logisticsTransportStopService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;

    @PostMapping("/create")
    @Operation(summary = "给任务追加一个停靠点", description = "停靠顺序接在最后；终态任务不接受追加")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_UPDATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody LogisticsTransportStopSaveReqVO createReqVO) {
        return success(logisticsTransportTaskService.addStop(createReqVO));
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消一个停靠点",
            description = "必填原因；只取消这一个点，同一任务里其它停靠点不受影响。已完成 / 已取消的不能再取消")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_UPDATE + "')")
    public CommonResult<Boolean> cancel(@Valid @RequestBody LogisticsTransportStopCancelReqVO cancelReqVO) {
        logisticsTransportStopService.cancelStop(cancelReqVO);
        return success(true);
    }

    @GetMapping("/list-by-task")
    @Operation(summary = "按运输任务取停靠点及其各自的进度与断点")
    @Parameter(name = "taskId", description = "运输任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_TASK_QUERY + "')")
    public CommonResult<List<LogisticsTransportStopRespVO>> listByTask(@RequestParam("taskId") Long taskId) {
        return success(logisticsTransportStopService.getStopRespListByTaskId(taskId));
    }

}
