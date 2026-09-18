package cn.iocoder.yudao.module.contract.service.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.log.vo.ContractOperationLogPageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.log.ContractOperationLogDO;

import java.util.List;

/**
 * 合同操作日志 Service 接口
 *
 * @author 芋道源码
 */
public interface ContractOperationLogService {

    /**
     * 创建合同操作日志
     *
     * @param contractId 合同ID
     * @param versionId 合同版本ID，可为空
     * @param operationType 操作类型
     * @param operationDescription 操作描述
     * @param oldStatus 操作前状态，可为空
     * @param newStatus 操作后状态，可为空
     * @param operationData 操作数据，可为空
     * @return 日志编号
     */
    Long createContractOperationLog(Long contractId, Long versionId, Integer operationType, String operationDescription,
                                    Integer oldStatus, Integer newStatus, String operationData);

    /**
     * 创建合同状态变更日志
     *
     * @param contractId 合同ID
     * @param operationType 操作类型
     * @param operationDescription 操作描述
     * @param oldStatus 操作前状态
     * @param newStatus 操作后状态
     * @return 日志编号
     */
    Long createContractStatusChangeLog(Long contractId, Integer operationType, String operationDescription,
                                      Integer oldStatus, Integer newStatus);

    /**
     * 获得合同操作日志列表
     *
     * @param contractId 合同ID
     * @return 操作日志列表
     */
    List<ContractOperationLogDO> getContractOperationLogList(Long contractId);

    /**
     * 获得合同操作日志分页
     *
     * @param pageReqVO 分页查询
     * @return 操作日志分页
     */
    PageResult<ContractOperationLogDO> getContractOperationLogPage(ContractOperationLogPageReqVO pageReqVO);

} 