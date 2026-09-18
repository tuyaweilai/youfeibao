package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RoleApiImpl} 的单元测试。
 */
public class RoleApiImplTest extends BaseMockitoUnitTest {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleApiImpl roleApi;

    @Test
    public void testGetRoleIdByCode_exists() {
        RoleDO role = new RoleDO();
        role.setId(10L);
        when(roleService.getRoleByCode("recycling_receiver")).thenReturn(role);
        assertEquals(10L, roleApi.getRoleIdByCode("recycling_receiver"));
    }

    @Test
    public void testGetRoleIdByCode_notExists() {
        when(roleService.getRoleByCode("recycling_receiver")).thenReturn(null);
        assertNull(roleApi.getRoleIdByCode("recycling_receiver"));
    }

    @Test
    public void testCreateRole() {
        when(roleService.createRole(argThat(reqVO -> {
            assertEquals("recycling_finance", reqVO.getCode());
            assertEquals("财务", reqVO.getName());
            assertEquals(CommonStatusEnum.ENABLE.getStatus(), reqVO.getStatus());
            return true;
        }), eq(RoleTypeEnum.CUSTOM.getType()))).thenReturn(11L);

        Long id = roleApi.createRole("recycling_finance", "财务");

        assertEquals(11L, id);
        ArgumentCaptor<RoleSaveReqVO> captor = ArgumentCaptor.forClass(RoleSaveReqVO.class);
        verify(roleService).createRole(captor.capture(), eq(RoleTypeEnum.CUSTOM.getType()));
        assertEquals("recycling_finance", captor.getValue().getCode());
    }

}
