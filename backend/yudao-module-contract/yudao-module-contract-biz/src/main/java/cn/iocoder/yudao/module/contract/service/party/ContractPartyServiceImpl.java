package cn.iocoder.yudao.module.contract.service.party;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyUpdateReqVO;
import cn.iocoder.yudao.module.contract.convert.party.ContractPartyConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.contract.ContractDO;
import cn.iocoder.yudao.module.contract.dal.dataobject.party.ContractPartyDO;
import cn.iocoder.yudao.module.contract.dal.mysql.party.ContractPartyMapper;
import cn.iocoder.yudao.module.contract.service.contract.ContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.contract.enums.ErrorCodeConstants.CONTRACT_PARTY_NOT_EXISTS;
import static cn.iocoder.yudao.module.contract.enums.ErrorCodeConstants.CONTRACT_PARTY_ENTERPRISE_DUPLICATE;

/**
 * 合同参与方 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ContractPartyServiceImpl implements ContractPartyService {

    @Resource
    private ContractPartyMapper contractPartyMapper;

    @Resource
    private ContractService contractService;

    @Override
    public Long createContractParty(ContractPartyCreateReqVO createReqVO) {
        // 校验合同是否存在
        contractService.validateContractExists(createReqVO.getContractId());
        
        // 校验企业是否已经是该合同的参与方
        validateEnterpriseNotDuplicate(createReqVO.getContractId(), createReqVO.getEnterpriseId());
        
        // 插入
        ContractPartyDO contractParty = ContractPartyConvert.INSTANCE.convert(createReqVO);
        contractPartyMapper.insert(contractParty);
        
        // 返回
        return contractParty.getId();
    }

    /**
     * 校验企业是否已经是该合同的参与方
     *
     * @param contractId 合同ID
     * @param enterpriseId 企业ID
     */
    private void validateEnterpriseNotDuplicate(Long contractId, Long enterpriseId) {
        ContractPartyDO existingParty = contractPartyMapper.selectByContractIdAndEnterpriseId(contractId, enterpriseId);
        if (existingParty != null) {
            throw exception(CONTRACT_PARTY_ENTERPRISE_DUPLICATE);
        }
    }

    @Override
    public void updateContractParty(ContractPartyUpdateReqVO updateReqVO) {
        // 校验存在
        ContractPartyDO existingParty = validateContractPartyExists(updateReqVO.getId());
        
        // 如果修改了企业ID，需要校验新企业是否已经是该合同的参与方
        if (!existingParty.getEnterpriseId().equals(updateReqVO.getEnterpriseId())) {
            validateEnterpriseNotDuplicate(updateReqVO.getContractId(), updateReqVO.getEnterpriseId());
        }
        
        // 更新
        ContractPartyDO updateObj = ContractPartyConvert.INSTANCE.convert(updateReqVO);
        contractPartyMapper.updateById(updateObj);
    }

    @Override
    public void deleteContractParty(Long id) {
        // 校验存在
        validateContractPartyExists(id);
        // 删除
        contractPartyMapper.deleteById(id);
    }

    @Override
    public ContractPartyDO getContractParty(Long id) {
        return contractPartyMapper.selectById(id);
    }

    @Override
    public PageResult<ContractPartyDO> getContractPartyPage(ContractPartyPageReqVO pageReqVO) {
        return contractPartyMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ContractPartyDO> getContractPartyListByContractId(Long contractId) {
        return contractPartyMapper.selectListByContractId(contractId);
    }

    @Override
    public ContractPartyDO validateContractPartyExists(Long id) {
        ContractPartyDO contractParty = contractPartyMapper.selectById(id);
        if (contractParty == null) {
            throw exception(CONTRACT_PARTY_NOT_EXISTS);
        }
        return contractParty;
    }

    @Override
    public void updateSignStatus(Long id, Integer signStatus, String signIp) {
        // 校验存在
        validateContractPartyExists(id);
        
        // 更新签署状态
        ContractPartyDO updateObj = new ContractPartyDO();
        updateObj.setId(id);
        updateObj.setSignStatus(signStatus);
        updateObj.setSignIp(signIp);
        updateObj.setSignedAt(LocalDateTime.now()); // 设置签署时间为当前时间
        
        contractPartyMapper.updateById(updateObj);
    }

    @Override
    public List<Long> batchCreateContractParties(Long contractId, List<ContractPartyCreateReqVO> parties) {
        List<Long> ids = new ArrayList<>();
        
        // 检查批次内是否有重复的企业ID
        Set<Long> enterpriseIds = new HashSet<>();
        for (ContractPartyCreateReqVO party : parties) {
            if (!enterpriseIds.add(party.getEnterpriseId())) {
                throw exception(CONTRACT_PARTY_ENTERPRISE_DUPLICATE);
            }
        }
        
        // 循环创建参与方
        for (ContractPartyCreateReqVO party : parties) {
            party.setContractId(contractId); // 确保使用传入的合同ID
            Long id = createContractParty(party);
            ids.add(id);
        }
        
        return ids;
    }
} 