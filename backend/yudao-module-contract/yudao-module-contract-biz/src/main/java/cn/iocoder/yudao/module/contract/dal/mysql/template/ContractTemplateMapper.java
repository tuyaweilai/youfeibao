package cn.iocoder.yudao.module.contract.dal.mysql.template;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.ContractTemplatePageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.template.ContractTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 合同模板 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractTemplateMapper extends BaseMapperX<ContractTemplateDO> {

    /**
     * 根据模板编码查询模板
     *
     * @param templateCode 模板编码
     * @return 合同模板
     */
    default ContractTemplateDO selectByTemplateCode(String templateCode) {
        return selectOne(ContractTemplateDO::getTemplateCode, templateCode);
    }

    /**
     * 根据合同类型ID查询模板列表
     *
     * @param contractTypeId 合同类型ID
     * @return 合同模板列表
     */
    default List<ContractTemplateDO> selectListByContractTypeId(Long contractTypeId) {
        return selectList(ContractTemplateDO::getContractTypeId, contractTypeId);
    }

    /**
     * 分页查询合同模板
     *
     * @param reqVO 分页查询条件
     * @return 合同模板分页结果
     */
    default PageResult<ContractTemplateDO> selectPage(ContractTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContractTemplateDO>()
                .likeIfPresent(ContractTemplateDO::getTemplateName, reqVO.getTemplateName())
                .likeIfPresent(ContractTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .eqIfPresent(ContractTemplateDO::getContractTypeId, reqVO.getContractTypeId())
                .eqIfPresent(ContractTemplateDO::getIsActive, reqVO.getIsActive())
                .betweenIfPresent(ContractTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContractTemplateDO::getId));
    }
} 