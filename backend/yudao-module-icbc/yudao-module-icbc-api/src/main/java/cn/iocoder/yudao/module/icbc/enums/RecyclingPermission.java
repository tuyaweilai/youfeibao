package cn.iocoder.yudao.module.icbc.enums;

/**
 * 回收企业反向开票域的权限标识。
 *
 * <p>权限标识是本平台自己的语言，不直接等于工行或税务侧的接口名。它们既用于
 * Controller 上的 {@code @PreAuthorize}，也用于 {@link RecyclingRoleEnum} 里
 * 「哪个租户内角色能做什么」的映射。角色与权限的对应关系只在本模块内维护，
 * 不依赖数据库里的菜单数据。
 */
public interface RecyclingPermission {

    // ========== 出售者档案 / 收购登记 ==========

    String PAYEE_CREATE = "icbc:payee-info:create";
    String PAYEE_UPDATE = "icbc:payee-info:update";
    String PAYEE_DELETE = "icbc:payee-info:delete";
    String PAYEE_QUERY = "icbc:payee-info:query";
    String PAYEE_EXPORT = "icbc:payee-info:export";

    // ========== 回收企业自身档案（付方） ==========

    String PAYER_CREATE = "icbc:payer-info:create";
    String PAYER_UPDATE = "icbc:payer-info:update";
    String PAYER_DELETE = "icbc:payer-info:delete";
    String PAYER_QUERY = "icbc:payer-info:query";

    // ========== 开票 ==========

    String INVOICE_CREATE = "icbc:invoice-order:create";
    String INVOICE_QUERY = "icbc:invoice-order:query";

    // ========== 付款 ==========

    String PAYMENT_CREATE = "icbc:payment:create";
    String PAYMENT_QUERY = "icbc:payment:query";

    // ========== 发票下载 / 证据 ==========

    String DOWNLOAD_EXECUTE = "icbc:invoice-download:download";
    String DOWNLOAD_QUERY = "icbc:invoice-download:query";
    String DOWNLOAD_RETRY = "icbc:invoice-download:retry";
    String DOWNLOAD_FILE = "icbc:invoice-download:download-file";

    // ========== 一票一档证据链 ==========

    String EVIDENCE_QUERY = "icbc:evidence:query";
    String EVIDENCE_ATTACH = "icbc:evidence:attach";
    String EVIDENCE_DELETE = "icbc:evidence:delete";
    String EVIDENCE_EXPORT = "icbc:evidence:export";

    // ========== 工行调用日志与异步通知（平台运营） ==========

    String API_LOG_QUERY = "icbc:api-log:query";
    String CALLBACK_QUERY = "icbc:callback:query";
    String CALLBACK_RETRY = "icbc:callback:retry";

    // ========== 连通性自检 ==========

    String TEST_QUERY = "icbc:test:query";

    // ========== 租户初始化 ==========

    String TENANT_ROLE_INIT = "icbc:tenant:role:init";

    // ========== 租户开票就绪：资质 ==========

    String QUALIFICATION_CREATE = "icbc:qualification:create";
    String QUALIFICATION_UPDATE = "icbc:qualification:update";
    String QUALIFICATION_DELETE = "icbc:qualification:delete";
    String QUALIFICATION_QUERY = "icbc:qualification:query";

    // ========== 租户开票就绪：编码配置 ==========

    String GOODS_CONFIG_CREATE = "icbc:goods-config:create";
    String GOODS_CONFIG_UPDATE = "icbc:goods-config:update";
    String GOODS_CONFIG_DELETE = "icbc:goods-config:delete";
    String GOODS_CONFIG_QUERY = "icbc:goods-config:query";

    // ========== 租户开票就绪：企业授权 ==========

    String ENTERPRISE_AUTH_INIT = "icbc:enterprise-auth:init";
    String ENTERPRISE_AUTH_UPDATE = "icbc:enterprise-auth:update";
    String ENTERPRISE_AUTH_QUERY = "icbc:enterprise-auth:query";

    // ========== 平台运营跨租户 ==========

    String PLATFORM_INVOICE_QUERY = "icbc:platform:invoice:query";
    String PLATFORM_QUALIFICATION_QUERY = "icbc:platform:qualification:query";
    String PLATFORM_QUALIFICATION_AUDIT = "icbc:platform:qualification:audit";

    // ========== 平台运营：报废产品税收分类编码表 ==========

    String SCRAP_CODE_CREATE = "icbc:scrap-code:create";
    String SCRAP_CODE_UPDATE = "icbc:scrap-code:update";
    String SCRAP_CODE_DELETE = "icbc:scrap-code:delete";
    String SCRAP_CODE_QUERY = "icbc:scrap-code:query";

    // ========== 租户：资质到期预警 ==========

    String EXPIRY_WARNING_QUERY = "icbc:expiry-warning:query";
    String EXPIRY_WARNING_ACK = "icbc:expiry-warning:ack";

}
