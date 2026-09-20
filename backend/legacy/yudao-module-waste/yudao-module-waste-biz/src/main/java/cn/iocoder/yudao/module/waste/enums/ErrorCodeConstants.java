package cn.iocoder.yudao.module.waste.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 危废模块错误码枚举类
 *
 * 危废模块，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 预约管理 1-030-001-000 ==========
    ErrorCode APPOINTMENT_NOT_EXISTS = new ErrorCode(1_030_001_000, "预约不存在");
    ErrorCode APPOINTMENT_STATUS_INVALID = new ErrorCode(1_030_001_001, "预约状态无效");
    ErrorCode APPOINTMENT_ALREADY_CONFIRMED = new ErrorCode(1_030_001_002, "预约已确认，无法修改");
    ErrorCode APPOINTMENT_ALREADY_REJECTED = new ErrorCode(1_030_001_003, "预约已拒绝，无法修改");
    ErrorCode APPOINTMENT_ALREADY_CANCELLED = new ErrorCode(1_030_001_004, "预约已取消，无法修改");
    ErrorCode APPOINTMENT_NO_EXISTS = new ErrorCode(1_030_001_005, "预约单号已存在");
    ErrorCode APPOINTMENT_CANNOT_CANCEL = new ErrorCode(1_030_001_006, "当前状态不允许取消预约");
    ErrorCode APPOINTMENT_CANNOT_CONFIRM = new ErrorCode(1_030_001_007, "当前状态不允许确认预约");
    ErrorCode APPOINTMENT_CANNOT_REJECT = new ErrorCode(1_030_001_008, "当前状态不允许拒绝预约");
    ErrorCode APPOINTMENT_RECYCLER_NOT_ASSIGNED = new ErrorCode(1_030_001_009, "预约尚未分配回收企业");

    // ========== 回收企业分配规则 1-030-002-000 ==========
    ErrorCode ASSIGNMENT_RULE_NOT_EXISTS = new ErrorCode(1_030_002_000, "分配规则不存在");
    ErrorCode ASSIGNMENT_RULE_NAME_EXISTS = new ErrorCode(1_030_002_001, "分配规则名称已存在");
    ErrorCode ASSIGNMENT_RULE_IN_USE = new ErrorCode(1_030_002_002, "分配规则正在使用中，无法删除");
    ErrorCode ASSIGNMENT_RULE_DISABLED = new ErrorCode(1_030_002_003, "分配规则已禁用");
    ErrorCode NO_AVAILABLE_RECYCLER = new ErrorCode(1_030_002_004, "没有可用的回收企业");
    ErrorCode ASSIGNMENT_RULE_CONFIG_INVALID = new ErrorCode(1_030_002_005, "分配规则配置无效");

    // ========== 企业相关 1-030-003-000 ==========
    ErrorCode PRODUCER_ENTERPRISE_NOT_EXISTS = new ErrorCode(1_030_003_000, "产废企业不存在");
    ErrorCode RECYCLER_ENTERPRISE_NOT_EXISTS = new ErrorCode(1_030_003_001, "回收企业不存在");
    ErrorCode ENTERPRISE_NOT_AUTHORIZED = new ErrorCode(1_030_003_002, "企业未授权");
    ErrorCode ENTERPRISE_STATUS_INVALID = new ErrorCode(1_030_003_003, "企业状态无效");

    // ========== 废物信息 1-030-004-000 ==========
    ErrorCode WASTE_CODE_INVALID = new ErrorCode(1_030_004_000, "危险废物代码无效");
    ErrorCode WASTE_QUANTITY_INVALID = new ErrorCode(1_030_004_001, "废物数量无效");
    ErrorCode WASTE_CATEGORY_NOT_SUPPORTED = new ErrorCode(1_030_004_002, "不支持的废物类别");

    // ========== 订单管理 1-030-005-000 ==========
    ErrorCode TRANSFER_ORDER_NOT_EXISTS = new ErrorCode(1_030_005_000, "转移订单不存在");
    ErrorCode TRANSFER_ORDER_STATUS_ERROR = new ErrorCode(1_030_005_001, "订单状态错误");
    ErrorCode TRANSFER_ORDER_ALREADY_CANCELLED = new ErrorCode(1_030_005_002, "订单已取消");
    ErrorCode ORDER_STATUS_INVALID = new ErrorCode(1_030_005_003, "订单状态无效");
    ErrorCode ORDER_CANNOT_CANCEL = new ErrorCode(1_030_005_004, "当前状态不允许取消订单");
    ErrorCode ORDER_CANNOT_CONFIRM = new ErrorCode(1_030_005_005, "当前状态不允许确认订单");
    ErrorCode ORDER_PRICE_ADJUSTMENT_NOT_EXISTS = new ErrorCode(1_030_005_006, "订单价格调整记录不存在");
    ErrorCode ORDER_ALLOCATION_RECORD_NOT_EXISTS = new ErrorCode(1_030_005_007, "订单分摊记录不存在");
    ErrorCode ORDER_STATUS_HISTORY_NOT_EXISTS = new ErrorCode(1_030_005_008, "订单状态历史记录不存在");

    // ========== 付款管理 1-030-006-000 ==========
    ErrorCode PRODUCER_PAYMENT_CONFIG_NOT_EXISTS = new ErrorCode(1_030_006_000, "产废企业付款配置不存在");
    ErrorCode COMPANY_PAYMENT_VOUCHER_NOT_EXISTS = new ErrorCode(1_030_006_001, "对公付款凭证不存在");
    ErrorCode PAYMENT_STATUS_HISTORY_NOT_EXISTS = new ErrorCode(1_030_006_003, "付款状态历史记录不存在");
    ErrorCode PAYMENT_CONFIG_ALREADY_DEFAULT = new ErrorCode(1_030_006_004, "付款配置已是默认配置");
    ErrorCode PAYMENT_VOUCHER_ALREADY_CONFIRMED = new ErrorCode(1_030_006_005, "付款凭证已确认");
    ErrorCode PAYMENT_VOUCHER_IN_DISPUTE = new ErrorCode(1_030_006_006, "付款凭证存在争议");

    // ========== 付款相关错误码 1-030-006-000 ==========
    ErrorCode PAYMENT_CONFIG_NOT_EXISTS = new ErrorCode(1_030_006_001, "付款配置不存在");

    // ========== 价格管理 1-030-007-000 ==========
    ErrorCode PRICE_BENCHMARK_NOT_EXISTS = new ErrorCode(1_030_007_000, "价格基准不存在");
    ErrorCode RECYCLER_PRICE_CONFIG_NOT_EXISTS = new ErrorCode(1_030_007_001, "回收企业价格配置不存在");
    ErrorCode RECYCLER_CUSTOMER_PRICE_NOT_EXISTS = new ErrorCode(1_030_007_002, "回收企业客户专属价格不存在");
    ErrorCode PRICE_BENCHMARK_EXPIRED = new ErrorCode(1_030_007_003, "价格基准已过期");
    ErrorCode PRICE_CONFIG_EXPIRED = new ErrorCode(1_030_007_004, "价格配置已过期");
    ErrorCode PRICE_CALCULATION_ERROR = new ErrorCode(1_030_007_005, "价格计算错误");

    // ========== 报价管理 1-030-008-000 ==========
    ErrorCode APPOINTMENT_QUOTATION_NOT_EXISTS = new ErrorCode(1_030_008_000, "预约报价记录不存在");
    ErrorCode QUOTATION_ALREADY_SUBMITTED = new ErrorCode(1_030_008_001, "报价已提交");
    ErrorCode QUOTATION_ALREADY_ACCEPTED = new ErrorCode(1_030_008_002, "报价已接受");
    ErrorCode QUOTATION_ALREADY_REJECTED = new ErrorCode(1_030_008_003, "报价已拒绝");
    ErrorCode QUOTATION_ALREADY_WITHDRAWN = new ErrorCode(1_030_008_004, "报价已撤回");
    ErrorCode QUOTATION_EXPIRED = new ErrorCode(1_030_008_005, "报价已过期");
    ErrorCode QUOTATION_PRICE_INVALID = new ErrorCode(1_030_008_006, "报价金额无效");
    ErrorCode QUOTATION_NOT_EXISTS = new ErrorCode(1_030_008_007, "报价不存在");
    ErrorCode QUOTATION_STATUS_NOT_PENDING = new ErrorCode(1_030_008_008, "报价状态不是待接受");
    ErrorCode QUOTATION_NOT_ACCEPTED = new ErrorCode(1_030_008_009, "报价未被接受");

    // ========== 回收企业配置 1-030-009-000 ==========
    ErrorCode RECYCLER_BUSINESS_CONFIG_NOT_EXISTS = new ErrorCode(1_030_009_000, "回收企业业务配置不存在");
    ErrorCode BUSINESS_MODE_NOT_SUPPORTED = new ErrorCode(1_030_009_001, "不支持的业务模式");
    ErrorCode QUOTATION_MODE_NOT_SUPPORTED = new ErrorCode(1_030_009_002, "不支持的报价模式");
    ErrorCode CUSTOMER_MODE_NOT_SUPPORTED = new ErrorCode(1_030_009_003, "不支持的客户模式");
    ErrorCode NEGOTIATION_CONFIG_INVALID = new ErrorCode(1_030_009_004, "价格协商配置无效");

    // ========== 合同管理 1-030-010-000 ==========
    ErrorCode CONTRACT_NOT_EXISTS = new ErrorCode(1_030_010_000, "合同不存在");
    ErrorCode CONTRACT_ALREADY_SIGNED = new ErrorCode(1_030_010_001, "合同已签署");
    ErrorCode CONTRACT_TEMPLATE_NOT_EXISTS = new ErrorCode(1_030_010_002, "合同模板不存在");
    ErrorCode CONTRACT_GENERATION_FAILED = new ErrorCode(1_030_010_003, "合同生成失败");
    ErrorCode CONTRACT_SIGNING_FAILED = new ErrorCode(1_030_010_004, "合同签署失败");

} 