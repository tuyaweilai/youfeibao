package cn.iocoder.yudao.module.logistics.service.permission.impl;

import cn.iocoder.yudao.module.logistics.enums.LogisticsRoleEnum;
import cn.iocoder.yudao.module.logistics.service.permission.LogisticsPermissionSyncService;
import cn.iocoder.yudao.module.logistics.service.permission.dto.LogisticsPermissionSyncResult;
import cn.iocoder.yudao.module.system.api.permission.MenuApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 物流域权限同步 Service 实现（V2a #77）。
 *
 * <p>三段都幂等：菜单按权限标识查、角色按 code 查、角色授权与套餐菜单只做新增，
 * 因此重复执行不产生重复行，返回的新增计数会收敛到全 0。
 *
 * <p>与 icbc 的 {@code RecyclingPermissionSyncServiceImpl} 同构，但**互不依赖**：两边各自维护
 * 自己的权限枚举、角色与套餐菜单集合（ADR 0032）。物流只往「回收企业套餐」里**追加**自己的菜单，
 * 不动 icbc 已有的部分。
 */
@Service
public class LogisticsPermissionSyncServiceImpl implements LogisticsPermissionSyncService {

    /**
     * 「回收企业套餐」编号，与 {@code backend/sql/mysql/icbc-menu.sql} 保持一致。
     */
    public static final long RECYCLING_TENANT_PACKAGE_ID = 200L;

    private final MenuApi menuApi;
    private final RoleApi roleApi;
    private final PermissionApi permissionApi;
    private final TenantApi tenantApi;

    public LogisticsPermissionSyncServiceImpl(MenuApi menuApi, RoleApi roleApi,
                                              PermissionApi permissionApi, TenantApi tenantApi) {
        this.menuApi = menuApi;
        this.roleApi = roleApi;
        this.permissionApi = permissionApi;
        this.tenantApi = tenantApi;
    }

    @Override
    public LogisticsPermissionSyncResult syncGlobal() {
        MenuSync menuSync = syncMenusAndPackage();
        return new LogisticsPermissionSyncResult(menuSync.createdMenuCount, 0, 0,
                menuSync.addedPackageMenuCount);
    }

    @Override
    public LogisticsPermissionSyncResult sync() {
        MenuSync menuSync = syncMenusAndPackage();
        int createdRoleCount = 0;
        int assignedRoleMenuCount = 0;
        for (LogisticsRoleEnum role : LogisticsRoleEnum.values()) {
            Long roleId = roleApi.getRoleIdByCode(role.getCode());
            if (roleId == null) {
                roleId = roleApi.createRole(role.getCode(), role.getName());
                createdRoleCount++;
            }
            assignedRoleMenuCount += permissionApi.addRoleMenus(roleId, menuIdsOf(role, menuSync.menuIdByPermission));
        }
        return new LogisticsPermissionSyncResult(menuSync.createdMenuCount, createdRoleCount,
                assignedRoleMenuCount, menuSync.addedPackageMenuCount);
    }

    /**
     * 同步权限行与租户套餐（全局，不依赖租户上下文）。
     */
    private MenuSync syncMenusAndPackage() {
        // 1. 权限行：登记过的权限都要有 system_menu 行，否则 @ss.hasPermission 的严格模式会判为无权限
        Map<String, Long> menuIdByPermission = new LinkedHashMap<>();
        int createdMenuCount = 0;
        for (String permission : LogisticsRoleEnum.allPermissions()) {
            Long menuId = menuApi.getMenuIdByPermission(permission);
            if (menuId == null) {
                menuId = menuApi.createPermissionMenu(permission, permission);
                createdMenuCount++;
            }
            menuIdByPermission.put(permission, menuId);
        }
        // 2. 套餐：租户内权限补进「回收企业套餐」，新开的租户开箱即得
        int addedPackageMenuCount = tenantApi.addTenantPackageMenuIds(RECYCLING_TENANT_PACKAGE_ID,
                menuIdsOf(LogisticsRoleEnum.ADMIN, menuIdByPermission));
        return new MenuSync(menuIdByPermission, createdMenuCount, addedPackageMenuCount);
    }

    private static Set<Long> menuIdsOf(LogisticsRoleEnum role, Map<String, Long> menuIdByPermission) {
        Set<Long> menuIds = new LinkedHashSet<>();
        for (String permission : role.getPermissions()) {
            Long menuId = menuIdByPermission.get(permission);
            if (menuId != null) {
                menuIds.add(menuId);
            }
        }
        return menuIds;
    }

    private static class MenuSync {

        private final Map<String, Long> menuIdByPermission;
        private final int createdMenuCount;
        private final int addedPackageMenuCount;

        private MenuSync(Map<String, Long> menuIdByPermission, int createdMenuCount, int addedPackageMenuCount) {
            this.menuIdByPermission = menuIdByPermission;
            this.createdMenuCount = createdMenuCount;
            this.addedPackageMenuCount = addedPackageMenuCount;
        }

    }

}
