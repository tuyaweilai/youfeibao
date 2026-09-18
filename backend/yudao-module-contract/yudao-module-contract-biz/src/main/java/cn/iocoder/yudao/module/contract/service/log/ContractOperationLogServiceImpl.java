package cn.iocoder.yudao.module.contract.service.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.contract.controller.admin.log.vo.ContractOperationLogPageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.log.ContractOperationLogDO;
import cn.iocoder.yudao.module.contract.dal.mysql.log.ContractOperationLogMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getRequest;

/**
 * 合同操作日志 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ContractOperationLogServiceImpl implements ContractOperationLogService {

    @Resource
    private ContractOperationLogMapper contractOperationLogMapper;
    
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public Long createContractOperationLog(Long contractId, Long versionId, Integer operationType, String operationDescription,
                                          Integer oldStatus, Integer newStatus, String operationData) {
        HttpServletRequest request = getRequest();
        
        // 获取操作人信息
        Long operatorId = WebFrameworkUtils.getLoginUserId();
        String operatorName = "未知用户";
        if (operatorId != null && operatorId > 0) {
            AdminUserRespDTO user = adminUserApi.getUser(operatorId);
            if (user != null) {
                operatorName = user.getNickname();
            }
        }
        
        // 创建日志
        ContractOperationLogDO logDO = new ContractOperationLogDO();
        logDO.setContractId(contractId);
        logDO.setVersionId(versionId);
        logDO.setOperationType(operationType);
        logDO.setOperationDescription(operationDescription);
        logDO.setOldStatus(oldStatus);
        logDO.setNewStatus(newStatus);
        logDO.setOperationData(operationData);
        logDO.setOperatorId(operatorId);
        logDO.setOperatorName(operatorName);
        logDO.setOperatorIp(getClientIP(request));
        logDO.setTenantId(WebFrameworkUtils.getTenantId(request));
        
        contractOperationLogMapper.insert(logDO);
        return logDO.getId();
    }

    @Override
    public Long createContractStatusChangeLog(Long contractId, Integer operationType, String operationDescription,
                                             Integer oldStatus, Integer newStatus) {
        return createContractOperationLog(contractId, null, operationType, operationDescription, 
                oldStatus, newStatus, null);
    }

    @Override
    public List<ContractOperationLogDO> getContractOperationLogList(Long contractId) {
        return contractOperationLogMapper.selectListByContractId(contractId);
    }

    @Override
    public PageResult<ContractOperationLogDO> getContractOperationLogPage(ContractOperationLogPageReqVO pageReqVO) {
        return contractOperationLogMapper.selectPage(pageReqVO);
    }
} 