package cn.iocoder.yudao.module.waste.controller.admin.order;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderStatusHistoryDO;
import cn.iocoder.yudao.module.waste.service.order.OrderStatusHistoryService;
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

@Tag(name = "管理后台 - 订单状态变更历史")
@RestController
@RequestMapping("/waste/order-status-history")
@Validated
public class OrderStatusHistoryController {

    @Resource
    private OrderStatusHistoryService orderStatusHistoryService;

    @PostMapping("/create")
    @Operation(summary = "创建订单状态变更历史")
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:create')")
    public CommonResult<Long> createOrderStatusHistory(@Valid @RequestBody OrderStatusHistoryCreateReqVO createReqVO) {
        return success(orderStatusHistoryService.createOrderStatusHistory(createReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除订单状态变更历史")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:delete')")
    public CommonResult<Boolean> deleteOrderStatusHistory(@RequestParam("id") Long id) {
        orderStatusHistoryService.deleteOrderStatusHistory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得订单状态变更历史")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:query')")
    public CommonResult<OrderStatusHistoryRespVO> getOrderStatusHistory(@RequestParam("id") Long id) {
        OrderStatusHistoryDO orderStatusHistory = orderStatusHistoryService.getOrderStatusHistory(id);
        return success(BeanUtils.toBean(orderStatusHistory, OrderStatusHistoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得订单状态变更历史分页")
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:query')")
    public CommonResult<PageResult<OrderStatusHistoryRespVO>> getOrderStatusHistoryPage(@Valid OrderStatusHistoryPageReqVO pageReqVO) {
        PageResult<OrderStatusHistoryDO> pageResult = orderStatusHistoryService.getOrderStatusHistoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OrderStatusHistoryRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出订单状态变更历史 Excel")
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderStatusHistoryExcel(@Valid OrderStatusHistoryPageReqVO pageReqVO,
                                              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderStatusHistoryDO> list = orderStatusHistoryService.getOrderStatusHistoryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "订单状态变更历史.xls", "数据", OrderStatusHistoryRespVO.class,
                BeanUtils.toBean(list, OrderStatusHistoryRespVO.class));
    }

    // ==================== 业务方法 ====================

    @PostMapping("/record-status-change")
    @Operation(summary = "记录状态变更")
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:create')")
    public CommonResult<Boolean> recordStatusChange(@RequestParam("orderId") Long orderId,
                                                    @RequestParam("fromStatus") Integer fromStatus,
                                                    @RequestParam("toStatus") Integer toStatus,
                                                    @RequestParam("changeReason") String changeReason) {
        orderStatusHistoryService.recordStatusChange(orderId, fromStatus, toStatus, changeReason);
        return success(true);
    }

    @PostMapping("/record-milestone")
    @Operation(summary = "记录里程碑状态")
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:create')")
    public CommonResult<Boolean> recordMilestone(@RequestParam("orderId") Long orderId,
                                                 @RequestParam("status") Integer status,
                                                 @RequestParam("milestone") String milestone) {
        orderStatusHistoryService.recordMilestone(orderId, status, milestone);
        return success(true);
    }

    @GetMapping("/by-order")
    @Operation(summary = "根据订单ID获取状态变更历史")
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:query')")
    public CommonResult<List<OrderStatusHistoryRespVO>> getHistoryByOrderId(@RequestParam("orderId") Long orderId) {
        List<OrderStatusHistoryDO> list = orderStatusHistoryService.getHistoryByOrderId(orderId);
        return success(BeanUtils.toBean(list, OrderStatusHistoryRespVO.class));
    }

    @GetMapping("/milestones")
    @Operation(summary = "获取订单里程碑状态")
    @PreAuthorize("@ss.hasPermission('waste:order-status-history:query')")
    public CommonResult<List<OrderStatusHistoryRespVO>> getMilestonesByOrderId(@RequestParam("orderId") Long orderId) {
        List<OrderStatusHistoryDO> list = orderStatusHistoryService.getMilestonesByOrderId(orderId);
        return success(BeanUtils.toBean(list, OrderStatusHistoryRespVO.class));
    }

} 