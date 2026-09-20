package cn.iocoder.yudao.module.logistics.controller.admin.transportnode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalResolveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 运输节点（V2b #78；V4 #71 开全五类并加异常）。
 *
 * <p>五类节点全部可上报（交接完成与卸货完成必须有照片）。异常（车辆故障、道路封闭等）是**独立标记**，
 * 走 `/abnormal/report` 落成一条没有节点类型的事实，**不推进任务状态机**；解决走 `/abnormal/resolve`。
 * 上报都是幂等的：同一客户端请求号重复提交返回既有编号。
 */
@Tag(name = "管理后台 - 运输节点")
@RestController
@RequestMapping("/logistics/transport-node")
@Validated
public class LogisticsTransportNodeController {

    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;

    @PostMapping("/report")
    @Operation(summary = "上报运输节点", description = "五类节点；交接完成与卸货完成必须有照片。幂等：同一 clientRequestId 重复提交返回既有节点编号")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_NODE_REPORT + "')")
    public CommonResult<Long> report(@Valid @RequestBody LogisticsTransportNodeReportReqVO reportReqVO) {
        return success(logisticsTransportNodeService.reportNode(reportReqVO));
    }

    @PostMapping("/abnormal/report")
    @Operation(summary = "上报运输异常", description = "异常是独立标记，不改任务状态机；幂等：同一 clientRequestId 重复提交返回既有记录编号")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_NODE_REPORT + "')")
    public CommonResult<Long> reportAbnormal(
            @Valid @RequestBody LogisticsTransportAbnormalReportReqVO reportReqVO) {
        return success(logisticsTransportNodeService.reportAbnormal(reportReqVO));
    }

    @PutMapping("/abnormal/resolve")
    @Operation(summary = "解决运输异常", description = "记录谁 / 什么时候 / 怎么解决的；已解决的不覆盖")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_NODE_ABNORMAL_RESOLVE + "')")
    public CommonResult<Boolean> resolveAbnormal(
            @Valid @RequestBody LogisticsTransportAbnormalResolveReqVO resolveReqVO) {
        logisticsTransportNodeService.resolveAbnormal(resolveReqVO);
        return success(true);
    }

    @GetMapping("/list-by-task")
    @Operation(summary = "按运输任务取节点时间线", description = "按发生时间正序")
    @Parameter(name = "taskId", description = "运输任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_NODE_QUERY + "')")
    public CommonResult<List<LogisticsTransportNodeRespVO>> listByTask(@RequestParam("taskId") Long taskId) {
        return success(logisticsTransportNodeService.toRespList(
                logisticsTransportNodeService.getNodeListByTaskId(taskId)));
    }

}
