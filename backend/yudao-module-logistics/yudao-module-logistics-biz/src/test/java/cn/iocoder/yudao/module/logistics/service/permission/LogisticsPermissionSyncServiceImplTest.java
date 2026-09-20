package cn.iocoder.yudao.module.logistics.service.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(menuApi.createPermissionMenu(anyString(), anyString()))
                .thenAnswer(invocation -> menuIdSequence.incrementAndGet());
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
        verify(menuApi).createPermissionMenu(anyString(), eq(LogisticsPermission.VEHICLE_CREATE));
        verify(menuApi).createPermissionMenu(anyString(), eq(LogisticsPermission.DRIVER_QUERY));
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

    @Test
    public void testSync_secondRun_isIdempotent() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(1L);
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(2L);
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(0);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        LogisticsPermissionSyncResult result = syncService.sync();

        assertTrue(result.isEmpty(), "第二次同步不该有任何新增，计数要收敛到 0");
        verify(menuApi, never()).createPermissionMenu(anyString(), anyString());
        verify(roleApi, never()).createRole(anyString(), anyString());
        verify(tenantApi).addTenantPackageMenuIds(anyLong(), anyCollection());
    }

    @Test
    public void testSyncGlobal_doesNotTouchTenantRoles() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(1L);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        LogisticsPermissionSyncResult result = syncService.syncGlobal();

        assertTrue(result.isEmpty());
        verify(roleApi, never()).getRoleIdByCode(anyString());
        verify(roleApi, never()).createRole(anyString(), anyString());
        verify(permissionApi, never()).addRoleMenus(anyLong(), anyCollection());
    }

    private static Map<String, Long> roleIds() {
        Map<String, Long> roleIds = new HashMap<>();
        roleIds.put(LogisticsRoleEnum.ADMIN.getCode(), 10L);
        roleIds.put(LogisticsRoleEnum.DISPATCHER.getCode(), 11L);
        roleIds.put(LogisticsRoleEnum.DRIVER.getCode(), 12L);
        return roleIds;
    }

}
