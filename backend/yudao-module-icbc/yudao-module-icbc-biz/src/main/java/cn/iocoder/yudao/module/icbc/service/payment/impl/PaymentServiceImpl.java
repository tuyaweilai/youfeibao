package cn.iocoder.yudao.module.icbc.service.payment.impl;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.icbc.config.IcbcProperties;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.icbc.service.payment.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 工行付方支付 Service 实现类
 *
 * @author 芋道源码
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private IcbcProperties icbcProperties;

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

        // 4. 创建支付订单记录
        PaymentOrderDO paymentOrder = createPaymentOrderFromRequest(paymentReqVO, orderNo, msgId);
        paymentOrderMapper.insert(paymentOrder);

        // 5. 生成支付页面重定向URL
        try {
            String redirectUrl = generatePaymentUrl(paymentReqVO);
            
            // 6. 更新订单的重定向URL
            paymentOrder.setRedirectUrl(redirectUrl);
            paymentOrderMapper.updateById(paymentOrder);

            // 7. 构造响应
            PaymentRespVO response = new PaymentRespVO();
            response.setReturnCode("0");
            response.setReturnMsg("成功");
            response.setRedirectUrl(redirectUrl);
            response.setMsgId(msgId);
            response.setOutOrderId(paymentReqVO.getOutOrderId());
            response.setIcbcOrderNo(orderNo);
            response.setPaymentStatus("PENDING");

            log.info("付方支付订单创建成功 - orderNo: {}, partnerOrderId: {}", orderNo, paymentReqVO.getOutOrderId());
            return response;

        } catch (Exception e) {
            log.error("生成支付URL失败 - orderNo: {}, partnerOrderId: {}", 
                orderNo, paymentReqVO.getOutOrderId(), e);
            throw exception(ErrorCodeConstants.ICBC_API_CALL_FAILED);
        }
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

        // 2. 调用工行查询接口（暂时返回本地数据）
        try {
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

        } catch (Exception e) {
            log.error("查询支付状态失败 - partnerOrderId: {}", queryReqVO.getOutOrderId(), e);
            throw exception(ErrorCodeConstants.ICBC_API_CALL_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentNotify(String notifyData) {
        // TODO: 实现支付回调通知处理逻辑
        log.info("收到支付回调通知: {}", notifyData);
        
        try {
            // 1. 解析回调数据
            // 2. 验证签名
            // 3. 更新支付订单状态
            // 4. 发送业务通知
            
            return true;
        } catch (Exception e) {
            log.error("处理支付回调通知失败", e);
            return false;
        }
    }

    @Override
    public String generatePaymentUrl(PaymentReqVO paymentReqVO) {
        // 根据聚富通开票付方支付接口文档生成支付URL
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(icbcProperties.getPaymentUrl());
        urlBuilder.append("?appId=").append(paymentReqVO.getAppId());
        urlBuilder.append("&outOrderId=").append(paymentReqVO.getOutOrderId());
        
        if (paymentReqVO.getOutVendorId() != null) {
            urlBuilder.append("&outVendorId=").append(paymentReqVO.getOutVendorId());
        }
        if (paymentReqVO.getOutUserId() != null) {
            urlBuilder.append("&outUserId=").append(paymentReqVO.getOutUserId());
        }
        if (paymentReqVO.getVerifiedCode() != null) {
            urlBuilder.append("&verifiedCode=").append(paymentReqVO.getVerifiedCode());
        }
        if (paymentReqVO.getUkeyId() != null) {
            urlBuilder.append("&ukeyId=").append(paymentReqVO.getUkeyId());
        }

        return urlBuilder.toString();
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