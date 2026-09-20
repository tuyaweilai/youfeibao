package cn.iocoder.yudao.module.waste.convert.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.CompanyPaymentVoucherDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对公付款凭证 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface CompanyPaymentVoucherConvert {

    CompanyPaymentVoucherConvert INSTANCE = Mappers.getMapper(CompanyPaymentVoucherConvert.class);

    CompanyPaymentVoucherDO convert(CompanyPaymentVoucherCreateReqVO bean);

    CompanyPaymentVoucherDO convert(CompanyPaymentVoucherUpdateReqVO bean);

    CompanyPaymentVoucherRespVO convert(CompanyPaymentVoucherDO bean);

    List<CompanyPaymentVoucherRespVO> convertList(List<CompanyPaymentVoucherDO> list);

    PageResult<CompanyPaymentVoucherRespVO> convertPage(PageResult<CompanyPaymentVoucherDO> page);

} 