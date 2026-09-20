package cn.iocoder.yudao.module.system.api.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.api.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 权限 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class PermissionApiImpl implements PermissionApi {

    @Resource
    private PermissionService permissionService;

    @Override
    public Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
        return permissionService.getUserRoleIdListByRoleId(roleIds);
    }

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        return permissionService.hasAnyPermissions(userId, permissions);
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        return permissionService.hasAnyRoles(userId, roles);
    }

    @Override
    public int addRoleMenus(Long roleId, Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return 0;
        }
        // 计算差集，只新增不删除，避免把租户套餐带来的菜单抹掉
        Set<Long> currentMenuIds = permissionService.getRoleMenuListByRoleId(Collections.singleton(roleId));
        Set<Long> toAddMenuIds = new LinkedHashSet<>(menuIds);
        toAddMenuIds.removeAll(currentMenuIds);
        if (toAddMenuIds.isEmpty()) {
            return 0;
        }
        Set<Long> unionMenuIds = new LinkedHashSet<>(currentMenuIds);
        unionMenuIds.addAll(menuIds);
        permissionService.assignRoleMenu(roleId, unionMenuIds);
        return toAddMenuIds.size();
    }

    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        return permissionService.getDeptDataPermission(userId);
    }

}
