package cn.iocoder.yudao.module.contract.service.party;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.ContractPartyUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.party.ContractPartyDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 合同参与方 Service 接口
 *
 * @author 芋道源码
 */
public interface ContractPartyService {

    /**
     * 创建合同参与方
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createContractParty(@Valid ContractPartyCreateReqVO createReqVO);

    /**
     * 更新合同参与方
     *
     * @param updateReqVO 更新信息
     */
    void updateContractParty(@Valid ContractPartyUpdateReqVO updateReqVO);

    /**
     * 删除合同参与方
     *
     * @param id 编号
     */
    void deleteContractParty(Long id);

    /**
     * 获得合同参与方
     *
     * @param id 编号
     * @return 合同参与方
     */
    ContractPartyDO getContractParty(Long id);

    /**
     * 获得合同参与方分页
     *
     * @param pageReqVO 分页查询
     * @return 合同参与方分页
     */
    PageResult<ContractPartyDO> getContractPartyPage(ContractPartyPageReqVO pageReqVO);

    /**
     * 获得合同参与方列表（根据合同ID）
     *
     * @param contractId 合同ID
     * @return 合同参与方列表
     */
    List<ContractPartyDO> getContractPartyListByContractId(Long contractId);

    /**
     * 校验合同参与方是否存在
     *
     * @param id 编号
     * @return 合同参与方
     */
    ContractPartyDO validateContractPartyExists(Long id);

    /**
     * 更新签署状态
     *
     * @param id 参与方ID
     * @param signStatus 签署状态
     * @param signIp 签署IP
     */
    void updateSignStatus(Long id, Integer signStatus, String signIp);

    /**
     * 批量创建合同参与方
     *
     * @param contractId 合同ID
     * @param parties 参与方列表
     * @return 创建的参与方IDs
     */
    List<Long> batchCreateContractParties(Long contractId, List<ContractPartyCreateReqVO> parties);
} 