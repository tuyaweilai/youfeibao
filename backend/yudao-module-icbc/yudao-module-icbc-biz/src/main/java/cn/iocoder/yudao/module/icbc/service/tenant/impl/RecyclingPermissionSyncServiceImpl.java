package cn.iocoder.yudao.module.icbc.service.tenant.impl;

import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermissionMenuEnum;
import cn.iocoder.yudao.module.icbc.enums.RecyclingRoleEnum;
import cn.iocoder.yudao.module.icbc.service.tenant.RecyclingPermissionSyncService;
import cn.iocoder.yudao.module.icbc.service.tenant.dto.RecyclingPermissionSyncResult;
import cn.iocoder.yudao.module.system.api.permission.MenuApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 回收域权限同步 Service 实现类。
 *
 * <p>三段都幂等：菜单按权限标识查、角色按 code 查、角色授权与套餐菜单只做新增，
 * 因此重复执行不产生重复行，返回的新增计数会收敛到全 0。
 */
@Service
@Slf4j
public class RecyclingPermissionSyncServiceImpl implements RecyclingPermissionSyncService {

    /**
     * 「回收企业套餐」编号，与 {@code backend/sql/mysql/icbc-menu.sql} 保持一致。
     */
    public static final long RECYCLING_TENANT_PACKAGE_ID = 200L;

    /**
     * 司机角色的 code（**跨模块的约定字符串**，见 ADR 0033）。
     *
     * <p>司机上门收货时现场没有收货员（ADR 0030），他要在自己的手机上完成自然人**准入四步**；
     * 而这四步的接口与权限都是 icbc 的，司机的角色却定义在物流模块（ADR 0032 禁止物流依赖 icbc）。
     * 于是约定：**由 icbc 侧按角色 code 授予**——icbc 只知道这一个字符串，不知道物流的任何类型。
     */
    public static final String DRIVER_ROLE_CODE = "logistics_driver";

    /**
     * 司机需要的 icbc 权限：**只有自然人档案与准入四步**，外加签发一次性令牌用于「把确认链接转达给本人」，
     * 以及只读的品类配置（交接登记要选权威品类，ADR 0028：品类权威是 goods_config_id，不用自由文本）。
     *
     * <p>刻意**不含**收购登记、结算确认、付款、开票——现场不产生金额（ADR 0031），
     * 确认一律由出售者本人做（ADR 0030）。收窄是这份清单的要点，加权限前先回头看那两条 ADR。
     */
    private static final List<String> DRIVER_PERMISSIONS = Arrays.asList(
            RecyclingPermission.PAYEE_CREATE,
            RecyclingPermission.PAYEE_QUERY,
            RecyclingPermission.SELLER_ONBOARDING_EXECUTE,
            RecyclingPermission.SELLER_AGREEMENT_MANAGE,
            RecyclingPermission.SELLER_AUTHORIZATION_MANAGE,
            RecyclingPermission.PUBLIC_TOKEN_CREATE,
            // 交接登记要选品类：只读，不能建 / 改 / 删品类配置
            RecyclingPermission.GOODS_CONFIG_QUERY);

    private final MenuApi menuApi;
    private final RoleApi roleApi;
    private final PermissionApi permissionApi;
    private final TenantApi tenantApi;

    public RecyclingPermissionSyncServiceImpl(MenuApi menuApi, RoleApi roleApi,
                                              PermissionApi permissionApi, TenantApi tenantApi) {
        this.menuApi = menuApi;
        this.roleApi = roleApi;
        this.permissionApi = permissionApi;
        this.tenantApi = tenantApi;
    }

    @Override
    public RecyclingPermissionSyncResult syncGlobal() {
        MenuSync menuSync = syncMenusAndPackage();
        return new RecyclingPermissionSyncResult(menuSync.createdMenuCount, 0, 0, menuSync.addedPackageMenuCount);
    }

    @Override
    public RecyclingPermissionSyncResult sync() {
        MenuSync menuSync = syncMenusAndPackage();
        // 角色与角色-菜单：四个租户内角色各拿到 RecyclingRoleEnum 里定义的权限
        int createdRoleCount = 0;
        int assignedRoleMenuCount = 0;
        for (RecyclingRoleEnum role : RecyclingRoleEnum.values()) {
            if (role.isCrossTenant()) {
                continue; // 平台运营只存在于平台租户，不在回收企业租户里建
            }
            Long roleId = roleApi.getRoleIdByCode(role.getCode());
            if (roleId == null) {
                roleId = roleApi.createRole(role.getCode(), role.getName());
                createdRoleCount++;
            }
            assignedRoleMenuCount += permissionApi.addRoleMenus(roleId,
                    menuIdsOf(role, menuSync.menuIdByPermission));
        }
        // 司机不是回收域的角色（他在物流域，见 ADR 0032/0033），但准入四步要用 icbc 的权限：
        // 按角色 code 授予，角色还不存在就跳过（先跑物流域的 init 再跑这个）。
        assignedRoleMenuCount += grantDriverPermissions(menuSync.menuIdByPermission);
        return new RecyclingPermissionSyncResult(menuSync.createdMenuCount, createdRoleCount,
                assignedRoleMenuCount, menuSync.addedPackageMenuCount);
    }

    /**
     * 把准入四步所需的 icbc 权限授予司机角色（ADR 0033）。
     *
     * <p>幂等：{@code addRoleMenus} 只做新增。司机角色在该租户不存在时**直接跳过**——
     * 这既让「先建司机角色、再同步 icbc 权限」的顺序可用，也避免 icbc 去建一个不属于它的角色。
     *
     * @return 新增的角色-菜单关联数
     */
    private int grantDriverPermissions(Map<String, Long> menuIdByPermission) {
        Long driverRoleId = roleApi.getRoleIdByCode(DRIVER_ROLE_CODE);
        if (driverRoleId == null) {
            return 0;
        }
        Set<Long> menuIds = new LinkedHashSet<>();
        for (String permission : DRIVER_PERMISSIONS) {
            Long menuId = menuIdByPermission.get(permission);
            if (menuId != null) {
                menuIds.add(menuId);
            }
        }
        // 父链也要授：只挂按钮行时，filterDisableMenus 会因父菜单不在角色集合里把权限判为禁用
        menuIds.addAll(menuApi.getAncestorMenuIds(menuIds));
        return permissionApi.addRoleMenus(driverRoleId, menuIds);
    }

    /**
     * 同步菜单权限行与租户套餐（全局，不依赖租户上下文）。
     *
     * <p>回收域的按钮行绝大部分由 {@code icbc-menu.sql} 显式编排（带中文名与父页面），这里只是**兜底**：
     * 某个权限在 SQL 里漏了行时，补一个裸标识行，保证 {@code @ss.hasPermission} 的严格模式不误判。
     * 名称与归属查 {@link RecyclingPermissionMenuEnum}（只登记 SQL 里没有编排的那几条）；
     * {@code ensurePermissionMenu} 只碰「未归集」的行，已由 SQL 或后台编排好的一律不动。
     */
    private MenuSync syncMenusAndPackage() {
        // 1. 权限行：登记过的权限都要有 system_menu 行
        Map<String, Long> menuIdByPermission = new LinkedHashMap<>();
        int createdMenuCount = 0;
        for (String permission : RecyclingRoleEnum.allPermissions()) {
            RecyclingPermissionMenuEnum placement = RecyclingPermissionMenuEnum.ofPermission(permission);
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
        // 2. 套餐：租户内权限补进「回收企业套餐」，新开的租户开箱即得（管理员权限由套餐带出）
        int addedPackageMenuCount = tenantApi.addTenantPackageMenuIds(RECYCLING_TENANT_PACKAGE_ID,
                menuIdsOf(RecyclingRoleEnum.ADMIN, menuIdByPermission));
        return new MenuSync(menuIdByPermission, createdMenuCount, addedPackageMenuCount);
    }

    private Set<Long> menuIdsOf(RecyclingRoleEnum role, Map<String, Long> menuIdByPermission) {
        Set<Long> menuIds = new LinkedHashSet<>();
        for (String permission : role.getPermissions()) {
            Long menuId = menuIdByPermission.get(permission);
            if (menuId != null) {
                menuIds.add(menuId);
            }
        }
        // 角色授权要连父链一起挂，否则 get-permission-info 的 filterDisableMenus 会判为禁用
        menuIds.addAll(menuApi.getAncestorMenuIds(menuIds));
        return menuIds;
    }

    /**
     * 解析权限行应挂到哪个页面菜单下。
     *
     * <p>用组件名而不是菜单 id：页面菜单由 {@code icbc-menu.sql} 用固定 id 建，但固定 id 对本域之外没有意义，
     * 组件名则在 SQL 里写死。页面不存在（本地库还没导菜单 SQL）时回退到根节点并打 warn——权限行本身比它挂在哪重要。
     *
     * @param permission 权限标识（只用于日志）
     * @param placement  归集方式；{@code null}（枚举里没登记）时回退到根节点
     * @return 父菜单编号；{@code null} 表示根节点
     */
    private Long resolveParentMenuId(String permission, RecyclingPermissionMenuEnum placement) {
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
