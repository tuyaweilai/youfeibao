package cn.iocoder.yudao.module.waste.controller.admin.order;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.TransferOrderDO;
import cn.iocoder.yudao.module.waste.service.order.TransferOrderService;
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

@Tag(name = "管理后台 - 危废转移订单")
@RestController
@RequestMapping("/waste/transfer-order")
@Validated
public class TransferOrderController {

    @Resource
    private TransferOrderService transferOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建危废转移订单")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:create')")
    public CommonResult<Long> createTransferOrder(@Valid @RequestBody TransferOrderCreateReqVO createReqVO) {
        return success(transferOrderService.createTransferOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新危废转移订单")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:update')")
    public CommonResult<Boolean> updateTransferOrder(@Valid @RequestBody TransferOrderUpdateReqVO updateReqVO) {
        transferOrderService.updateTransferOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除危废转移订单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:delete')")
    public CommonResult<Boolean> deleteTransferOrder(@RequestParam("id") Long id) {
        transferOrderService.deleteTransferOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得危废转移订单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:query')")
    public CommonResult<TransferOrderRespVO> getTransferOrder(@RequestParam("id") Long id) {
        TransferOrderDO transferOrder = transferOrderService.getTransferOrder(id);
        return success(BeanUtils.toBean(transferOrder, TransferOrderRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得危废转移订单分页")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:query')")
    public CommonResult<PageResult<TransferOrderRespVO>> getTransferOrderPage(@Valid TransferOrderPageReqVO pageReqVO) {
        PageResult<TransferOrderDO> pageResult = transferOrderService.getTransferOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TransferOrderRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出危废转移订单 Excel")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTransferOrderExcel(@Valid TransferOrderPageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TransferOrderDO> list = transferOrderService.getTransferOrderPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "危废转移订单.xls", "数据", TransferOrderRespVO.class,
                BeanUtils.toBean(list, TransferOrderRespVO.class));
    }

    // ==================== 业务方法 ====================

    @PutMapping("/confirm")
    @Operation(summary = "确认订单")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:confirm')")
    public CommonResult<Boolean> confirmOrder(@RequestParam("id") Long id) {
        transferOrderService.confirmOrder(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:cancel')")
    public CommonResult<Boolean> cancelOrder(@RequestParam("id") Long id, 
                                            @RequestParam("reason") String reason) {
        transferOrderService.cancelOrder(id, reason);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成订单")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:complete')")
    public CommonResult<Boolean> completeOrder(@RequestParam("id") Long id) {
        transferOrderService.completeOrder(id);
        return success(true);
    }

    @GetMapping("/pending-allocation")
    @Operation(summary = "获取待分摊订单列表")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:query')")
    public CommonResult<List<TransferOrderRespVO>> getPendingAllocationOrders() {
        List<TransferOrderDO> list = transferOrderService.getPendingAllocationOrders();
        return success(BeanUtils.toBean(list, TransferOrderRespVO.class));
    }

    @GetMapping("/pending-payment")
    @Operation(summary = "获取待付款订单列表")
    @PreAuthorize("@ss.hasPermission('waste:transfer-order:query')")
    public CommonResult<List<TransferOrderRespVO>> getPendingPaymentOrders() {
        List<TransferOrderDO> list = transferOrderService.getPendingPaymentOrders();
        return success(BeanUtils.toBean(list, TransferOrderRespVO.class));
    }

} 