package cn.iocoder.yudao.module.logistics.service.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermissionMenuEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsRoleEnum;
import cn.iocoder.yudao.module.logistics.service.permission.dto.LogisticsPermissionSyncResult;
import cn.iocoder.yudao.module.logistics.service.permission.impl.LogisticsPermissionSyncServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.MenuApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link LogisticsPermissionSyncServiceImpl} 的单元测试（V2a #77）。
 *
 * <p>关注两件事：**幂等**（第二次同步不建行、不授权），以及「角色拿到的菜单就是
 * {@link LogisticsRoleEnum} 里给它挂的那些权限」。这两条是 ADR 0026 在物流侧的落地形态——
 * 权限不手写 SQL，而是从枚举生成，所以「枚举与代码一致」这件事必须有测试守住。
 */
public class LogisticsPermissionSyncServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private MenuApi menuApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private TenantApi tenantApi;

    @InjectMocks
    private LogisticsPermissionSyncServiceImpl syncService;

    @Test
    public void testSync_firstRun_createsMenusRolesAndAssignments() {
        // 菜单都不存在 → 逐个建；角色都不存在 → 逐个建
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(null);
        AtomicLong menuIdSequence = new AtomicLong(2000L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> menuIdSequence.incrementAndGet());
        // 页面菜单都在（logistics-menu.sql 已导）
        when(menuApi.getMenuIdByComponentName(anyString())).thenReturn(3000L);
        Map<String, Long> roleIds = roleIds();
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(null);
        when(roleApi.createRole(anyString(), anyString()))
                .thenAnswer(invocation -> roleIds.get(invocation.getArgument(0)));
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(3);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(5);

        // 调用
        LogisticsPermissionSyncResult result = syncService.sync();

        // 断言：每个权限都建了菜单行；三个租户内角色都建了
        assertEquals(LogisticsRoleEnum.allPermissions().size(), result.getCreatedMenuCount());
        assertEquals(LogisticsRoleEnum.values().length, result.getCreatedRoleCount());
        assertEquals(3 * LogisticsRoleEnum.values().length, result.getAssignedRoleMenuCount());
        assertEquals(5, result.getAddedPackageMenuCount());
        // 车辆与司机的查询权是派车的基础，必须真的被建出来
        verify(menuApi).ensurePermissionMenu(anyString(), eq(LogisticsPermission.VEHICLE_CREATE), any(), any());
        verify(menuApi).ensurePermissionMenu(anyString(), eq(LogisticsPermission.DRIVER_QUERY), any(), any());
        verify(roleApi).createRole(eq(LogisticsRoleEnum.DISPATCHER.getCode()), anyString());
        verify(roleApi).createRole(eq(LogisticsRoleEnum.DRIVER.getCode()), anyString());
        verify(tenantApi).addTenantPackageMenuIds(
                eq(LogisticsPermissionSyncServiceImpl.RECYCLING_TENANT_PACKAGE_ID), anyCollection());
        // 调度拿到的菜单数量 = 它在枚举里登记的权限数量（本票是车辆 + 司机查询两条）
        verify(permissionApi).addRoleMenus(eq(roleIds.get(LogisticsRoleEnum.DISPATCHER.getCode())),
                argThat(menuIds -> menuIds.size() == LogisticsRoleEnum.DISPATCHER.getPermissions().size()));
        // 司机只拿司机端的三个动作（V2c #79）：数量要对得上枚举里登记的条数
        verify(permissionApi).addRoleMenus(eq(roleIds.get(LogisticsRoleEnum.DRIVER.getCode())),
                argThat(menuIds -> menuIds.size() == LogisticsRoleEnum.DRIVER.getPermissions().size()));
    }

    /**
     * 权限行要按 {@link LogisticsPermissionMenuEnum} 归集：中文名 + 挂在页面菜单下。
     *
     * <p>否则「菜单管理」里就是一片 {@code logistics:transport-cost:delete} 这样的裸标识，
     * 既看不懂也散落在根节点（本测试是这次修复的回归护栏）。
     */
    @Test
    public void testSync_placesPermissionMenusUnderPages() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(null);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any())).thenReturn(2001L);
        when(menuApi.getMenuIdByComponentName(anyString())).thenReturn(3000L);
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(2L);
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(0);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        syncService.sync();

        // 中文名 + 排序 + 挂在页面上：不再是 (permission, permission, 根节点)
        verify(menuApi).ensurePermissionMenu(eq("车辆新增"), eq(LogisticsPermission.VEHICLE_CREATE), eq(3000L), eq(1));
        verify(menuApi).ensurePermissionMenu(eq("运输费用删除"), eq(LogisticsPermission.TRANSPORT_COST_DELETE),
                eq(3000L), eq(9));
        verify(menuApi).ensurePermissionMenu(eq("司机端-接单"), eq(LogisticsPermission.DRIVER_APP_TASK_ACCEPT),
                eq(3000L), eq(7));
        // 没有对应页面的权限留在根：名字仍要是中文
        verify(menuApi).ensurePermissionMenu(eq("物流权限初始化"), eq(LogisticsPermission.TENANT_ROLE_INIT), isNull(), eq(1));
        // 不该再出现「菜单名 = 权限标识」的行
        for (LogisticsPermissionMenuEnum placement : LogisticsPermissionMenuEnum.values()) {
            assertFalse(placement.getName().equals(placement.getPermission()),
                    "归集后的菜单名不该等于权限标识：" + placement.getPermission());
        }
    }

    /**
     * 页面菜单还没建（本地库还没导 {@code logistics-menu.sql}）时不能把权限行丢掉：
     * 落在根节点，比 {@code @ss.hasPermission} 判为无权限要好。
     */
    @Test
    public void testSync_pageMenuMissing_fallsBackToRoot() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(null);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any())).thenReturn(2001L);
        when(menuApi.getMenuIdByComponentName(anyString())).thenReturn(null);
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(2L);
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(0);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        LogisticsPermissionSyncResult result = syncService.sync();

        assertEquals(LogisticsRoleEnum.allPermissions().size(), result.getCreatedMenuCount());
        verify(menuApi).ensurePermissionMenu(eq("车辆新增"), eq(LogisticsPermission.VEHICLE_CREATE), isNull(), eq(1));
    }

    @Test
    public void testSync_secondRun_isIdempotent() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(1L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any())).thenReturn(1L);
        when(menuApi.getMenuIdByComponentName(anyString())).thenReturn(3000L);
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(2L);
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(0);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        LogisticsPermissionSyncResult result = syncService.sync();

        assertTrue(result.isEmpty(), "第二次同步不该有任何新增，计数要收敛到 0");
        verify(roleApi, never()).createRole(anyString(), anyString());
        verify(tenantApi).addTenantPackageMenuIds(anyLong(), anyCollection());
    }

    @Test
    public void testSyncGlobal_doesNotTouchTenantRoles() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(1L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any())).thenReturn(1L);
        when(menuApi.getMenuIdByComponentName(anyString())).thenReturn(3000L);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        LogisticsPermissionSyncResult result = syncService.syncGlobal();

        assertTrue(result.isEmpty());
        verify(roleApi, never()).getRoleIdByCode(anyString());
        verify(roleApi, never()).createRole(anyString(), anyString());
        verify(permissionApi, never()).addRoleMenus(anyLong(), anyCollection());
    }

    /**
     * 角色授权必须把权限菜单的**父链**一起挂上。
     *
     * <p>否则 {@code get-permission-info} 的 {@code filterDisableMenus} 会因「父菜单不在角色集合里」
     * 把按钮型权限判为禁用，权限列表变空：后端 {@code @ss.hasPermission} 放行、前端却提示没有权限。
     * 这是本测试要守住的回归护栏。
     */
    @Test
    public void testSync_roleMenusIncludeAncestors() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(null);
        AtomicLong menuIdSequence = new AtomicLong(2000L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> menuIdSequence.incrementAndGet());
        when(menuApi.getMenuIdByComponentName(anyString())).thenReturn(3000L);
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(2L);
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(0);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);
        // 每个权限菜单的父链：页面菜单 3000 + 模块菜单 2999
        when(menuApi.getAncestorMenuIds(anyCollection())).thenReturn(new LinkedHashSet<>(List.of(2999L, 3000L)));

        syncService.sync();

        // 调度角色拿到的菜单数 = 它登记的权限数 + 2 个祖先
        verify(permissionApi).addRoleMenus(eq(2L), argThat(menuIds ->
                menuIds.size() == LogisticsRoleEnum.DISPATCHER.getPermissions().size() + 2
                        && menuIds.contains(2999L) && menuIds.contains(3000L)));
        // 父链也要进「回收企业套餐」，否则租户开箱即得时同样缺父菜单
        verify(tenantApi).addTenantPackageMenuIds(anyLong(), argThat(menuIds -> menuIds.contains(2999L)));
    }

    private static Map<String, Long> roleIds() {
        Map<String, Long> roleIds = new HashMap<>();
        roleIds.put(LogisticsRoleEnum.ADMIN.getCode(), 10L);
        roleIds.put(LogisticsRoleEnum.DISPATCHER.getCode(), 11L);
        roleIds.put(LogisticsRoleEnum.DRIVER.getCode(), 12L);
        roleIds.put(LogisticsRoleEnum.FINANCE.getCode(), 13L);
        return roleIds;
    }

}
