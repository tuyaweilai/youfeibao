package cn.iocoder.yudao.module.enterprise.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationPageReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseUserRelationConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import cn.iocoder.yudao.module.enterprise.dal.mysql.EnterpriseUserRelationMapper;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseStoreService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseUserRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.enterprise.enums.ErrorCodeConstants.*;

/**
 * 用户企业关系 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class EnterpriseUserRelationServiceImpl implements EnterpriseUserRelationService {

    @Resource
    private EnterpriseUserRelationMapper enterpriseUserRelationMapper;

    @Resource
    private EnterpriseInfoService enterpriseInfoService;

    @Resource
    private EnterpriseStoreService enterpriseStoreService;

    @Override
    public Long createUserRelation(EnterpriseUserRelationCreateReqVO createReqVO) {
        // 校验企业存在
        validateEnterpriseExists(createReqVO.getEnterpriseId());
        // 校验门店存在
        validateStoreExists(createReqVO.getStoreId());

        // 校验是否已存在相同关系（用户-企业-门店）
        validateUserRelationNotExists(null, createReqVO.getUserId(), createReqVO.getEnterpriseId(), createReqVO.getStoreId());

        // 创建新关系
        EnterpriseUserRelationDO relation = EnterpriseUserRelationConvert.INSTANCE.convert(createReqVO);
        
        // 设置默认值
        if (relation.getIsPrimaryContact() == null) {
            relation.setIsPrimaryContact(false);
        }
        if (relation.getIsDefaultEnterprise() == null) {
            relation.setIsDefaultEnterprise(false);
        }

        // 如果设置为默认企业，则将用户其他关系的默认标志清除
        if (Boolean.TRUE.equals(relation.getIsDefaultEnterprise())) {
            updateOtherRelationsNotDefault(relation.getUserId());
        }

        // 保存关系记录
        enterpriseUserRelationMapper.insert(relation);
        return relation.getId();
    }

    @Override
    public void updateUserRelation(EnterpriseUserRelationUpdateReqVO updateReqVO) {
        // 校验关系存在
        validateUserRelationExists(updateReqVO.getId());
        // 校验企业存在
        validateEnterpriseExists(updateReqVO.getEnterpriseId());
        // 校验门店存在
        validateStoreExists(updateReqVO.getStoreId());
        
        // 校验是否已存在相同关系（用户-企业-门店）
        validateUserRelationNotExists(updateReqVO.getId(), updateReqVO.getUserId(), 
                updateReqVO.getEnterpriseId(), updateReqVO.getStoreId());

        // 更新关系
        EnterpriseUserRelationDO updateObj = EnterpriseUserRelationConvert.INSTANCE.convert(updateReqVO);

        // 如果设置为默认企业，则将用户其他关系的默认标志清除
        if (Boolean.TRUE.equals(updateObj.getIsDefaultEnterprise())) {
            updateOtherRelationsNotDefault(updateObj.getUserId());
        }
        
        enterpriseUserRelationMapper.updateById(updateObj);
    }

    @Override
    public void deleteUserRelation(Long id) {
        // 校验关系存在
        validateUserRelationExists(id);
        // 执行删除
        enterpriseUserRelationMapper.deleteById(id);
    }

    @Override
    public EnterpriseUserRelationDO getUserRelation(Long id) {
        return enterpriseUserRelationMapper.selectById(id);
    }

    @Override
    public EnterpriseUserRelationDO getUserRelation(Long userId, Long enterpriseId) {
        return enterpriseUserRelationMapper.selectByUserIdAndEnterpriseId(userId, enterpriseId);
    }

    @Override
    public EnterpriseUserRelationDO getUserRelation(Long userId, Long enterpriseId, Long storeId) {
        return enterpriseUserRelationMapper.selectByUserIdAndEnterpriseIdAndStoreId(userId, enterpriseId, storeId);
    }

    @Override
    public List<EnterpriseUserRelationDO> getUserRelationListByUserId(Long userId) {
        return enterpriseUserRelationMapper.selectListByUserId(userId);
    }

    @Override
    public List<EnterpriseUserRelationDO> getUserRelationListByEnterpriseId(Long enterpriseId) {
        return enterpriseUserRelationMapper.selectListByEnterpriseId(enterpriseId);
    }

    @Override
    public List<EnterpriseUserRelationDO> getUserRelationListByStoreId(Long storeId) {
        return enterpriseUserRelationMapper.selectListByStoreId(storeId);
    }

    @Override
    public PageResult<EnterpriseUserRelationDO> pageUserRelation(EnterpriseUserRelationPageReqVO pageReqVO) {
        return enterpriseUserRelationMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultEnterprise(Long userId, Long relationId) {
        // 校验关系存在
        EnterpriseUserRelationDO relation = validateUserRelationExists(relationId);
        // 校验关系属于指定用户
        if (!userId.equals(relation.getUserId())) {
            throw exception(ENTERPRISE_USER_RELATION_NOT_MATCH_USER);
        }
        
        // 清除用户其他关系的默认标志
        updateOtherRelationsNotDefault(userId);
        
        // 设置当前关系为默认
        EnterpriseUserRelationDO updateObj = new EnterpriseUserRelationDO();
        updateObj.setId(relationId);
        updateObj.setIsDefaultEnterprise(true);
        enterpriseUserRelationMapper.updateById(updateObj);
    }

    /**
     * 更新用户其他关系为非默认
     *
     * @param userId 用户ID
     */
    private void updateOtherRelationsNotDefault(Long userId) {
        List<EnterpriseUserRelationDO> userRelations = enterpriseUserRelationMapper.selectListByUserId(userId);
        for (EnterpriseUserRelationDO relation : userRelations) {
            if (Boolean.TRUE.equals(relation.getIsDefaultEnterprise())) {
                EnterpriseUserRelationDO updateObj = new EnterpriseUserRelationDO();
                updateObj.setId(relation.getId());
                updateObj.setIsDefaultEnterprise(false);
                enterpriseUserRelationMapper.updateById(updateObj);
            }
        }
    }

    private EnterpriseUserRelationDO validateUserRelationExists(Long id) {
        EnterpriseUserRelationDO relation = enterpriseUserRelationMapper.selectById(id);
        if (relation == null) {
            throw exception(ENTERPRISE_USER_RELATION_NOT_EXISTS);
        }
        return relation;
    }

    private void validateEnterpriseExists(Long enterpriseId) {
        if (enterpriseId == null) {
            return;
        }
        if (enterpriseInfoService.getEnterpriseInfo(enterpriseId) == null) {
            throw exception(ENTERPRISE_INFO_NOT_EXISTS);
        }
    }

    private void validateStoreExists(Long storeId) {
        if (storeId == null) {
            return;
        }
        if (enterpriseStoreService.getEnterpriseStore(storeId) == null) {
            throw exception(ENTERPRISE_STORE_NOT_EXISTS);
        }
    }

    private void validateUserRelationNotExists(Long id, Long userId, Long enterpriseId, Long storeId) {
        EnterpriseUserRelationDO existRelation = enterpriseUserRelationMapper.selectByUserIdAndEnterpriseIdAndStoreId(
                userId, enterpriseId, storeId);
        if (existRelation == null) {
            return;
        }
        // 如果是更新且ID相同则可以通过
        if (id != null && existRelation.getId().equals(id)) {
            return;
        }
        throw exception(ENTERPRISE_USER_RELATION_ALREADY_EXISTS);
    }
} 