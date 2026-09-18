package cn.iocoder.yudao.module.waste.controller.app.appointment;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.app.appointment.vo.*;
import cn.iocoder.yudao.module.waste.service.appointment.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 APP - 危废转移预约")
@RestController
@RequestMapping("/waste/app/appointment")
@Validated
@Slf4j
public class AppAppointmentController {

    @Resource
    private AppointmentService appointmentService;

    @PostMapping("/create")
    @Operation(summary = "创建危废转移预约")
    @PreAuthorize("hasRole('USER')")
    public CommonResult<Long> createAppointment(@Valid @RequestBody AppAppointmentCreateReqVO createReqVO) {
        // TODO: 从登录用户信息中获取产废企业ID，这里暂时使用固定值
        // 实际应该从用户的企业关联信息中获取
        Long producerEnterpriseId = getProducerEnterpriseIdFromUser();
        
        Long appointmentId = appointmentService.createAppointmentByApp(createReqVO, producerEnterpriseId);
        return success(appointmentId);
    }

    @GetMapping("/page")
    @Operation(summary = "获得我的预约分页")
    @PreAuthorize("hasRole('USER')")
    public CommonResult<PageResult<AppAppointmentRespVO>> getMyAppointmentPage(@Valid AppAppointmentPageReqVO pageReqVO) {
        // TODO: 从登录用户信息中获取产废企业ID
        Long producerEnterpriseId = getProducerEnterpriseIdFromUser();
        
        PageResult<AppAppointmentRespVO> pageResult = appointmentService.getMyAppointmentPage(pageReqVO, producerEnterpriseId);
        return success(pageResult);
    }

    @GetMapping("/get")
    @Operation(summary = "获得预约详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("hasRole('USER')")
    public CommonResult<AppAppointmentRespVO> getMyAppointment(@RequestParam("id") Long id) {
        // TODO: 从登录用户信息中获取产废企业ID
        Long producerEnterpriseId = getProducerEnterpriseIdFromUser();
        
        AppAppointmentRespVO appointment = appointmentService.getMyAppointmentDetail(id, producerEnterpriseId);
        return success(appointment);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消预约")
    @PreAuthorize("hasRole('USER')")
    public CommonResult<Boolean> cancelMyAppointment(@RequestParam("id") Long id,
                                                     @RequestParam(value = "cancelReason", required = false) String cancelReason) {
        // TODO: 从登录用户信息中获取产废企业ID
        Long producerEnterpriseId = getProducerEnterpriseIdFromUser();
        
        appointmentService.cancelMyAppointment(id, cancelReason, producerEnterpriseId);
        return success(true);
    }

    /**
     * 从登录用户信息中获取产废企业ID
     * TODO: 实际实现需要根据用户与企业的关联关系来获取
     */
    private Long getProducerEnterpriseIdFromUser() {
        // 获取当前登录用户ID
        Long userId = getLoginUserId();
        
        // TODO: 根据用户ID查询用户关联的企业ID
        // 这里需要调用用户企业关联服务来获取企业ID
        // 暂时返回固定值，实际应该从用户企业关联表中查询
        
        log.debug("[getProducerEnterpriseIdFromUser][当前登录用户ID：{}]", userId);
        
        // 暂时返回固定值，实际项目中需要实现用户与企业的关联查询
        return 1L;
    }

} 