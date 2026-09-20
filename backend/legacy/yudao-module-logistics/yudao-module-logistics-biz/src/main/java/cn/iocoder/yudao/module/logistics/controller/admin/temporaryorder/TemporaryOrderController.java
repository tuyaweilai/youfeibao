package cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo.*;
import cn.iocoder.yudao.module.logistics.convert.temporaryorder.TemporaryOrderConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.temporaryorder.TemporaryOrderDO;
import cn.iocoder.yudao.module.logistics.service.temporaryorder.TemporaryOrderService;
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

@Tag(name = "管理后台 - 物流临时订单")
@RestController
@RequestMapping("/logistics/temporary-order")
@Validated
public class TemporaryOrderController {

    @Resource
    private TemporaryOrderService temporaryOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建物流临时订单")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:create')")
    public CommonResult<Long> createTemporaryOrder(@Valid @RequestBody TemporaryOrderCreateReqVO createReqVO) {
        return success(temporaryOrderService.createTemporaryOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新物流临时订单")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:update')")
    public CommonResult<Boolean> updateTemporaryOrder(@Valid @RequestBody TemporaryOrderUpdateReqVO updateReqVO) {
        temporaryOrderService.updateTemporaryOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物流临时订单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:delete')")
    public CommonResult<Boolean> deleteTemporaryOrder(@RequestParam("id") Long id) {
        temporaryOrderService.deleteTemporaryOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得物流临时订单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<TemporaryOrderRespVO> getTemporaryOrder(@RequestParam("id") Long id) {
        return success(temporaryOrderService.getTemporaryOrderDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得物流临时订单分页")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<PageResult<TemporaryOrderRespVO>> getTemporaryOrderPage(@Valid TemporaryOrderPageReqVO pageReqVO) {
        return success(temporaryOrderService.getTemporaryOrderPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出物流临时订单 Excel")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTemporaryOrderExcel(@Valid TemporaryOrderPageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TemporaryOrderDO> list = temporaryOrderService.getTemporaryOrderList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "物流临时订单.xls", "数据", TemporaryOrderExcelVO.class,
                TemporaryOrderConvert.INSTANCE.convertExcelList(list));
    }

    @GetMapping("/get-by-order-no")
    @Operation(summary = "根据订单编号获得物流临时订单")
    @Parameter(name = "orderNo", description = "订单编号", required = true, example = "TO202401010001")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<TemporaryOrderRespVO> getTemporaryOrderByOrderNo(@RequestParam("orderNo") String orderNo) {
        TemporaryOrderDO temporaryOrder = temporaryOrderService.getTemporaryOrderByOrderNo(orderNo);
        return success(temporaryOrder != null ? TemporaryOrderConvert.INSTANCE.convert(temporaryOrder) : null);
    }

    @GetMapping("/list-by-task-id")
    @Operation(summary = "根据任务ID获得物流临时订单列表")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<List<TemporaryOrderRespVO>> getTemporaryOrderListByTaskId(@RequestParam("taskId") Long taskId) {
        List<TemporaryOrderDO> list = temporaryOrderService.getTemporaryOrderListByTaskId(taskId);
        return success(TemporaryOrderConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-driver-id")
    @Operation(summary = "根据司机ID获得物流临时订单列表")
    @Parameter(name = "driverId", description = "司机ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<List<TemporaryOrderRespVO>> getTemporaryOrderListByDriverId(@RequestParam("driverId") Long driverId) {
        List<TemporaryOrderDO> list = temporaryOrderService.getTemporaryOrderListByDriverId(driverId);
        return success(TemporaryOrderConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-payment-status")
    @Operation(summary = "根据支付状态获得物流临时订单列表")
    @Parameter(name = "paymentStatus", description = "支付状态", required = true, example = "0")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<List<TemporaryOrderRespVO>> getTemporaryOrderListByPaymentStatus(@RequestParam("paymentStatus") Integer paymentStatus) {
        List<TemporaryOrderDO> list = temporaryOrderService.getTemporaryOrderListByPaymentStatus(paymentStatus);
        return success(TemporaryOrderConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-converted-to-formal")
    @Operation(summary = "根据是否转为正式订单获得物流临时订单列表")
    @Parameter(name = "convertedToFormal", description = "是否已转为正式订单", required = true, example = "false")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<List<TemporaryOrderRespVO>> getTemporaryOrderListByConvertedToFormal(@RequestParam("convertedToFormal") Boolean convertedToFormal) {
        List<TemporaryOrderDO> list = temporaryOrderService.getTemporaryOrderListByConvertedToFormal(convertedToFormal);
        return success(TemporaryOrderConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-waste-type")
    @Operation(summary = "根据废料类型获得物流临时订单列表")
    @Parameter(name = "wasteType", description = "废料类型", required = true, example = "HW01")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<List<TemporaryOrderRespVO>> getTemporaryOrderListByWasteType(@RequestParam("wasteType") String wasteType) {
        List<TemporaryOrderDO> list = temporaryOrderService.getTemporaryOrderListByWasteType(wasteType);
        return success(TemporaryOrderConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/count-by-task-id")
    @Operation(summary = "统计任务的临时订单数量")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<Long> getTemporaryOrderCountByTaskId(@RequestParam("taskId") Long taskId) {
        return success(temporaryOrderService.getTemporaryOrderCountByTaskId(taskId));
    }

    @GetMapping("/count-by-driver-id")
    @Operation(summary = "统计司机的临时订单数量")
    @Parameter(name = "driverId", description = "司机ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:query')")
    public CommonResult<Long> getTemporaryOrderCountByDriverId(@RequestParam("driverId") Long driverId) {
        return success(temporaryOrderService.getTemporaryOrderCountByDriverId(driverId));
    }

    // ========== 业务接口 ==========

    @PostMapping("/pay")
    @Operation(summary = "支付临时订单")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:pay')")
    public CommonResult<Boolean> payTemporaryOrder(@RequestParam("id") Long id,
                                                  @RequestParam("paymentAmount") BigDecimal paymentAmount,
                                                  @RequestParam("paymentMethod") String paymentMethod,
                                                  @RequestParam(value = "paymentVoucherUrl", required = false) String paymentVoucherUrl) {
        temporaryOrderService.payTemporaryOrder(id, paymentAmount, paymentMethod, paymentVoucherUrl);
        return success(true);
    }

    @PostMapping("/refund")
    @Operation(summary = "退款临时订单")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:refund')")
    public CommonResult<Boolean> refundTemporaryOrder(@RequestParam("id") Long id,
                                                     @RequestParam("refundReason") String refundReason) {
        temporaryOrderService.refundTemporaryOrder(id, refundReason);
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消临时订单")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:cancel')")
    public CommonResult<Boolean> cancelTemporaryOrder(@RequestParam("id") Long id,
                                                     @RequestParam("cancelReason") String cancelReason) {
        temporaryOrderService.cancelTemporaryOrder(id, cancelReason);
        return success(true);
    }

    @PostMapping("/convert-to-formal")
    @Operation(summary = "转为正式订单")
    @PreAuthorize("@ss.hasPermission('logistics:temporary-order:convert')")
    public CommonResult<Boolean> convertToFormalOrder(@RequestParam("id") Long id,
                                                     @RequestParam("formalOrderId") Long formalOrderId) {
        temporaryOrderService.convertToFormalOrder(id, formalOrderId);
        return success(true);
    }

} 