package cn.iocoder.yudao.module.contract.convert.version;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionRespVO;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.version.ContractVersionDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 合同版本 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractVersionConvert {

    ContractVersionConvert INSTANCE = Mappers.getMapper(ContractVersionConvert.class);

    ContractVersionDO convert(ContractVersionCreateReqVO bean);

    ContractVersionDO convert(ContractVersionUpdateReqVO bean);

    ContractVersionRespVO convert(ContractVersionDO bean);

    List<ContractVersionRespVO> convertList(List<ContractVersionDO> list);

    PageResult<ContractVersionRespVO> convertPage(PageResult<ContractVersionDO> page);
} 