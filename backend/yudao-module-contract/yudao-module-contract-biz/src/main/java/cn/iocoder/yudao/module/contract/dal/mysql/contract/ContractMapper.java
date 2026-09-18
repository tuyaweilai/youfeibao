package cn.iocoder.yudao.module.contract.dal.mysql.contract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.ContractPageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.contract.ContractDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 合同 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractMapper extends BaseMapperX<ContractDO> {

    default PageResult<ContractDO> selectPage(ContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContractDO>()
                .likeIfPresent(ContractDO::getContractNo, reqVO.getContractNo())
                .likeIfPresent(ContractDO::getContractName, reqVO.getContractName())
                .eqIfPresent(ContractDO::getTypeId, reqVO.getTypeId())
                .eqIfPresent(ContractDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ContractDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContractDO::getId));
    }

    /**
     * 根据合同编号查询合同
     * 
     * @param contractNo 合同编号
     * @return 合同对象
     */
    default ContractDO selectByContractNo(String contractNo) {
        return selectOne(ContractDO::getContractNo, contractNo);
    }
    
    /**
     * 查询特定前缀下的最大序号
     * 通过自定义SQL查询，提取合同编号中的序号部分并找出最大值
     * 
     * @param prefix 合同编号前缀
     * @return 最大序号
     */
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(contract_no, '-', -1) AS SIGNED)) " +
           "FROM contracts WHERE contract_no LIKE CONCAT(#{prefix}, '-%') " +
           "AND deleted = 0")
    Integer selectMaxSequenceByPrefix(@Param("prefix") String prefix);
} 