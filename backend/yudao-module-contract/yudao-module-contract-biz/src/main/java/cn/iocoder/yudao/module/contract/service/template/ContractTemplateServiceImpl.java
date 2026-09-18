package cn.iocoder.yudao.module.contract.service.template;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplateCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplatePageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplateUpdateReqVO;
import cn.iocoder.yudao.module.contract.convert.template.ContractTemplateConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.template.ContractTemplateDO;
import cn.iocoder.yudao.module.contract.dal.mysql.template.ContractTemplateMapper;
import cn.iocoder.yudao.module.contract.service.type.ContractTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.contract.enums.ErrorCodeConstants.*;

/**
 * 合同模板 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ContractTemplateServiceImpl implements ContractTemplateService {

    @Resource
    private ContractTemplateMapper contractTemplateMapper;

    @Resource
    private ContractTypeService contractTypeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContractTemplate(ContractTemplateCreateReqVO createReqVO) {
        // 校验合同类型
        contractTypeService.validateContractTypeExists(createReqVO.getContractTypeId());
        
        // 校验模板编码唯一性
        validateContractTemplateCodeUnique(createReqVO.getTemplateCode(), null);
        
        // 插入
        ContractTemplateDO contractTemplate = ContractTemplateConvert.INSTANCE.convert(createReqVO);
        contractTemplateMapper.insert(contractTemplate);
        
        // 返回
        return contractTemplate.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractTemplate(ContractTemplateUpdateReqVO updateReqVO) {
        // 校验存在
        validateContractTemplateExists(updateReqVO.getId());
        
        // 校验合同类型
        contractTypeService.validateContractTypeExists(updateReqVO.getContractTypeId());
        
        // 校验模板编码唯一性
        validateContractTemplateCodeUnique(updateReqVO.getTemplateCode(), updateReqVO.getId());
        
        // 更新
        ContractTemplateDO updateObj = ContractTemplateConvert.INSTANCE.convert(updateReqVO);
        contractTemplateMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContractTemplate(Long id) {
        // 校验存在
        validateContractTemplateExists(id);
        
        // TODO: 校验是否有合同在使用该模板
        // 删除
        contractTemplateMapper.deleteById(id);
    }

    @Override
    public ContractTemplateDO getContractTemplate(Long id) {
        return contractTemplateMapper.selectById(id);
    }

    @Override
    public PageResult<ContractTemplateDO> getContractTemplatePage(ContractTemplatePageReqVO pageReqVO) {
        return contractTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ContractTemplateDO> getContractTemplateListByTypeId(Long contractTypeId) {
        return contractTemplateMapper.selectListByContractTypeId(contractTypeId);
    }

    @Override
    public ContractTemplateDO validateContractTemplateExists(Long id) {
        ContractTemplateDO contractTemplate = contractTemplateMapper.selectById(id);
        if (contractTemplate == null) {
            throw exception(CONTRACT_TEMPLATE_NOT_EXISTS);
        }
        return contractTemplate;
    }

    @Override
    public void validateContractTemplateCodeUnique(String templateCode, Long id) {
        ContractTemplateDO template = contractTemplateMapper.selectByTemplateCode(templateCode);
        if (template == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的模板
        if (id == null) {
            throw exception(CONTRACT_TEMPLATE_CODE_DUPLICATE);
        }
        if (!template.getId().equals(id)) {
            throw exception(CONTRACT_TEMPLATE_CODE_DUPLICATE);
        }
    }
} 