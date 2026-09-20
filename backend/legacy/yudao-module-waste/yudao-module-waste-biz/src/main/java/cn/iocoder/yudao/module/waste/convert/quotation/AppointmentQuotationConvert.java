package cn.iocoder.yudao.module.waste.convert.quotation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 预约报价记录 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface AppointmentQuotationConvert {

    AppointmentQuotationConvert INSTANCE = Mappers.getMapper(AppointmentQuotationConvert.class);

    AppointmentQuotationDO convert(AppointmentQuotationCreateReqVO bean);

    AppointmentQuotationDO convert(AppointmentQuotationUpdateReqVO bean);

    AppointmentQuotationRespVO convert(AppointmentQuotationDO bean);

    List<AppointmentQuotationRespVO> convertList(List<AppointmentQuotationDO> list);

    PageResult<AppointmentQuotationRespVO> convertPage(PageResult<AppointmentQuotationDO> page);

} 