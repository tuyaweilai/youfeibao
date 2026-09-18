package cn.iocoder.yudao.module.icbc.convert.payment;

import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 工行付方支付 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface PaymentConvert {

    PaymentConvert INSTANCE = Mappers.getMapper(PaymentConvert.class);

    /**
     * 将支付请求VO转换为支付订单DO
     */
    @Mapping(source = "outOrderId", target = "partnerOrderId")
    @Mapping(source = "outVendorId", target = "payerNo")
    @Mapping(source = "outUserId", target = "payeeNo")
    PaymentOrderDO convert(PaymentReqVO bean);

    /**
     * 将支付订单DO转换为支付响应VO
     */
    @Mapping(source = "partnerOrderId", target = "outOrderId")
    @Mapping(source = "icbcOrderNo", target = "icbcOrderNo")
    PaymentRespVO convert(PaymentOrderDO bean);

    /**
     * 将支付订单DO转换为支付状态查询响应VO
     */
    @Mapping(source = "partnerOrderId", target = "outOrderId")
    @Mapping(source = "payeeNo", target = "outUserId")
    @Mapping(source = "payerNo", target = "outVendorId")
    PaymentStatusQueryRespVO convertToQueryResp(PaymentOrderDO bean);

} 