package cn.iocoder.yudao.module.contract.dal.mysql.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.contract.controller.admin.log.vo.ContractOperationLogPageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.log.ContractOperationLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 合同操作日志 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractOperationLogMapper extends BaseMapperX<ContractOperationLogDO> {

    /**
     * 查询合同操作日志列表
     *
     * @param contractId 合同ID
     * @return 操作日志列表
     */
    default List<ContractOperationLogDO> selectListByContractId(Long contractId) {
        return selectList(ContractOperationLogDO::getContractId, contractId);
    }

    /**
     * 分页查询合同操作日志
     *
     * @param reqVO 分页请求
     * @return 分页结果
     */
    default PageResult<ContractOperationLogDO> selectPage(ContractOperationLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContractOperationLogDO>()
                .eqIfPresent(ContractOperationLogDO::getContractId, reqVO.getContractId())
                .eqIfPresent(ContractOperationLogDO::getOperationType, reqVO.getOperationType())
                .likeIfPresent(ContractOperationLogDO::getOperatorName, reqVO.getOperatorName())
                .betweenIfPresent(ContractOperationLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContractOperationLogDO::getId));
    }

} 