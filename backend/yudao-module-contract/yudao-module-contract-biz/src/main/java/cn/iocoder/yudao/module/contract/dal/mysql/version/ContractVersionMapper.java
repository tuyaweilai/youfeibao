package cn.iocoder.yudao.module.contract.dal.mysql.version;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionPageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.version.ContractVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 合同版本 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractVersionMapper extends BaseMapperX<ContractVersionDO> {

    /**
     * 根据合同ID查询版本列表
     *
     * @param contractId 合同ID
     * @return 合同版本列表
     */
    default List<ContractVersionDO> selectListByContractId(Long contractId) {
        return selectList(ContractVersionDO::getContractId, contractId);
    }

    /**
     * 根据合同ID和版本号查询版本
     *
     * @param contractId 合同ID
     * @param versionNumber 版本号
     * @return 合同版本
     */
    default ContractVersionDO selectByContractIdAndVersionNumber(Long contractId, String versionNumber) {
        return selectOne(new LambdaQueryWrapperX<ContractVersionDO>()
                .eq(ContractVersionDO::getContractId, contractId)
                .eq(ContractVersionDO::getVersionNumber, versionNumber));
    }

    /**
     * 分页查询合同版本
     *
     * @param reqVO 分页查询条件
     * @return 合同版本分页结果
     */
    default PageResult<ContractVersionDO> selectPage(ContractVersionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContractVersionDO>()
                .eqIfPresent(ContractVersionDO::getContractId, reqVO.getContractId())
                .likeIfPresent(ContractVersionDO::getVersionNumber, reqVO.getVersionNumber())
                .eqIfPresent(ContractVersionDO::getEsignatureStatus, reqVO.getEsignatureStatus())
                .betweenIfPresent(ContractVersionDO::getEffectiveDate, reqVO.getEffectiveDate())
                .betweenIfPresent(ContractVersionDO::getExpiryDate, reqVO.getExpiryDate())
                .betweenIfPresent(ContractVersionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContractVersionDO::getId));
    }
} 