package cn.iocoder.yudao.module.icbc.framework.security;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.enums.RecyclingRoleEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.DeptDataPermissionRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RecyclingPermissionChecker} 的单元测试。
 *
 * <p>关注两点：权限被翻译成了正确的角色集合；未登记权限与未登录一律拒绝。
 */
public class RecyclingPermissionCheckerTest {

    private final RecordingPermissionApi permissionApi = new RecordingPermissionApi();
    private final RecyclingPermissionChecker permissionChecker = new RecyclingPermissionChecker(permissionApi);

    @BeforeEach
    public void setUp() {
        permissionApi.reset();
        LoginUser loginUser = new LoginUser();
        loginUser.setId(99L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
        permissionApi.reset();
    }

    @Test
    public void testHasPermission_translatesToRoles() {
        permissionApi.setResult(true);

        assertTrue(permissionChecker.hasPermission(RecyclingPermission.PAYEE_CREATE));

        assertEquals(99L, permissionApi.lastUserId);
        assertTrue(Arrays.asList(permissionApi.lastRoles).contains(RecyclingRoleEnum.RECEIVER.getCode()));
        assertTrue(Arrays.asList(permissionApi.lastRoles).contains(RecyclingRoleEnum.ADMIN.getCode()));
        assertFalse(Arrays.asList(permissionApi.lastRoles).contains(RecyclingRoleEnum.INVOICER.getCode()));
    }

    @Test
    public void testHasPermission_denied() {
        permissionApi.setResult(false);
        assertFalse(permissionChecker.hasPermission(RecyclingPermission.PLATFORM_INVOICE_QUERY));
    }

    @Test
    public void testHasPermission_unknownPermissionDeniedWithoutQuery() {
        assertFalse(permissionChecker.hasPermission("icbc:not-registered"));
        assertFalse(permissionApi.queried);
    }

    @Test
    public void testHasPermission_notLoggedInDenied() {
        SecurityContextHolder.clearContext();
        assertFalse(permissionChecker.hasPermission(RecyclingPermission.PAYEE_QUERY));
        assertFalse(permissionApi.queried);
    }

    /**
     * 记录调用参数的 {@link PermissionApi} 假实现，避免 Mockito 对可变参数的匹配麻烦。
     */
    private static class RecordingPermissionApi implements PermissionApi {

        private Long lastUserId;
        private String[] lastRoles;
        private boolean result;
        private boolean queried;

        void setResult(boolean result) {
            this.result = result;
        }

        void reset() {
            this.lastUserId = null;
            this.lastRoles = null;
            this.result = false;
            this.queried = false;
        }

        @Override
        public Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
            return Collections.emptySet();
        }

        @Override
        public boolean hasAnyPermissions(Long userId, String... permissions) {
            return false;
        }

        @Override
        public boolean hasAnyRoles(Long userId, String... roles) {
            this.queried = true;
            this.lastUserId = userId;
            this.lastRoles = roles;
            return result;
        }

        @Override
        public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
            return null;
        }

    }

}
