package cn.iocoder.yudao.module.logistics.service.permission.impl;

import cn.iocoder.yudao.module.logistics.enums.LogisticsPermissionMenuEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsRoleEnum;
import cn.iocoder.yudao.module.logistics.service.permission.LogisticsPermissionSyncService;
import cn.iocoder.yudao.module.logistics.service.permission.dto.LogisticsPermissionSyncResult;
import cn.iocoder.yudao.module.system.api.permission.MenuApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        // 1. 权限行：登记过的权限都要有 system_menu 行，否则 @ss.hasPermission 的严格模式会判为无权限。
        //    名称与父页面取自 LogisticsPermissionMenuEnum（ADR 0026 的物流侧实现）：按钮行不进菜单 SQL，
        //    但也不该在「菜单管理」里显示成裸权限标识。已归集过的行与原行 id 不变，角色授权与套餐不受影响。
        Map<String, Long> menuIdByPermission = new LinkedHashMap<>();
        int createdMenuCount = 0;
        for (String permission : LogisticsRoleEnum.allPermissions()) {
            LogisticsPermissionMenuEnum placement = LogisticsPermissionMenuEnum.ofPermission(permission);
            Long parentId = resolveParentMenuId(permission, placement);
            String name = placement != null ? placement.getName() : permission;
            int sort = placement != null ? placement.getSort() : 0;
            Long menuId = menuApi.getMenuIdByPermission(permission);
            Long ensuredMenuId = menuApi.ensurePermissionMenu(name, permission, parentId, sort);
            if (menuId == null) {
                createdMenuCount++;
            }
            menuIdByPermission.put(permission, ensuredMenuId);
        }
        // 2. 套餐：租户内权限补进「回收企业套餐」，新开的租户开箱即得
        int addedPackageMenuCount = tenantApi.addTenantPackageMenuIds(RECYCLING_TENANT_PACKAGE_ID,
                menuIdsOf(LogisticsRoleEnum.ADMIN, menuIdByPermission));
        return new MenuSync(menuIdByPermission, createdMenuCount, addedPackageMenuCount);
    }

    /**
     * 解析权限行应挂到哪个页面菜单下。
     *
     * <p>用组件名而不是菜单 id：页面菜单由 {@code logistics-menu.sql} 用自增 id 建，id 不稳定，
     * 组件名则在 SQL 里写死。页面还没建（比如本地库还没导菜单 SQL）时回退到根节点并打 warn——
     * 权限行本身比它挂在哪重要：缺行会让 {@code @ss.hasPermission} 的严格模式判为无权限。
     *
     * @param permission 权限标识（只用于日志）
     * @param placement  归集方式；{@code null}（枚举里没登记）时回退到根节点
     * @return 父菜单编号；{@code null} 表示根节点
     */
    private Long resolveParentMenuId(String permission, LogisticsPermissionMenuEnum placement) {
        if (placement == null || placement.getParentComponentName() == null) {
            return null;
        }
        Long parentId = menuApi.getMenuIdByComponentName(placement.getParentComponentName());
        if (parentId == null) {
            log.warn("[resolveParentMenuId][权限({})要挂的页面({})不存在，先落在根节点；导入菜单 SQL 后重启即会归集]",
                    permission, placement.getParentComponentName());
        }
        return parentId;
    }

    private Set<Long> menuIdsOf(LogisticsRoleEnum role, Map<String, Long> menuIdByPermission) {
        Set<Long> menuIds = new LinkedHashSet<>();
        for (String permission : role.getPermissions()) {
            Long menuId = menuIdByPermission.get(permission);
            if (menuId != null) {
                menuIds.add(menuId);
            }
        }
        // 角色授权要连**父链**一起挂：yudao 的 filterDisableMenus 沿父链判断「是否自身与所有祖先是启用」，
        // 父菜单不在角色已授权的菜单集合里时，按钮型权限行会被判为禁用，get-permission-info 的
        // permissions 就空了——表现为后端 @ss.hasPermission 放行、前端却提示没有权限。
        menuIds.addAll(menuApi.getAncestorMenuIds(menuIds));
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
