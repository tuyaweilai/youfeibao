package cn.iocoder.yudao.module.icbc.enums;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RecyclingRoleEnum} 的单元测试。
 *
 * <p>这套角色策略是 #4 的核心：谁只能做什么，在这里被写死并测试。
 */
public class RecyclingRoleEnumTest {

    @Test
    public void testReceiverOnlyOwnsAcquisition() {
        // 收货员能做收购登记相关的全部操作
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PAYEE_CREATE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PAYEE_QUERY));
        // 但发起不了开票、付款
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.INVOICE_CREATE));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PAYMENT_CREATE));
    }

    @Test
    public void testInvoicerOnlyOwnsInvoiceFlow() {
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.INVOICE_CREATE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.PAYMENT_CREATE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.DOWNLOAD_EXECUTE));
        // 不能改出售者档案
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.PAYEE_CREATE));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.PAYEE_DELETE));
    }

    @Test
    public void testFinanceOnlyReadsAndReconciles() {
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PAYMENT_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.DOWNLOAD_QUERY));
        // 财务不能自己发起付款或开票
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PAYMENT_CREATE));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.INVOICE_CREATE));
    }

    @Test
    public void testAdminOwnsAllTenantScopedPermissions() {
        for (String permission : RecyclingRoleEnum.allPermissions()) {
            if (permission.equals(RecyclingPermission.PLATFORM_INVOICE_QUERY)
                    || permission.equals(RecyclingPermission.CALLBACK_RETRY)) {
                continue; // 平台运营专属，租户管理员不碰
            }
            assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(), permission),
                    "管理员应拥有权限 " + permission);
        }
    }

    @Test
    public void testPlatformOperatorIsTheOnlyCrossTenantRole() {
        assertTrue(RecyclingRoleEnum.PLATFORM_OPERATOR.isCrossTenant());
        assertFalse(RecyclingRoleEnum.ADMIN.isCrossTenant());
        assertFalse(RecyclingRoleEnum.RECEIVER.isCrossTenant());
        assertFalse(RecyclingRoleEnum.INVOICER.isCrossTenant());
        assertFalse(RecyclingRoleEnum.FINANCE.isCrossTenant());

        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PLATFORM_INVOICE_QUERY));
        // 平台运营不参与租户内的收购登记
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PAYEE_CREATE));
    }

    @Test
    public void testRoleCodesForPermission() {
        Set<String> roles = RecyclingRoleEnum.roleCodesForPermission(RecyclingPermission.PAYEE_CREATE);
        assertTrue(roles.contains(RecyclingRoleEnum.RECEIVER.getCode()));
        assertTrue(roles.contains(RecyclingRoleEnum.ADMIN.getCode()));
        assertTrue(roles.contains(RecyclingRoleEnum.SUPER_ADMIN_CODE));
        assertFalse(roles.contains(RecyclingRoleEnum.INVOICER.getCode()));
        assertFalse(roles.contains(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode()));
    }

    @Test
    public void testUnknownPermissionDenied() {
        assertEquals(Set.of(), RecyclingRoleEnum.roleCodesForPermission("icbc:not-registered"));
        assertEquals(Set.of(), RecyclingRoleEnum.roleCodesForPermission(null));
    }

    @Test
    public void testOfCode() {
        assertEquals(RecyclingRoleEnum.ADMIN,
                RecyclingRoleEnum.ofCode(RecyclingRoleEnum.ADMIN.getCode()).orElseThrow());
        assertTrue(RecyclingRoleEnum.ofCode("nobody").isEmpty());
    }

}
