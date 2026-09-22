package cn.iocoder.yudao.module.icbc.service.tenant;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.enums.RecyclingRoleEnum;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 司机角色的 icbc 权限授予（ADR 0033）。
 *
 * <p>这一条是**跨模块的约定字符串**（icbc 只认 {@code logistics_driver} 这个 code），
 * 所以要有测试守着两件事：该授的授到、**不该授的一条都不给**。
 *
 * <p>「不该授的」尤其要守：司机在现场能拿到收购登记、结算确认、付款、开票任何一个权限，
 * 都意味着绕开了 ADR 0030（确认一律由出售者本人做）与 ADR 0031（现场不产生金额）。
 */
public class RecyclingPermissionSyncDriverGrantTest extends BaseMockitoUnitTest {

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
    public void testSync_grantsOnboardingPermissionsToDriverRole() {
        // 权限行都还不存在 → 逐个建，且各自拿到不同的 id（同一个 id 会被 Set 去重，验不出条数）
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(null);
        java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong(100L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> seq.incrementAndGet());
        // 回收域的四个角色已存在；司机角色也在（物流域的 init 先跑过）
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(2L);
        when(roleApi.getRoleIdByCode(RecyclingPermissionSyncServiceImpl.DRIVER_ROLE_CODE)).thenReturn(99L);
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(0);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        syncService.sync();

        // 司机角色拿到了「准入四步 + 建自然人档案 + 签发一次性令牌 + 只读品类配置」这几条
        // 7 条：建自然人档案、查档案、实名/入驻、框架协议、首次授权、签发一次性令牌、查品类配置
        // 最后一条是 V6 #73 加的：交接登记要选权威品类（ADR 0028），只读
        verify(permissionApi).addRoleMenus(eq(99L), argThat(menuIds -> menuIds.size() == 7));
    }

    @Test
    public void testSync_driverRoleNotExists_skipsWithoutError() {
        when(menuApi.getMenuIdByPermission(anyString())).thenReturn(1L);
        when(menuApi.ensurePermissionMenu(anyString(), anyString(), any(), any())).thenReturn(1L);
        Map<String, Long> roleIds = new HashMap<>();
        roleIds.put(RecyclingRoleEnum.ADMIN.getCode(), 10L);
        roleIds.put(RecyclingRoleEnum.RECEIVER.getCode(), 11L);
        roleIds.put(RecyclingRoleEnum.INVOICER.getCode(), 12L);
        roleIds.put(RecyclingRoleEnum.FINANCE.getCode(), 13L);
        when(roleApi.getRoleIdByCode(anyString()))
                .thenAnswer(invocation -> roleIds.get(invocation.getArgument(0, String.class)));
        when(permissionApi.addRoleMenus(anyLong(), anyCollection())).thenReturn(0);
        when(tenantApi.addTenantPackageMenuIds(anyLong(), anyCollection())).thenReturn(0);

        // 司机角色不存在（还没跑物流域的 init）→ 跳过而不是报错，顺序才不会变成硬约束
        assertDoesNotThrow(() -> syncService.sync());
        verify(permissionApi, never()).addRoleMenus(isNull(), anyCollection());
    }

    @Test
    public void testDriverPermissionList_staysNarrow() {
        // 这份清单的要点是「窄」：加权限前先回头看 ADR 0030 / 0031
        java.lang.reflect.Field field;
        try {
            field = RecyclingPermissionSyncServiceImpl.class.getDeclaredField("DRIVER_PERMISSIONS");
        } catch (NoSuchFieldException e) {
            fail("DRIVER_PERMISSIONS 不见了：确认司机权限清单还在");
            return;
        }
        field.setAccessible(true);
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> permissions = (java.util.List<String>) field.get(null);
            assertTrue(permissions.contains(RecyclingPermission.SELLER_ONBOARDING_EXECUTE));
            assertTrue(permissions.contains(RecyclingPermission.PAYEE_CREATE));
            assertTrue(permissions.contains(RecyclingPermission.GOODS_CONFIG_QUERY), "交接登记要选品类");
            assertFalse(permissions.contains(RecyclingPermission.GOODS_CONFIG_CREATE),
                    "司机只能读品类配置，不能改它");
            // 现场不产生金额、确认由出售者本人做：这几类一个都不能给司机
            assertFalse(permissions.contains(RecyclingPermission.ACQUISITION_CREATE), "司机不该有收购登记权限");
            assertFalse(permissions.contains(RecyclingPermission.INVOICE_APPLICATION_APPLY), "司机不该有开票申请权限");
            assertFalse(permissions.contains(RecyclingPermission.PAYMENT_CREATE), "司机不该有付款权限");
            assertFalse(permissions.contains(RecyclingPermission.RED_INVOICE_APPLY), "司机不该有红冲权限");
        } catch (IllegalAccessException e) {
            fail("读不到 DRIVER_PERMISSIONS：" + e.getMessage());
        }
    }

}
