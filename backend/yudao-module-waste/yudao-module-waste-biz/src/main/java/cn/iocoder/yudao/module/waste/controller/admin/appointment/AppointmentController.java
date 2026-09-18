package cn.iocoder.yudao.module.waste.controller.admin.appointment;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.*;
import cn.iocoder.yudao.module.waste.convert.appointment.AppointmentConvert;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.service.appointment.AppointmentService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

@Tag(name = "管理后台 - 危废转移预约")
@RestController
@RequestMapping("/waste/transfer/appointment")
@Validated
public class AppointmentController {

    @Resource
    private AppointmentService appointmentService;

    @PostMapping("/create")
    @Operation(summary = "创建危废转移预约")
    @PreAuthorize("@ss.hasPermission('waste:appointment:create')")
    public CommonResult<Long> createAppointment(@Valid @RequestBody AppointmentCreateReqVO createReqVO) {
        return success(appointmentService.createAppointment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新危废转移预约")
    @PreAuthorize("@ss.hasPermission('waste:appointment:update')")
    public CommonResult<Boolean> updateAppointment(@Valid @RequestBody AppointmentUpdateReqVO updateReqVO) {
        appointmentService.updateAppointment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除危废转移预约")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:appointment:delete')")
    public CommonResult<Boolean> deleteAppointment(@RequestParam("id") Long id) {
        appointmentService.deleteAppointment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得危废转移预约")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:appointment:query')")
    public CommonResult<AppointmentRespVO> getAppointment(@RequestParam("id") Long id) {
        return success(appointmentService.getAppointmentDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得危废转移预约分页")
    @PreAuthorize("@ss.hasPermission('waste:appointment:query')")
    public CommonResult<PageResult<AppointmentRespVO>> getAppointmentPage(@Valid AppointmentPageReqVO pageReqVO) {
        return success(appointmentService.getAppointmentPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出危废转移预约 Excel")
    @PreAuthorize("@ss.hasPermission('waste:appointment:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAppointmentExcel(@Valid AppointmentPageReqVO pageReqVO,
                                       HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AppointmentDO> list = appointmentService.getAppointmentList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "危废转移预约.xls", "数据", AppointmentExcelVO.class,
                AppointmentConvert.INSTANCE.convertExcelList(list));
    }

    @GetMapping("/get-by-no")
    @Operation(summary = "根据预约单号获得预约信息")
    @Parameter(name = "appointmentNo", description = "预约单号", required = true, example = "AP202412010001")
    @PreAuthorize("@ss.hasPermission('waste:appointment:query')")
    public CommonResult<AppointmentRespVO> getAppointmentByNo(@RequestParam("appointmentNo") String appointmentNo) {
        AppointmentDO appointment = appointmentService.getAppointmentByNo(appointmentNo);
        return success(appointment != null ? AppointmentConvert.INSTANCE.convert(appointment) : null);
    }

    @GetMapping("/list-by-producer")
    @Operation(summary = "根据产废企业ID获得预约列表")
    @Parameter(name = "producerEnterpriseId", description = "产废企业ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:appointment:query')")
    public CommonResult<List<AppointmentRespVO>> getAppointmentListByProducerEnterpriseId(
            @RequestParam("producerEnterpriseId") Long producerEnterpriseId) {
        List<AppointmentDO> list = appointmentService.getAppointmentListByProducerEnterpriseId(producerEnterpriseId);
        return success(AppointmentConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-recycler")
    @Operation(summary = "根据回收企业ID获得预约列表")
    @Parameter(name = "recyclerEnterpriseId", description = "回收企业ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('waste:appointment:query')")
    public CommonResult<List<AppointmentRespVO>> getAppointmentListByRecyclerEnterpriseId(
            @RequestParam("recyclerEnterpriseId") Long recyclerEnterpriseId) {
        List<AppointmentDO> list = appointmentService.getAppointmentListByRecyclerEnterpriseId(recyclerEnterpriseId);
        return success(AppointmentConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-status")
    @Operation(summary = "根据状态获得预约列表")
    @Parameter(name = "status", description = "预约状态", required = true, example = "0")
    @PreAuthorize("@ss.hasPermission('waste:appointment:query')")
    public CommonResult<List<AppointmentRespVO>> getAppointmentListByStatus(@RequestParam("status") Integer status) {
        List<AppointmentDO> list = appointmentService.getAppointmentListByStatus(status);
        return success(AppointmentConvert.INSTANCE.convertList(list));
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认预约")
    @PreAuthorize("@ss.hasPermission('waste:appointment:confirm')")
    public CommonResult<Boolean> confirmAppointment(@RequestParam("id") Long id,
                                                     @RequestParam(value = "confirmReason", required = false) String confirmReason) {
        appointmentService.confirmAppointment(id, confirmReason);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "拒绝预约")
    @PreAuthorize("@ss.hasPermission('waste:appointment:reject')")
    public CommonResult<Boolean> rejectAppointment(@RequestParam("id") Long id,
                                                    @RequestParam("rejectReason") String rejectReason) {
        appointmentService.rejectAppointment(id, rejectReason);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消预约")
    @PreAuthorize("@ss.hasPermission('waste:appointment:cancel')")
    public CommonResult<Boolean> cancelAppointment(@RequestParam("id") Long id,
                                                    @RequestParam("cancelReason") String cancelReason) {
        appointmentService.cancelAppointment(id, cancelReason);
        return success(true);
    }

    @PutMapping("/assign-recycler")
    @Operation(summary = "分配回收企业")
    @PreAuthorize("@ss.hasPermission('waste:appointment:assign')")
    public CommonResult<Boolean> assignRecyclerEnterprise(@RequestParam("id") Long id,
                                                           @RequestParam("recyclerEnterpriseId") Long recyclerEnterpriseId,
                                                           @RequestParam(value = "assignmentType", defaultValue = "1") Integer assignmentType,
                                                           @RequestParam(value = "operator", required = false) String operator) {
        appointmentService.assignRecyclerEnterprise(id, recyclerEnterpriseId, assignmentType, operator);
        return success(true);
    }

    @PutMapping("/auto-assign-recycler")
    @Operation(summary = "自动分配回收企业")
    @PreAuthorize("@ss.hasPermission('waste:appointment:assign')")
    public CommonResult<Boolean> autoAssignRecyclerEnterprise(@RequestParam("id") Long id) {
        appointmentService.autoAssignRecyclerEnterprise(id);
        return success(true);
    }

    @PostMapping("/generate-order")
    @Operation(summary = "生成订单")
    @PreAuthorize("@ss.hasPermission('waste:appointment:generate-order')")
    public CommonResult<Long> generateOrder(@RequestParam("id") Long id) {
        return success(appointmentService.generateOrder(id));
    }

} 