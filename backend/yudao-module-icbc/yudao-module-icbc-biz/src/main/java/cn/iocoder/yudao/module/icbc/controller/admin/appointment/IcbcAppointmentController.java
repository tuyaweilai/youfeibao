package cn.iocoder.yudao.module.icbc.controller.admin.appointment;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentArriveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentNoShowReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentRespVO;
import cn.iocoder.yudao.module.icbc.service.appointment.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 / 收货员现场端 - 到站预约（#35，ADR 0020）。
 *
 * <p>收货员登记收购时用 {@code /pending} 带出预约内容；到场 / 未到场是两个显式动作，
 * **没有**接单 / 拒单。预约不是订单，也不进任何统计与额度口径。
 */
@Tag(name = "管理后台 - 到站预约")
@RestController
@RequestMapping("/icbc/appointment")
@Validated
public class IcbcAppointmentController {

    @Resource
    private AppointmentService appointmentService;

    @GetMapping("/page")
    @Operation(summary = "获得到站预约分页")
    @PreAuthorize("@icbc.hasPermission('icbc:appointment:query')")
    public CommonResult<PageResult<AppointmentRespVO>> page(@Valid AppointmentPageReqVO pageReqVO) {
        return success(appointmentService.getPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得到站预约")
    @Parameter(name = "id", description = "预约编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:appointment:query')")
    public CommonResult<AppointmentRespVO> get(@RequestParam("id") Long id) {
        return success(appointmentService.getAppointment(id));
    }

    @GetMapping("/pending")
    @Operation(summary = "某出售者待到站的预约", description = "现场登记收购时据此带出品类、约多少与车牌")
    @Parameter(name = "payeeId", description = "收方（出售者）档案编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:appointment:query')")
    public CommonResult<List<AppointmentRespVO>> pending(@RequestParam("payeeId") Long payeeId) {
        return success(appointmentService.listPendingForPayee(payeeId));
    }

    @PostMapping("/arrive")
    @Operation(summary = "标记到场", description = "可同时挂上到场后建的收购单")
    @PreAuthorize("@icbc.hasPermission('icbc:appointment:manage')")
    public CommonResult<Boolean> arrive(@Valid @RequestBody AppointmentArriveReqVO reqVO) {
        appointmentService.markArrived(reqVO);
        return success(true);
    }

    @PostMapping("/no-show")
    @Operation(summary = "标记未到场")
    @PreAuthorize("@icbc.hasPermission('icbc:appointment:manage')")
    public CommonResult<Boolean> noShow(@Valid @RequestBody AppointmentNoShowReqVO reqVO) {
        appointmentService.markNoShow(reqVO);
        return success(true);
    }

}
