package cn.iocoder.yudao.module.enterprise.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStoreCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStorePageReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStoreUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseStoreDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 企业门店 Service 接口
 *
 * @author 芋道源码
 */
public interface EnterpriseStoreService {

    /**
     * 创建企业门店
     *
     * @param createReqVO 创建信息
     * @return 门店ID
     */
    Long createEnterpriseStore(@Valid EnterpriseStoreCreateReqVO createReqVO);

    /**
     * 更新企业门店
     *
     * @param updateReqVO 更新信息
     */
    void updateEnterpriseStore(@Valid EnterpriseStoreUpdateReqVO updateReqVO);

    /**
     * 删除企业门店
     *
     * @param id 门店ID
     */
    void deleteEnterpriseStore(Long id);

    /**
     * 获得企业门店
     *
     * @param id 门店ID
     * @return 企业门店
     */
    EnterpriseStoreDO getEnterpriseStore(Long id);

    /**
     * 获得企业门店列表
     *
     * @param enterpriseId 企业ID
     * @return 企业门店列表
     */
    List<EnterpriseStoreDO> getEnterpriseStoreListByEnterpriseId(Long enterpriseId);

    /**
     * 获得企业门店子列表
     *
     * @param parentId 父门店ID
     * @return 企业门店列表
     */
    List<EnterpriseStoreDO> getEnterpriseStoreListByParentId(Long parentId);

    /**
     * 获得企业门店分页
     *
     * @param pageReqVO 分页查询条件
     * @return 企业门店分页
     */
    PageResult<EnterpriseStoreDO> pageEnterpriseStore(EnterpriseStorePageReqVO pageReqVO);

    /**
     * 更新企业门店状态
     *
     * @param id 门店ID
     * @param status 状态
     */
    void updateEnterpriseStoreStatus(Long id, Integer status);

    /**
     * 获得企业门店树形列表
     *
     * @param enterpriseId 企业ID
     * @return 企业门店树形列表
     */
    List<EnterpriseStoreDO> getEnterpriseStoreTree(Long enterpriseId);
} 