package cn.iocoder.yudao.module.contract.service.link;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.link.vo.ContractLinkedObjectCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.link.vo.ContractLinkedObjectPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.link.vo.ContractLinkedObjectUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.link.ContractLinkedObjectDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 合同关联对象 Service 接口
 *
 * @author 芋道源码
 */
public interface ContractLinkedObjectService {

    /**
     * 创建合同关联对象
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createContractLinkedObject(@Valid ContractLinkedObjectCreateReqVO createReqVO);

    /**
     * 更新合同关联对象
     *
     * @param updateReqVO 更新信息
     */
    void updateContractLinkedObject(@Valid ContractLinkedObjectUpdateReqVO updateReqVO);

    /**
     * 删除合同关联对象
     *
     * @param id 编号
     */
    void deleteContractLinkedObject(Long id);

    /**
     * 获得合同关联对象
     *
     * @param id 编号
     * @return 合同关联对象
     */
    ContractLinkedObjectDO getContractLinkedObject(Long id);

    /**
     * 获得合同关联对象分页
     *
     * @param pageReqVO 分页查询
     * @return 合同关联对象分页
     */
    PageResult<ContractLinkedObjectDO> getContractLinkedObjectPage(ContractLinkedObjectPageReqVO pageReqVO);

    /**
     * 获得合同关联对象列表
     *
     * @param contractId 合同ID
     * @return 合同关联对象列表
     */
    List<ContractLinkedObjectDO> getContractLinkedObjectListByContractId(Long contractId);

    /**
     * 获得对象关联的合同列表
     *
     * @param objectId 对象ID
     * @param objectType 对象类型
     * @return 合同关联对象列表
     */
    List<ContractLinkedObjectDO> getContractLinkedObjectListByObjectIdAndType(Long objectId, String objectType);

    /**
     * 校验合同关联对象是否存在
     *
     * @param id 编号
     * @return 合同关联对象
     */
    ContractLinkedObjectDO validateContractLinkedObjectExists(Long id);

    /**
     * 更新关联状态
     *
     * @param id 关联ID
     * @param linkStatus 关联状态
     */
    void updateLinkStatus(Long id, Integer linkStatus);

    /**
     * 检查关联是否存在
     *
     * @param contractId 合同ID
     * @param objectId 对象ID
     * @param objectType 对象类型
     * @return 是否存在
     */
    boolean checkLinkExists(Long contractId, Long objectId, String objectType);
} 