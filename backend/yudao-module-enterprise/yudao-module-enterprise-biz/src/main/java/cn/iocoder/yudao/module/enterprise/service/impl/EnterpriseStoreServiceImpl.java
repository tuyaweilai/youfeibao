package cn.iocoder.yudao.module.enterprise.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStoreCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStorePageReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStoreUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseStoreConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseStoreDO;
import cn.iocoder.yudao.module.enterprise.dal.mysql.EnterpriseStoreMapper;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseStoreStatusEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.enterprise.enums.ErrorCodeConstants.*;
import cn.hutool.core.collection.CollUtil;

/**
 * 企业门店 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class EnterpriseStoreServiceImpl implements EnterpriseStoreService {

    @Resource
    private EnterpriseStoreMapper enterpriseStoreMapper;

    @Resource
    private EnterpriseInfoService enterpriseInfoService;

    @Override
    public Long createEnterpriseStore(EnterpriseStoreCreateReqVO createReqVO) {
        // 校验父门店存在
        if (createReqVO.getParentId() != null && createReqVO.getParentId() > 0) {
            validateParentStoreExists(createReqVO.getParentId());
        }
        // 校验门店编码唯一
        validateStoreCode(createReqVO.getStoreCode());
        // 校验门店名称唯一
        validateStoreNameUnique(null, createReqVO.getParentId(), createReqVO.getName());

        // 插入门店
        EnterpriseStoreDO store = EnterpriseStoreConvert.INSTANCE.convert(createReqVO);
        store.setStatus(EnterpriseStoreStatusEnum.ENABLED.getStatus()); // 默认开启
        enterpriseStoreMapper.insert(store);
        return store.getId();
    }

    @Override
    public void updateEnterpriseStore(EnterpriseStoreUpdateReqVO updateReqVO) {
        // 校验门店存在
        validateStoreExists(updateReqVO.getId());
        // 校验企业存在
        validateEnterpriseExists(updateReqVO.getEnterpriseId());
        // 校验父门店是否存在
        validateParentStoreExists(updateReqVO.getParentId());
        // 校验父门店不能是自己或自己的子门店
        validateParentStoreNotSelfOrChild(updateReqVO.getId(), updateReqVO.getParentId());
        // 校验门店名称在同一父门店下是否唯一
        validateStoreNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());

        // 更新企业门店
        EnterpriseStoreDO updateObj = EnterpriseStoreConvert.INSTANCE.convert(updateReqVO);
        enterpriseStoreMapper.updateById(updateObj);
    }

    @Override
    public void deleteEnterpriseStore(Long id) {
        // 校验门店存在
        validateStoreExists(id);
        // 校验是否有子门店
        validateNoChildrenStore(id);
        // 执行删除
        enterpriseStoreMapper.deleteById(id);
    }

    private void validateStoreExists(Long id) {
        if (id == null) {
            return;
        }
        if (enterpriseStoreMapper.selectById(id) == null) {
            throw exception(ENTERPRISE_STORE_NOT_EXISTS);
        }
    }

    private void validateEnterpriseExists(Long enterpriseId) {
        if (enterpriseId == null) {
            return;
        }
        if (enterpriseInfoService.getEnterpriseInfo(enterpriseId) == null) {
            throw exception(ENTERPRISE_INFO_NOT_EXISTS);
        }
    }

    private void validateParentStoreExists(Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (enterpriseStoreMapper.selectById(parentId) == null) {
            throw exception(ENTERPRISE_STORE_PARENT_NOT_EXISTS);
        }
    }

    private void validateParentStoreNotSelfOrChild(Long id, Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        // 父门店不能是自己
        if (parentId.equals(id)) {
            throw exception(ENTERPRISE_STORE_PARENT_ERROR);
        }
        // 父门店不能是自己的子门店
        List<EnterpriseStoreDO> children = getEnterpriseStoreListByParentId(id);
        if (children != null && !children.isEmpty()) {
            for (EnterpriseStoreDO child : children) {
                if (parentId.equals(child.getId())) {
                    throw exception(ENTERPRISE_STORE_PARENT_IS_CHILD);
                }
                // 递归检查子门店的子门店
                validateParentStoreNotSelfOrChild(child.getId(), parentId);
            }
        }
    }

    private void validateNoChildrenStore(Long id) {
        List<EnterpriseStoreDO> children = getEnterpriseStoreListByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw exception(ENTERPRISE_STORE_CONTAINS_CHILDREN);
        }
    }

    @Override
    public EnterpriseStoreDO getEnterpriseStore(Long id) {
        return enterpriseStoreMapper.selectById(id);
    }

    @Override
    public List<EnterpriseStoreDO> getEnterpriseStoreListByEnterpriseId(Long enterpriseId) {
        return enterpriseStoreMapper.selectListByEnterpriseId(enterpriseId);
    }

    @Override
    public List<EnterpriseStoreDO> getEnterpriseStoreListByParentId(Long parentId) {
        return enterpriseStoreMapper.selectListByParentId(parentId);
    }

    @Override
    public PageResult<EnterpriseStoreDO> pageEnterpriseStore(EnterpriseStorePageReqVO pageReqVO) {
        return enterpriseStoreMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateEnterpriseStoreStatus(Long id, Integer status) {
        // 校验存在
        validateStoreExists(id);
        // 校验状态值是否合法
        validateStoreStatus(status);

        // 更新状态
        EnterpriseStoreDO updateObj = new EnterpriseStoreDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        enterpriseStoreMapper.updateById(updateObj);
    }

    private void validateStoreStatus(Integer status) {
        boolean valid = false;
        for (EnterpriseStoreStatusEnum statusEnum : EnterpriseStoreStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw exception(ENTERPRISE_STORE_STATUS_NOT_VALID);
        }
    }

    private void validateStoreCode(String code) {
        if (enterpriseStoreMapper.selectByCode(code) != null) {
            throw exception(ENTERPRISE_STORE_CODE_EXISTS);
        }
    }

    private void validateStoreNameUnique(Long id, Long parentId, String name) {
        EnterpriseStoreDO store = enterpriseStoreMapper.selectByParentIdAndName(parentId, name);
        if (store == null) {
            return;
        }
        // 如果是更新且ID相同则可以通过
        if (id != null && store.getId().equals(id)) {
            return;
        }
        throw exception(ENTERPRISE_STORE_NAME_EXISTS);
    }

    @Override
    public List<EnterpriseStoreDO> getEnterpriseStoreTree(Long enterpriseId) {
        // 1. 获取企业下所有门店
        List<EnterpriseStoreDO> list = getEnterpriseStoreListByEnterpriseId(enterpriseId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        // 2. 构建父子结构
        Map<Long, List<EnterpriseStoreDO>> parentChildrenMap = new HashMap<>();
        List<EnterpriseStoreDO> rootList = new ArrayList<>();
        for (EnterpriseStoreDO store : list) {
            if (store.getParentId() == null || store.getParentId() == 0) {
                rootList.add(store);
                continue;
            }
            parentChildrenMap.computeIfAbsent(store.getParentId(), k -> new ArrayList<>())
                    .add(store);
        }

        // 3. 递归设置子门店
        setChildren(rootList, parentChildrenMap);
        return rootList;
    }

    private void setChildren(List<EnterpriseStoreDO> stores, Map<Long, List<EnterpriseStoreDO>> parentChildrenMap) {
        if (CollUtil.isEmpty(stores)) {
            return;
        }
        for (EnterpriseStoreDO store : stores) {
            List<EnterpriseStoreDO> children = parentChildrenMap.get(store.getId());
            if (CollUtil.isNotEmpty(children)) {
                setChildren(children, parentChildrenMap);
            }
        }
    }
} 