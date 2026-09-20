package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 回收企业租户内的角色，以及平台运营角色。
 *
 * <p>四个租户内角色（管理员 / 开票员 / 收货员 / 财务）都属于回收企业这一租户，
 * 只能在本租户内做事；平台运营是跨租户角色，只存在于平台自己的租户里。
 *
 * <p><b>本枚举是权限归属的唯一来源</b>：{@code icbc} 侧 Controller 的
 * {@code @ss.hasPermission} 运行时走 yudao 原生的菜单权限判断，而菜单行（{@code system_menu}）
 * 与角色-菜单关联（{@code system_role_menu}）由 {@code RecyclingPermissionSyncService}
 * 依据本枚举幂等生成。新增权限时只需在 {@code RecyclingPermission} 登记、把它挂到这里的某个角色，
 * 再在 Controller 上写 {@code @ss.hasPermission}；不用另行维护菜单种子。
 *
 * <p>角色的 {@code code} 是 {@code system_role.code}。管理员直接复用 yudao 建租户时自动生成的
 * {@code tenant_admin}，这样新租户开出来就天然是管理员，不必额外造角色。
 */
public enum RecyclingRoleEnum {

    /**
     * 管理员：回收企业负责人，本租户内全部操作的最终责任人。
     */
    ADMIN("tenant_admin", "管理员", Set.of(
            RecyclingPermission.PAYEE_CREATE, RecyclingPermission.PAYEE_UPDATE,
            RecyclingPermission.PAYEE_DELETE, RecyclingPermission.PAYEE_QUERY,
            RecyclingPermission.PAYEE_EXPORT,
            RecyclingPermission.SELLER_ONBOARDING_EXECUTE, RecyclingPermission.SELLER_AGREEMENT_MANAGE,
            RecyclingPermission.SELLER_AUTHORIZATION_MANAGE,
            RecyclingPermission.ACQUISITION_CREATE, RecyclingPermission.ACQUISITION_UPDATE,
            RecyclingPermission.ACQUISITION_QUERY, RecyclingPermission.ACQUISITION_EXPORT,
            RecyclingPermission.PAYER_CREATE, RecyclingPermission.PAYER_UPDATE,
            RecyclingPermission.PAYER_DELETE, RecyclingPermission.PAYER_QUERY,
            RecyclingPermission.INVOICE_CREATE, RecyclingPermission.INVOICE_QUERY,
            RecyclingPermission.INVOICE_CANCEL,
            RecyclingPermission.RED_INVOICE_APPLY, RecyclingPermission.RED_INVOICE_REVOKE,
            RecyclingPermission.RED_INVOICE_QUERY,
            RecyclingPermission.INVOICE_APPLICATION_APPLY, RecyclingPermission.INVOICE_APPLICATION_QUERY,
            RecyclingPermission.PAYMENT_CREATE, RecyclingPermission.PAYMENT_QUERY,
            RecyclingPermission.DOWNLOAD_EXECUTE, RecyclingPermission.DOWNLOAD_QUERY,
            RecyclingPermission.DOWNLOAD_RETRY, RecyclingPermission.DOWNLOAD_FILE,
            RecyclingPermission.EVIDENCE_QUERY, RecyclingPermission.EVIDENCE_ATTACH,
            RecyclingPermission.EVIDENCE_DELETE, RecyclingPermission.EVIDENCE_EXPORT,
            RecyclingPermission.PUBLIC_TOKEN_CREATE,
            RecyclingPermission.API_LOG_QUERY,
            RecyclingPermission.TEST_QUERY, RecyclingPermission.TENANT_ROLE_INIT,
            RecyclingPermission.QUALIFICATION_CREATE, RecyclingPermission.QUALIFICATION_UPDATE,
            RecyclingPermission.QUALIFICATION_DELETE, RecyclingPermission.QUALIFICATION_QUERY,
            RecyclingPermission.GOODS_CONFIG_CREATE, RecyclingPermission.GOODS_CONFIG_UPDATE,
            RecyclingPermission.GOODS_CONFIG_DELETE, RecyclingPermission.GOODS_CONFIG_QUERY,
            RecyclingPermission.ENTERPRISE_AUTH_INIT, RecyclingPermission.ENTERPRISE_AUTH_UPDATE,
            RecyclingPermission.ENTERPRISE_AUTH_QUERY,
            RecyclingPermission.SCRAP_CODE_QUERY,
            RecyclingPermission.EXPIRY_WARNING_QUERY,
            RecyclingPermission.EXPIRY_WARNING_ACK,
            RecyclingPermission.QUOTA_QUERY, RecyclingPermission.QUOTA_GUIDANCE_HANDLE,
            RecyclingPermission.TAX_DECLARATION_QUERY, RecyclingPermission.TAX_DECLARATION_MANAGE,
            RecyclingPermission.TAX_SUPPLEMENT_MANAGE,
            RecyclingPermission.SETTLEMENT_QUERY, RecyclingPermission.SETTLEMENT_REMIND,
            RecyclingPermission.SETTLEMENT_CONFIRM_QUERY, RecyclingPermission.SETTLEMENT_CONFIRM_MANAGE,
            RecyclingPermission.STATION_QUERY, RecyclingPermission.STATION_MANAGE,
            RecyclingPermission.APPOINTMENT_QUERY, RecyclingPermission.APPOINTMENT_MANAGE,
            RecyclingPermission.SELLER_NOTIFY_QUERY, RecyclingPermission.SELLER_NOTIFY_MANAGE,
            RecyclingPermission.WORKBENCH_QUERY)),

    /**
     * 收货员：收购现场登记，维护出售者档案与收购单。
     */
    RECEIVER("recycling_receiver", "收货员", Set.of(
            RecyclingPermission.PAYEE_CREATE, RecyclingPermission.PAYEE_UPDATE,
            RecyclingPermission.PAYEE_DELETE, RecyclingPermission.PAYEE_QUERY,
            RecyclingPermission.PAYEE_EXPORT,
            RecyclingPermission.SELLER_ONBOARDING_EXECUTE, RecyclingPermission.SELLER_AGREEMENT_MANAGE,
            RecyclingPermission.SELLER_AUTHORIZATION_MANAGE,
            RecyclingPermission.ACQUISITION_CREATE, RecyclingPermission.ACQUISITION_UPDATE,
            RecyclingPermission.ACQUISITION_QUERY, RecyclingPermission.ACQUISITION_EXPORT,
            RecyclingPermission.EVIDENCE_QUERY, RecyclingPermission.EVIDENCE_ATTACH,
            RecyclingPermission.GOODS_CONFIG_QUERY,
            RecyclingPermission.QUOTA_QUERY, RecyclingPermission.QUOTA_GUIDANCE_HANDLE,
            RecyclingPermission.PUBLIC_TOKEN_CREATE,
            RecyclingPermission.STATION_QUERY,
            RecyclingPermission.SETTLEMENT_CONFIRM_QUERY, RecyclingPermission.SETTLEMENT_CONFIRM_MANAGE,
            RecyclingPermission.SELLER_NOTIFY_QUERY, RecyclingPermission.SELLER_NOTIFY_MANAGE,
            RecyclingPermission.APPOINTMENT_QUERY, RecyclingPermission.APPOINTMENT_MANAGE,
            RecyclingPermission.WORKBENCH_QUERY)),

    /**
     * 开票员：发起反向开票与付款，下载发票原件。
     */
    INVOICER("recycling_invoicer", "开票员", Set.of(
            RecyclingPermission.PAYER_QUERY,
            RecyclingPermission.INVOICE_CREATE, RecyclingPermission.INVOICE_QUERY,
            RecyclingPermission.INVOICE_CANCEL,
            RecyclingPermission.RED_INVOICE_APPLY, RecyclingPermission.RED_INVOICE_REVOKE,
            RecyclingPermission.RED_INVOICE_QUERY,
            RecyclingPermission.INVOICE_APPLICATION_APPLY, RecyclingPermission.INVOICE_APPLICATION_QUERY,
            RecyclingPermission.ACQUISITION_QUERY,
            RecyclingPermission.PAYMENT_CREATE, RecyclingPermission.PAYMENT_QUERY,
            RecyclingPermission.DOWNLOAD_EXECUTE, RecyclingPermission.DOWNLOAD_QUERY,
            RecyclingPermission.DOWNLOAD_RETRY, RecyclingPermission.DOWNLOAD_FILE,
            RecyclingPermission.EVIDENCE_QUERY, RecyclingPermission.EVIDENCE_ATTACH,
            RecyclingPermission.PUBLIC_TOKEN_CREATE,
            RecyclingPermission.GOODS_CONFIG_QUERY,
            RecyclingPermission.SCRAP_CODE_QUERY,
            RecyclingPermission.QUOTA_QUERY,
            RecyclingPermission.TAX_DECLARATION_QUERY,
            RecyclingPermission.SETTLEMENT_QUERY,
            RecyclingPermission.SETTLEMENT_CONFIRM_QUERY,
            RecyclingPermission.WORKBENCH_QUERY)),

    /**
     * 财务：代办税费、对账、归集发票。
     */
    FINANCE("recycling_finance", "财务", Set.of(
            RecyclingPermission.INVOICE_QUERY,
            RecyclingPermission.RED_INVOICE_QUERY,
            RecyclingPermission.INVOICE_APPLICATION_QUERY,
            RecyclingPermission.ACQUISITION_QUERY, RecyclingPermission.ACQUISITION_EXPORT,
            RecyclingPermission.PAYMENT_QUERY,
            RecyclingPermission.DOWNLOAD_QUERY, RecyclingPermission.DOWNLOAD_FILE,
            RecyclingPermission.EVIDENCE_QUERY, RecyclingPermission.EVIDENCE_EXPORT,
            RecyclingPermission.QUOTA_QUERY,
            RecyclingPermission.TAX_DECLARATION_QUERY, RecyclingPermission.TAX_DECLARATION_MANAGE,
            RecyclingPermission.TAX_SUPPLEMENT_MANAGE,
            RecyclingPermission.SETTLEMENT_QUERY, RecyclingPermission.SETTLEMENT_REMIND,
            RecyclingPermission.SETTLEMENT_CONFIRM_QUERY,
            RecyclingPermission.SELLER_NOTIFY_QUERY,
            RecyclingPermission.PUBLIC_TOKEN_CREATE,
            RecyclingPermission.WORKBENCH_QUERY)),

    /**
     * 平台运营：平台方角色，可跨租户查看工行日志、通知与全平台发票。
     */
    PLATFORM_OPERATOR("recycling_platform_operator", "平台运营", Set.of(
            RecyclingPermission.API_LOG_QUERY,
            RecyclingPermission.PLATFORM_CALLBACK_QUERY, RecyclingPermission.PLATFORM_CALLBACK_RETRY,
            RecyclingPermission.PLATFORM_INVOICE_QUERY, RecyclingPermission.PLATFORM_EVIDENCE_QUERY,
            RecyclingPermission.PLATFORM_BILLING_QUERY, RecyclingPermission.PLATFORM_BILLING_MANAGE,
            RecyclingPermission.RED_INVOICE_QUERY,
            RecyclingPermission.PLATFORM_QUALIFICATION_QUERY, RecyclingPermission.PLATFORM_QUALIFICATION_AUDIT,
            RecyclingPermission.QUALIFICATION_QUERY, RecyclingPermission.QUALIFICATION_UPDATE,
            RecyclingPermission.GOODS_CONFIG_CREATE, RecyclingPermission.GOODS_CONFIG_UPDATE,
            RecyclingPermission.GOODS_CONFIG_DELETE, RecyclingPermission.GOODS_CONFIG_QUERY,
            RecyclingPermission.SCRAP_CODE_CREATE, RecyclingPermission.SCRAP_CODE_UPDATE,
            RecyclingPermission.SCRAP_CODE_DELETE, RecyclingPermission.SCRAP_CODE_QUERY,
            RecyclingPermission.QUOTA_QUERY,
            RecyclingPermission.TAX_DECLARATION_QUERY,
            RecyclingPermission.SETTLEMENT_QUERY,
            RecyclingPermission.SETTLEMENT_CONFIRM_QUERY,
            RecyclingPermission.PLATFORM_NATURAL_PERSON_QUERY,
            RecyclingPermission.PLATFORM_NATURAL_PERSON_MANAGE));

    private final String code;
    private final String name;
    private final Set<String> permissions;

    RecyclingRoleEnum(String code, String name, Set<String> permissions) {
        this.code = code;
        this.name = name;
        this.permissions = permissions;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    /**
     * 是否跨租户角色。只有平台运营（以及 yudao 超管）可以越过租户边界。
     */
    public boolean isCrossTenant() {
        return this == PLATFORM_OPERATOR;
    }

    public static Optional<RecyclingRoleEnum> ofCode(String code) {
        return Arrays.stream(values())
                .filter(role -> role.code.equals(code))
                .findFirst();
    }

    /**
     * 某角色是否拥有某权限。
     */
    public static boolean roleHasPermission(String roleCode, String permission) {
        return ofCode(roleCode)
                .map(role -> role.permissions.contains(permission))
                .orElse(false);
    }

    /**
     * 本平台登记的全部权限标识。
     */
    public static Set<String> allPermissions() {
        Set<String> permissions = new LinkedHashSet<>();
        for (RecyclingRoleEnum role : values()) {
            permissions.addAll(role.permissions);
        }
        return permissions;
    }

}
