package cn.iocoder.yudao.module.waste.convert.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.ProducerPaymentConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 产废企业付款配置 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ProducerPaymentConfigConvert {

    ProducerPaymentConfigConvert INSTANCE = Mappers.getMapper(ProducerPaymentConfigConvert.class);

    ProducerPaymentConfigDO convert(ProducerPaymentConfigCreateReqVO bean);

    ProducerPaymentConfigDO convert(ProducerPaymentConfigUpdateReqVO bean);

    ProducerPaymentConfigRespVO convert(ProducerPaymentConfigDO bean);

    List<ProducerPaymentConfigRespVO> convertList(List<ProducerPaymentConfigDO> list);

    PageResult<ProducerPaymentConfigRespVO> convertPage(PageResult<ProducerPaymentConfigDO> page);

} 