package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.hutool.core.collection.CollUtil;

/**
 * 企业模块的错误码枚举
 *
 * 企业模块，使用 1-028-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 企业信息相关 ==========
    ErrorCode ENTERPRISE_INFO_NOT_EXISTS = new ErrorCode(1_028_000_001, "企业信息不存在");
    ErrorCode ENTERPRISE_CREDIT_CODE_EXISTS = new ErrorCode(1_028_000_002, "统一社会信用代码已存在");
    ErrorCode ENTERPRISE_NAME_EXISTS = new ErrorCode(1_028_000_003, "企业名称已存在");
    ErrorCode ENTERPRISE_STATUS_NOT_VALID = new ErrorCode(1_028_000_004, "企业状态不正确");
    ErrorCode ENTERPRISE_INFO_STATUS_NOT_PENDING = new ErrorCode(1_028_000_005, "企业当前状态不允许审核");

    // ========== 企业认证相关 ==========
    ErrorCode ENTERPRISE_AUTH_NOT_EXISTS = new ErrorCode(1_028_001_001, "企业认证记录不存在");
    ErrorCode ENTERPRISE_AUTH_ALREADY_EXISTS = new ErrorCode(1_028_001_002, "企业认证记录已存在");

    // ========== 企业资质相关 ==========
    ErrorCode ENTERPRISE_QUALIFICATION_NOT_EXISTS = new ErrorCode(1_028_002_001, "企业资质不存在");
    ErrorCode ENTERPRISE_QUALIFICATION_ALREADY_EXISTS = new ErrorCode(1_028_002_002, "企业资质已存在");
    ErrorCode ENTERPRISE_QUALIFICATION_EXPIRED = new ErrorCode(1_028_002_003, "企业资质已过期");
    ErrorCode ENTERPRISE_QUALIFICATION_INVALID = new ErrorCode(1_028_002_004, "企业资质已作废");
    ErrorCode ENTERPRISE_QUALIFICATION_STATUS_NOT_VALID = new ErrorCode(1_028_002_005, "企业资质状态不正确");
    ErrorCode ENTERPRISE_QUALIFICATION_TYPE_NOT_VALID = new ErrorCode(1_028_002_006, "企业资质类型不正确");

    // ========== 企业门店相关 ==========
    ErrorCode ENTERPRISE_STORE_NOT_EXISTS = new ErrorCode(1_028_003_001, "企业门店不存在");
    ErrorCode ENTERPRISE_STORE_ALREADY_EXISTS = new ErrorCode(1_028_003_002, "企业门店已存在");
    ErrorCode ENTERPRISE_STORE_PARENT_NOT_EXISTS = new ErrorCode(1_028_003_003, "父门店不存在");
    ErrorCode ENTERPRISE_STORE_PARENT_ERROR = new ErrorCode(1_028_003_004, "不能将自己设为父门店");
    ErrorCode ENTERPRISE_STORE_PARENT_IS_CHILD = new ErrorCode(1_028_003_005, "不能将自己的子门店设为父门店");
    ErrorCode ENTERPRISE_STORE_CONTAINS_CHILDREN = new ErrorCode(1_028_003_006, "门店下存在子门店，无法删除");
    ErrorCode ENTERPRISE_STORE_NAME_EXISTS = new ErrorCode(1_028_003_007, "同一父门店下，门店名称已存在");
    ErrorCode ENTERPRISE_STORE_STATUS_NOT_VALID = new ErrorCode(1_028_003_008, "门店状态不正确");
    ErrorCode ENTERPRISE_STORE_CODE_EXISTS = new ErrorCode(1_028_003_009, "门店编码已存在");

    // ========== 用户企业关系相关 ==========
    ErrorCode ENTERPRISE_USER_RELATION_NOT_EXISTS = new ErrorCode(1_028_004_001, "用户企业关系不存在");
    ErrorCode ENTERPRISE_USER_RELATION_ALREADY_EXISTS = new ErrorCode(1_028_004_002, "用户企业关系已存在");
    ErrorCode ENTERPRISE_USER_RELATION_NOT_MATCH_USER = new ErrorCode(1_028_004_003, "用户企业关系不属于该用户");
    ErrorCode ENTERPRISE_USER_RELATION_TYPE_NOT_VALID = new ErrorCode(1_028_004_004, "用户企业关系类型不正确");
    ErrorCode ENTERPRISE_USER_RELATION_DEFAULT_EXISTS = new ErrorCode(1_028_004_005, "已存在默认企业，请先取消原默认企业");

} 