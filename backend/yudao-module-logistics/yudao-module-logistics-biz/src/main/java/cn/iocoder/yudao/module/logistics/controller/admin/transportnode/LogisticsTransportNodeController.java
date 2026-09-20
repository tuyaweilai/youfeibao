package cn.iocoder.yudao.module.logistics.controller.admin.transportnode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
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
 * 管理后台 - 运输节点（V2b #78）。
 *
 * <p>本票只开放**起运**一类节点的上报（其余四类与异常见 V4 #71），现场动作由调度在 PC 上代录。
 * 上报是幂等的：同一客户端请求号重复提交返回既有节点编号。
 */
@Tag(name = "管理后台 - 运输节点")
@RestController
@RequestMapping("/logistics/transport-node")
@Validated
public class LogisticsTransportNodeController {

    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;

    @PostMapping("/report")
    @Operation(summary = "上报运输节点", description = "幂等：同一 clientRequestId 重复提交返回既有节点编号")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_NODE_REPORT + "')")
    public CommonResult<Long> report(@Valid @RequestBody LogisticsTransportNodeReportReqVO reportReqVO) {
        return success(logisticsTransportNodeService.reportNode(reportReqVO));
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
