package cn.iocoder.yudao.module.icbc.framework.security;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.icbc.enums.RecyclingRoleEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 回收企业反向开票域的权限判断。
 *
 * <p>为什么不用 yudao 默认的 {@code @ss.hasPermission}：那套依赖数据库里的菜单
 * （{@code system_menu.permission}）与角色-菜单关联，而回收企业的四个角色是一套
 * 固定的、跟代码一起演进的角色策略。这里把「哪个角色能做哪个操作」写死在
 * {@link RecyclingRoleEnum} 里，判断时只看当前登录用户是否拥有对应角色标识，
 * 从而不依赖菜单数据。
 *
 * <p>用法：{@code @PreAuthorize("@icbc.hasPermission('icbc:payee-info:create')")}。
 */
@Component("icbc")
public class RecyclingPermissionChecker {

    private final PermissionApi permissionApi;

    public RecyclingPermissionChecker(PermissionApi permissionApi) {
        this.permissionApi = permissionApi;
    }

    /**
     * 判断当前登录用户是否拥有指定权限。
     *
     * @param permission 权限标识，见 {@link cn.iocoder.yudao.module.icbc.enums.RecyclingPermission}
     * @return 是否拥有
     */
    public boolean hasPermission(String permission) {
        Set<String> roleCodes = RecyclingRoleEnum.roleCodesForPermission(permission);
        if (CollUtil.isEmpty(roleCodes)) {
            return false; // 未登记的权限一律拒绝
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return false; // 未登录
        }
        return permissionApi.hasAnyRoles(userId, roleCodes.toArray(new String[0]));
    }

}
