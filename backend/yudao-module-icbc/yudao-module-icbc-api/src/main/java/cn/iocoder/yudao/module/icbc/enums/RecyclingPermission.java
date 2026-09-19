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

    // ========== 出售者建档（实名 / 入驻 / 协议 / 授权） ==========

    String SELLER_ONBOARDING_EXECUTE = "icbc:seller-onboarding:execute";
    String SELLER_AGREEMENT_MANAGE = "icbc:seller-agreement:manage";
    String SELLER_AUTHORIZATION_MANAGE = "icbc:seller-authorization:manage";

    // ========== 收购登记（#7） ==========

    String ACQUISITION_CREATE = "icbc:acquisition:create";
    String ACQUISITION_UPDATE = "icbc:acquisition:update";
    String ACQUISITION_QUERY = "icbc:acquisition:query";
    String ACQUISITION_EXPORT = "icbc:acquisition:export";

    // ========== 回收企业自身档案（付方） ==========

    String PAYER_CREATE = "icbc:payer-info:create";
    String PAYER_UPDATE = "icbc:payer-info:update";
    String PAYER_DELETE = "icbc:payer-info:delete";
    String PAYER_QUERY = "icbc:payer-info:query";

    // ========== 开票 ==========

    String INVOICE_CREATE = "icbc:invoice-order:create";
    String INVOICE_QUERY = "icbc:invoice-order:query";
    String INVOICE_CANCEL = "icbc:invoice-order:cancel";

    // ========== 红冲（#14：红字冲销与撤销） ==========

    String RED_INVOICE_APPLY = "icbc:red-invoice:apply";
    String RED_INVOICE_REVOKE = "icbc:red-invoice:revoke";
    String RED_INVOICE_QUERY = "icbc:red-invoice:query";

    // ========== 开票申请（#8：预下单与自然人确认） ==========

    String INVOICE_APPLICATION_APPLY = "icbc:invoice-application:apply";
    String INVOICE_APPLICATION_QUERY = "icbc:invoice-application:query";

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

    // ========== 公开令牌（自然人免登录端点） ==========

    String PUBLIC_TOKEN_CREATE = "icbc:public-token:create";

    // ========== 工行调用日志（平台运营） ==========

    String API_LOG_QUERY = "icbc:api-log:query";

    // ========== 通知监控与重放（#15：平台运营） ==========

    /** 查看九类通知的处理结果、时间与关联业务（跨租户） */
    String PLATFORM_CALLBACK_QUERY = "icbc:platform:callback:query";
    /** 手动重放处理失败的通知（跨租户） */
    String PLATFORM_CALLBACK_RETRY = "icbc:platform:callback:retry";

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
    /** 全平台五流齐备率与异常票清单（#15） */
    String PLATFORM_EVIDENCE_QUERY = "icbc:platform:evidence:query";

    // ========== 计费计量（#16：按成功开具的收购发票张数计费） ==========

    /** 查看各租户按期间的成功开票张数与应计费用（跨租户） */
    // ========== 自然人主体（#31：平台级身份层，跨租户） ==========

    String PLATFORM_NATURAL_PERSON_QUERY = "icbc:platform:natural-person:query";
    String PLATFORM_NATURAL_PERSON_MANAGE = "icbc:platform:natural-person:manage";

    String PLATFORM_BILLING_QUERY = "icbc:platform:billing:query";
    /** 重新计量并落台账（跨租户） */
    String PLATFORM_BILLING_MANAGE = "icbc:platform:billing:manage";

    // ========== 平台运营：报废产品税收分类编码表 ==========

    String SCRAP_CODE_CREATE = "icbc:scrap-code:create";
    String SCRAP_CODE_UPDATE = "icbc:scrap-code:update";
    String SCRAP_CODE_DELETE = "icbc:scrap-code:delete";
    String SCRAP_CODE_QUERY = "icbc:scrap-code:query";

    // ========== 租户：资质到期预警 ==========

    String EXPIRY_WARNING_QUERY = "icbc:expiry-warning:query";
    String EXPIRY_WARNING_ACK = "icbc:expiry-warning:ack";

    // ========== 额度风控（#12：滚动额度台账与经营主体登记引导） ==========

    String QUOTA_QUERY = "icbc:quota:query";
    String QUOTA_GUIDANCE_HANDLE = "icbc:quota:guidance:handle";

    // ========== 代办税费申报（#13：按月申报、补缴、汇算清缴） ==========

    /** 查看申报清单、缺项预警、补缴记录与汇算清缴对账 */
    String TAX_DECLARATION_QUERY = "icbc:tax-declaration:query";
    /** 生成申报清单、报送报告表、缴款归档、登记补缴 */
    String TAX_DECLARATION_MANAGE = "icbc:tax-declaration:manage";
    /** 缴清补缴 */
    String TAX_SUPPLEMENT_MANAGE = "icbc:tax-supplement:manage";
    /** 查看汇算清缴提醒与出售者对账单 */
    String SETTLEMENT_QUERY = "icbc:settlement:query";
    /** 生成 / 标记汇算清缴提醒 */
    String SETTLEMENT_REMIND = "icbc:settlement:remind";

    // ========== 结算单与确认门禁（#33：一次到场批次一次确认） ==========

    /** 查看结算单、确认记录与版本快照 */
    String SETTLEMENT_CONFIRM_QUERY = "icbc:settlement-confirm:query";
    /** 生成结算单、处理异议（改 / 不改但附说明）、线下签字、作废收购单 */
    String SETTLEMENT_CONFIRM_MANAGE = "icbc:settlement-confirm:manage";

    // ========== 场站与场站二维码（#34） ==========

    /** 查看场站与场站码 */
    String STATION_QUERY = "icbc:station:query";
    /** 维护场站与场站码 */
    String STATION_MANAGE = "icbc:station:manage";

}
