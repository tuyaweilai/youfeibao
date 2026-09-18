package cn.iocoder.yudao.module.icbc.controller.admin.payment;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryRespVO;
import cn.iocoder.yudao.module.icbc.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.CREATE;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.GET;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工行付方支付
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 工行付方支付")
@RestController
@RequestMapping("/icbc/payment")
@Validated
@Slf4j
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @PostMapping("/create")
    @Operation(summary = "创建付方支付")
    @PreAuthorize("@icbc.hasPermission('icbc:payment:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<PaymentRespVO> createPayment(@Valid @RequestBody PaymentReqVO createReqVO) {
        log.info("收到付方支付请求 - partnerOrderId: {}", createReqVO.getOutOrderId());
        PaymentRespVO response = paymentService.createPayment(createReqVO);
        return success(response);
    }

    @GetMapping("/query")
    @Operation(summary = "查询支付状态")
    @PreAuthorize("@icbc.hasPermission('icbc:payment:query')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<PaymentStatusQueryRespVO> queryPaymentStatus(@Valid PaymentStatusQueryReqVO queryReqVO) {
        log.info("收到支付状态查询请求 - partnerOrderId: {}", queryReqVO.getOutOrderId());
        PaymentStatusQueryRespVO response = paymentService.queryPaymentStatus(queryReqVO);
        return success(response);
    }

    @PostMapping("/query")
    @Operation(summary = "查询支付状态（POST方式）")
    @PreAuthorize("@icbc.hasPermission('icbc:payment:query')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<PaymentStatusQueryRespVO> queryPaymentStatusPost(@Valid @RequestBody PaymentStatusQueryReqVO queryReqVO) {
        log.info("收到支付状态查询请求（POST） - partnerOrderId: {}", queryReqVO.getOutOrderId());
        PaymentStatusQueryRespVO response = paymentService.queryPaymentStatus(queryReqVO);
        return success(response);
    }

    @PostMapping("/notify")
    @Operation(summary = "支付回调通知")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Boolean> handlePaymentNotify(@RequestBody String notifyData) {
        log.info("收到支付回调通知");
        boolean result = paymentService.handlePaymentNotify(notifyData);
        return success(result);
    }

} 