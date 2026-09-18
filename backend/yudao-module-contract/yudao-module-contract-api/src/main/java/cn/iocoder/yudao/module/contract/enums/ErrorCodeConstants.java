package cn.iocoder.yudao.module.contract.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Contract 错误码枚举类
 *
 * contract 系统，使用 1-040-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 合同类型 1-040-001-000 ==========
    ErrorCode CONTRACT_TYPE_NOT_EXISTS = new ErrorCode(1_040_001_000, "合同类型不存在");
    ErrorCode CONTRACT_TYPE_CODE_DUPLICATE = new ErrorCode(1_040_001_001, "合同类型编码已存在");
    ErrorCode CONTRACT_TYPE_HAS_CONTRACTS = new ErrorCode(1_040_001_002, "该合同类型下存在合同，无法删除");
    ErrorCode CONTRACT_TYPE_SYSTEM_DEFINED = new ErrorCode(1_040_001_003, "系统预定义类型不允许修改");

    // ========== 合同模板 1-040-002-000 ==========
    ErrorCode CONTRACT_TEMPLATE_NOT_EXISTS = new ErrorCode(1_040_002_000, "合同模板不存在");
    ErrorCode CONTRACT_TEMPLATE_CODE_DUPLICATE = new ErrorCode(1_040_002_001, "合同模板编码已存在");
    ErrorCode CONTRACT_TEMPLATE_IN_USE = new ErrorCode(1_040_002_002, "该模板正在使用中，无法删除");

    // ========== 合同主体 1-040-003-000 ==========
    ErrorCode CONTRACT_NOT_EXISTS = new ErrorCode(1_040_003_000, "合同不存在");
    ErrorCode CONTRACT_NO_DUPLICATE = new ErrorCode(1_040_003_001, "合同编号已存在");
    ErrorCode CONTRACT_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_040_003_002, "当前合同状态不允许修改");
    ErrorCode CONTRACT_STATUS_NOT_ALLOW_DELETE = new ErrorCode(1_040_003_003, "当前合同状态不允许删除");
    ErrorCode CONTRACT_STATUS_NOT_ALLOW_SIGN = new ErrorCode(1_040_003_004, "当前合同状态不允许签署");
    ErrorCode CONTRACT_EXPIRED = new ErrorCode(1_040_003_005, "合同已过期");
    ErrorCode CONTRACT_NOT_EFFECTIVE = new ErrorCode(1_040_003_006, "合同未生效");
    ErrorCode CONTRACT_PARTIES_REQUIRED = new ErrorCode(1_040_003_007, "合同参与方不能为空");
    ErrorCode CONTRACT_PARTIES_INSUFFICIENT = new ErrorCode(1_040_003_008, "合同参与方至少需要两方");

    // ========== 合同版本 1-040-004-000 ==========
    ErrorCode CONTRACT_VERSION_NOT_EXISTS = new ErrorCode(1_040_004_000, "合同版本不存在");
    ErrorCode CONTRACT_VERSION_ALREADY_ACTIVE = new ErrorCode(1_040_004_001, "该版本已经是激活状态");
    ErrorCode CONTRACT_VERSION_NOT_SIGNED = new ErrorCode(1_040_004_002, "合同版本未完成签署");

    // ========== 合同签署 1-040-005-000 ==========
    ErrorCode CONTRACT_SIGN_NOT_YOUR_TURN = new ErrorCode(1_040_005_000, "当前不是您的签署顺序");
    ErrorCode CONTRACT_SIGN_ALREADY_SIGNED = new ErrorCode(1_040_005_001, "您已经签署过该合同");
    ErrorCode CONTRACT_SIGN_EXPIRED = new ErrorCode(1_040_005_002, "签署链接已过期");
    ErrorCode CONTRACT_SIGN_PROCESS_ERROR = new ErrorCode(1_040_005_003, "电子签章流程异常");
    ErrorCode CONTRACT_PARTY_NOT_EXISTS = new ErrorCode(1_040_005_004, "合同参与方不存在");
    ErrorCode CONTRACT_PARTY_ENTERPRISE_DUPLICATE = new ErrorCode(1_040_005_005, "该企业已经是合同参与方，不能重复添加");

    // ========== 合同附件 1-040-006-000 ==========
    ErrorCode CONTRACT_ATTACHMENT_NOT_EXISTS = new ErrorCode(1_040_006_000, "合同附件不存在");
    ErrorCode CONTRACT_ATTACHMENT_SIZE_EXCEED = new ErrorCode(1_040_006_001, "附件大小超过限制");
    ErrorCode CONTRACT_ATTACHMENT_TYPE_NOT_ALLOWED = new ErrorCode(1_040_006_002, "不支持的附件类型");

    // ========== 合同关联 1-040-007-000 ==========
    ErrorCode CONTRACT_LINK_NOT_EXISTS = new ErrorCode(1_040_007_000, "合同关联关系不存在");
    ErrorCode CONTRACT_LINK_ALREADY_EXISTS = new ErrorCode(1_040_007_001, "合同关联关系已存在");
    ErrorCode CONTRACT_LINK_INVALID = new ErrorCode(1_040_007_002, "无效的合同关联");

    // ========== 合同校验 1-040-008-000 ==========
    ErrorCode CONTRACT_VALIDATION_NO_VALID_CONTRACT = new ErrorCode(1_040_008_000, "未找到有效的合同");
    ErrorCode CONTRACT_VALIDATION_MULTIPLE_CONTRACTS = new ErrorCode(1_040_008_001, "存在多份有效合同，请指定具体合同");
    ErrorCode CONTRACT_VALIDATION_SCOPE_NOT_MATCH = new ErrorCode(1_040_008_002, "合同范围与业务不匹配");
} 