package cn.iocoder.yudao.module.contract.convert.party;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyRespVO;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.party.ContractPartyDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 合同参与方 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractPartyConvert {

    ContractPartyConvert INSTANCE = Mappers.getMapper(ContractPartyConvert.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "signStatus", ignore = true)
    @Mapping(target = "signedAt", ignore = true)
    @Mapping(target = "signIp", ignore = true)
    @Mapping(target = "esignatureIndividualId", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    ContractPartyDO convert(ContractPartyCreateReqVO bean);

    @Mapping(target = "signStatus", ignore = true)
    @Mapping(target = "signedAt", ignore = true)
    @Mapping(target = "signIp", ignore = true)
    @Mapping(target = "esignatureIndividualId", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    ContractPartyDO convert(ContractPartyUpdateReqVO bean);

    ContractPartyRespVO convert(ContractPartyDO bean);

    List<ContractPartyRespVO> convertList(List<ContractPartyDO> list);

    PageResult<ContractPartyRespVO> convertPage(PageResult<ContractPartyDO> page);

} 