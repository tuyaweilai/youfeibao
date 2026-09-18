package cn.iocoder.yudao.module.contract.service.contract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.*;
import cn.iocoder.yudao.module.contract.dal.dataobject.contract.ContractDO;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 合同 Service 接口
 *
 * @author 芋道源码
 */
public interface ContractService {

    /**
     * 创建合同
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createContract(@Valid ContractCreateReqVO createReqVO);

    /**
     * 更新合同
     *
     * @param updateReqVO 更新信息
     */
    void updateContract(@Valid ContractUpdateReqVO updateReqVO);

    /**
     * 删除合同
     *
     * @param id 编号
     */
    void deleteContract(Long id);

    /**
     * 获得合同
     *
     * @param id 编号
     * @return 合同
     */
    ContractDO getContract(Long id);

    /**
     * 获得合同分页
     *
     * @param pageReqVO 分页查询
     * @return 合同分页
     */
    PageResult<ContractDO> getContractPage(ContractPageReqVO pageReqVO);

    /**
     * 获得合同列表
     *
     * @param ids 编号列表
     * @return 合同列表
     */
    List<ContractDO> getContractList(List<Long> ids);

    /**
     * 校验合同是否存在
     *
     * @param id 编号
     * @return 合同
     */
    ContractDO validateContractExists(Long id);

    /**
     * 提交合同审批
     *
     * @param id 编号
     */
    void submitContract(Long id);

    /**
     * 更新合同状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updateContractStatus(Long id, Integer status);

    /**
     * 获取即将到期的合同列表
     *
     * @param days 天数
     * @return 合同列表
     */
    List<ContractDO> getExpiringContracts(Integer days);

    /**
     * 根据合同编号获取合同
     *
     * @param contractNo 合同编号
     * @return 合同
     */
    ContractDO getContractByNo(String contractNo);

    /**
     * 生成合同编号
     *
     * @param typeId 合同类型ID
     * @return 合同编号
     */
    String generateContractNo(Long typeId);

    /**
     * 发起电子签署
     *
     * @param id 合同编号
     * @return 签署流程ID
     */
    String initiateEsignature(Long id);

    /**
     * 签署合同
     *
     * @param id 合同编号
     * @param partyId 参与方编号
     * @param signatureData 签署数据
     */
    void signContract(Long id, Long partyId, String signatureData);

    /**
     * 完成签署
     *
     * @param id 合同编号
     */
    void completeSignature(Long id);

    /**
     * 取消签署
     *
     * @param id 合同编号
     * @param reason 取消原因
     */
    void cancelSignature(Long id, String reason);

} 