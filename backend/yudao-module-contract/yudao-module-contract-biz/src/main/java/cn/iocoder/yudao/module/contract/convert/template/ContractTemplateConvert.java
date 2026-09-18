package cn.iocoder.yudao.module.contract.convert.template;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplateCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplateRespVO;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplateUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.template.ContractTemplateDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 合同模板 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractTemplateConvert {

    ContractTemplateConvert INSTANCE = Mappers.getMapper(ContractTemplateConvert.class);

    ContractTemplateDO convert(ContractTemplateCreateReqVO bean);

    ContractTemplateDO convert(ContractTemplateUpdateReqVO bean);

    ContractTemplateRespVO convert(ContractTemplateDO bean);

    List<ContractTemplateRespVO> convertList(List<ContractTemplateDO> list);

    PageResult<ContractTemplateRespVO> convertPage(PageResult<ContractTemplateDO> page);
} 