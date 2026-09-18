package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo.*;
import cn.iocoder.yudao.module.logistics.convert.cashadvance.CashAdvanceConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.cashadvance.CashAdvanceDO;
import cn.iocoder.yudao.module.logistics.service.cashadvance.CashAdvanceService;
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


@Tag(name = "管理后台 - 现金代付记录")
@RestController
@RequestMapping("/logistics/cash-advance")
@Validated
public class CashAdvanceController {

    @Resource
    private CashAdvanceService cashAdvanceService;

    @PostMapping("/create")
    @Operation(summary = "创建现金代付记录")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:create')")
    public CommonResult<Long> createCashAdvance(@Valid @RequestBody CashAdvanceCreateReqVO createReqVO) {
        return success(cashAdvanceService.createCashAdvance(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新现金代付记录")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:update')")
    public CommonResult<Boolean> updateCashAdvance(@Valid @RequestBody CashAdvanceUpdateReqVO updateReqVO) {
        cashAdvanceService.updateCashAdvance(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除现金代付记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:delete')")
    public CommonResult<Boolean> deleteCashAdvance(@RequestParam("id") Long id) {
        cashAdvanceService.deleteCashAdvance(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得现金代付记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<CashAdvanceRespVO> getCashAdvance(@RequestParam("id") Long id) {
        return success(cashAdvanceService.getCashAdvanceDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得现金代付记录分页")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<PageResult<CashAdvanceRespVO>> getCashAdvancePage(@Valid CashAdvancePageReqVO pageReqVO) {
        return success(cashAdvanceService.getCashAdvancePage(pageReqVO));
    }

    @PostMapping("/reconcile/confirm")
    @Operation(summary = "对账确认")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:reconcile')")
    public CommonResult<Boolean> reconcileConfirm(@Valid @RequestBody CashAdvanceReconcileReqVO reconcileReqVO) {
        cashAdvanceService.reconcileConfirm(reconcileReqVO);
        return success(true);
    }

    @PostMapping("/reconcile/batch-confirm")
    @Operation(summary = "批量对账确认")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:reconcile')")
    public CommonResult<CashAdvanceBatchReconcileResultVO> batchReconcileConfirm(@Valid @RequestBody CashAdvanceBatchReconcileReqVO batchReqVO) {
        return success(cashAdvanceService.batchReconcileConfirm(batchReqVO));
    }

    @GetMapping("/reconcile/summary")
    @Operation(summary = "获取对账汇总")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<CashAdvanceReconcileSummaryVO> getReconcileSummary(@Valid CashAdvanceReconcileSummaryReqVO summaryReqVO) {
        return success(cashAdvanceService.getReconcileSummary(summaryReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出现金代付记录 Excel")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCashAdvanceExcel(@Valid CashAdvancePageReqVO pageReqVO,
                                      HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CashAdvanceRespVO> list = cashAdvanceService.getCashAdvancePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "现金代付记录.xls", "数据", CashAdvanceRespVO.class, list);
    }

    @PostMapping("/notify-waste-module")
    @Operation(summary = "通知危废模块")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:notify')")
    public CommonResult<Boolean> notifyWasteModule(@RequestParam("id") Long id) {
        cashAdvanceService.notifyWasteModule(id);
        return success(true);
    }

    @GetMapping("/list-by-task-id")
    @Operation(summary = "根据任务ID获得物流现金代付记录列表")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<List<CashAdvanceRespVO>> getCashAdvanceListByTaskId(@RequestParam("taskId") Long taskId) {
        List<CashAdvanceDO> list = cashAdvanceService.getCashAdvanceListByTaskId(taskId);
        return success(CashAdvanceConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-order-id")
    @Operation(summary = "根据订单ID获得物流现金代付记录列表")
    @Parameter(name = "orderId", description = "订单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<List<CashAdvanceRespVO>> getCashAdvanceListByOrderId(@RequestParam("orderId") Long orderId) {
        List<CashAdvanceDO> list = cashAdvanceService.getCashAdvanceListByOrderId(orderId);
        return success(CashAdvanceConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-driver-id")
    @Operation(summary = "根据司机ID获得物流现金代付记录列表")
    @Parameter(name = "driverId", description = "司机ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<List<CashAdvanceRespVO>> getCashAdvanceListByDriverId(@RequestParam("driverId") Long driverId) {
        List<CashAdvanceDO> list = cashAdvanceService.getCashAdvanceListByDriverId(driverId);
        return success(CashAdvanceConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-notify-status")
    @Operation(summary = "根据通知状态获得物流现金代付记录列表")
    @Parameter(name = "notifyStatus", description = "通知状态", required = true, example = "0")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<List<CashAdvanceRespVO>> getCashAdvanceListByNotifyStatus(@RequestParam("notifyStatus") Integer notifyStatus) {
        List<CashAdvanceDO> list = cashAdvanceService.getCashAdvanceListByNotifyStatus(notifyStatus);
        return success(CashAdvanceConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-reconcile-status")
    @Operation(summary = "根据对账状态获得物流现金代付记录列表")
    @Parameter(name = "reconcileStatus", description = "对账状态", required = true, example = "0")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<List<CashAdvanceRespVO>> getCashAdvanceListByReconcileStatus(@RequestParam("reconcileStatus") Integer reconcileStatus) {
        List<CashAdvanceDO> list = cashAdvanceService.getCashAdvanceListByReconcileStatus(reconcileStatus);
        return success(CashAdvanceConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-payment-method")
    @Operation(summary = "根据支付方式获得物流现金代付记录列表")
    @Parameter(name = "paymentMethod", description = "支付方式", required = true, example = "CASH")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<List<CashAdvanceRespVO>> getCashAdvanceListByPaymentMethod(@RequestParam("paymentMethod") String paymentMethod) {
        List<CashAdvanceDO> list = cashAdvanceService.getCashAdvanceListByPaymentMethod(paymentMethod);
        return success(CashAdvanceConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/count-by-task-id")
    @Operation(summary = "统计任务的现金代付记录数量")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<Long> getCashAdvanceCountByTaskId(@RequestParam("taskId") Long taskId) {
        return success(cashAdvanceService.getCashAdvanceCountByTaskId(taskId));
    }

    @GetMapping("/count-by-order-id")
    @Operation(summary = "统计订单的现金代付记录数量")
    @Parameter(name = "orderId", description = "订单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<Long> getCashAdvanceCountByOrderId(@RequestParam("orderId") Long orderId) {
        return success(cashAdvanceService.getCashAdvanceCountByOrderId(orderId));
    }

    @GetMapping("/count-by-driver-id")
    @Operation(summary = "统计司机的现金代付记录数量")
    @Parameter(name = "driverId", description = "司机ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:query')")
    public CommonResult<Long> getCashAdvanceCountByDriverId(@RequestParam("driverId") Long driverId) {
        return success(cashAdvanceService.getCashAdvanceCountByDriverId(driverId));
    }

    // ========== 业务接口 ==========

    @PostMapping("/notify")
    @Operation(summary = "通知现金代付记录")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:notify')")
    public CommonResult<Boolean> notifyCashAdvance(@RequestParam("id") Long id) {
        cashAdvanceService.notifyCashAdvance(id);
        return success(true);
    }

    @PostMapping("/reconcile")
    @Operation(summary = "对账现金代付记录")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:reconcile')")
    public CommonResult<Boolean> reconcileCashAdvance(@RequestParam("id") Long id,
                                                     @RequestParam("reconcileRemark") String reconcileRemark,
                                                     @RequestParam("reconcileOperatorId") Long reconcileOperatorId,
                                                     @RequestParam("reconcileOperatorName") String reconcileOperatorName) {
        cashAdvanceService.reconcileCashAdvance(id, reconcileRemark, reconcileOperatorId, reconcileOperatorName);
        return success(true);
    }

    @PostMapping("/reconcile-failed")
    @Operation(summary = "对账失败")
    @PreAuthorize("@ss.hasPermission('logistics:cash-advance:reconcile')")
    public CommonResult<Boolean> reconcileFailedCashAdvance(@RequestParam("id") Long id,
                                                           @RequestParam("reconcileRemark") String reconcileRemark,
                                                           @RequestParam("reconcileOperatorId") Long reconcileOperatorId,
                                                           @RequestParam("reconcileOperatorName") String reconcileOperatorName) {
        cashAdvanceService.reconcileFailedCashAdvance(id, reconcileRemark, reconcileOperatorId, reconcileOperatorName);
        return success(true);
    }

} 