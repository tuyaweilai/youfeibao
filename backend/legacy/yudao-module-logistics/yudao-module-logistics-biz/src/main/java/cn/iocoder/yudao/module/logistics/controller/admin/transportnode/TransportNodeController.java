package cn.iocoder.yudao.module.logistics.controller.admin.transportnode;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.*;
import cn.iocoder.yudao.module.logistics.convert.transportnode.TransportNodeConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.TransportNodeDO;
import cn.iocoder.yudao.module.logistics.service.transportnode.TransportNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@Tag(name = "管理后台 - 运输节点记录")
@RestController
@RequestMapping("/logistics/transport-node")
@Validated
public class TransportNodeController {

    @Resource
    private TransportNodeService transportNodeService;

    @PostMapping("/create")
    @Operation(summary = "创建运输节点记录")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:create')")
    public CommonResult<Long> createTransportNode(@Valid @RequestBody TransportNodeCreateReqVO createReqVO) {
        return success(transportNodeService.createTransportNode(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新运输节点记录")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:update')")
    public CommonResult<Boolean> updateTransportNode(@Valid @RequestBody TransportNodeUpdateReqVO updateReqVO) {
        transportNodeService.updateTransportNode(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除运输节点记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:delete')")
    public CommonResult<Boolean> deleteTransportNode(@RequestParam("id") Long id) {
        transportNodeService.deleteTransportNode(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得运输节点记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<TransportNodeRespVO> getTransportNode(@RequestParam("id") Long id) {
        return success(transportNodeService.getTransportNodeDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得运输节点记录分页")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<PageResult<TransportNodeRespVO>> getTransportNodePage(@Valid TransportNodePageReqVO pageReqVO) {
        return success(transportNodeService.getTransportNodePage(pageReqVO));
    }

    @GetMapping("/by-task")
    @Operation(summary = "根据任务ID获取节点记录")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<List<TransportNodeRespVO>> getTransportNodesByTask(@RequestParam("taskId") Long taskId) {
        return success(transportNodeService.getTransportNodeListByTaskIdOrderByTime(taskId));
    }



    @GetMapping("/export-excel")
    @Operation(summary = "导出运输节点记录 Excel")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTransportNodeExcel(@Valid TransportNodePageReqVO pageReqVO,
                                        HttpServletResponse response) throws IOException {
        List<TransportNodeRespVO> list = transportNodeService.getTransportNodePage(pageReqVO).getList();
        ExcelUtils.write(response, "运输节点记录.xls", "数据", TransportNodeRespVO.class, list);
    }

    @GetMapping("/list-by-task-id")
    @Operation(summary = "根据任务ID获得物流运输节点记录列表")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<List<TransportNodeRespVO>> getTransportNodeListByTaskId(@RequestParam("taskId") Long taskId) {
        List<TransportNodeDO> list = transportNodeService.getTransportNodeListByTaskId(taskId);
        return success(TransportNodeConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-task-id-order-by-time")
    @Operation(summary = "根据任务ID获得物流运输节点记录列表（按时间排序）")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<List<TransportNodeRespVO>> getTransportNodeListByTaskIdOrderByTime(@RequestParam("taskId") Long taskId) {
        return success(transportNodeService.getTransportNodeListByTaskIdOrderByTime(taskId));
    }

    @GetMapping("/list-by-node-type")
    @Operation(summary = "根据节点类型获得物流运输节点记录列表")
    @Parameter(name = "nodeType", description = "节点类型", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<List<TransportNodeRespVO>> getTransportNodeListByNodeType(@RequestParam("nodeType") Integer nodeType) {
        List<TransportNodeDO> list = transportNodeService.getTransportNodeListByNodeType(nodeType);
        return success(TransportNodeConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-operator-id")
    @Operation(summary = "根据操作员ID获得物流运输节点记录列表")
    @Parameter(name = "operatorId", description = "操作员ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<List<TransportNodeRespVO>> getTransportNodeListByOperatorId(@RequestParam("operatorId") Long operatorId) {
        List<TransportNodeDO> list = transportNodeService.getTransportNodeListByOperatorId(operatorId);
        return success(TransportNodeConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/get-latest-by-task-id")
    @Operation(summary = "获得任务的最新节点记录")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<TransportNodeRespVO> getLatestTransportNodeByTaskId(@RequestParam("taskId") Long taskId) {
        TransportNodeDO transportNode = transportNodeService.getLatestTransportNodeByTaskId(taskId);
        return success(transportNode != null ? TransportNodeConvert.INSTANCE.convert(transportNode) : null);
    }

    @GetMapping("/count-by-task-id")
    @Operation(summary = "统计任务的节点记录数量")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-node:query')")
    public CommonResult<Long> getTransportNodeCountByTaskId(@RequestParam("taskId") Long taskId) {
        return success(transportNodeService.getTransportNodeCountByTaskId(taskId));
    }

} 