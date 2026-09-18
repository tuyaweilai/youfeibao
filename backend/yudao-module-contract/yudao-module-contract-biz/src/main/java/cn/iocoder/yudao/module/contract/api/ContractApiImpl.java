package cn.iocoder.yudao.module.contract.api;

import cn.iocoder.yudao.module.contract.api.dto.ContractValidateReqDTO;
import cn.iocoder.yudao.module.contract.api.dto.ContractValidateRespDTO;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.ContractCreateReqVO;
import cn.iocoder.yudao.module.contract.convert.contract.ContractConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.contract.ContractDO;
import cn.iocoder.yudao.module.contract.enums.ContractStatusEnum;
import cn.iocoder.yudao.module.contract.service.contract.ContractService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 合同 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ContractApiImpl implements ContractApi {

    @Resource
    private ContractService contractService;

    @Override
    public ContractValidateRespDTO validateBusinessContract(ContractValidateReqDTO reqDTO) {
        // TODO: 实现合同校验逻辑
        // 1. 根据业务类型、参与企业、业务日期等条件查询合同
        // 2. 过滤出有效的合同（状态为已生效，且在有效期内）
        // 3. 根据废物类型等额外条件进一步筛选
        // 4. 计算匹配分数，返回校验结果
        
        ContractValidateRespDTO respDTO = new ContractValidateRespDTO();
        respDTO.setIsValid(false);
        respDTO.setValidContracts(new ArrayList<>());
        
        ContractValidateRespDTO.ValidationDetails details = new ContractValidateRespDTO.ValidationDetails();
        details.setHasValidContract(false);
        details.setContractCount(0);
        details.setExpiringContracts(new ArrayList<>());
        details.setRecommendations(new ArrayList<>());
        respDTO.setValidationDetails(details);
        
        return respDTO;
    }

    @Override
    public ContractRespDTO getContract(Long id) {
        ContractDO contract = contractService.getContract(id);
        return ContractConvert.INSTANCE.convertApi(contract);
    }

    @Override
    public void createContractLink(Long contractId, Long objectId, String objectType, Integer linkType) {
        // TODO: 实现合同关联创建逻辑
        // 1. 校验合同是否存在
        // 2. 创建合同关联记录
    }

    @Override
    public List<ContractRespDTO> getContractList(List<Long> ids) {
        List<ContractDO> contracts = contractService.getContractList(ids);
        return ContractConvert.INSTANCE.convertApiList(contracts);
    }

    @Override
    public boolean isContractValid(Long id) {
        ContractDO contract = contractService.getContract(id);
        if (contract == null) {
            return false;
        }
        
        // 检查合同状态是否为已生效
        return ContractStatusEnum.EFFECTIVE.getStatus().equals(contract.getStatus());
    }

    @Override
    public Long createContract(ContractCreateReqDTO createReqDTO) {
        // 转换DTO为VO
        ContractCreateReqVO createReqVO = ContractConvert.INSTANCE.convertApiToVO(createReqDTO);
        
        // 调用服务层创建合同
        return contractService.createContract(createReqVO);
    }

    @Override
    public String initiateEsignature(Long contractId) {
        // 调用服务层发起电子签署
        return contractService.initiateEsignature(contractId);
    }

} 