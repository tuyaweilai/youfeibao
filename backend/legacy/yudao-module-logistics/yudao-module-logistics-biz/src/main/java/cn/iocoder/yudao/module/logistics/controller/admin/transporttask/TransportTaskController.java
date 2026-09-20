package cn.iocoder.yudao.module.logistics.controller.admin.transporttask;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.*;
import cn.iocoder.yudao.module.logistics.convert.transporttask.TransportTaskConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.TransportTaskDO;
import cn.iocoder.yudao.module.logistics.service.transporttask.TransportTaskService;
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
import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 物流运输任务")
@RestController
@RequestMapping("/logistics/transport-task")
@Validated
public class TransportTaskController {

    @Resource
    private TransportTaskService transportTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建物流运输任务")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:create')")
    public CommonResult<Long> createTransportTask(@Valid @RequestBody TransportTaskCreateReqVO createReqVO) {
        return success(transportTaskService.createTransportTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新物流运输任务")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:update')")
    public CommonResult<Boolean> updateTransportTask(@Valid @RequestBody TransportTaskUpdateReqVO updateReqVO) {
        transportTaskService.updateTransportTask(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物流运输任务")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:delete')")
    public CommonResult<Boolean> deleteTransportTask(@RequestParam("id") Long id) {
        transportTaskService.deleteTransportTask(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得物流运输任务")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:query')")
    public CommonResult<TransportTaskRespVO> getTransportTask(@RequestParam("id") Long id) {
        return success(transportTaskService.getTransportTaskDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得物流运输任务分页")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:query')")
    public CommonResult<PageResult<TransportTaskRespVO>> getTransportTaskPage(@Valid TransportTaskPageReqVO pageReqVO) {
        return success(transportTaskService.getTransportTaskPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出物流运输任务 Excel")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTransportTaskExcel(@Valid TransportTaskPageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TransportTaskDO> list = transportTaskService.getTransportTaskList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "物流运输任务.xls", "数据", TransportTaskExcelVO.class,
                TransportTaskConvert.INSTANCE.convertExcelList(list));
    }

    @GetMapping("/get-by-task-no")
    @Operation(summary = "根据任务编号获得物流运输任务")
    @Parameter(name = "taskNo", description = "任务编号", required = true, example = "TT202401010001")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:query')")
    public CommonResult<TransportTaskRespVO> getTransportTaskByTaskNo(@RequestParam("taskNo") String taskNo) {
        TransportTaskDO transportTask = transportTaskService.getTransportTaskByTaskNo(taskNo);
        return success(transportTask != null ? TransportTaskConvert.INSTANCE.convert(transportTask) : null);
    }

    @GetMapping("/list-by-order-id")
    @Operation(summary = "根据订单ID获得物流运输任务列表")
    @Parameter(name = "orderId", description = "订单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:query')")
    public CommonResult<List<TransportTaskRespVO>> getTransportTaskListByOrderId(@RequestParam("orderId") Long orderId) {
        List<TransportTaskDO> list = transportTaskService.getTransportTaskListByOrderId(orderId);
        return success(TransportTaskConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-vehicle-id")
    @Operation(summary = "根据车辆ID获得物流运输任务列表")
    @Parameter(name = "vehicleId", description = "车辆ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:query')")
    public CommonResult<List<TransportTaskRespVO>> getTransportTaskListByVehicleId(@RequestParam("vehicleId") Long vehicleId) {
        List<TransportTaskDO> list = transportTaskService.getTransportTaskListByVehicleId(vehicleId);
        return success(TransportTaskConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-driver-id")
    @Operation(summary = "根据司机ID获得物流运输任务列表")
    @Parameter(name = "driverId", description = "司机ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:query')")
    public CommonResult<List<TransportTaskRespVO>> getTransportTaskListByDriverId(@RequestParam("driverId") Long driverId) {
        List<TransportTaskDO> list = transportTaskService.getTransportTaskListByDriverId(driverId);
        return success(TransportTaskConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-task-status")
    @Operation(summary = "根据任务状态获得物流运输任务列表")
    @Parameter(name = "taskStatus", description = "任务状态", required = true, example = "0")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:query')")
    public CommonResult<List<TransportTaskRespVO>> getTransportTaskListByTaskStatus(@RequestParam("taskStatus") Integer taskStatus) {
        List<TransportTaskDO> list = transportTaskService.getTransportTaskListByTaskStatus(taskStatus);
        return success(TransportTaskConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-enterprise-id")
    @Operation(summary = "根据企业ID获得物流运输任务列表")
    @Parameter(name = "enterpriseId", description = "企业ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:query')")
    public CommonResult<List<TransportTaskRespVO>> getTransportTaskListByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        List<TransportTaskDO> list = transportTaskService.getTransportTaskListByEnterpriseId(enterpriseId);
        return success(TransportTaskConvert.INSTANCE.convertList(list));
    }

    @PutMapping("/assign")
    @Operation(summary = "分配运输任务")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:assign')")
    public CommonResult<Boolean> assignTransportTask(@RequestParam("id") Long id,
                                                     @RequestParam("vehicleId") Long vehicleId,
                                                     @RequestParam("driverId") Long driverId) {
        transportTaskService.assignTransportTask(id, vehicleId, driverId);
        return success(true);
    }

    @PutMapping("/batch-assign")
    @Operation(summary = "批量分配运输任务")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:assign')")
    public CommonResult<BatchAssignResultVO> batchAssignTransportTask(@Valid @RequestBody BatchAssignReqVO batchAssignReqVO) {
        return success(transportTaskService.batchAssignTransportTask(batchAssignReqVO));
    }

    @GetMapping("/recommend-assignment")
    @Operation(summary = "智能推荐任务分配")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:assign')")
    public CommonResult<List<AssignmentRecommendationVO>> recommendAssignment(@RequestParam("taskId") Long taskId) {
        return success(transportTaskService.recommendAssignment(taskId));
    }

    @PutMapping("/reassign")
    @Operation(summary = "重新分配运输任务")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:assign')")
    public CommonResult<Boolean> reassignTransportTask(@RequestParam("id") Long id,
                                                      @RequestParam("vehicleId") Long vehicleId,
                                                      @RequestParam("driverId") Long driverId,
                                                      @RequestParam("reason") String reason) {
        transportTaskService.reassignTransportTask(id, vehicleId, driverId, reason);
        return success(true);
    }

    @PutMapping("/accept")
    @Operation(summary = "接受运输任务")
    @Parameter(name = "id", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:accept')")
    public CommonResult<Boolean> acceptTransportTask(@RequestParam("id") Long id) {
        transportTaskService.acceptTransportTask(id);
        return success(true);
    }

    @PutMapping("/start-transport")
    @Operation(summary = "开始运输")
    @Parameter(name = "id", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:start')")
    public CommonResult<Boolean> startTransport(@RequestParam("id") Long id) {
        transportTaskService.startTransport(id);
        return success(true);
    }

    @PutMapping("/confirm-pickup")
    @Operation(summary = "确认取货")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:pickup')")
    public CommonResult<Boolean> confirmPickup(@RequestParam("id") Long id,
                                               @RequestParam("actualQuantity") BigDecimal actualQuantity) {
        transportTaskService.confirmPickup(id, actualQuantity);
        return success(true);
    }

    @PutMapping("/confirm-delivery")
    @Operation(summary = "确认送达")
    @Parameter(name = "id", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:delivery')")
    public CommonResult<Boolean> confirmDelivery(@RequestParam("id") Long id) {
        transportTaskService.confirmDelivery(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成任务")
    @Parameter(name = "id", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:complete')")
    public CommonResult<Boolean> completeTask(@RequestParam("id") Long id) {
        transportTaskService.completeTask(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消任务")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:cancel')")
    public CommonResult<Boolean> cancelTask(@RequestParam("id") Long id,
                                           @RequestParam("reason") String reason) {
        transportTaskService.cancelTask(id, reason);
        return success(true);
    }

    @PutMapping("/update-location")
    @Operation(summary = "更新位置信息")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:location')")
    public CommonResult<Boolean> updateLocation(@RequestParam("id") Long id,
                                               @RequestParam("location") String location,
                                               @RequestParam("latitude") BigDecimal latitude,
                                               @RequestParam("longitude") BigDecimal longitude) {
        transportTaskService.updateLocation(id, location, latitude, longitude);
        return success(true);
    }

    @PutMapping("/report-abnormal")
    @Operation(summary = "报告异常")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:abnormal')")
    public CommonResult<Boolean> reportAbnormal(@RequestParam("id") Long id,
                                               @RequestParam("abnormalType") Integer abnormalType,
                                               @RequestParam("abnormalReason") String abnormalReason) {
        transportTaskService.reportAbnormal(id, abnormalType, abnormalReason);
        return success(true);
    }

    @PutMapping("/resolve-abnormal")
    @Operation(summary = "解决异常")
    @Parameter(name = "id", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:transport-task:abnormal')")
    public CommonResult<Boolean> resolveAbnormal(@RequestParam("id") Long id) {
        transportTaskService.resolveAbnormal(id);
        return success(true);
    }

} 