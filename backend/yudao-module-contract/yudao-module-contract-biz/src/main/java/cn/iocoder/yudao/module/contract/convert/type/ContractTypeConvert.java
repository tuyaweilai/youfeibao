package cn.iocoder.yudao.module.contract.convert.type;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.ContractTypeCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.ContractTypeRespVO;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.ContractTypeUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.type.ContractTypeDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 合同类型 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractTypeConvert {

    ContractTypeConvert INSTANCE = Mappers.getMapper(ContractTypeConvert.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "systemDefined", ignore = true)
    ContractTypeDO convert(ContractTypeCreateReqVO bean);

    @Mapping(target = "systemDefined", ignore = true)
    ContractTypeDO convert(ContractTypeUpdateReqVO bean);

    ContractTypeRespVO convert(ContractTypeDO bean);

    List<ContractTypeRespVO> convertList(List<ContractTypeDO> list);

    PageResult<ContractTypeRespVO> convertPage(PageResult<ContractTypeDO> page);

} 