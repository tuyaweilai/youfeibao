package cn.iocoder.yudao.module.enterprise.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoPageReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoAuditReqVO;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseInfoConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import cn.iocoder.yudao.module.enterprise.dal.mysql.EnterpriseInfoMapper;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseStatusEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.enterprise.enums.ErrorCodeConstants.*;
import cn.hutool.core.collection.CollUtil;

/**
 * 企业信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class EnterpriseInfoServiceImpl implements EnterpriseInfoService {

    @Resource
    private EnterpriseInfoMapper enterpriseInfoMapper;

    @Override
    public Long createEnterpriseInfo(EnterpriseInfoCreateReqVO createReqVO) {
        // 校验企业名称是否唯一
        validateEnterpriseNameUnique(null, createReqVO.getName());
        // 校验统一社会信用代码是否唯一
        validateCreditCodeUnique(null, createReqVO.getCreditCode());

        // 插入企业信息
        EnterpriseInfoDO enterpriseInfo = EnterpriseInfoConvert.INSTANCE.convert(createReqVO);
        // 设置初始状态为入驻待审核
        enterpriseInfo.setStatus(EnterpriseStatusEnum.PENDING_ONBOARDING_REVIEW.getStatus());
        enterpriseInfoMapper.insert(enterpriseInfo);
        return enterpriseInfo.getId();
    }

    @Override
    public void updateEnterpriseInfo(EnterpriseInfoUpdateReqVO updateReqVO) {
        // 校验企业信息存在
        validateEnterpriseInfoExists(updateReqVO.getId());
        // 校验企业名称是否唯一
        validateEnterpriseNameUnique(updateReqVO.getId(), updateReqVO.getName());
        // 校验统一社会信用代码是否重复
        validateCreditCodeUnique(updateReqVO.getId(), updateReqVO.getCreditCode());

        // 更新企业信息
        EnterpriseInfoDO updateObj = EnterpriseInfoConvert.INSTANCE.convert(updateReqVO);
        enterpriseInfoMapper.updateById(updateObj);
    }

    @Override
    public void deleteEnterpriseInfo(Long id) {
        // 校验企业信息存在
        validateEnterpriseInfoExists(id);
        // 执行删除
        enterpriseInfoMapper.deleteById(id);
    }

    private void validateEnterpriseInfoExists(Long id) {
        if (enterpriseInfoMapper.selectById(id) == null) {
            throw exception(ENTERPRISE_INFO_NOT_EXISTS);
        }
    }

    private void validateCreditCodeUnique(Long id, String creditCode) {
        EnterpriseInfoDO enterpriseInfo = enterpriseInfoMapper.selectByCreditCode(creditCode);
        if (enterpriseInfo == null) {
            return;
        }
        // 如果是更新且ID相同则可以通过
        if (id != null && enterpriseInfo.getId().equals(id)) {
            return;
        }
        throw exception(ENTERPRISE_CREDIT_CODE_EXISTS);
    }

    private void validateEnterpriseNameUnique(Long id, String name) {
        EnterpriseInfoDO enterpriseInfo = enterpriseInfoMapper.selectByName(name);
        if (enterpriseInfo == null) {
            return;
        }
        // 如果是更新且ID相同则可以通过
        if (id != null && enterpriseInfo.getId().equals(id)) {
            return;
        }
        throw exception(ENTERPRISE_NAME_EXISTS);
    }

    @Override
    public EnterpriseInfoDO getEnterpriseInfo(Long id) {
        return enterpriseInfoMapper.selectById(id);
    }

    @Override
    public PageResult<EnterpriseInfoDO> pageEnterpriseInfo(EnterpriseInfoPageReqVO pageReqVO) {
        return enterpriseInfoMapper.selectPage(pageReqVO);
    }

    @Override
    public EnterpriseInfoDO getEnterpriseInfoByCreditCode(String creditCode) {
        return enterpriseInfoMapper.selectByCreditCode(creditCode);
    }
    
    @Override
    public List<EnterpriseInfoDO> getEnterpriseInfoSimpleList(String name, Integer status, Integer enterpriseType) {
        return enterpriseInfoMapper.selectSimpleList(name, status, enterpriseType);
    }

    @Override
    public List<EnterpriseInfoDO> getEnterpriseInfoList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return enterpriseInfoMapper.selectBatchIds(ids);
    }

    @Override
    public void auditEnterpriseInfo(EnterpriseInfoAuditReqVO auditReqVO) {
        // 1. 校验企业存在
        EnterpriseInfoDO enterprise = enterpriseInfoMapper.selectById(auditReqVO.getId());
        if (enterprise == null) {
            throw exception(ENTERPRISE_INFO_NOT_EXISTS);
        }
        // 2. 只有入驻待审核状态才能审核
        if (!EnterpriseStatusEnum.PENDING_ONBOARDING_REVIEW.getStatus().equals(enterprise.getStatus())) {
            throw exception(ENTERPRISE_INFO_STATUS_NOT_PENDING);
        }
        // 3. 更新状态和审核备注
        enterprise.setAuditRemarks(auditReqVO.getAuditRemarks());
        if (Boolean.TRUE.equals(auditReqVO.getApproved())) {
            enterprise.setStatus(EnterpriseStatusEnum.ONBOARDING_APPROVED_AWAITING_CERTIFICATION.getStatus());
        } else {
            enterprise.setStatus(EnterpriseStatusEnum.ONBOARDING_REJECTED.getStatus());
        }
        enterpriseInfoMapper.updateById(enterprise);
        // TODO 4. 写入企业审核日志
        // TODO 5. 为申请人用户分配角色（企业初级管理员或其他）
    }
} 