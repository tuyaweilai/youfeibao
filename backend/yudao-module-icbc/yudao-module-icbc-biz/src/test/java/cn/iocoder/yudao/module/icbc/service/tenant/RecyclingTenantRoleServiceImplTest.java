package cn.iocoder.yudao.module.icbc.service.tenant;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.icbc.enums.RecyclingRoleEnum;
import cn.iocoder.yudao.module.icbc.service.tenant.impl.RecyclingTenantRoleServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RecyclingTenantRoleServiceImpl} 的单元测试。
 */
public class RecyclingTenantRoleServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private RoleApi roleApi;

    @InjectMocks
    private RecyclingTenantRoleServiceImpl recyclingTenantRoleService;

    @Test
    public void testInitTenantRoles_createsMissingOnly() {
        // 管理员已由建租户流程生成，另外三个不存在
        when(roleApi.getRoleIdByCode(RecyclingRoleEnum.ADMIN.getCode())).thenReturn(1L);
        when(roleApi.getRoleIdByCode(RecyclingRoleEnum.RECEIVER.getCode())).thenReturn(null);
        when(roleApi.getRoleIdByCode(RecyclingRoleEnum.INVOICER.getCode())).thenReturn(null);
        when(roleApi.getRoleIdByCode(RecyclingRoleEnum.FINANCE.getCode())).thenReturn(null);
        when(roleApi.createRole(anyString(), anyString())).thenReturn(2L);

        // 调用
        recyclingTenantRoleService.initTenantRoles();

        // 断言：管理员不重复建，三个角色各自补齐，平台运营不在租户内建
        verify(roleApi, never()).createRole(eq(RecyclingRoleEnum.ADMIN.getCode()), anyString());
        verify(roleApi).createRole(eq(RecyclingRoleEnum.RECEIVER.getCode()), eq(RecyclingRoleEnum.RECEIVER.getName()));
        verify(roleApi).createRole(eq(RecyclingRoleEnum.INVOICER.getCode()), eq(RecyclingRoleEnum.INVOICER.getName()));
        verify(roleApi).createRole(eq(RecyclingRoleEnum.FINANCE.getCode()), eq(RecyclingRoleEnum.FINANCE.getName()));
        verify(roleApi, never()).createRole(eq(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode()), anyString());
    }

    @Test
    public void testInitTenantRoles_idempotent() {
        // 全部已存在
        when(roleApi.getRoleIdByCode(anyString())).thenReturn(1L);

        // 调用
        recyclingTenantRoleService.initTenantRoles();

        // 断言：一个都不重复建
        verify(roleApi, never()).createRole(anyString(), anyString());
    }

}
