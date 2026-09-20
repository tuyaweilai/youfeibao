package cn.iocoder.yudao.module.waste.convert.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.PaymentStatusHistoryDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 付款状态变更历史 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface PaymentStatusHistoryConvert {

    PaymentStatusHistoryConvert INSTANCE = Mappers.getMapper(PaymentStatusHistoryConvert.class);

    PaymentStatusHistoryDO convert(PaymentStatusHistoryCreateReqVO bean);

    PaymentStatusHistoryRespVO convert(PaymentStatusHistoryDO bean);

    List<PaymentStatusHistoryRespVO> convertList(List<PaymentStatusHistoryDO> list);

    PageResult<PaymentStatusHistoryRespVO> convertPage(PageResult<PaymentStatusHistoryDO> page);

} 