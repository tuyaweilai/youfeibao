package cn.iocoder.yudao.module.enterprise.api.binding;

import cn.iocoder.yudao.module.enterprise.api.binding.dto.EnterpriseBindingResultVO;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * 企业绑定服务 API 接口
 *
 * @author 芋道源码
 */
public interface EnterpriseBindingApi {

    /**
     * 检查并尝试自动绑定用户到企业
     *
     * @param userId 用户ID
     * @param deptId 用户所属部门ID
     * @param tenantId 用户所属租户ID
     * @param userRoles 用户角色列表 (用于判断是否为超级管理员等)
     * @return 绑定结果
     */
    EnterpriseBindingResultVO checkAndBindUserToEnterprise(@NotNull Long userId, Long deptId, @NotNull Long tenantId, Set<String> userRoles);
} 