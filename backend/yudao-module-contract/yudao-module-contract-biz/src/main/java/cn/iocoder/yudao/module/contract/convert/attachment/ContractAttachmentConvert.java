package cn.iocoder.yudao.module.contract.convert.attachment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentRespVO;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.attachment.ContractAttachmentDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 合同附件 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractAttachmentConvert {

    ContractAttachmentConvert INSTANCE = Mappers.getMapper(ContractAttachmentConvert.class);

    ContractAttachmentDO convert(ContractAttachmentCreateReqVO bean);

    ContractAttachmentDO convert(ContractAttachmentUpdateReqVO bean);

    ContractAttachmentRespVO convert(ContractAttachmentDO bean);

    List<ContractAttachmentRespVO> convertList(List<ContractAttachmentDO> list);

    PageResult<ContractAttachmentRespVO> convertPage(PageResult<ContractAttachmentDO> page);
} 