package cn.iocoder.yudao.module.waste.controller.admin.payment;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.PaymentStatusHistoryDO;
import cn.iocoder.yudao.module.waste.service.payment.PaymentStatusHistoryService;
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

@Tag(name = "管理后台 - 付款状态变更历史")
@RestController
@RequestMapping("/waste/payment-status-history")
@Validated
public class PaymentStatusHistoryController {

    @Resource
    private PaymentStatusHistoryService paymentStatusHistoryService;

    @PostMapping("/create")
    @Operation(summary = "创建付款状态变更历史")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:create')")
    public CommonResult<Long> createPaymentStatusHistory(@Valid @RequestBody PaymentStatusHistoryCreateReqVO createReqVO) {
        return success(paymentStatusHistoryService.createPaymentStatusHistory(createReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除付款状态变更历史")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:delete')")
    public CommonResult<Boolean> deletePaymentStatusHistory(@RequestParam("id") Long id) {
        paymentStatusHistoryService.deletePaymentStatusHistory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得付款状态变更历史")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:query')")
    public CommonResult<PaymentStatusHistoryRespVO> getPaymentStatusHistory(@RequestParam("id") Long id) {
        PaymentStatusHistoryDO paymentStatusHistory = paymentStatusHistoryService.getPaymentStatusHistory(id);
        return success(BeanUtils.toBean(paymentStatusHistory, PaymentStatusHistoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得付款状态变更历史分页")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:query')")
    public CommonResult<PageResult<PaymentStatusHistoryRespVO>> getPaymentStatusHistoryPage(@Valid PaymentStatusHistoryPageReqVO pageReqVO) {
        PageResult<PaymentStatusHistoryDO> pageResult = paymentStatusHistoryService.getPaymentStatusHistoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, PaymentStatusHistoryRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出付款状态变更历史 Excel")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPaymentStatusHistoryExcel(@Valid PaymentStatusHistoryPageReqVO pageReqVO,
                                                HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PaymentStatusHistoryDO> list = paymentStatusHistoryService.getPaymentStatusHistoryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "付款状态变更历史.xls", "数据", PaymentStatusHistoryRespVO.class,
                BeanUtils.toBean(list, PaymentStatusHistoryRespVO.class));
    }

    // ==================== 业务方法 ====================

    @PostMapping("/record-status-change")
    @Operation(summary = "记录付款状态变更")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:create')")
    public CommonResult<Boolean> recordStatusChange(@RequestParam("orderId") Long orderId,
                                                    @RequestParam("paymentRecordId") Long paymentRecordId,
                                                    @RequestParam("fromStatus") Integer fromStatus,
                                                    @RequestParam("toStatus") Integer toStatus,
                                                    @RequestParam("changeReason") String changeReason) {
        paymentStatusHistoryService.recordStatusChange(orderId, paymentRecordId, fromStatus, toStatus, changeReason);
        return success(true);
    }

    @PostMapping("/record-system-change")
    @Operation(summary = "记录系统自动状态变更")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:create')")
    public CommonResult<Boolean> recordSystemChange(@RequestParam("orderId") Long orderId,
                                                    @RequestParam("paymentRecordId") Long paymentRecordId,
                                                    @RequestParam("fromStatus") Integer fromStatus,
                                                    @RequestParam("toStatus") Integer toStatus,
                                                    @RequestParam("changeReason") String changeReason,
                                                    @RequestParam("businessData") String businessData) {
        paymentStatusHistoryService.recordSystemChange(orderId, paymentRecordId, fromStatus, toStatus, changeReason, businessData);
        return success(true);
    }

    @GetMapping("/by-order")
    @Operation(summary = "根据订单ID获取付款状态变更历史")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:query')")
    public CommonResult<List<PaymentStatusHistoryRespVO>> getHistoryByOrderId(@RequestParam("orderId") Long orderId) {
        List<PaymentStatusHistoryDO> list = paymentStatusHistoryService.getHistoryByOrderId(orderId);
        return success(BeanUtils.toBean(list, PaymentStatusHistoryRespVO.class));
    }

    @GetMapping("/by-payment-record")
    @Operation(summary = "根据付款记录ID获取状态变更历史")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:query')")
    public CommonResult<List<PaymentStatusHistoryRespVO>> getHistoryByPaymentRecordId(@RequestParam("paymentRecordId") Long paymentRecordId) {
        List<PaymentStatusHistoryDO> list = paymentStatusHistoryService.getHistoryByPaymentRecordId(paymentRecordId);
        return success(BeanUtils.toBean(list, PaymentStatusHistoryRespVO.class));
    }

    @GetMapping("/by-operator")
    @Operation(summary = "根据操作人获取状态变更历史")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:query')")
    public CommonResult<List<PaymentStatusHistoryRespVO>> getHistoryByOperator(@RequestParam("operatorId") Long operatorId) {
        List<PaymentStatusHistoryDO> list = paymentStatusHistoryService.getHistoryByOperator(operatorId);
        return success(BeanUtils.toBean(list, PaymentStatusHistoryRespVO.class));
    }

    @GetMapping("/system-changes")
    @Operation(summary = "获取系统自动变更记录")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:query')")
    public CommonResult<List<PaymentStatusHistoryRespVO>> getSystemChanges() {
        List<PaymentStatusHistoryDO> list = paymentStatusHistoryService.getSystemChanges();
        return success(BeanUtils.toBean(list, PaymentStatusHistoryRespVO.class));
    }

    @GetMapping("/manual-changes")
    @Operation(summary = "获取人工变更记录")
    @PreAuthorize("@ss.hasPermission('waste:payment-status-history:query')")
    public CommonResult<List<PaymentStatusHistoryRespVO>> getManualChanges() {
        List<PaymentStatusHistoryDO> list = paymentStatusHistoryService.getManualChanges();
        return success(BeanUtils.toBean(list, PaymentStatusHistoryRespVO.class));
    }

} 