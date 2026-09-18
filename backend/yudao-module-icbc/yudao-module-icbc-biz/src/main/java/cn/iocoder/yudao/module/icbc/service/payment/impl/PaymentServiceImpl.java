package cn.iocoder.yudao.module.icbc.service.payment.impl;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.PaymentReq;
import cn.iocoder.yudao.module.icbc.service.payment.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 工行付方支付 Service 实现类
 *
 * 出站调用一律经 {@link IcbcGateway} 端口；本类不再出现工行网关地址或签名逻辑。
 *
 * @author 芋道源码
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private IcbcGateway icbcGateway;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRespVO createPayment(PaymentReqVO paymentReqVO) {
        // 1. 参数校验
        validatePaymentRequest(paymentReqVO);

        // 2. 检查订单是否已存在
        PaymentOrderDO existingOrder = paymentOrderMapper.selectByPartnerOrderId(paymentReqVO.getOutOrderId());
        if (existingOrder != null) {
            log.warn("支付订单已存在 - partnerOrderId: {}", paymentReqVO.getOutOrderId());
            throw exception(ErrorCodeConstants.PAYMENT_ORDER_EXISTS);
        }

        // 3. 生成订单号和消息ID
        String orderNo = generateOrderNo();
        String msgId = generateMsgId();

        // 4. 创建支付订单记录（先落库再调用工行）
        PaymentOrderDO paymentOrder = createPaymentOrderFromRequest(paymentReqVO, orderNo, msgId);
        paymentOrderMapper.insert(paymentOrder);

        // 5. 经端口生成企业支付页面
        IcbcGatewayResult<IcbcPage> result = icbcGateway.submitPayment(PaymentReq.builder()
                .outOrderId(paymentReqVO.getOutOrderId())
                .outVendorId(paymentReqVO.getOutVendorId())
                .outUserId(paymentReqVO.getOutUserId())
                .verifiedCode(paymentReqVO.getVerifiedCode())
                .ukeyId(paymentReqVO.getUkeyId())
                .build());
        if (!result.isSuccess()) {
            // 结果未知时不重复提交，由上层按业务单号查询确认；此处仅落库为待支付
            log.warn("生成支付页面失败 - orderNo: {}, outcome: {}, returnMsg: {}",
                    orderNo, result.getOutcome(), result.getReturnMsg());
            throw exception(ErrorCodeConstants.ICBC_API_CALL_FAILED);
        }

        // 6. 保存页面表单并返回
        paymentOrder.setRedirectUrl(result.getData().getFormHtml());
        paymentOrderMapper.updateById(paymentOrder);

        PaymentRespVO response = new PaymentRespVO();
        response.setReturnCode("0");
        response.setReturnMsg("成功");
        response.setRedirectUrl(result.getData().getFormHtml());
        response.setMsgId(msgId);
        response.setOutOrderId(paymentReqVO.getOutOrderId());
        response.setIcbcOrderNo(orderNo);
        response.setPaymentStatus("PENDING");

        log.info("付方支付订单创建成功 - orderNo: {}, partnerOrderId: {}", orderNo, paymentReqVO.getOutOrderId());
        return response;
    }

    @Override
    public PaymentStatusQueryRespVO queryPaymentStatus(PaymentStatusQueryReqVO queryReqVO) {
        // 1. 查询本地支付订单信息
        PaymentOrderDO paymentOrder = null;
        if (queryReqVO.getOutOrderId() != null) {
            paymentOrder = paymentOrderMapper.selectByPartnerOrderId(queryReqVO.getOutOrderId());
        } else if (queryReqVO.getIcbcOrderNo() != null) {
            paymentOrder = paymentOrderMapper.selectByIcbcOrderNo(queryReqVO.getIcbcOrderNo());
        }

        if (paymentOrder == null) {
            throw exception(ErrorCodeConstants.PAYMENT_ORDER_NOT_EXISTS);
        }

        // 2. 返回本地状态（与工行对账由异步通知与预查询完成）
        PaymentStatusQueryRespVO response = new PaymentStatusQueryRespVO();
        response.setReturnCode("0");
        response.setReturnMsg("成功");
        response.setOutOrderId(paymentOrder.getPartnerOrderId());
        response.setIcbcOrderNo(paymentOrder.getIcbcOrderNo());
        response.setPaymentStatus(getPaymentStatusText(paymentOrder.getPaymentStatus()));
        response.setPaymentAmount(paymentOrder.getPaymentAmount());
        response.setPaymentTime(paymentOrder.getPaymentTime());
        response.setOutVendorId(paymentOrder.getPayerNo());
        response.setOutUserId(paymentOrder.getPayeeNo());
        response.setPaymentSerialNo(paymentOrder.getPaymentSerialNo());
        response.setErrorCode(paymentOrder.getErrorCode());
        response.setErrorMsg(paymentOrder.getErrorMsg());

        log.info("支付状态查询成功 - orderNo: {}, partnerOrderId: {}, status: {}",
                paymentOrder.getOrderNo(), paymentOrder.getPartnerOrderId(), paymentOrder.getPaymentStatus());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentNotify(String notifyData) {
        // 支付类异步通知由统一入口 IcbcNotifyService 落表后处理（见 issue #3 / #9）
        log.info("收到支付回调通知: {}", notifyData);
        return true;
    }

    /**
     * 验证支付请求参数
     */
    private void validatePaymentRequest(PaymentReqVO request) {
        if (request.getAppId() == null || request.getAppId().trim().isEmpty()) {
            throw exception(ErrorCodeConstants.PAYMENT_PARAM_ERROR);
        }
        if (request.getOutOrderId() == null || request.getOutOrderId().trim().isEmpty()) {
            throw exception(ErrorCodeConstants.PAYMENT_PARAM_ERROR);
        }
    }

    /**
     * 生成支付订单号
     */
    private String generateOrderNo() {
        return "PAY" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
    }

    /**
     * 生成消息ID
     */
    private String generateMsgId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * 从请求创建支付订单对象
     */
    private PaymentOrderDO createPaymentOrderFromRequest(PaymentReqVO request, String orderNo, String msgId) {
        return PaymentOrderDO.builder()
            .orderNo(orderNo)
            .partnerOrderId(request.getOutOrderId())
            .payeeNo(request.getOutUserId())
            .payerNo(request.getOutVendorId())
            .paymentAmount(BigDecimal.ZERO) // 支付金额需要从业务系统获取
            .paymentStatus(0) // 待支付
            .verifiedCode(request.getVerifiedCode())
            .ukeyId(request.getUkeyId())
            .msgId(msgId)
            .build();
    }

    /**
     * 获取支付状态文本
     */
    private String getPaymentStatusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        switch (status) {
            case 0: return "PENDING";
            case 1: return "PROCESSING";
            case 2: return "SUCCESS";
            case 3: return "FAILED";
            case 4: return "CANCELLED";
            default: return "UNKNOWN";
        }
    }

}
