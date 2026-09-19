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
        // 现场选品类要读配置（单位 / 税率 / 税收分类编码），但只能读
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.GOODS_CONFIG_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.GOODS_CONFIG_CREATE));
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
            if (permission.startsWith("icbc:platform:")) {
                continue; // 平台运营专属（跨租户），租户管理员不碰
            }
            if (permission.equals(RecyclingPermission.PLATFORM_INVOICE_QUERY)
                    || permission.equals(RecyclingPermission.PLATFORM_QUALIFICATION_QUERY)
                    || permission.equals(RecyclingPermission.PLATFORM_QUALIFICATION_AUDIT)
                    || permission.equals(RecyclingPermission.SCRAP_CODE_CREATE)
                    || permission.equals(RecyclingPermission.SCRAP_CODE_UPDATE)
                    || permission.equals(RecyclingPermission.SCRAP_CODE_DELETE)
                    || permission.equals(RecyclingPermission.PLATFORM_CALLBACK_QUERY)
                    || permission.equals(RecyclingPermission.PLATFORM_CALLBACK_RETRY)
                    || permission.equals(RecyclingPermission.PLATFORM_EVIDENCE_QUERY)
                    || permission.equals(RecyclingPermission.PLATFORM_BILLING_QUERY)
                    || permission.equals(RecyclingPermission.PLATFORM_BILLING_MANAGE)) {
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
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PLATFORM_CALLBACK_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PLATFORM_CALLBACK_RETRY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PLATFORM_EVIDENCE_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PLATFORM_BILLING_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PLATFORM_BILLING_MANAGE));
        // 计费台账是平台自己的账，租户内角色一个都不能看 / 改
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PLATFORM_BILLING_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PLATFORM_BILLING_MANAGE));
        // 通知监控与重放是平台专属，租户管理员不能跨租户看
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PLATFORM_CALLBACK_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PLATFORM_CALLBACK_RETRY));
        // 平台运营不参与租户内的收购登记
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PAYEE_CREATE));
    }

    @Test
    public void testEvidencePermissionsAcrossRoles() {
        // 收货员能在现场补录证据，但不能导出
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.EVIDENCE_ATTACH));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.EVIDENCE_EXPORT));
        // 财务能查阅与导出，但不补录
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.EVIDENCE_EXPORT));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.EVIDENCE_ATTACH));
        // 开票员能查、能补录
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.EVIDENCE_QUERY));
        // 平台运营不参与租户内的证据补录
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.EVIDENCE_ATTACH));
    }

    @Test
    public void testPublicTokenPermission() {
        // 能签发公开令牌的是租户内的四个角色，平台运营不参与
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.PUBLIC_TOKEN_CREATE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PUBLIC_TOKEN_CREATE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PUBLIC_TOKEN_CREATE));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PUBLIC_TOKEN_CREATE));
    }

    @Test
    public void testAppointmentPermissions() {
        // 收货员现场登记要带出预约，并能标到场 / 未到场
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.APPOINTMENT_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.APPOINTMENT_MANAGE));
        // 预约不进统计与额度口径，平台运营不参与
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.APPOINTMENT_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.APPOINTMENT_MANAGE));
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
