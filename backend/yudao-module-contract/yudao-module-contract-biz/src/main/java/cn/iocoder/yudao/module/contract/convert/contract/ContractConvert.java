package cn.iocoder.yudao.module.contract.convert.contract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.api.ContractCreateReqDTO;
import cn.iocoder.yudao.module.contract.api.ContractRespDTO;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.ContractCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.ContractRespVO;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.ContractUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.contract.ContractDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 合同 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractConvert {

    ContractConvert INSTANCE = Mappers.getMapper(ContractConvert.class);

    @Mapping(source = "name", target = "contractName")
    @Mapping(source = "typeId", target = "contractTypeId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contractNo", ignore = true)
    @Mapping(target = "contractUuid", ignore = true)
    @Mapping(target = "currentVersionId", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    ContractDO convert(ContractCreateReqVO bean);

    @Mapping(source = "name", target = "contractName")
    @Mapping(source = "typeId", target = "contractTypeId")
    @Mapping(target = "contractNo", ignore = true)
    @Mapping(target = "contractUuid", ignore = true)
    @Mapping(target = "currentVersionId", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    ContractDO convert(ContractUpdateReqVO bean);

    @Mapping(source = "contractName", target = "name")
    @Mapping(source = "contractTypeId", target = "typeId")
    @Mapping(target = "typeName", ignore = true)
    ContractRespVO convert(ContractDO bean);

    List<ContractRespVO> convertList(List<ContractDO> list);

    PageResult<ContractRespVO> convertPage(PageResult<ContractDO> page);

    // ==================== API相关转换方法 ====================

    /**
     * 转换 ContractDO 为 ContractRespDTO
     */
    @Mapping(source = "contractName", target = "name")
    @Mapping(source = "contractTypeId", target = "typeId")
    ContractRespDTO convertApi(ContractDO bean);

    /**
     * 批量转换 ContractDO 为 ContractRespDTO
     */
    List<ContractRespDTO> convertApiList(List<ContractDO> list);

    /**
     * 转换 ContractCreateReqDTO 为 ContractCreateReqVO
     */
    @Mapping(source = "contractName", target = "name")
    @Mapping(source = "contractTypeId", target = "typeId")
    ContractCreateReqVO convertApiToVO(ContractCreateReqDTO bean);

}