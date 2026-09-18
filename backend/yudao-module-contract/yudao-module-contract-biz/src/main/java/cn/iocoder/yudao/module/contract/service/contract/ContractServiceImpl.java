package cn.iocoder.yudao.module.contract.service.contract;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.ContractCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.ContractPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.ContractUpdateReqVO;
import cn.iocoder.yudao.module.contract.convert.contract.ContractConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.contract.ContractDO;
import cn.iocoder.yudao.module.contract.dal.dataobject.version.ContractVersionDO;
import cn.iocoder.yudao.module.contract.dal.dataobject.type.ContractTypeDO;
import cn.iocoder.yudao.module.contract.dal.mysql.contract.ContractMapper;
import cn.iocoder.yudao.module.contract.dal.mysql.version.ContractVersionMapper;
import cn.iocoder.yudao.module.contract.enums.ContractOperationTypeEnum;
import cn.iocoder.yudao.module.contract.enums.ContractStatusEnum;
import cn.iocoder.yudao.module.contract.service.log.ContractOperationLogService;
import cn.iocoder.yudao.module.contract.service.type.ContractTypeService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.contract.enums.ErrorCodeConstants.*;

/**
 * 合同 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ContractServiceImpl implements ContractService {

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private ContractVersionMapper contractVersionMapper;

    @Resource
    private ContractTypeService contractTypeService;
    
    @Resource
    private ContractOperationLogService contractOperationLogService;
    
    @Resource
    private ConfigApi configApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContract(ContractCreateReqVO createReqVO) {
        // 校验合同类型
        contractTypeService.validateContractTypeExists(createReqVO.getTypeId());
        
        // 生成合同编号
        String contractNo = generateContractNo(createReqVO.getTypeId());
        
        // 插入合同主记录
        ContractDO contract = ContractConvert.INSTANCE.convert(createReqVO);
        contract.setContractNo(contractNo);
        contract.setContractUuid(UUID.randomUUID().toString()); // 生成UUID
        contract.setStatus(ContractStatusEnum.DRAFT.getStatus());
        
        // 设置电子合同标记，默认为true
        if (contract.getIsElectronic() == null) {
            contract.setIsElectronic(Boolean.TRUE);
        }
        
        // 插入合同主记录（此时current_version_id为NULL）
        contractMapper.insert(contract);
        
        // 创建初始版本
        ContractVersionDO initialVersion = new ContractVersionDO();
        initialVersion.setContractId(contract.getId());
        initialVersion.setVersionNumber("1.0");
        initialVersion.setDescriptionOfChanges("初始版本");
        initialVersion.setEsignatureStatus(0); // 未发起
        initialVersion.setAutoRemindDays("30,15,7");
        
        // 设置版本的时间信息
        if (createReqVO.getEffectiveDate() != null) {
            initialVersion.setEffectiveDate(createReqVO.getEffectiveDate());
        }
        if (createReqVO.getExpiryDate() != null) {
            initialVersion.setExpiryDate(createReqVO.getExpiryDate());
        }
        
        // 设置合同内容
        if (StrUtil.isNotEmpty(createReqVO.getContractContent())) {
            initialVersion.setContractContent(createReqVO.getContractContent());
        }
        
        // 插入版本记录
        contractVersionMapper.insert(initialVersion);
        
        // 更新合同的当前版本ID
        ContractDO updateContract = new ContractDO();
        updateContract.setId(contract.getId());
        updateContract.setCurrentVersionId(initialVersion.getId());
        contractMapper.updateById(updateContract);
        
        // 记录操作日志
        contractOperationLogService.createContractOperationLog(
            contract.getId(), 
            initialVersion.getId(), 
            ContractOperationTypeEnum.CREATE.getType(), 
            "创建合同: " + contract.getContractName(),
            null, 
            ContractStatusEnum.DRAFT.getStatus(),
            JSONUtil.toJsonStr(contract)
        );
        
        // 返回
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(ContractUpdateReqVO updateReqVO) {
        // 校验存在
        ContractDO contract = validateContractExists(updateReqVO.getId());
        
        // 校验状态是否允许修改
        if (!ContractStatusEnum.canEdit(contract.getStatus())) {
            throw exception(CONTRACT_STATUS_NOT_ALLOW_UPDATE);
        }
        
        // 校验合同类型
        if (!contract.getTypeId().equals(updateReqVO.getTypeId())) {
            contractTypeService.validateContractTypeExists(updateReqVO.getTypeId());
        }
        
        // 更新
        ContractDO updateObj = ContractConvert.INSTANCE.convert(updateReqVO);
        contractMapper.updateById(updateObj);
        
        // 记录操作日志
        contractOperationLogService.createContractOperationLog(
            contract.getId(), 
            contract.getCurrentVersionId(), 
            ContractOperationTypeEnum.UPDATE.getType(), 
            "修改合同: " + contract.getContractName(),
            contract.getStatus(), 
            contract.getStatus(),
            JSONUtil.toJsonStr(updateObj)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        // 校验存在
        ContractDO contract = validateContractExists(id);
        
        // 校验状态是否允许删除
        if (!ContractStatusEnum.canDelete(contract.getStatus())) {
            throw exception(CONTRACT_STATUS_NOT_ALLOW_DELETE);
        }
        
        // 记录操作日志
        contractOperationLogService.createContractOperationLog(
            contract.getId(), 
            contract.getCurrentVersionId(), 
            ContractOperationTypeEnum.UPDATE.getType(), 
            "删除合同: " + contract.getContractName(),
            contract.getStatus(), 
            null,
            null
        );
        
        // 删除
        contractMapper.deleteById(id);
    }

    @Override
    public ContractDO getContract(Long id) {
        return contractMapper.selectById(id);
    }

    @Override
    public PageResult<ContractDO> getContractPage(ContractPageReqVO pageReqVO) {
        return contractMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ContractDO> getContractList(List<Long> ids) {
        return contractMapper.selectBatchIds(ids);
    }

    @Override
    public ContractDO validateContractExists(Long id) {
        ContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitContract(Long id) {
        // 校验存在
        ContractDO contract = validateContractExists(id);
        
        // 校验状态
        if (!ContractStatusEnum.DRAFT.getStatus().equals(contract.getStatus())) {
            throw exception(CONTRACT_STATUS_NOT_ALLOW_UPDATE);
        }
        
        // 更新状态为待签署
        Integer oldStatus = contract.getStatus();
        Integer newStatus = ContractStatusEnum.PENDING_SIGN.getStatus();
        updateContractStatus(id, newStatus);
        
        // 记录操作日志
        contractOperationLogService.createContractStatusChangeLog(
            contract.getId(), 
            ContractOperationTypeEnum.SUBMIT_APPROVAL.getType(), 
            "提交合同审核: " + contract.getContractName(),
            oldStatus, 
            newStatus
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractStatus(Long id, Integer status) {
        // 获取原合同信息
        ContractDO contract = validateContractExists(id);
        Integer oldStatus = contract.getStatus();
        
        // 更新状态
        ContractDO updateObj = new ContractDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        contractMapper.updateById(updateObj);
        
        // 状态发生变化时记录日志
        if (!status.equals(oldStatus)) {
            contractOperationLogService.createContractStatusChangeLog(
                id, 
                ContractOperationTypeEnum.UPDATE.getType(), 
                "更新合同状态: " + contract.getContractName(),
                oldStatus, 
                status
            );
        }
    }

    @Override
    public List<ContractDO> getExpiringContracts(Integer days) {
        // 由于expiryDate字段已删除，此方法暂时返回空列表
        return Collections.emptyList();
    }

    @Override
    public ContractDO getContractByNo(String contractNo) {
        return contractMapper.selectByContractNo(contractNo);
    }

    @Override
    public String generateContractNo(Long typeId) {
        // 获取合同类型
        ContractTypeDO contractType = contractTypeService.getContractType(typeId);
        
        // 从配置中获取合同编号前缀，如果获取不到则使用默认值 "CT"
        String configPrefix = configApi.getConfigValueByKey("contract.number.prefix");
        String prefix = StrUtil.isNotEmpty(configPrefix) ? configPrefix : "CT";
        
        // 获取当前日期
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 组合前缀部分: 配置前缀 + 类型编码 + 日期
        String fullPrefix = prefix + "-" + contractType.getCode() + "-" + dateStr;
        
        // 查询当天该类型最大序号
        Integer maxSequence = contractMapper.selectMaxSequenceByPrefix(fullPrefix);
        int sequence = (maxSequence == null) ? 1 : maxSequence + 1;
        
        // 组合最终编号: 前缀-类型码-日期-4位序号
        return fullPrefix + "-" + StrUtil.padPre(String.valueOf(sequence), 4, '0');
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initiateEsignature(Long id) {
        // 校验合同存在
        ContractDO contract = validateContractExists(id);
        
        // 校验状态是否允许发起签署
        if (!ContractStatusEnum.canSign(contract.getStatus())) {
            throw exception(CONTRACT_STATUS_NOT_ALLOW_SIGN);
        }
        
        // TODO: 集成e签宝API发起签署流程
        // 这里应该调用e签宝的API创建签署流程
        String esignProcessId = "ESIGN_" + System.currentTimeMillis();
        
        // 更新合同状态为签署中
        updateContractStatus(id, ContractStatusEnum.SIGNING.getStatus());
        
        // 记录操作日志
        contractOperationLogService.createContractOperationLog(
            contract.getId(), 
            contract.getCurrentVersionId(), 
            ContractOperationTypeEnum.SIGN.getType(), 
            "发起电子签署",
            contract.getStatus(), 
            ContractStatusEnum.SIGNING.getStatus(),
            JSONUtil.toJsonStr(Collections.singletonMap("esignProcessId", esignProcessId))
        );
        
        return esignProcessId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signContract(Long id, Long partyId, String signatureData) {
        // 校验合同存在
        ContractDO contract = validateContractExists(id);
        
        // 校验状态是否允许签署
        if (!ContractStatusEnum.canSign(contract.getStatus())) {
            throw exception(CONTRACT_STATUS_NOT_ALLOW_SIGN);
        }
        
        // TODO: 调用参与方服务更新签署状态
        // contractPartyService.updateSignStatus(partyId, ContractSignStatusEnum.SIGNED.getStatus(), null);
        
        // 记录操作日志
        contractOperationLogService.createContractOperationLog(
            contract.getId(), 
            contract.getCurrentVersionId(), 
            ContractOperationTypeEnum.SIGN.getType(), 
            "参与方签署合同",
            contract.getStatus(), 
            contract.getStatus(),
            JSONUtil.toJsonStr(Collections.singletonMap("partyId", partyId))
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSignature(Long id) {
        // 校验合同存在
        ContractDO contract = validateContractExists(id);
        
        // 更新合同状态为已生效
        updateContractStatus(id, ContractStatusEnum.EFFECTIVE.getStatus());
        
        // 记录操作日志
        contractOperationLogService.createContractOperationLog(
            contract.getId(), 
            contract.getCurrentVersionId(), 
            ContractOperationTypeEnum.ACTIVATE.getType(), 
            "合同签署完成，状态变更为已生效",
            contract.getStatus(), 
            ContractStatusEnum.EFFECTIVE.getStatus(),
            null
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignature(Long id, String reason) {
        // 校验合同存在
        ContractDO contract = validateContractExists(id);
        
        // 更新合同状态为已作废
        updateContractStatus(id, ContractStatusEnum.CANCELLED.getStatus());
        
        // 记录操作日志
        contractOperationLogService.createContractOperationLog(
            contract.getId(), 
            contract.getCurrentVersionId(), 
            ContractOperationTypeEnum.TERMINATE.getType(), 
            "取消签署：" + reason,
            contract.getStatus(), 
            ContractStatusEnum.CANCELLED.getStatus(),
            JSONUtil.toJsonStr(Collections.singletonMap("reason", reason))
        );
    }
} 