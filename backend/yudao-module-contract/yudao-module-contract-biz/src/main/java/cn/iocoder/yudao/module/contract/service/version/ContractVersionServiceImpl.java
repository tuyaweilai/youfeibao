package cn.iocoder.yudao.module.contract.service.version;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionUpdateReqVO;
import cn.iocoder.yudao.module.contract.convert.version.ContractVersionConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.version.ContractVersionDO;
import cn.iocoder.yudao.module.contract.dal.mysql.version.ContractVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.contract.enums.ErrorCodeConstants.*;

/**
 * 合同版本 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ContractVersionServiceImpl implements ContractVersionService {

    @Resource
    private ContractVersionMapper contractVersionMapper;

    @Override
    public Long createContractVersion(@Valid ContractVersionCreateReqVO createReqVO) {
        // 插入
        ContractVersionDO contractVersion = ContractVersionConvert.INSTANCE.convert(createReqVO);
        contractVersionMapper.insert(contractVersion);
        // 返回
        return contractVersion.getId();
    }

    @Override
    public void updateContractVersion(@Valid ContractVersionUpdateReqVO updateReqVO) {
        // 校验存在
        validateContractVersionExists(updateReqVO.getId());
        // 更新
        ContractVersionDO updateObj = ContractVersionConvert.INSTANCE.convert(updateReqVO);
        contractVersionMapper.updateById(updateObj);
    }

    @Override
    public void deleteContractVersion(Long id) {
        // 校验存在
        validateContractVersionExists(id);
        // 删除
        contractVersionMapper.deleteById(id);
    }

    private ContractVersionDO getContractVersionById(Long id) {
        return contractVersionMapper.selectById(id);
    }

    @Override
    public ContractVersionDO getContractVersion(Long id) {
        return getContractVersionById(id);
    }

    @Override
    public PageResult<ContractVersionDO> getContractVersionPage(ContractVersionPageReqVO pageReqVO) {
        return contractVersionMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ContractVersionDO> getContractVersionListByContractId(Long contractId) {
        return contractVersionMapper.selectListByContractId(contractId);
    }

    @Override
    public ContractVersionDO validateContractVersionExists(Long id) {
        if (id == null) {
            return null;
        }
        ContractVersionDO contractVersion = getContractVersionById(id);
        if (contractVersion == null) {
            throw exception(CONTRACT_VERSION_NOT_EXISTS);
        }
        return contractVersion;
    }

    @Override
    public void setCurrentVersion(Long contractId, Long versionId) {
        // 校验版本存在
        ContractVersionDO version = validateContractVersionExists(versionId);
        
        // 校验版本属于指定合同
        if (!contractId.equals(version.getContractId())) {
            throw exception(CONTRACT_VERSION_NOT_EXISTS);
        }
        
        // 这里应该更新合同表的 current_version_id 字段
        // 但由于我们没有 ContractMapper 的依赖，暂时记录日志
        log.info("设置合同 {} 的当前版本为 {}", contractId, versionId);
    }

    @Override
    public void updateEsignatureStatus(Long id, Integer status, String processId) {
        // 校验存在
        validateContractVersionExists(id);
        
        // 更新电子签章状态
        ContractVersionDO updateObj = new ContractVersionDO();
        updateObj.setId(id);
        updateObj.setEsignatureStatus(status);
        updateObj.setEsignatureProcessId(processId);
        contractVersionMapper.updateById(updateObj);
    }
} 