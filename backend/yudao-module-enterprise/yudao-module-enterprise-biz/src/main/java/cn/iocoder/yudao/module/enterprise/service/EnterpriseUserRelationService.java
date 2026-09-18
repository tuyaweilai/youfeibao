package cn.iocoder.yudao.module.enterprise.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationPageReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户企业关系 Service 接口
 *
 * @author 芋道源码
 */
public interface EnterpriseUserRelationService {

    /**
     * 创建用户企业关系
     *
     * @param createReqVO 创建信息
     * @return 关系ID
     */
    Long createUserRelation(@Valid EnterpriseUserRelationCreateReqVO createReqVO);

    /**
     * 更新用户企业关系
     *
     * @param updateReqVO 更新信息
     */
    void updateUserRelation(@Valid EnterpriseUserRelationUpdateReqVO updateReqVO);

    /**
     * 删除用户企业关系
     *
     * @param id 关系ID
     */
    void deleteUserRelation(Long id);

    /**
     * 获得用户企业关系
     *
     * @param id 关系ID
     * @return 用户企业关系
     */
    EnterpriseUserRelationDO getUserRelation(Long id);

    /**
     * 获得用户企业关系
     *
     * @param userId 用户ID
     * @param enterpriseId 企业ID
     * @return 用户企业关系
     */
    EnterpriseUserRelationDO getUserRelation(Long userId, Long enterpriseId);

    /**
     * 获得用户企业关系
     *
     * @param userId 用户ID
     * @param enterpriseId 企业ID
     * @param storeId 门店ID
     * @return 用户企业关系
     */
    EnterpriseUserRelationDO getUserRelation(Long userId, Long enterpriseId, Long storeId);

    /**
     * 获得用户企业关系列表
     *
     * @param userId 用户ID
     * @return 用户企业关系列表
     */
    List<EnterpriseUserRelationDO> getUserRelationListByUserId(Long userId);

    /**
     * 获得企业用户关系列表
     *
     * @param enterpriseId 企业ID
     * @return 用户企业关系列表
     */
    List<EnterpriseUserRelationDO> getUserRelationListByEnterpriseId(Long enterpriseId);

    /**
     * 获得门店用户关系列表
     *
     * @param storeId 门店ID
     * @return 用户企业关系列表
     */
    List<EnterpriseUserRelationDO> getUserRelationListByStoreId(Long storeId);

    /**
     * 获得用户企业关系分页
     *
     * @param pageReqVO 分页查询条件
     * @return 用户企业关系分页
     */
    PageResult<EnterpriseUserRelationDO> pageUserRelation(EnterpriseUserRelationPageReqVO pageReqVO);

    /**
     * 设置默认企业
     *
     * @param userId 用户ID
     * @param relationId 要设为默认的关系ID
     */
    void setDefaultEnterprise(Long userId, Long relationId);
} 