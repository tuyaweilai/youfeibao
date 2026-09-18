package cn.iocoder.yudao.module.contract.dal.mysql.link;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.contract.controller.admin.link.vo.ContractLinkedObjectPageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.link.ContractLinkedObjectDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 合同关联对象 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractLinkedObjectMapper extends BaseMapperX<ContractLinkedObjectDO> {

    /**
     * 根据合同ID查询关联对象列表
     *
     * @param contractId 合同ID
     * @return 关联对象列表
     */
    default List<ContractLinkedObjectDO> selectListByContractId(Long contractId) {
        return selectList(ContractLinkedObjectDO::getContractId, contractId);
    }

    /**
     * 根据版本ID查询关联对象列表
     *
     * @param versionId 版本ID
     * @return 关联对象列表
     */
    default List<ContractLinkedObjectDO> selectListByVersionId(Long versionId) {
        return selectList(ContractLinkedObjectDO::getVersionId, versionId);
    }

    /**
     * 根据对象ID和对象类型查询关联列表
     *
     * @param objectId 对象ID
     * @param objectType 对象类型
     * @return 关联对象列表
     */
    default List<ContractLinkedObjectDO> selectListByObjectIdAndType(Long objectId, String objectType) {
        return selectList(new LambdaQueryWrapperX<ContractLinkedObjectDO>()
                .eq(ContractLinkedObjectDO::getObjectId, objectId)
                .eq(ContractLinkedObjectDO::getObjectType, objectType));
    }

    /**
     * 分页查询合同关联对象
     *
     * @param reqVO 分页查询条件
     * @return 合同关联对象分页结果
     */
    default PageResult<ContractLinkedObjectDO> selectPage(ContractLinkedObjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContractLinkedObjectDO>()
                .eqIfPresent(ContractLinkedObjectDO::getContractId, reqVO.getContractId())
                .eqIfPresent(ContractLinkedObjectDO::getVersionId, reqVO.getVersionId())
                .eqIfPresent(ContractLinkedObjectDO::getObjectType, reqVO.getObjectType())
                .eqIfPresent(ContractLinkedObjectDO::getLinkType, reqVO.getLinkType())
                .eqIfPresent(ContractLinkedObjectDO::getLinkStatus, reqVO.getLinkStatus())
                .betweenIfPresent(ContractLinkedObjectDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContractLinkedObjectDO::getId));
    }

    /**
     * 查询关联是否已存在
     *
     * @param contractId 合同ID
     * @param objectId 对象ID
     * @param objectType 对象类型
     * @return 关联对象
     */
    default ContractLinkedObjectDO selectLinkExists(Long contractId, Long objectId, String objectType) {
        return selectOne(new LambdaQueryWrapperX<ContractLinkedObjectDO>()
                .eq(ContractLinkedObjectDO::getContractId, contractId)
                .eq(ContractLinkedObjectDO::getObjectId, objectId)
                .eq(ContractLinkedObjectDO::getObjectType, objectType));
    }
} 