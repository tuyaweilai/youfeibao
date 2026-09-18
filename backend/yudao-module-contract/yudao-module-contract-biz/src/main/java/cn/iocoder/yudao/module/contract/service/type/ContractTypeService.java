package cn.iocoder.yudao.module.contract.service.type;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.*;
import cn.iocoder.yudao.module.contract.dal.dataobject.type.ContractTypeDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 合同类型 Service 接口
 *
 * @author 芋道源码
 */
public interface ContractTypeService {

    /**
     * 创建合同类型
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createContractType(@Valid ContractTypeCreateReqVO createReqVO);

    /**
     * 更新合同类型
     *
     * @param updateReqVO 更新信息
     */
    void updateContractType(@Valid ContractTypeUpdateReqVO updateReqVO);

    /**
     * 删除合同类型
     *
     * @param id 编号
     */
    void deleteContractType(Long id);

    /**
     * 获得合同类型
     *
     * @param id 编号
     * @return 合同类型
     */
    ContractTypeDO getContractType(Long id);

    /**
     * 获得合同类型分页
     *
     * @param pageReqVO 分页查询
     * @return 合同类型分页
     */
    PageResult<ContractTypeDO> getContractTypePage(ContractTypePageReqVO pageReqVO);

    /**
     * 获得所有合同类型列表
     *
     * @return 合同类型列表
     */
    List<ContractTypeDO> getContractTypeList();

    /**
     * 校验合同类型是否存在
     *
     * @param id 编号
     * @return 合同类型
     */
    ContractTypeDO validateContractTypeExists(Long id);

} 