package cn.iocoder.yudao.module.contract.api;

import cn.iocoder.yudao.module.contract.api.dto.ContractValidateReqDTO;
import cn.iocoder.yudao.module.contract.api.dto.ContractValidateRespDTO;

import javax.validation.Valid;
import java.util.List;

/**
 * 合同 API 接口
 *
 * @author 芋道源码
 */
public interface ContractApi {

    /**
     * 校验业务合同有效性
     *
     * @param reqDTO 校验请求
     * @return 校验结果
     */
    ContractValidateRespDTO validateBusinessContract(@Valid ContractValidateReqDTO reqDTO);

    /**
     * 获取合同信息
     *
     * @param id 合同编号
     * @return 合同信息
     */
    ContractRespDTO getContract(Long id);

    /**
     * 创建合同关联
     *
     * @param contractId 合同编号
     * @param objectId 关联对象编号
     * @param objectType 关联对象类型
     * @param linkType 关联类型
     */
    void createContractLink(Long contractId, Long objectId, String objectType, Integer linkType);

    /**
     * 批量获取合同信息
     *
     * @param ids 合同编号列表
     * @return 合同信息列表
     */
    List<ContractRespDTO> getContractList(List<Long> ids);

    /**
     * 检查合同是否有效
     *
     * @param id 合同编号
     * @return 是否有效
     */
    boolean isContractValid(Long id);

    /**
     * 创建合同
     *
     * @param createReqDTO 创建请求
     * @return 合同ID
     */
    Long createContract(@Valid ContractCreateReqDTO createReqDTO);

    /**
     * 发起电子签署
     *
     * @param contractId 合同ID
     * @return 签署流程ID
     */
    String initiateEsignature(Long contractId);

} 