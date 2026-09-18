package cn.iocoder.yudao.module.icbc.controller.admin.payment;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentApplyRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReceiptRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusRespVO;
import cn.iocoder.yudao.module.icbc.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.CREATE;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.GET;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 付方支付（issue #9）
 *
 * <p>平台代回收企业向出售者付款：生成企业支付页面、查询支付状态、取转账回单。
 * 支付回调不经这里，走工行异步通知唯一入口 {@code POST /admin-api/icbc/callback/notify}。
 */
@Tag(name = "管理后台 - 工行付方支付")
@RestController
@RequestMapping("/icbc/payment")
@Validated
@Slf4j
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @PostMapping("/apply")
    @Operation(summary = "对预开票成功的收购发起付款")
    @PreAuthorize("@icbc.hasPermission('icbc:payment:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<PaymentApplyRespVO> applyPayment(@Valid @RequestBody PaymentApplyReqVO reqVO) {
        return success(paymentService.applyPayment(reqVO));
    }

    @GetMapping("/query")
    @Operation(summary = "查询支付状态（自动经工行收敛一次）")
    @PreAuthorize("@icbc.hasPermission('icbc:payment:query')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<PaymentStatusRespVO> queryPaymentStatus(@Valid PaymentQueryReqVO reqVO) {
        return success(paymentService.queryPaymentStatus(reqVO));
    }

    @GetMapping("/receipt")
    @Operation(summary = "查询转账回单归档信息")
    @PreAuthorize("@icbc.hasPermission('icbc:payment:query')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<PaymentReceiptRespVO> getReceipt(@RequestParam("partnerOrderId") String partnerOrderId) {
        return success(paymentService.getReceipt(partnerOrderId));
    }

}
