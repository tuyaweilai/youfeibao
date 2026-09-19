package cn.iocoder.yudao.module.icbc.controller.app.appointment;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentGoodsRespVO;
import cn.iocoder.yudao.module.icbc.service.appointment.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 自然人出售者端 - 到站预约（#35，ADR 0020）。
 *
 * <p>他主动声明「什么时候去哪个场站卖什么」，用于排队与到站登记带出。**不是订单**：
 * 不占额度、不产生开票、不进五流；没有「企业接受 / 拒绝」，只有到场与未到场。
 */
@Tag(name = "自然人出售者端 - 到站预约")
@RestController
@RequestMapping("/icbc/seller/appointment")
@Validated
@Slf4j
public class SellerAppointmentController {

    @Resource
    private AppointmentService appointmentService;

    @PostMapping("/create")
    @Operation(summary = "发起预约到站", description = "不是订单：不占额度、不产生开票、不进五流")
    public CommonResult<Long> create(@Valid @RequestBody AppointmentCreateReqVO reqVO) {
        return success(appointmentService.create(reqVO));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消预约到站", description = "只有「待到站」可取消")
    public CommonResult<Boolean> cancel(@Valid @RequestBody AppointmentCancelReqVO reqVO) {
        appointmentService.cancel(reqVO);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "我的预约（跨企业，仅本人可见）")
    @Parameter(name = "naturalPersonId", description = "自然人主体编号", required = true)
    public CommonResult<List<AppointmentRespVO>> list(@RequestParam("naturalPersonId") Long naturalPersonId) {
        return success(appointmentService.getListForSeller(naturalPersonId));
    }

    @GetMapping("/goods")
    @Operation(summary = "可预约的品类（当前租户启用中的品类）")
    public CommonResult<List<AppointmentGoodsRespVO>> goods() {
        return success(appointmentService.listEnabledGoods());
    }

}
