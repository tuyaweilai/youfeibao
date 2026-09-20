package cn.iocoder.yudao.module.icbc.enums;

import org.junit.jupiter.api.Test;

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
    public void testPurchaseContractPermissions() {
        // 管理员既能维护也能审核
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_MANAGE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_AUDIT));
        // 收货员现场要选「有效采购安排」，只读；采购合同不是他的维护对象
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_MANAGE));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_AUDIT));
        // 财务只读
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_MANAGE));
        // 平台运营不参与租户内的采购合同
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PURCHASE_CONTRACT_QUERY));
    }

    @Test
    public void testHandoverBatchPermissions() {
        // 交接批次与磅次是现场动作：管理员 / 收货员可登记与指定有效磅次
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.HANDOVER_BATCH_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.HANDOVER_BATCH_MANAGE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.HANDOVER_BATCH_MANAGE));
        // 开票员 / 财务只在追溯时看得到，不改磅次
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.HANDOVER_BATCH_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.HANDOVER_BATCH_MANAGE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.HANDOVER_BATCH_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.HANDOVER_BATCH_MANAGE));
    }

    @Test
    public void testPurchaseOrderPermissions() {
        // 管理员维护采购订单（建单 / 改单 / 状态流转 / 登记成交价格）
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_ORDER_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_ORDER_MANAGE));
        // 现场只选「有效采购安排」，只读；不是他的维护对象
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_ORDER_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_ORDER_MANAGE));
        // 开票员 / 财务只读
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.PURCHASE_ORDER_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.PURCHASE_ORDER_MANAGE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PURCHASE_ORDER_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PURCHASE_ORDER_MANAGE));
        // 平台运营不参与租户内的采购订单
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PURCHASE_ORDER_QUERY));
    }

    @Test
    public void testPurchaseProgressAndExceptionPermissions() {
        // 履约配置与异常授权是管理员的事：口径与拦截规则由企业负责人定，授权也由他审
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_SETTING_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_SETTING_MANAGE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_REQUEST));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_AUDIT));
        // 现场碰到超量 / 过期 / 跨场站时能提交授权申请、能查，但不能自己审、也不能改配置
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_REQUEST));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_AUDIT));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.PURCHASE_SETTING_MANAGE));
        // 财务能看配置与授权（对账要看），但不提交也不审
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PURCHASE_SETTING_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_AUDIT));
        // 平台运营不参与租户内的履约配置与授权
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PURCHASE_EXCEPTION_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.PURCHASE_SETTING_QUERY));
    }

    @Test
    public void testInputInvoicePermissions() {
        // 进项收票是财务的事：登记与勾稽归财务，管理员全量
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.INPUT_INVOICE_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.INPUT_INVOICE_MANAGE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.INPUT_INVOICE_MANAGE));
        // 开票员只看得到，不做收票登记
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.INPUT_INVOICE_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.INPUT_INVOICE_MANAGE));
        // 收货员与平台运营不参与租户内的进项收票
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.INPUT_INVOICE_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.INPUT_INVOICE_QUERY));
    }

    @Test
    public void testStockInPermissions() {
        // 入库是仓管的事：管理员全量；没有单独的仓管角色，收货员兼做现场到入库的收尾
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.STOCK_IN_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.STOCK_IN_MANAGE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.STOCK_IN_MANAGE));
        // 开票员 / 财务对账要看库存流水，但不确认入库
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.STOCK_IN_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.STOCK_IN_MANAGE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.STOCK_IN_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.STOCK_IN_MANAGE));
        // 平台运营不参与租户内的入库
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.STOCK_IN_QUERY));
    }

    @Test
    public void testAcceptanceAndWeightDiffPermissions() {
        // 接收结论是现场动作：管理员 / 收货员可记录
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.ACQUISITION_ACCEPTANCE));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.ACQUISITION_ACCEPTANCE));
        // 差异清单是只读的：四个租户内角色都能看（财务对账、开票员开票前核）
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.ACQUISITION_WEIGHT_DIFF_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.ACQUISITION_WEIGHT_DIFF_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.ACQUISITION_WEIGHT_DIFF_QUERY));
        // 开票员 / 财务不记录接收结论（现场结论由收货员出）
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.ACQUISITION_ACCEPTANCE));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.ACQUISITION_ACCEPTANCE));
        // 平台运营不参与租户内的接收结论与差异清单
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.ACQUISITION_ACCEPTANCE));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.ACQUISITION_WEIGHT_DIFF_QUERY));
    }

    @Test
    public void testStockOpsPermissions() {
        // 库存作业（非销售出库 / 调拨 / 盘点 / 期初）是仓管的事：管理员全量，收货员兼做收尾
        for (String permission : java.util.List.of(
                RecyclingPermission.STOCK_OUT_MANAGE, RecyclingPermission.STOCK_MOVE_MANAGE,
                RecyclingPermission.STOCK_CHECK_MANAGE, RecyclingPermission.STOCK_OPENING_MANAGE)) {
            assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(), permission));
            assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(), permission));
        }
        // 开票员 / 财务对账要看库存，但不登记出库 / 调拨 / 盘点 / 期初；「当前库存」口径谁都能读
        for (String permission : java.util.List.of(
                RecyclingPermission.STOCK_OUT_QUERY, RecyclingPermission.STOCK_MOVE_QUERY,
                RecyclingPermission.STOCK_CHECK_QUERY, RecyclingPermission.STOCK_OPENING_QUERY,
                RecyclingPermission.STOCK_READINESS_QUERY)) {
            assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(), permission));
            assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(), permission));
        }
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.STOCK_CHECK_MANAGE));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.STOCK_OPENING_MANAGE));
        // 平台运营不参与租户内的库存作业
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.STOCK_OUT_QUERY));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.STOCK_READINESS_QUERY));
    }

    @Test
    public void testTracePermissions() {
        // 关联单据查询（#55）：四个租户内角色都能查链路
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.TRACE_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.TRACE_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.TRACE_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.TRACE_QUERY));
        // 未脱敏查看与导出只给对账岗位（管理员 / 财务）；收货员 / 开票员看脱敏值
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.TRACE_SENSITIVE_VIEW));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.TRACE_EXPORT));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.RECEIVER.getCode(),
                RecyclingPermission.TRACE_SENSITIVE_VIEW));
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.INVOICER.getCode(),
                RecyclingPermission.TRACE_EXPORT));
        // 平台运营不参与租户内的关联单据查询
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.TRACE_QUERY));
    }

    @Test
    public void testReportPermissions() {
        // 经营报表面向管理与财务：管理员 / 财务可看
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.ADMIN.getCode(),
                RecyclingPermission.REPORT_QUERY));
        assertTrue(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.FINANCE.getCode(),
                RecyclingPermission.REPORT_QUERY));
        // 报表只读，平台运营不参与租户内的经营数据
        assertFalse(RecyclingRoleEnum.roleHasPermission(RecyclingRoleEnum.PLATFORM_OPERATOR.getCode(),
                RecyclingPermission.REPORT_QUERY));
    }

    @Test
    public void testOfCode() {
        assertEquals(RecyclingRoleEnum.ADMIN,
                RecyclingRoleEnum.ofCode(RecyclingRoleEnum.ADMIN.getCode()).orElseThrow());
        assertTrue(RecyclingRoleEnum.ofCode("nobody").isEmpty());
    }
}
