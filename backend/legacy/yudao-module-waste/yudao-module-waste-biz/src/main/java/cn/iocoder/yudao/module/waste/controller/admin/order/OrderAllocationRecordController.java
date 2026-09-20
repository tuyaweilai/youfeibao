package cn.iocoder.yudao.module.waste.controller.admin.order;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderAllocationRecordDO;
import cn.iocoder.yudao.module.waste.service.order.OrderAllocationRecordService;
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

@Tag(name = "管理后台 - 订单过磅分摊记录")
@RestController
@RequestMapping("/waste/order-allocation-record")
@Validated
public class OrderAllocationRecordController {

    @Resource
    private OrderAllocationRecordService orderAllocationRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建订单过磅分摊记录")
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:create')")
    public CommonResult<Long> createOrderAllocationRecord(@Valid @RequestBody OrderAllocationRecordCreateReqVO createReqVO) {
        return success(orderAllocationRecordService.createOrderAllocationRecord(createReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除订单过磅分摊记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:delete')")
    public CommonResult<Boolean> deleteOrderAllocationRecord(@RequestParam("id") Long id) {
        orderAllocationRecordService.deleteOrderAllocationRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得订单过磅分摊记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:query')")
    public CommonResult<OrderAllocationRecordRespVO> getOrderAllocationRecord(@RequestParam("id") Long id) {
        OrderAllocationRecordDO orderAllocationRecord = orderAllocationRecordService.getOrderAllocationRecord(id);
        return success(BeanUtils.toBean(orderAllocationRecord, OrderAllocationRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得订单过磅分摊记录分页")
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:query')")
    public CommonResult<PageResult<OrderAllocationRecordRespVO>> getOrderAllocationRecordPage(@Valid OrderAllocationRecordPageReqVO pageReqVO) {
        PageResult<OrderAllocationRecordDO> pageResult = orderAllocationRecordService.getOrderAllocationRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderAllocationRecordRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出订单过磅分摊记录 Excel")
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderAllocationRecordExcel(@Valid OrderAllocationRecordPageReqVO pageReqVO,
                                                  HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderAllocationRecordDO> list = orderAllocationRecordService.getOrderAllocationRecordPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "订单过磅分摊记录.xls", "数据", OrderAllocationRecordRespVO.class,
                BeanUtils.toBean(list, OrderAllocationRecordRespVO.class));
    }

    // ==================== 业务方法 ====================

    @PostMapping("/execute-auto-allocation")
    @Operation(summary = "执行自动分摊")
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:allocate')")
    public CommonResult<Boolean> executeAutoAllocation(@RequestParam("orderId") Long orderId,
                                                       @RequestParam("weighingRecordId") Long weighingRecordId) {
        orderAllocationRecordService.executeAutoAllocation(orderId, weighingRecordId);
        return success(true);
    }

    @PostMapping("/manual-adjustment")
    @Operation(summary = "人工调整分摊")
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:adjust')")
    public CommonResult<Boolean> manualAdjustment(@RequestParam("id") Long id,
                                                  @RequestParam("adjustedQuantity") String adjustedQuantity,
                                                  @RequestParam("adjustedAmount") String adjustedAmount,
                                                  @RequestParam("adjustmentReason") String adjustmentReason) {
        orderAllocationRecordService.manualAdjustment(id, adjustedQuantity, adjustedAmount, adjustmentReason);
        return success(true);
    }

    @GetMapping("/by-order")
    @Operation(summary = "根据订单ID获取分摊记录")
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:query')")
    public CommonResult<List<OrderAllocationRecordRespVO>> getRecordsByOrderId(@RequestParam("orderId") Long orderId) {
        List<OrderAllocationRecordDO> list = orderAllocationRecordService.getRecordsByOrderId(orderId);
        return success(BeanUtils.toBean(list, OrderAllocationRecordRespVO.class));
    }

    @GetMapping("/by-weighing-record")
    @Operation(summary = "根据车辆过磅ID获取分摊记录")
    @PreAuthorize("@ss.hasPermission('waste:order-allocation-record:query')")
    public CommonResult<List<OrderAllocationRecordRespVO>> getRecordsByWeighingRecordId(@RequestParam("weighingRecordId") Long weighingRecordId) {
        List<OrderAllocationRecordDO> list = orderAllocationRecordService.getRecordsByWeighingRecordId(weighingRecordId);
        return success(BeanUtils.toBean(list, OrderAllocationRecordRespVO.class));
    }

} 