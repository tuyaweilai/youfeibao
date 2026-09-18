package cn.iocoder.yudao.module.enterprise.service.binding;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.enterprise.api.binding.EnterpriseBindingApi;
import cn.iocoder.yudao.module.enterprise.api.binding.dto.EnterpriseBindingResultVO;
import cn.iocoder.yudao.module.enterprise.api.deptlink.EnterpriseDeptLinkApi;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationCreateReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseUserRelationTypeEnum;
import cn.iocoder.yudao.module.enterprise.enums.binding.EnterpriseBindingStatusEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseUserRelationService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.enums.dept.DeptTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class EnterpriseBindingServiceImpl implements EnterpriseBindingApi {

    @Resource
    private DeptApi deptApi;

    @Resource
    private EnterpriseUserRelationService enterpriseUserRelationService;

    @Resource
    private EnterpriseInfoService enterpriseInfoService;

    @Resource
    private EnterpriseDeptLinkApi enterpriseDeptLinkApi;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public EnterpriseBindingResultVO checkAndBindUserToEnterprise(@NotNull Long userId, Long deptId, @NotNull Long tenantId, Set<String> userRoles) {
        log.info("[checkAndBindUserToEnterprise] 开始处理用户企业绑定, userId: {}, deptId: {}, tenantId: {}, roles: {}", userId, deptId, tenantId, userRoles);

        // 1. 检查是否为超级管理员或租户管理员
        if (isAdminUser(userRoles)) {
            return buildSkipBindingResult();
        }

        // 2. 检查用户是否已有关联的企业
        List<EnterpriseUserRelationDO> existingRelations = enterpriseUserRelationService.getUserRelationListByUserId(userId);
        if (CollUtil.isNotEmpty(existingRelations)) {
            return handleExistingEnterpriseRelations(existingRelations);
        }

        // 3. 如果用户没有部门ID，则无法进行部门链判断
        if (deptId == null) {
            return buildManualAuthRequiredResult("您未归属任何部门，请手动进行企业认证");
        }

        // 4. 获取并遍历部门链
        List<DeptRespDTO> departmentChain = getDepartmentChain(deptId);
        log.debug("[checkAndBindUserToEnterprise] 获取到用户的部门链: {} 个部门", departmentChain.size());

        // 5. 尝试自动绑定企业
        return tryAutoBindUserToEnterprise(userId, tenantId, departmentChain);
    }

    /**
     * 判断用户是否为管理员
     */
    private boolean isAdminUser(Set<String> userRoles) {
        return CollUtil.isNotEmpty(userRoles) &&
                (userRoles.contains(RoleCodeEnum.SUPER_ADMIN.getCode()) || 
                 userRoles.contains(RoleCodeEnum.TENANT_ADMIN.getCode()));
    }

    /**
     * 构建跳过绑定的结果
     */
    private EnterpriseBindingResultVO buildSkipBindingResult() {
        log.info("[buildSkipBindingResult] 用户是超级/租户管理员，跳过企业绑定");
        return EnterpriseBindingResultVO.builder()
                .enterpriseBindingStatus(EnterpriseBindingStatusEnum.SKIP_BINDING.getCode())
                .enterpriseBindingMessage("超级/租户管理员无需自动绑定企业")
                .build();
    }

    /**
     * 处理已存在的企业关联
     */
    private EnterpriseBindingResultVO handleExistingEnterpriseRelations(List<EnterpriseUserRelationDO> existingRelations) {
        EnterpriseUserRelationDO defaultRelation = findDefaultRelation(existingRelations);
        
        if (defaultRelation != null) {
            return buildAlreadyBoundDefaultResult(defaultRelation);
        } else {
            return buildAlreadyBoundNotDefaultResult();
        }
    }

    /**
     * 查找默认关联关系
     */
    private EnterpriseUserRelationDO findDefaultRelation(List<EnterpriseUserRelationDO> relations) {
        // 查找默认企业关系，如果没有默认但只有一个企业，则视为默认
        return relations.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsDefaultEnterprise()))
                .findFirst()
                .orElse(relations.size() == 1 ? relations.get(0) : null);
    }

    /**
     * 构建已绑定默认企业的结果
     */
    private EnterpriseBindingResultVO buildAlreadyBoundDefaultResult(EnterpriseUserRelationDO relation) {
        String enterpriseName = getEnterpriseName(relation.getEnterpriseId());
        log.info("[buildAlreadyBoundDefaultResult] 用户已有默认企业: {}", enterpriseName);
        
        return EnterpriseBindingResultVO.builder()
                .enterpriseBindingStatus(EnterpriseBindingStatusEnum.ALREADY_BOUND_DEFAULT.getCode())
                .boundEnterpriseId(relation.getEnterpriseId())
                .boundEnterpriseName(enterpriseName)
                .enterpriseBindingMessage("您已关联默认企业：" + enterpriseName)
                .build();
    }

    /**
     * 获取企业名称，处理空值情况
     */
    private String getEnterpriseName(Long enterpriseId) {
        if (enterpriseId == null) {
            return "未知企业";
        }
        
        return Optional.ofNullable(enterpriseInfoService.getEnterpriseInfo(enterpriseId))
                .map(EnterpriseInfoDO::getName)
                .orElse("未知企业");
    }

    /**
     * 构建已绑定非默认企业的结果
     */
    private EnterpriseBindingResultVO buildAlreadyBoundNotDefaultResult() {
        log.info("[buildAlreadyBoundNotDefaultResult] 用户有多个企业关联但无默认企业");
        return EnterpriseBindingResultVO.builder()
                .enterpriseBindingStatus(EnterpriseBindingStatusEnum.ALREADY_BOUND_NOT_DEFAULT.getCode())
                .enterpriseBindingMessage("您已关联多个企业，请选择一个进行操作或设置默认企业")
                .build();
    }

    /**
     * 获取部门链（从当前部门到根部门）
     */
    private List<DeptRespDTO> getDepartmentChain(Long deptId) {
        List<DeptRespDTO> departmentChain = new ArrayList<>();
        DeptRespDTO currentDept = deptApi.getDept(deptId);
        
        while (currentDept != null) {
            departmentChain.add(currentDept);
            if (currentDept.getParentId() == null || 
                DeptRespDTO.PARENT_ID_ROOT.equals(currentDept.getParentId())) {
                break;
            }
            currentDept = deptApi.getDept(currentDept.getParentId());
        }
        
        return departmentChain;
    }

    /**
     * 尝试自动绑定用户到企业
     */
    private EnterpriseBindingResultVO tryAutoBindUserToEnterprise(Long userId, Long tenantId, List<DeptRespDTO> departmentChain) {
        // 遍历部门链，查找机构类型部门
        for (DeptRespDTO dept : departmentChain) {
            if (!DeptTypeEnum.isInstitution(dept.getDeptType())) {
                continue;
            }
            
            log.info("[tryAutoBindUserToEnterprise] 发现机构类型部门: {}", dept.getName());
            Long linkedEnterpriseId = enterpriseDeptLinkApi.getEnterpriseIdByDeptId(dept.getId(), tenantId);
            
            if (linkedEnterpriseId == null) {
                log.debug("[tryAutoBindUserToEnterprise] 机构部门未关联企业: {}", dept.getName());
                continue;
            }
            
            // 获取企业信息
            EnterpriseInfoDO enterprise = enterpriseInfoService.getEnterpriseInfo(linkedEnterpriseId);
            if (enterprise == null) {
                log.warn("[tryAutoBindUserToEnterprise] 关联的企业不存在: {}", linkedEnterpriseId);
                continue;
            }
            
            // 检查用户是否已与该企业关联
            EnterpriseUserRelationDO existingRelation = enterpriseUserRelationService.getUserRelation(userId, linkedEnterpriseId);
            if (existingRelation != null) {
                log.info("[tryAutoBindUserToEnterprise] 用户已关联此企业: {}", enterprise.getName());
                boolean isDefault = Boolean.TRUE.equals(existingRelation.getIsDefaultEnterprise());
                return buildExistingEnterpriseResult(enterprise, isDefault);
            }
            
            // 创建新的关联关系
            log.info("[tryAutoBindUserToEnterprise] 为用户创建企业关联: {}", enterprise.getName());
            createEnterpriseUserRelation(userId, enterprise.getId());
            
            return buildAutoBindSuccessResult(enterprise);
        }
        
        // 未找到可关联的企业
        return buildManualAuthRequiredResult("未找到可自动关联的企业，请手动认证或创建企业");
    }

    /**
     * 构建已存在企业关系的结果
     */
    private EnterpriseBindingResultVO buildExistingEnterpriseResult(EnterpriseInfoDO enterprise, boolean isDefault) {
        return EnterpriseBindingResultVO.builder()
                .enterpriseBindingStatus(isDefault ? 
                        EnterpriseBindingStatusEnum.ALREADY_BOUND_DEFAULT.getCode() : 
                        EnterpriseBindingStatusEnum.ALREADY_BOUND_NOT_DEFAULT.getCode())
                .boundEnterpriseId(enterprise.getId())
                .boundEnterpriseName(enterprise.getName())
                .enterpriseBindingMessage("您已关联企业：" + enterprise.getName())
                .build();
    }

    /**
     * 创建用户与企业的关联关系
     */
    private void createEnterpriseUserRelation(Long userId, Long enterpriseId) {
        EnterpriseUserRelationCreateReqVO createReqVO = new EnterpriseUserRelationCreateReqVO();
        createReqVO.setUserId(userId);
        createReqVO.setEnterpriseId(enterpriseId);
        createReqVO.setRelationType(EnterpriseUserRelationTypeEnum.EMPLOYEE.getType());
        createReqVO.setIsPrimaryContact(false);
        
        // 如果是用户的第一个企业关联，则设为默认
        List<EnterpriseUserRelationDO> currentRelations = 
                enterpriseUserRelationService.getUserRelationListByUserId(userId);
        createReqVO.setIsDefaultEnterprise(CollUtil.isEmpty(currentRelations));
        
        enterpriseUserRelationService.createUserRelation(createReqVO);
    }

    /**
     * 构建自动绑定成功的结果
     */
    private EnterpriseBindingResultVO buildAutoBindSuccessResult(EnterpriseInfoDO enterprise) {
        return EnterpriseBindingResultVO.builder()
                .enterpriseBindingStatus(EnterpriseBindingStatusEnum.AUTO_BIND_SUCCESS.getCode())
                .boundEnterpriseId(enterprise.getId())
                .boundEnterpriseName(enterprise.getName())
                .enterpriseBindingMessage("已为您自动关联到企业：" + enterprise.getName())
                .build();
    }

    /**
     * 构建需要手动认证的结果
     */
    private EnterpriseBindingResultVO buildManualAuthRequiredResult(String message) {
        log.info("[buildManualAuthRequiredResult] 用户需要手动认证: {}", message);
        return EnterpriseBindingResultVO.builder()
                .enterpriseBindingStatus(EnterpriseBindingStatusEnum.MANUAL_AUTH_REQUIRED.getCode())
                .enterpriseBindingMessage(message)
                .build();
    }
} 