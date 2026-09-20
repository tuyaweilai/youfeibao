package cn.iocoder.yudao.module.icbc.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 工商银行反向开票模块错误码枚举类
 * 
 * 工行模块，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 收方信息管理 1-030-001-000 ==========
    ErrorCode PAYEE_NOT_EXISTS = new ErrorCode(1_030_001_000, "收方信息不存在");
    ErrorCode PAYEE_ID_CARD_EXISTS = new ErrorCode(1_030_001_001, "身份证号码已存在");
    ErrorCode PAYEE_MOBILE_EXISTS = new ErrorCode(1_030_001_002, "手机号码已存在");
    ErrorCode PAYEE_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_030_001_003, "收方状态不允许修改");
    ErrorCode PAYEE_AUDIT_FAILED = new ErrorCode(1_030_001_004, "收方审核失败");

    // ========== 付方信息管理 1-030-002-000 ==========
    ErrorCode PAYER_NOT_EXISTS = new ErrorCode(1_030_002_000, "付方信息不存在");
    ErrorCode PAYER_TAX_NO_EXISTS = new ErrorCode(1_030_002_001, "纳税人识别号已存在");
    ErrorCode PAYER_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_030_002_002, "付方状态不允许修改");
    ErrorCode PAYER_AUDIT_FAILED = new ErrorCode(1_030_002_003, "付方审核失败");

    // ========== 开票订单管理 1-030-003-000 ==========
    ErrorCode INVOICE_ORDER_NOT_EXISTS = new ErrorCode(1_030_003_000, "开票订单不存在");
    ErrorCode INVOICE_ORDER_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_030_003_001, "订单状态不允许修改");
    ErrorCode INVOICE_ORDER_AMOUNT_ERROR = new ErrorCode(1_030_003_002, "订单金额错误");
    ErrorCode INVOICE_ORDER_ITEMS_EMPTY = new ErrorCode(1_030_003_003, "订单商品明细不能为空");
    ErrorCode INVOICE_ORDER_PAYEE_NOT_APPROVED = new ErrorCode(1_030_003_004, "收方信息未审核通过");
    ErrorCode INVOICE_ORDER_PAYER_NOT_APPROVED = new ErrorCode(1_030_003_005, "付方信息未审核通过");

    // ========== 支付管理 1-030-004-000 ==========
    ErrorCode PAYMENT_ORDER_NOT_EXISTS = new ErrorCode(1_030_004_000, "支付订单不存在");
    ErrorCode PAYMENT_STATUS_NOT_ALLOW_PAY = new ErrorCode(1_030_004_001, "订单状态不允许支付");
    ErrorCode PAYMENT_AMOUNT_ERROR = new ErrorCode(1_030_004_002, "支付金额错误");
    ErrorCode PAYMENT_FAILED = new ErrorCode(1_030_004_003, "支付失败");
    ErrorCode PAYMENT_ORDER_EXISTS = new ErrorCode(1_030_004_004, "支付订单已存在");
    ErrorCode PAYMENT_PARAM_ERROR = new ErrorCode(1_030_004_005, "支付参数错误");
    ErrorCode PAYMENT_PRE_INVOICE_NOT_SUCCESS = new ErrorCode(1_030_004_006, "该笔收购尚未预开票成功，不能发起付款");
    ErrorCode PAYMENT_AMOUNT_MISMATCH = new ErrorCode(1_030_004_007, "付款金额与收购单金额不一致：应付 {}，实付 {}");
    ErrorCode PAYMENT_ACQUISITION_NOT_LINKED = new ErrorCode(1_030_004_008, "该笔开票申请未挂回收购单，无法付款");
    ErrorCode PAYMENT_RECEIPT_NOT_AVAILABLE = new ErrorCode(1_030_004_009, "支付尚未成功，暂无转账回单");
    ErrorCode PAYMENT_RESULT_UNKNOWN = new ErrorCode(1_030_004_010, "工行返回结果未知，请勿重复提交，稍后查询支付状态");
    ErrorCode PAYMENT_INVOICE_ORDER_NOT_EXISTS = new ErrorCode(1_030_004_011, "按合作方订单号未找到开票申请，不能付款");

    // ========== 发票管理 1-030-005-000 ==========
    ErrorCode INVOICE_NOT_EXISTS = new ErrorCode(1_030_005_000, "发票不存在");
    ErrorCode INVOICE_STATUS_NOT_ALLOW_DOWNLOAD = new ErrorCode(1_030_005_001, "发票状态不允许下载");
    ErrorCode INVOICE_RED_AMOUNT_ERROR = new ErrorCode(1_030_005_003, "红冲金额错误");
    ErrorCode INVOICE_ALREADY_RED = new ErrorCode(1_030_005_004, "发票已红冲");

    // ========== 工行接口调用 1-030-006-000 ==========
    ErrorCode ICBC_API_CALL_FAILED = new ErrorCode(1_030_006_000, "工行接口调用失败");
    ErrorCode ICBC_API_SIGN_ERROR = new ErrorCode(1_030_006_001, "工行接口签名错误");
    ErrorCode ICBC_API_ENCRYPT_ERROR = new ErrorCode(1_030_006_002, "工行接口加密错误");
    ErrorCode ICBC_API_RESPONSE_ERROR = new ErrorCode(1_030_006_003, "工行接口响应异常");
    ErrorCode ICBC_API_TIMEOUT = new ErrorCode(1_030_006_004, "工行接口调用超时");

    // ========== 回调处理 1-030-007-000 ==========
    ErrorCode CALLBACK_SIGN_VERIFY_FAILED = new ErrorCode(1_030_007_000, "回调签名验证失败");
    ErrorCode CALLBACK_DATA_FORMAT_ERROR = new ErrorCode(1_030_007_001, "回调数据格式错误");
    ErrorCode CALLBACK_BUSINESS_NOT_EXISTS = new ErrorCode(1_030_007_002, "回调业务数据不存在");
    ErrorCode CALLBACK_PROCESS_FAILED = new ErrorCode(1_030_007_003, "回调处理失败");

    // ========== 系统配置 1-030-008-000 ==========
    ErrorCode CONFIG_NOT_EXISTS = new ErrorCode(1_030_008_000, "系统配置不存在");
    ErrorCode CONFIG_KEY_EXISTS = new ErrorCode(1_030_008_001, "配置键已存在");
    ErrorCode CONFIG_DECRYPT_FAILED = new ErrorCode(1_030_008_002, "配置解密失败");

    // ========== 收方信息 1-018-000-100 ==========
    ErrorCode PAYEE_INFO_NOT_EXISTS = new ErrorCode(1_018_000_100, "收方信息不存在");
    ErrorCode PAYEE_INFO_ID_CARD_EXISTS = new ErrorCode(1_018_000_101, "身份证号已存在");
    ErrorCode PAYEE_INFO_MOBILE_EXISTS = new ErrorCode(1_018_000_102, "手机号已存在");
    
    // ========== 付方信息 1-018-000-200 ==========
    ErrorCode PAYER_INFO_NOT_EXISTS = new ErrorCode(1_018_000_200, "付方信息不存在");
    ErrorCode PAYER_INFO_CREDIT_CODE_EXISTS = new ErrorCode(1_018_000_201, "统一社会信用代码已存在");
    ErrorCode PAYER_INFO_TAX_NO_EXISTS = new ErrorCode(1_018_000_202, "纳税人识别号已存在");

    // ========== 发票下载 1-030-009-000 ==========
    ErrorCode INVOICE_DOWNLOAD_NOT_FOUND = new ErrorCode(1_030_009_000, "发票下载记录不存在");
    ErrorCode INVOICE_DOWNLOAD_FAILED = new ErrorCode(1_030_009_001, "发票下载失败");
    ErrorCode INVOICE_DOWNLOAD_MAX_RETRY = new ErrorCode(1_030_009_002, "发票下载重试次数已达上限");
    ErrorCode INVOICE_FILE_NOT_FOUND = new ErrorCode(1_030_009_003, "发票文件记录不存在");
    ErrorCode INVOICE_FILE_NOT_EXISTS = new ErrorCode(1_030_009_004, "发票文件不存在");
    ErrorCode INVOICE_FILE_DOWNLOAD_FAILED = new ErrorCode(1_030_009_005, "发票文件下载失败");

    // ========== 工行发票下载模块 1-004-015-000 ==========
    ErrorCode INVOICE_DOWNLOAD_NOT_EXISTS = new ErrorCode(1_004_015_001, "发票下载记录不存在");
    ErrorCode INVOICE_DOWNLOAD_STATUS_ERROR = new ErrorCode(1_004_015_002, "发票下载状态错误");
    ErrorCode INVOICE_DOWNLOAD_URL_INVALID = new ErrorCode(1_004_015_003, "发票下载链接无效");

    // ========== 工行接口日志模块 1-004-016-000 ==========
    ErrorCode API_LOG_NOT_EXISTS = new ErrorCode(1_004_016_001, "接口调用日志不存在");
    ErrorCode API_LOG_MSG_ID_DUPLICATE = new ErrorCode(1_004_016_002, "消息ID重复");

    // ========== 工行回调通知模块 1-004-017-000 ==========
    ErrorCode CALLBACK_NOTIFY_NOT_EXISTS = new ErrorCode(1_004_017_001, "回调通知不存在");
    ErrorCode CALLBACK_NOTIFY_ALREADY_PROCESSED = new ErrorCode(1_004_017_002, "回调通知已处理");
    ErrorCode CALLBACK_NOTIFY_SIGN_INVALID = new ErrorCode(1_004_017_003, "回调通知签名无效");
    ErrorCode CALLBACK_NOTIFY_TYPE_INVALID = new ErrorCode(1_004_017_004, "回调通知类型无效");
    ErrorCode CALLBACK_NOTIFY_PROCESS_FAILED = new ErrorCode(1_004_017_005, "回调通知处理失败");

    // ========== 租户开票就绪 1-030-010-000 ==========
    ErrorCode QUALIFICATION_NOT_EXISTS = new ErrorCode(1_030_010_000, "资质不存在");
    ErrorCode GOODS_CONFIG_NOT_EXISTS = new ErrorCode(1_030_010_001, "品类配置不存在");
    ErrorCode GOODS_CONFIG_NAME_EXISTS = new ErrorCode(1_030_010_002, "品类名称已存在");
    ErrorCode ENTERPRISE_AUTH_NOT_EXISTS = new ErrorCode(1_030_010_003, "企业授权记录不存在");
    ErrorCode TENANT_NOT_READY = new ErrorCode(1_030_010_004, "租户开票未就绪：三层资质不齐或已失效");
    ErrorCode GOODS_CONFIG_TAX_METHOD_INVALID = new ErrorCode(1_030_010_005, "计税方法不合法：只能是 SIMPLE 或 GENERAL");
    ErrorCode SIMPLE_TAX_METHOD_NO_SPECIAL_INVOICE = new ErrorCode(1_030_010_006, "简易计税不得开具增值税专用发票");
    ErrorCode SCRAP_CODE_NOT_EXISTS = new ErrorCode(1_030_010_007, "报废产品编码不存在");
    ErrorCode SCRAP_CODE_MERGED_CODE_EXISTS = new ErrorCode(1_030_010_008, "报废产品编码已存在");
    ErrorCode EXPIRY_WARNING_NOT_EXISTS = new ErrorCode(1_030_010_009, "到期预警记录不存在");

    // ========== 一票一档证据链 1-030-011-000 ==========
    ErrorCode EVIDENCE_NOT_EXISTS = new ErrorCode(1_030_011_000, "证据记录不存在");
    ErrorCode EVIDENCE_TYPE_INVALID = new ErrorCode(1_030_011_001, "证据类型不合法");
    ErrorCode EVIDENCE_PACKAGE_NO_INVOICE = new ErrorCode(1_030_011_002, "证据包请求不能为空");
    ErrorCode EVIDENCE_EXPORT_FAILED = new ErrorCode(1_030_011_003, "证据导出失败");

    // ========== 公开令牌端点 1-030-012-000 ==========
    ErrorCode PUBLIC_TOKEN_INVALID = new ErrorCode(1_030_012_000, "令牌无效");
    ErrorCode PUBLIC_TOKEN_EXPIRED = new ErrorCode(1_030_012_001, "令牌已过期");
    ErrorCode PUBLIC_TOKEN_USED_UP = new ErrorCode(1_030_012_002, "令牌已用尽");
    ErrorCode PUBLIC_TOKEN_PURPOSE_MISMATCH = new ErrorCode(1_030_012_003, "令牌用途不符");
    ErrorCode PUBLIC_TOKEN_SECRET_MISSING = new ErrorCode(1_030_012_004, "公开令牌签名密钥未配置");
    ErrorCode CONTACT_LEAD_NOT_EXISTS = new ErrorCode(1_030_012_005, "留联系方式记录不存在");
    ErrorCode PUBLIC_TOKEN_PURPOSE_INVALID = new ErrorCode(1_030_012_006, "令牌用途不合法");
    ErrorCode PUBLIC_TOKEN_BUSINESS_KEY_MISSING = new ErrorCode(1_030_012_007, "令牌缺少绑定的业务键");

    // ========== 出售者建档 1-030-013-000 ==========
    ErrorCode SELLER_REAL_NAME_NOT_PASSED = new ErrorCode(1_030_013_000, "出售者实人认证未通过，不能继续收方入驻");
    ErrorCode SELLER_ONBOARDING_NOT_READY = new ErrorCode(1_030_013_001, "出售者收方入驻未完成或未通过，不能用于开票");
    ErrorCode FRAMEWORK_AGREEMENT_NOT_EXISTS = new ErrorCode(1_030_013_002, "框架收购协议不存在");
    ErrorCode FRAMEWORK_AGREEMENT_REQUIRED = new ErrorCode(1_030_013_003, "出售者尚未签署生效的框架收购协议");
    ErrorCode SELLER_AUTHORIZATION_NOT_EXISTS = new ErrorCode(1_030_013_004, "出售者首次授权不存在");
    ErrorCode SELLER_AUTHORIZATION_INCOMPLETE = new ErrorCode(1_030_013_005, "出售者尚未完成反向开票与代办税费授权");
    ErrorCode SELLER_REAL_NAME_RESULT_UNKNOWN = new ErrorCode(1_030_013_006, "实人认证结果尚未返回");
    ErrorCode SELLER_BANK_CARD_REQUIRED = new ErrorCode(1_030_013_007, "出售者未绑定银行卡，不能发起收方入驻");
    ErrorCode SELLER_ONBOARDING_PAYER_NOT_CONFIGURED = new ErrorCode(1_030_013_008, "本租户尚未配置付方档案（开票主体），不能发起收方入驻");
    ErrorCode SELLER_ONBOARDING_VENDOR_UNRESOLVED = new ErrorCode(1_030_013_009, "收方入驻通知无法确定所属回收企业（子商户 {}），请人工核对");

    // ========== 收购登记 1-030-014-000 ==========
    ErrorCode ACQUISITION_NOT_EXISTS = new ErrorCode(1_030_014_000, "收购单不存在");
    ErrorCode ACQUISITION_REQUIRED_ELEMENT_MISSING = new ErrorCode(1_030_014_001, "收购登记缺少关键要件：{}");
    ErrorCode ACQUISITION_GOODS_CONFIG_REQUIRED = new ErrorCode(1_030_014_002, "收购登记未选择品类");
    ErrorCode ACQUISITION_GOODS_CONFIG_NOT_EXISTS = new ErrorCode(1_030_014_003, "品类配置不存在");
    ErrorCode ACQUISITION_WEIGHT_INVALID = new ErrorCode(1_030_014_004, "重量不合法：毛重不能小于皮重");
    ErrorCode ACQUISITION_AMOUNT_INVALID = new ErrorCode(1_030_014_005, "金额不合法：数量与单价必须大于 0");
    ErrorCode ACQUISITION_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_030_014_006, "收购单状态不允许修改");
    ErrorCode ACQUISITION_CONFIRMATION_EXPORT_FAILED = new ErrorCode(1_030_014_007, "收购确认书导出失败");
    ErrorCode ACQUISITION_SELLER_NOT_EXISTS = new ErrorCode(1_030_014_008, "出售者档案不存在，请先建档再登记收购");
    ErrorCode ACQUISITION_SETTLEMENT_WEIGHT_INVALID = new ErrorCode(1_030_014_009, "结算重量不合法：毛重 − 皮重 − 扣杂不能小于 0");
    ErrorCode ACQUISITION_ADJUSTMENT_REASON_REQUIRED = new ErrorCode(1_030_014_010, "调整项必须填写原因（运费 / 补贴 / 折让）");
    ErrorCode ACQUISITION_DEDUCTION_INVALID = new ErrorCode(1_030_014_011, "扣杂不合法：按重量不能为负，按比例须在 0~1 之间");

    // ========== 开票申请（#8） 1-030-015-000 ==========
    ErrorCode INVOICE_APPLICATION_PRECHECK_FAILED = new ErrorCode(1_030_015_000, "开票申请校验未通过");
    ErrorCode INVOICE_APPLICATION_ACQUISITION_STATUS_INVALID = new ErrorCode(1_030_015_001, "收购单当前状态不允许发起开票申请：{}");
    ErrorCode INVOICE_APPLICATION_PAYER_NOT_READY = new ErrorCode(1_030_015_002, "回收企业付方档案未就绪，请先在「付方档案」补齐纳税人识别号与付方编号");
    ErrorCode INVOICE_APPLICATION_PAYEE_NOT_READY = new ErrorCode(1_030_015_003, "出售者档案信息不全，请先在「出售者建档」补齐姓名、身份证、手机号与地址");
    ErrorCode INVOICE_APPLICATION_ELEMENT_MISSING = new ErrorCode(1_030_015_004, "收购单关键要件不全：{}");
    ErrorCode INVOICE_APPLICATION_GOODS_CODE_MISSING = new ErrorCode(1_030_015_005, "品类未配置商品和服务税收分类合并编码，请先在「编码配置」补齐");

    // ========== 开票 / 缴税 / 上传状态与缴税凭证（#10） 1-030-016-000 ==========
    ErrorCode INVOICE_TAX_CERTIFICATE_NOT_AVAILABLE = new ErrorCode(1_030_016_000, "缴税尚未成功，暂不能出具缴税凭证");
    ErrorCode INVOICE_TAX_CERTIFICATE_EXPORT_FAILED = new ErrorCode(1_030_016_001, "缴税凭证导出失败");

    // ========== 红冲与发票取消（#14） 1-030-017-000 ==========
    ErrorCode RED_INVOICE_NOT_EXISTS = new ErrorCode(1_030_017_000, "红冲记录不存在");
    ErrorCode RED_INVOICE_REASON_INVALID = new ErrorCode(1_030_017_001, "红冲原因不合法：只能是 01 开票有误 / 02 销货退回 / 03 服务中止 / 04 销售折让");
    ErrorCode RED_INVOICE_BLUE_NOT_ISSUED = new ErrorCode(1_030_017_002, "原蓝票尚未开具，不能发起红冲");
    ErrorCode RED_INVOICE_AMOUNT_MISMATCH = new ErrorCode(1_030_017_003, "开票有误必须全额红冲：红冲金额 {} 与蓝票金额 {} 不一致");
    ErrorCode RED_INVOICE_GOODS_MISMATCH = new ErrorCode(1_030_017_004, "开票有误必须全额红冲：红冲明细的单价、金额、数量必须与原蓝票一致");
    ErrorCode RED_INVOICE_ALREADY_EXISTS = new ErrorCode(1_030_017_005, "该蓝票已有生效中的红冲记录，不能重复发起");
    ErrorCode RED_INVOICE_NOT_REVOCABLE = new ErrorCode(1_030_017_006, "红字确认单当前状态不允许撤销：{}");
    ErrorCode RED_INVOICE_AMOUNT_REQUIRED = new ErrorCode(1_030_017_007, "红冲必须填写红冲金额");
    ErrorCode INVOICE_CANCEL_NOT_PRE_SUCCESS = new ErrorCode(1_030_017_008, "仅「预开票成功」的发票可以取消，当前状态不允许取消");
    ErrorCode INVOICE_CANCEL_PAID = new ErrorCode(1_030_017_009, "已支付的发票不能取消，请走红冲流程");
    ErrorCode INVOICE_CANCEL_RESULT_UNKNOWN = new ErrorCode(1_030_017_010, "工行取消结果未知，请勿重复提交，稍后查询发票状态");
    ErrorCode INVOICE_CANCEL_FAILED = new ErrorCode(1_030_017_012, "发票取消未成功：{}");
    ErrorCode RED_INVOICE_RESULT_UNKNOWN = new ErrorCode(1_030_017_011, "工行红冲结果未知，请勿重复提交，稍后查询红冲状态");

    // ========== 额度风控（#12：500 万滚动额度与经营主体登记引导） 1-030-018-000 ==========
    ErrorCode SELLER_QUOTA_EXCEEDED = new ErrorCode(1_030_018_000, "该出售者连续 12 个月反向开票累计销售额已达 {} 元，超过 500 万元上限，不能再发起开票申请");
    ErrorCode SELLER_QUOTA_EXCEEDED_BY_THIS_ONE = new ErrorCode(1_030_018_001, "本次 {} 元将超过 500 万元上限：已用 {} 元，余量 {} 元");
    ErrorCode SELLER_QUOTA_GUIDANCE_NOT_EXISTS = new ErrorCode(1_030_018_002, "额度引导记录不存在");
    ErrorCode SELLER_QUOTA_GUIDANCE_STATUS_INVALID = new ErrorCode(1_030_018_003, "额度引导状态不合法：{}");

    // ========== 代办税费申报（#13：按月申报、补缴与汇算清缴） 1-030-019-000 ==========
    ErrorCode TAX_DECLARATION_NOT_EXISTS = new ErrorCode(1_030_019_000, "{} 的代办税费申报单不存在，请先生成申报清单");
    ErrorCode TAX_DECLARATION_STATUS_INVALID = new ErrorCode(1_030_019_001, "{} 的申报单当前状态不允许{}：{}（{}）");
    ErrorCode TAX_DECLARATION_NOT_READY = new ErrorCode(1_030_019_002, "{} 的申报数据不齐，不能申报或缴款：{}");
    ErrorCode TAX_DECLARATION_PAID_IMMUTABLE = new ErrorCode(1_030_019_003, "{} 的申报单已缴款，不能重新生成；差额请走补缴");
    ErrorCode TAX_SUPPLEMENT_NOT_EXISTS = new ErrorCode(1_030_019_004, "补缴记录不存在");
    ErrorCode TAX_SUPPLEMENT_STATUS_INVALID = new ErrorCode(1_030_019_005, "补缴记录当前状态不允许{}：{}（{}）");
    ErrorCode SETTLEMENT_REMINDER_NOT_EXISTS = new ErrorCode(1_030_019_006, "汇算清缴提醒不存在");
    ErrorCode TAX_PERIOD_MONTH_INVALID = new ErrorCode(1_030_019_007, "申报月格式不正确，应为 yyyy-MM：{}");

    // ========== 平台计费计量（#16：按成功开具的收购发票张数计费） 1-030-020-000 ==========
    ErrorCode BILLING_PERIOD_MONTH_INVALID = new ErrorCode(1_030_020_000, "计费期间格式不正确，应为 yyyy-MM：{}");
    ErrorCode BILLING_TENANT_REQUIRED = new ErrorCode(1_030_020_001, "计费计量必须指定租户");

    // ========== 自然人主体（#31：平台级身份层与登录凭证） 1-030-021-000 ==========
    ErrorCode NATURAL_PERSON_NOT_EXISTS = new ErrorCode(1_030_021_000, "自然人主体不存在");
    ErrorCode NATURAL_PERSON_IDENTITY_TAKEN = new ErrorCode(1_030_021_001, "该身份已建档，请用原手机号登录或联系客服");
    ErrorCode NATURAL_PERSON_DISABLED = new ErrorCode(1_030_021_002, "该身份已停用，请联系客服");
    ErrorCode NATURAL_PERSON_NOT_BOUND_TO_LOGIN = new ErrorCode(1_030_021_003, "所选身份不在当前登录名下，不能代为操作");
    ErrorCode NATURAL_PERSON_ID_CARD_CHANGED = new ErrorCode(1_030_021_004, "收方档案的身份证件号码与已建档身份不一致，请先联系客服处理");
    ErrorCode NATURAL_PERSON_OUT_USER_ID_DUPLICATED = new ErrorCode(1_030_021_005, "平台级外部用户编号已存在：{}");
    ErrorCode SELLER_STATION_TENANT_REQUIRED = new ErrorCode(1_030_021_006, "缺少场站所属回收企业的租户标识，无法定位收方档案");
    ErrorCode NATURAL_PERSON_ID_CARD_REQUIRED = new ErrorCode(1_030_021_007, "收方档案缺少身份证件号码，无法建立或复用自然人身份档案");

    // ========== 结算单与确认门禁（#33） 1-030-022-000 ==========
    ErrorCode SETTLEMENT_NOT_EXISTS = new ErrorCode(1_030_022_000, "结算单不存在");
    ErrorCode SETTLEMENT_NO_ACQUISITION = new ErrorCode(1_030_022_001, "没有可生成结算单的收购单（可能都已归入结算单或已作废）");
    ErrorCode SETTLEMENT_ACQUISITION_ALREADY_GROUPED = new ErrorCode(1_030_022_002, "该收购单已归入结算单，生成后不得再往里加收购单");
    ErrorCode SETTLEMENT_NOT_CONFIRMED = new ErrorCode(1_030_022_003, "该笔收购所在结算单尚未经出售者确认，不能发起开票");
    ErrorCode SETTLEMENT_DISPUTE_REASON_INVALID = new ErrorCode(1_030_022_004, "异议原因不合法：{}");
    ErrorCode SETTLEMENT_DISPUTE_NOTE_REQUIRED = new ErrorCode(1_030_022_005, "选择「其他」异议原因时必须附说明");
    ErrorCode SETTLEMENT_STATUS_NOT_ALLOW = new ErrorCode(1_030_022_006, "结算单当前状态不允许该操作：{}");
    ErrorCode SETTLEMENT_INVOICED_NOT_EDITABLE = new ErrorCode(1_030_022_007, "结算单已开票，不能再改，只能红冲");
    ErrorCode SETTLEMENT_VERSION_NOT_EXISTS = new ErrorCode(1_030_022_008, "结算单版本不存在：{}");
    ErrorCode SETTLEMENT_CANCEL_REASON_REQUIRED = new ErrorCode(1_030_022_009, "作废收购单必须填写原因");
    ErrorCode SETTLEMENT_OFFLINE_SIGN_REQUIRED = new ErrorCode(1_030_022_010, "该结算单需线下签字确认，请上传带签字的纸质确认书并标注办理人");
    ErrorCode SETTLEMENT_ACQUISITION_NOT_IN_SETTLEMENT = new ErrorCode(1_030_022_011, "收购单 {} 不属于该结算单，不能在这里改动");
    ErrorCode SETTLEMENT_ACQUISITION_NOT_GROUPED = new ErrorCode(1_030_022_012, "收购单尚未归入结算单，不能在此作废");

    // ========== 场站与场站二维码（#34） 1-030-023-000 ==========
    ErrorCode STATION_NOT_EXISTS = new ErrorCode(1_030_023_000, "场站不存在");
    ErrorCode STATION_CODE_EXISTS = new ErrorCode(1_030_023_001, "场站码已存在，请换一个（二维码只编码场站码，租户内唯一）");
    ErrorCode STATION_CODE_REQUIRED = new ErrorCode(1_030_023_002, "场站码不能为空");
    ErrorCode STATION_PUBLIC_NOT_FOUND = new ErrorCode(1_030_023_003, "场站码无效或已停用，请让收货员确认二维码");

    // ========== 自然人端首页与记录（#34） 1-030-024-000 ==========
    ErrorCode SELLER_AUTHORIZATION_ALREADY_REVOKED = new ErrorCode(1_030_024_001, "该企业的授权已撤销");
    ErrorCode SELLER_RECORD_NOT_FOUND = new ErrorCode(1_030_024_002, "记录不存在或不属于当前身份");

    // ========== 预约到站（#35，ADR 0020：不是订单） 1-030-025-000 ==========
    ErrorCode APPOINTMENT_NOT_EXISTS = new ErrorCode(1_030_025_000, "预约不存在");
    ErrorCode APPOINTMENT_NOT_CANCELLABLE = new ErrorCode(1_030_025_001, "只能取消尚未到站的预约");
    ErrorCode APPOINTMENT_STATUS_NOT_ALLOW = new ErrorCode(1_030_025_002, "预约当前状态不允许该操作：{}");
    ErrorCode APPOINTMENT_EXPECTED_QUANTITY_INVALID = new ErrorCode(1_030_025_003, "预计数量不能为负");
    ErrorCode APPOINTMENT_ARRIVAL_TIME_REQUIRED = new ErrorCode(1_030_025_004, "请填写预计到站时间");

    // ========== 出售者触达（#36，ADR 0023：只靠短信与收货员转达） 1-030-026-000 ==========
    ErrorCode SELLER_NOTIFY_NOT_EXISTS = new ErrorCode(1_030_026_000, "触达记录不存在");
    ErrorCode SELLER_NOTIFY_LINK_UNAVAILABLE = new ErrorCode(1_030_026_001,
            "尚未配置自然人端入口地址（icbc.notify.seller-app-url），无法生成确认链接");

    // ========== 换银行卡（#37，ADR 0010：换卡重走收方入驻，审核期间付款挂起） 1-030-027-000 ==========
    ErrorCode PAYEE_BANK_CARD_CHANGE_IN_PROGRESS = new ErrorCode(1_030_027_000,
            "该出售者的收款账户正在银行审核中，新交易的付款已挂起；审核通过或取消变更后才能发起付款");
    ErrorCode PAYEE_BANK_CARD_CHANGE_NOT_EXISTS = new ErrorCode(1_030_027_001, "收款账户变更单不存在");
    ErrorCode PAYEE_BANK_CARD_CHANGE_ALREADY_PENDING = new ErrorCode(1_030_027_002,
            "该出售者已有一笔收款账户变更在银行审核中，不能同时再发起一笔");
    ErrorCode PAYEE_BANK_CARD_CHANGE_NOT_CANCELLABLE = new ErrorCode(1_030_027_003,
            "只有「银行审核中」的变更可以取消，当前状态：{}");
    ErrorCode PAYEE_BANK_CARD_CHANGE_NOT_ONBOARDED = new ErrorCode(1_030_027_004,
            "该出售者尚未完成首次收方入驻，请先完成建档再变更银行卡");

    // ========== 采购合同（#45 T07，ADR 0027） 1-030-028-000 ==========
    ErrorCode PURCHASE_CONTRACT_NOT_EXISTS = new ErrorCode(1_030_028_000, "采购合同不存在");
    ErrorCode PURCHASE_CONTRACT_STATUS_NOT_ALLOW = new ErrorCode(1_030_028_001,
            "采购合同当前状态不允许该操作：{}");
    ErrorCode PURCHASE_CONTRACT_COUNTERPARTY_REQUIRED = new ErrorCode(1_030_028_002,
            "请选择交易对方，且自然人出售者与单位供货方只能二选一");
    ErrorCode PURCHASE_CONTRACT_PAYEE_NOT_EXISTS = new ErrorCode(1_030_028_003,
            "出售者档案不存在或不属于本租户，不能作为合同对手方");
    ErrorCode PURCHASE_CONTRACT_SUPPLIER_NAME_REQUIRED = new ErrorCode(1_030_028_004,
            "单位供货方合同的对手方名称不能为空");
    ErrorCode PURCHASE_CONTRACT_DATE_INVALID = new ErrorCode(1_030_028_005,
            "采购合同的有效期止不能早于有效期起");
    ErrorCode PURCHASE_CONTRACT_CATEGORY_REQUIRED = new ErrorCode(1_030_028_006,
            "采购合同至少需要一个适用品类");
    ErrorCode PURCHASE_CONTRACT_CATEGORY_NOT_EXISTS = new ErrorCode(1_030_028_007,
            "适用品类不存在或不属于本租户：{}");
    ErrorCode PURCHASE_CONTRACT_CHANGE_REASON_REQUIRED = new ErrorCode(1_030_028_008,
            "变更已生效的采购合同必须填写变更原因");
    ErrorCode PURCHASE_CONTRACT_AUDIT_REMARK_REQUIRED = new ErrorCode(1_030_028_009,
            "驳回采购合同必须填写审核意见");
    ErrorCode PURCHASE_CONTRACT_NOT_EFFECTIVE = new ErrorCode(1_030_028_010,
            "采购合同未审核生效或已过期，不能作为采购依据：{}");

    // ========== 卖方主体准入（#48，ADR 0029：反向开票只对自然人） 1-030-029-000 ==========
    ErrorCode SELLER_SUBJECT_TYPE_NOT_NATURAL = new ErrorCode(1_030_029_000,
            "{}不是自然人，不能反向开票；请由对方自行开具增值税发票，并在「进项收票」登记与勾稽");

    // ========== 交接批次与有效磅次（#50 T12） 1-030-030-000 ==========
    ErrorCode HANDOVER_BATCH_NOT_EXISTS = new ErrorCode(1_030_030_000, "交接批次不存在");
    ErrorCode HANDOVER_BATCH_PAYEE_REQUIRED = new ErrorCode(1_030_030_001, "交接批次必须登记交易对方，请先带出售者档案");
    ErrorCode HANDOVER_BATCH_LOCATION_REQUIRED = new ErrorCode(1_030_030_002, "场站与上门地址至少填一个（上门回收填地址，到场收货填场站）");
    ErrorCode HANDOVER_BATCH_PLATE_REQUIRED = new ErrorCode(1_030_030_003, "车牌号不能为空（同一车同一天两次送货是两个批次，靠车牌与磅次区分）");
    ErrorCode HANDOVER_SOURCE_TYPE_INVALID = new ErrorCode(1_030_030_004, "来源方式不合法：{}");
    ErrorCode WEIGHING_NOT_EXISTS = new ErrorCode(1_030_030_005, "磅次不存在");
    ErrorCode WEIGHING_NOT_IN_BATCH = new ErrorCode(1_030_030_006, "该磅次不属于这个交接批次，不能在这里指定");
    ErrorCode WEIGHING_WEIGHT_INVALID = new ErrorCode(1_030_030_007, "磅次重量不合法：{}（毛重与皮重不能为负，皮重不能大于毛重）");
    ErrorCode WEIGHING_EFFECTIVE_NOT_SELECTED = new ErrorCode(1_030_030_008,
            "该交接批次还没有指定有效磅次，请先指定哪一次参与计量（其余磅次留档但不参与）");
    ErrorCode WEIGHING_BATCH_IN_USE = new ErrorCode(1_030_030_009,
            "该交接批次已产生收购单，计量结果已引用当时那一次磅次，不能再改有效磅次；请作废收购单或新建批次");
    ErrorCode ACQUISITION_BATCH_PAYEE_MISMATCH = new ErrorCode(1_030_030_010,
            "收购单的出售者与交接批次的交易对方不一致，不能挂在同一个批次上");
    ErrorCode WEIGHING_LOCKED_FOR_ACQUISITION = new ErrorCode(1_030_030_011,
            "该收购单按交接批次的有效磅次计量，不能手工改重量（毛重 / 皮重 / 净重）；请先作废收购单再改磅次，或另建批次");
}
