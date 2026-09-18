package cn.iocoder.yudao.module.contract.dal.mysql.party;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyPageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.party.ContractPartyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 合同参与方 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractPartyMapper extends BaseMapperX<ContractPartyDO> {

    /**
     * 根据合同ID查询参与方列表
     *
     * @param contractId 合同ID
     * @return 参与方列表
     */
    default List<ContractPartyDO> selectListByContractId(Long contractId) {
        return selectList(ContractPartyDO::getContractId, contractId);
    }

    /**
     * 根据合同ID和企业ID查询参与方
     *
     * @param contractId 合同ID
     * @param enterpriseId 企业ID
     * @return 参与方
     */
    default ContractPartyDO selectByContractIdAndEnterpriseId(Long contractId, Long enterpriseId) {
        return selectOne(new LambdaQueryWrapperX<ContractPartyDO>()
                .eq(ContractPartyDO::getContractId, contractId)
                .eq(ContractPartyDO::getEnterpriseId, enterpriseId));
    }

    /**
     * 根据合同ID和用户ID查询参与方
     *
     * @param contractId 合同ID
     * @param userId 用户ID
     * @return 参与方
     */
    default ContractPartyDO selectByContractIdAndUserId(Long contractId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<ContractPartyDO>()
                .eq(ContractPartyDO::getContractId, contractId)
                .eq(ContractPartyDO::getSignatoryUserId, userId));
    }

    /**
     * 分页查询合同参与方
     *
     * @param reqVO 分页查询条件
     * @return 合同参与方分页结果
     */
    default PageResult<ContractPartyDO> selectPage(ContractPartyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContractPartyDO>()
                .eqIfPresent(ContractPartyDO::getContractId, reqVO.getContractId())
                .eqIfPresent(ContractPartyDO::getEnterpriseId, reqVO.getEnterpriseId())
                .likeIfPresent(ContractPartyDO::getEnterpriseName, reqVO.getEnterpriseName())
                .eqIfPresent(ContractPartyDO::getSignStatus, reqVO.getSignStatus())
                .betweenIfPresent(ContractPartyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContractPartyDO::getId));
    }

    default List<ContractPartyDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(new LambdaQueryWrapperX<ContractPartyDO>()
                .eq(ContractPartyDO::getEnterpriseId, enterpriseId));
    }

} 