package cn.iocoder.yudao.module.contract.service.version;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.ContractVersionUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.version.ContractVersionDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 合同版本 Service 接口
 *
 * @author 芋道源码
 */
public interface ContractVersionService {

    /**
     * 创建合同版本
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createContractVersion(@Valid ContractVersionCreateReqVO createReqVO);

    /**
     * 更新合同版本
     *
     * @param updateReqVO 更新信息
     */
    void updateContractVersion(@Valid ContractVersionUpdateReqVO updateReqVO);

    /**
     * 删除合同版本
     *
     * @param id 编号
     */
    void deleteContractVersion(Long id);

    /**
     * 获得合同版本
     *
     * @param id 编号
     * @return 合同版本
     */
    ContractVersionDO getContractVersion(Long id);

    /**
     * 获得合同版本分页
     *
     * @param pageReqVO 分页查询
     * @return 合同版本分页
     */
    PageResult<ContractVersionDO> getContractVersionPage(ContractVersionPageReqVO pageReqVO);

    /**
     * 获得合同版本列表
     *
     * @param contractId 合同ID
     * @return 合同版本列表
     */
    List<ContractVersionDO> getContractVersionListByContractId(Long contractId);

    /**
     * 校验合同版本是否存在
     *
     * @param id 编号
     * @return 合同版本
     */
    ContractVersionDO validateContractVersionExists(Long id);

    /**
     * 设置为当前版本
     *
     * @param contractId 合同ID
     * @param versionId 版本ID
     */
    void setCurrentVersion(Long contractId, Long versionId);

    /**
     * 更新电子签章状态
     *
     * @param id 版本ID
     * @param status 状态
     * @param processId 第三方流程ID
     */
    void updateEsignatureStatus(Long id, Integer status, String processId);
} 