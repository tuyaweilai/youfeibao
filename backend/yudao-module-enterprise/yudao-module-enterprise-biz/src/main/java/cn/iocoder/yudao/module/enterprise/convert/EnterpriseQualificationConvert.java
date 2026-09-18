package cn.iocoder.yudao.module.enterprise.convert;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationRespVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseQualificationDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 企业资质 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseQualificationConvert {

    EnterpriseQualificationConvert INSTANCE = Mappers.getMapper(EnterpriseQualificationConvert.class);

    EnterpriseQualificationDO convert(EnterpriseQualificationCreateReqVO bean);

    EnterpriseQualificationDO convert(EnterpriseQualificationUpdateReqVO bean);

    EnterpriseQualificationRespVO convert(EnterpriseQualificationDO bean);

    List<EnterpriseQualificationRespVO> convertList(List<EnterpriseQualificationDO> list);

    PageResult<EnterpriseQualificationRespVO> convertPage(PageResult<EnterpriseQualificationDO> page);

} 