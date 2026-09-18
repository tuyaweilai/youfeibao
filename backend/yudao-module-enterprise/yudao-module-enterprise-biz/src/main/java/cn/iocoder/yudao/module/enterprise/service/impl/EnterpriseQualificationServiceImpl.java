package cn.iocoder.yudao.module.enterprise.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationAuditReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationPageReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseQualificationConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseQualificationDO;
import cn.iocoder.yudao.module.enterprise.dal.mysql.EnterpriseQualificationMapper;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseQualificationStatusEnum;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseQualificationTypeEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseQualificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.enterprise.enums.ErrorCodeConstants.*;

/**
 * 企业资质 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class EnterpriseQualificationServiceImpl implements EnterpriseQualificationService {

    @Resource
    private EnterpriseQualificationMapper enterpriseQualificationMapper;

    @Resource
    private EnterpriseInfoService enterpriseInfoService;

    @Override
    public Long createEnterpriseQualification(EnterpriseQualificationCreateReqVO createReqVO) {
        // 校验企业存在
        validateEnterpriseExists(createReqVO.getEnterpriseId());
        // 校验资质类型是否合法
        validateQualificationType(createReqVO.getQualificationType());
        // 校验资质是否已存在
        validateQualificationUnique(null, createReqVO.getEnterpriseId(), createReqVO.getQualificationType());

        // 插入企业资质
        EnterpriseQualificationDO qualification = EnterpriseQualificationConvert.INSTANCE.convert(createReqVO);
        // 设置初始状态为待审核
        qualification.setStatus(EnterpriseQualificationStatusEnum.PENDING_AUDIT.getStatus());
        enterpriseQualificationMapper.insert(qualification);
        return qualification.getId();
    }

    @Override
    public void updateEnterpriseQualification(EnterpriseQualificationUpdateReqVO updateReqVO) {
        // 校验企业资质存在
        validateEnterpriseQualificationExists(updateReqVO.getId());
        // 校验企业存在
        validateEnterpriseExists(updateReqVO.getEnterpriseId());
        // 校验资质是否重复
        validateQualificationUnique(updateReqVO.getId(), updateReqVO.getEnterpriseId(), updateReqVO.getQualificationType());

        // 更新企业资质
        EnterpriseQualificationDO updateObj = EnterpriseQualificationConvert.INSTANCE.convert(updateReqVO);
        // 如果更新了关键信息，重置状态为待审核
        updateObj.setStatus(EnterpriseQualificationStatusEnum.PENDING_AUDIT.getStatus());
        enterpriseQualificationMapper.updateById(updateObj);
    }

    @Override
    public void deleteEnterpriseQualification(Long id) {
        // 校验企业资质存在
        validateEnterpriseQualificationExists(id);
        // 执行删除
        enterpriseQualificationMapper.deleteById(id);
    }

    private EnterpriseQualificationDO validateEnterpriseQualificationExists(Long id) {
        EnterpriseQualificationDO qualification = enterpriseQualificationMapper.selectById(id);
        if (qualification == null) {
            throw exception(ENTERPRISE_QUALIFICATION_NOT_EXISTS);
        }
        return qualification;
    }

    private void validateEnterpriseExists(Long enterpriseId) {
        if (enterpriseInfoService.getEnterpriseInfo(enterpriseId) == null) {
            throw exception(ENTERPRISE_INFO_NOT_EXISTS);
        }
    }

    private void validateQualificationUnique(Long id, Long enterpriseId, Integer qualificationType) {
        EnterpriseQualificationDO qualification = enterpriseQualificationMapper.selectByEnterpriseIdAndType(enterpriseId, qualificationType);
        if (qualification == null) {
            return;
        }
        // 如果是更新且ID相同则可以通过
        if (id != null && qualification.getId().equals(id)) {
            return;
        }
        throw exception(ENTERPRISE_QUALIFICATION_ALREADY_EXISTS);
    }

    @Override
    public EnterpriseQualificationDO getEnterpriseQualification(Long id) {
        return enterpriseQualificationMapper.selectById(id);
    }

    @Override
    public List<EnterpriseQualificationDO> getEnterpriseQualificationListByEnterpriseId(Long enterpriseId) {
        return enterpriseQualificationMapper.selectListByEnterpriseId(enterpriseId);
    }

    @Override
    public PageResult<EnterpriseQualificationDO> pageEnterpriseQualification(EnterpriseQualificationPageReqVO pageReqVO) {
        return enterpriseQualificationMapper.selectPage(pageReqVO);
    }

    @Override
    public void auditEnterpriseQualification(EnterpriseQualificationAuditReqVO auditReqVO) {
        // 校验企业资质存在
        EnterpriseQualificationDO qualification = validateEnterpriseQualificationExists(auditReqVO.getId());
        // 校验资质状态是否为待审核
        validateQualificationStatus(qualification.getStatus(), EnterpriseQualificationStatusEnum.PENDING_AUDIT.getStatus());

        // 更新资质状态
        EnterpriseQualificationDO updateObj = new EnterpriseQualificationDO();
        updateObj.setId(auditReqVO.getId());
        updateObj.setStatus(auditReqVO.getAuditDecision() ? 
                EnterpriseQualificationStatusEnum.VALID.getStatus() : 
                EnterpriseQualificationStatusEnum.AUDIT_REJECTED.getStatus());
        updateObj.setAuditRemarks(auditReqVO.getAuditRemarks());
        enterpriseQualificationMapper.updateById(updateObj);
    }

    private void validateQualificationStatus(Integer currentStatus, Integer expectedStatus) {
        if (!currentStatus.equals(expectedStatus)) {
            throw exception(ENTERPRISE_QUALIFICATION_STATUS_NOT_VALID);
        }
    }

    private void validateQualificationType(Integer qualificationType) {
        boolean valid = false;
        for (EnterpriseQualificationTypeEnum type : EnterpriseQualificationTypeEnum.values()) {
            if (type.getType().equals(qualificationType)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw exception(ENTERPRISE_QUALIFICATION_TYPE_NOT_VALID);
        }
    }
} 