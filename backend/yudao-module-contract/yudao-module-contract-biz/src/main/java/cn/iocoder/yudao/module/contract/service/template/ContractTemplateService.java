package cn.iocoder.yudao.module.contract.service.template;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplateCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplatePageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplateUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.template.ContractTemplateDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 合同模板 Service 接口
 *
 * @author 芋道源码
 */
public interface ContractTemplateService {

    /**
     * 创建合同模板
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createContractTemplate(@Valid ContractTemplateCreateReqVO createReqVO);

    /**
     * 更新合同模板
     *
     * @param updateReqVO 更新信息
     */
    void updateContractTemplate(@Valid ContractTemplateUpdateReqVO updateReqVO);

    /**
     * 删除合同模板
     *
     * @param id 编号
     */
    void deleteContractTemplate(Long id);

    /**
     * 获得合同模板
     *
     * @param id 编号
     * @return 合同模板
     */
    ContractTemplateDO getContractTemplate(Long id);

    /**
     * 获得合同模板分页
     *
     * @param pageReqVO 分页查询
     * @return 合同模板分页
     */
    PageResult<ContractTemplateDO> getContractTemplatePage(ContractTemplatePageReqVO pageReqVO);

    /**
     * 获得合同模板列表
     *
     * @param contractTypeId 合同类型ID
     * @return 合同模板列表
     */
    List<ContractTemplateDO> getContractTemplateListByTypeId(Long contractTypeId);

    /**
     * 校验合同模板是否存在
     *
     * @param id 编号
     * @return 合同模板
     */
    ContractTemplateDO validateContractTemplateExists(Long id);

    /**
     * 校验合同模板编码是否存在
     *
     * @param templateCode 模板编码
     * @param id 编号（可为 null）
     */
    void validateContractTemplateCodeUnique(String templateCode, Long id);
} 