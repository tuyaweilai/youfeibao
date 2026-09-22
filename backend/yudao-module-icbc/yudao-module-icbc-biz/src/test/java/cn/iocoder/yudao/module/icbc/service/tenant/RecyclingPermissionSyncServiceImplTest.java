package cn.iocoder.yudao.module.icbc.service.tenant;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.enums.RecyclingRoleEnum;
import cn.iocoder.yudao.module.icbc.service.tenant.dto.RecyclingPermissionSyncResult;
import cn.iocoder.yudao.module.icbc.service.tenant.impl.RecyclingPermissionSyncServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RecyclingPermissionSyncServiceImpl} 的单元测试。
 *
 * <p>关注幂等：第二次同步不建行、不授权；以及「角色拿到的菜单就是
 * {@link RecyclingRoleEnum} 里给它挂的那些权限」。
 */
public class RecyclingPermissionSyncServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private MenuApi menuApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private TenantApi tenantApi;

    @InjectMocks
    private RecyclingPermissionSyncServiceImpl syncService;

    @Test
    public void testSync_firstRun_createsMenusRolesAndAssignments() {
        // 菜单都不存在 → 逐个建；角色都不存在 → 逐个建
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(null);
        AtomicLong menuIdSequence = new AtomicLong(1000L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> menuIdSequence.incrementAndGet());
        // 页面菜单都在（icbc-menu.sql 已导）
        when(menuApi.getMenuIdByComponentName(anyString())).thenReturn(3000L);
        Map<String, Long> roleIds = roleIds();
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(null);
        when(roleApi.createRole(anyString(), anyString())).thenAnswer(invocation -> roleIds.get(invocation.getArgument(0)));
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(3);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(5);

        // 调用
        RecyclingPermissionSyncResult result = syncService.sync();

        // 断言：每个权限都建了菜单行；四个租户内角色都建了；平台运营不在租户内建
        assertEquals(RecyclingRoleEnum.allPermissions().size(), result.getCreatedMenuCount());
        assertEquals(4, result.getCreatedRoleCount());
        assertEquals(12, result.getAssignedRoleMenuCount()); // 4 个角色 × mock 返回 3
        assertEquals(5, result.getAddedPackageMenuCount());
        verify(menuApi).ensurePermissionMenu(anyString(), eq(RecyclingPermission.PAYEE_CREATE), any(), any());
        // 菜单 SQL 里编排过的权限：同步过程不参与取名与位置（传回权限标识、根节点，交回 ensurePermissionMenu 自己判断不动）
        verify(menuApi).ensurePermissionMenu(eq(RecyclingPermission.PAYEE_CREATE), eq(RecyclingPermission.PAYEE_CREATE),
                isNull(), eq(0));
        // SQL 里没编排的（RecyclingPermissionMenuEnum 登记的）：带上中文名与父页面，开机时就地归集
        verify(menuApi).ensurePermissionMenu(eq("到期提醒查询"), eq(RecyclingPermission.EXPIRY_WARNING_QUERY),
                eq(3000L), eq(1));
        verify(menuApi).ensurePermissionMenu(eq("回收域权限初始化"), eq(RecyclingPermission.TENANT_ROLE_INIT),
                isNull(), eq(2));
        verify(roleApi).createRole(eq(RecyclingRoleEnum.RECEIVER.getCode()), anyString());
        verify(roleApi).createRole(eq(RecyclingRoleEnum.INVOICER.getCode()), anyString());
        verify(roleApi).createRole(eq(RecyclingRoleEnum.FINANCE.getCode()), anyString());
        verify(roleApi, never()).createRole(eq(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode()), anyString());
        verify(tenantApi).addTenantPackageMenuIds(eq(RecyclingPermissionSyncServiceImpl.RECYCLING_TENANT_PACKAGE_ID),
                anyCollection());
        // 收货员拿到的菜单数量 = 它在 RecyclingRoleEnum 里登记的权限数量
        verify(permissionApi).addRoleMenus(eq(roleIds.get(RecyclingRoleEnum.RECEIVER.getCode())),
                argThat(menuIds -> menuIds.size() == RecyclingRoleEnum.RECEIVER.getPermissions().size()));
        // 平台运营不参与租户内的授权
        verify(permissionApi, never()).addRoleMenus(eq(roleIds.get(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode())),
                anyCollection());
    }

    @Test
    public void testSync_secondRun_isIdempotent() {
        // 菜单与角色都已存在，授权与套餐也已补齐
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(1L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any())).thenReturn(1L);
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(2L);
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(0);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        // 调用
        RecyclingPermissionSyncResult result = syncService.sync();

        // 断言：没有新建任何行，结果为空
        assertTrue(result.isEmpty());
        verify(roleApi, never()).createRole(anyString(), anyString());
        verify(tenantApi).addTenantPackageMenuIds(anyLong(), anyCollection());
    }

    @Test
    public void testSyncGlobal_doesNotTouchTenantRoles() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(1L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any())).thenReturn(1L);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        RecyclingPermissionSyncResult result = syncService.syncGlobal();

        assertTrue(result.isEmpty());
        verify(roleApi, never()).getRoleIdByCode(anyString());
        verify(roleApi, never()).createRole(anyString(), anyString());
        verify(permissionApi, never()).addRoleMenus(anyLong(), anyCollection());
    }

    private static Map<String, Long> roleIds() {
        Map<String, Long> roleIds = new HashMap<>();
        roleIds.put(RecyclingRoleEnum.ADMIN.getCode(), 10L);
        roleIds.put(RecyclingRoleEnum.RECEIVER.getCode(), 11L);
        roleIds.put(RecyclingRoleEnum.INVOICER.getCode(), 12L);
        roleIds.put(RecyclingRoleEnum.FINANCE.getCode(), 13L);
        roleIds.put(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(), 14L);
        return roleIds;
    }

}
