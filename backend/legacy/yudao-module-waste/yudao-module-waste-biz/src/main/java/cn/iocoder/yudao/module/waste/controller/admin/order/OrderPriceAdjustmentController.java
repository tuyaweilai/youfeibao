package cn.iocoder.yudao.module.waste.controller.admin.order;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderPriceAdjustmentDO;
import cn.iocoder.yudao.module.waste.service.order.OrderPriceAdjustmentService;
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

@Tag(name = "管理后台 - 订单价格调整记录")
@RestController
@RequestMapping("/waste/order-price-adjustment")
@Validated
public class OrderPriceAdjustmentController {

    @Resource
    private OrderPriceAdjustmentService orderPriceAdjustmentService;

    @PostMapping("/create")
    @Operation(summary = "创建订单价格调整记录")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:create')")
    public CommonResult<Long> createOrderPriceAdjustment(@Valid @RequestBody OrderPriceAdjustmentCreateReqVO createReqVO) {
        return success(orderPriceAdjustmentService.createOrderPriceAdjustment(createReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除订单价格调整记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:delete')")
    public CommonResult<Boolean> deleteOrderPriceAdjustment(@RequestParam("id") Long id) {
        orderPriceAdjustmentService.deleteOrderPriceAdjustment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得订单价格调整记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:query')")
    public CommonResult<OrderPriceAdjustmentRespVO> getOrderPriceAdjustment(@RequestParam("id") Long id) {
        OrderPriceAdjustmentDO orderPriceAdjustment = orderPriceAdjustmentService.getOrderPriceAdjustment(id);
        return success(BeanUtils.toBean(orderPriceAdjustment, OrderPriceAdjustmentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得订单价格调整记录分页")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:query')")
    public CommonResult<PageResult<OrderPriceAdjustmentRespVO>> getOrderPriceAdjustmentPage(@Valid OrderPriceAdjustmentPageReqVO pageReqVO) {
        PageResult<OrderPriceAdjustmentDO> pageResult = orderPriceAdjustmentService.getOrderPriceAdjustmentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderPriceAdjustmentRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出订单价格调整记录 Excel")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderPriceAdjustmentExcel(@Valid OrderPriceAdjustmentPageReqVO pageReqVO,
                                                HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderPriceAdjustmentDO> list = orderPriceAdjustmentService.getOrderPriceAdjustmentPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "订单价格调整记录.xls", "数据", OrderPriceAdjustmentRespVO.class,
                BeanUtils.toBean(list, OrderPriceAdjustmentRespVO.class));
    }

    // ==================== 业务方法 ====================

    @PostMapping("/apply-adjustment")
    @Operation(summary = "申请价格调整")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:apply')")
    public CommonResult<Boolean> applyPriceAdjustment(@RequestParam("orderId") Long orderId,
                                                     @RequestParam("adjustmentType") Integer adjustmentType,
                                                     @RequestParam("adjustmentAmount") BigDecimal adjustmentAmount,
                                                     @RequestParam("adjustmentReason") String adjustmentReason) {
        orderPriceAdjustmentService.applyPriceAdjustment(orderId, adjustmentType, adjustmentAmount, adjustmentReason);
        return success(true);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批价格调整")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:approve')")
    public CommonResult<Boolean> approveAdjustment(@RequestParam("id") Long id,
                                                   @RequestParam("approved") Boolean approved,
                                                   @RequestParam("approvalComments") String approvalComments) {
        orderPriceAdjustmentService.approveAdjustment(id, approved, approvalComments);
        return success(true);
    }

    @PostMapping("/execute")
    @Operation(summary = "执行价格调整")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:execute')")
    public CommonResult<Boolean> executeAdjustment(@RequestParam("id") Long id) {
        orderPriceAdjustmentService.executeAdjustment(id);
        return success(true);
    }

    @GetMapping("/by-order")
    @Operation(summary = "根据订单ID获取价格调整记录")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:query')")
    public CommonResult<List<OrderPriceAdjustmentRespVO>> getAdjustmentsByOrderId(@RequestParam("orderId") Long orderId) {
        List<OrderPriceAdjustmentDO> list = orderPriceAdjustmentService.getAdjustmentsByOrderId(orderId);
        return success(BeanUtils.toBean(list, OrderPriceAdjustmentRespVO.class));
    }

    @GetMapping("/pending-approval")
    @Operation(summary = "获取待审批的价格调整记录")
    @PreAuthorize("@ss.hasPermission('waste:order-price-adjustment:query')")
    public CommonResult<List<OrderPriceAdjustmentRespVO>> getPendingApprovalAdjustments() {
        List<OrderPriceAdjustmentDO> list = orderPriceAdjustmentService.getPendingApprovalAdjustments();
        return success(BeanUtils.toBean(list, OrderPriceAdjustmentRespVO.class));
    }

} 