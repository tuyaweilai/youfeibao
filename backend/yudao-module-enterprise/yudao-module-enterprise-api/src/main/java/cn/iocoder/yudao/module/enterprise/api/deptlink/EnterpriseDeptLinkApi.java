package cn.iocoder.yudao.module.enterprise.api.deptlink;

import javax.validation.constraints.NotNull;

/**
 * 部门与企业关联API接口
 * 
 * @author 芋道源码
 */
public interface EnterpriseDeptLinkApi {

    /**
     * 根据部门ID和租户ID获取关联的企业ID
     * 
     * @param deptId 部门ID
     * @param tenantId 租户ID
     * @return 关联的企业ID，如果无关联则返回null
     */
    Long getEnterpriseIdByDeptId(@NotNull Long deptId, @NotNull Long tenantId);
} 