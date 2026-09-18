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
} 