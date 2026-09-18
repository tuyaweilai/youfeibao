package cn.iocoder.yudao.module.waste.controller.admin.quotation;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import cn.iocoder.yudao.module.waste.service.quotation.AppointmentQuotationService;
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
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 预约报价记录")
@RestController
@RequestMapping("/waste/appointment-quotation")
@Validated
public class AppointmentQuotationController {

    @Resource
    private AppointmentQuotationService appointmentQuotationService;

    @PostMapping("/create")
    @Operation(summary = "创建预约报价记录")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:create')")
    public CommonResult<Long> createAppointmentQuotation(@Valid @RequestBody AppointmentQuotationCreateReqVO createReqVO) {
        return success(appointmentQuotationService.createAppointmentQuotation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新预约报价记录")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:update')")
    public CommonResult<Boolean> updateAppointmentQuotation(@Valid @RequestBody AppointmentQuotationUpdateReqVO updateReqVO) {
        appointmentQuotationService.updateAppointmentQuotation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除预约报价记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:delete')")
    public CommonResult<Boolean> deleteAppointmentQuotation(@RequestParam("id") Long id) {
        appointmentQuotationService.deleteAppointmentQuotation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得预约报价记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:query')")
    public CommonResult<AppointmentQuotationRespVO> getAppointmentQuotation(@RequestParam("id") Long id) {
        AppointmentQuotationDO appointmentQuotation = appointmentQuotationService.getAppointmentQuotation(id);
        return success(BeanUtils.toBean(appointmentQuotation, AppointmentQuotationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得预约报价记录分页")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:query')")
    public CommonResult<PageResult<AppointmentQuotationRespVO>> getAppointmentQuotationPage(@Valid AppointmentQuotationPageReqVO pageReqVO) {
        PageResult<AppointmentQuotationRespVO> pageResult = appointmentQuotationService.getAppointmentQuotationPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出预约报价记录 Excel")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAppointmentQuotationExcel(@Valid AppointmentQuotationPageReqVO pageReqVO,
                                                HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AppointmentQuotationRespVO> list = appointmentQuotationService.getAppointmentQuotationPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "预约报价记录.xls", "数据", AppointmentQuotationRespVO.class, list);
    }

    // ==================== 业务方法 ====================

    @PostMapping("/submit-quotation")
    @Operation(summary = "提交报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:submit')")
    public CommonResult<Boolean> submitQuotation(@RequestParam("appointmentId") Long appointmentId,
                                                 @RequestParam("recyclerEnterpriseId") Long recyclerEnterpriseId,
                                                 @RequestParam("quotedPrice") BigDecimal quotedPrice,
                                                 @RequestParam("quotationNotes") String quotationNotes) {
        appointmentQuotationService.submitQuotation(appointmentId, recyclerEnterpriseId, quotedPrice, quotationNotes);
        return success(true);
    }

    @PostMapping("/accept-quotation")
    @Operation(summary = "接受报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:accept')")
    public CommonResult<Boolean> acceptQuotation(@RequestParam("id") Long id) {
        appointmentQuotationService.acceptQuotation(id);
        return success(true);
    }

    @PostMapping("/reject-quotation")
    @Operation(summary = "拒绝报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:reject')")
    public CommonResult<Boolean> rejectQuotation(@RequestParam("id") Long id,
                                                 @RequestParam("rejectionReason") String rejectionReason) {
        appointmentQuotationService.rejectQuotation(id, rejectionReason);
        return success(true);
    }

    @PostMapping("/withdraw-quotation")
    @Operation(summary = "撤回报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:withdraw')")
    public CommonResult<Boolean> withdrawQuotation(@RequestParam("id") Long id,
                                                   @RequestParam("withdrawalReason") String withdrawalReason) {
        appointmentQuotationService.withdrawQuotation(id, withdrawalReason);
        return success(true);
    }

    @GetMapping("/by-appointment")
    @Operation(summary = "根据预约单ID获取报价记录")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:query')")
    public CommonResult<List<AppointmentQuotationRespVO>> getQuotationsByAppointmentId(@RequestParam("appointmentId") Long appointmentId) {
        List<AppointmentQuotationDO> list = appointmentQuotationService.getQuotationsByAppointmentId(appointmentId);
        return success(BeanUtils.toBean(list, AppointmentQuotationRespVO.class));
    }

    @GetMapping("/by-recycler")
    @Operation(summary = "根据回收企业ID获取报价记录")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:query')")
    public CommonResult<List<AppointmentQuotationRespVO>> getQuotationsByRecyclerEnterpriseId(@RequestParam("recyclerEnterpriseId") Long recyclerEnterpriseId) {
        List<AppointmentQuotationDO> list = appointmentQuotationService.getQuotationsByRecyclerEnterpriseId(recyclerEnterpriseId);
        return success(BeanUtils.toBean(list, AppointmentQuotationRespVO.class));
    }

    @GetMapping("/valid-quotations")
    @Operation(summary = "获取有效报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:query')")
    public CommonResult<List<AppointmentQuotationRespVO>> getValidQuotations(@RequestParam("appointmentId") Long appointmentId) {
        List<AppointmentQuotationDO> list = appointmentQuotationService.getValidQuotations(appointmentId);
        return success(BeanUtils.toBean(list, AppointmentQuotationRespVO.class));
    }

    @GetMapping("/expired-quotations")
    @Operation(summary = "获取过期报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:query')")
    public CommonResult<List<AppointmentQuotationRespVO>> getExpiredQuotations(@RequestParam("appointmentId") Long appointmentId) {
        List<AppointmentQuotationDO> list = appointmentQuotationService.getExpiredQuotations(appointmentId);
        return success(BeanUtils.toBean(list, AppointmentQuotationRespVO.class));
    }

    @GetMapping("/accepted-quotations")
    @Operation(summary = "获取已接受报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:query')")
    public CommonResult<List<AppointmentQuotationRespVO>> getAcceptedQuotations(@RequestParam("appointmentId") Long appointmentId) {
        List<AppointmentQuotationDO> list = appointmentQuotationService.getAcceptedQuotations(appointmentId);
        return success(BeanUtils.toBean(list, AppointmentQuotationRespVO.class));
    }

    // ==================== 批量操作方法 ====================

    @PostMapping("/batch-accept")
    @Operation(summary = "批量接受报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:accept')")
    public CommonResult<Boolean> batchAcceptQuotations(@RequestParam("ids") List<Long> ids,
                                                       @RequestParam(value = "acceptReason", required = false) String acceptReason) {
        for (Long id : ids) {
            appointmentQuotationService.acceptQuotation(id);
        }
        return success(true);
    }

    @PostMapping("/batch-reject")
    @Operation(summary = "批量拒绝报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:reject')")
    public CommonResult<Boolean> batchRejectQuotations(@RequestParam("ids") List<Long> ids,
                                                       @RequestParam("rejectionReason") String rejectionReason) {
        for (Long id : ids) {
            appointmentQuotationService.rejectQuotation(id, rejectionReason);
        }
        return success(true);
    }

    @PostMapping("/batch-expire")
    @Operation(summary = "批量过期报价")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:update')")
    public CommonResult<Boolean> batchExpireQuotations(@RequestParam("ids") List<Long> ids) {
        appointmentQuotationService.batchExpireQuotations(ids);
        return success(true);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取报价统计信息")
    @PreAuthorize("@ss.hasPermission('waste:appointment-quotation:query')")
    public CommonResult<AppointmentQuotationStatisticsVO> getQuotationStatistics(@RequestParam("appointmentId") Long appointmentId) {
        List<AppointmentQuotationDO> quotations = appointmentQuotationService.getQuotationsByAppointmentId(appointmentId);
        
        AppointmentQuotationStatisticsVO statistics = new AppointmentQuotationStatisticsVO();
        statistics.setTotalCount(quotations.size());
        statistics.setValidCount((int) quotations.stream().filter(q -> q.getStatus() == 0 && q.getValidUntil().isAfter(LocalDateTime.now())).count());
        statistics.setAcceptedCount((int) quotations.stream().filter(q -> q.getStatus() == 1).count());
        statistics.setRejectedCount((int) quotations.stream().filter(q -> q.getStatus() == 2).count());
        statistics.setExpiredCount((int) quotations.stream().filter(q -> q.getStatus() == 0 && q.getValidUntil().isBefore(LocalDateTime.now())).count());
        
        if (!quotations.isEmpty()) {
            BigDecimal minPrice = quotations.stream().map(AppointmentQuotationDO::getQuotedPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxPrice = quotations.stream().map(AppointmentQuotationDO::getQuotedPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal avgPrice = quotations.stream().map(AppointmentQuotationDO::getQuotedPrice).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(quotations.size()), 2, BigDecimal.ROUND_HALF_UP);
            
            statistics.setMinPrice(minPrice);
            statistics.setMaxPrice(maxPrice);
            statistics.setAvgPrice(avgPrice);
        }
        
        return success(statistics);
    }

} 