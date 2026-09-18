package cn.iocoder.yudao.module.contract.service.type;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.ContractTypeCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.ContractTypePageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.ContractTypeUpdateReqVO;
import cn.iocoder.yudao.module.contract.convert.type.ContractTypeConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.type.ContractTypeDO;
import cn.iocoder.yudao.module.contract.dal.mysql.type.ContractTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.contract.enums.ErrorCodeConstants.*;

/**
 * 合同类型 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ContractTypeServiceImpl implements ContractTypeService {

    @Resource
    private ContractTypeMapper contractTypeMapper;

    @Override
    public Long createContractType(ContractTypeCreateReqVO createReqVO) {
        // 校验编码唯一性
        validateCodeUnique(null, createReqVO.getCode());
        
        // 插入
        ContractTypeDO contractType = ContractTypeConvert.INSTANCE.convert(createReqVO);
        contractType.setSystemDefined(false); // 非系统预定义
        contractTypeMapper.insert(contractType);
        // 返回
        return contractType.getId();
    }

    @Override
    public void updateContractType(ContractTypeUpdateReqVO updateReqVO) {
        // 校验存在
        ContractTypeDO contractType = validateContractTypeExists(updateReqVO.getId());
        // 校验是否系统预定义
        if (Boolean.TRUE.equals(contractType.getSystemDefined())) {
            throw exception(CONTRACT_TYPE_SYSTEM_DEFINED);
        }
        // 校验编码唯一性
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        
        // 更新
        ContractTypeDO updateObj = ContractTypeConvert.INSTANCE.convert(updateReqVO);
        contractTypeMapper.updateById(updateObj);
    }

    @Override
    public void deleteContractType(Long id) {
        // 校验存在
        ContractTypeDO contractType = validateContractTypeExists(id);
        // 校验是否系统预定义
        if (Boolean.TRUE.equals(contractType.getSystemDefined())) {
            throw exception(CONTRACT_TYPE_SYSTEM_DEFINED);
        }
        // TODO: 校验是否有合同使用该类型
        
        // 删除
        contractTypeMapper.deleteById(id);
    }

    @Override
    public ContractTypeDO getContractType(Long id) {
        return contractTypeMapper.selectById(id);
    }

    @Override
    public PageResult<ContractTypeDO> getContractTypePage(ContractTypePageReqVO pageReqVO) {
        return contractTypeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ContractTypeDO> getContractTypeList() {
        return contractTypeMapper.selectList();
    }

    @Override
    public ContractTypeDO validateContractTypeExists(Long id) {
        ContractTypeDO contractType = contractTypeMapper.selectById(id);
        if (contractType == null) {
            throw exception(CONTRACT_TYPE_NOT_EXISTS);
        }
        return contractType;
    }

    private void validateCodeUnique(Long id, String code) {
        ContractTypeDO contractType = contractTypeMapper.selectByCode(code);
        if (contractType == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的合同类型
        if (id == null) {
            throw exception(CONTRACT_TYPE_CODE_DUPLICATE);
        }
        if (!contractType.getId().equals(id)) {
            throw exception(CONTRACT_TYPE_CODE_DUPLICATE);
        }
    }

} 