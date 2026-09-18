-- 企业模块数据库修复脚本
-- 执行日期：2024-12-02
-- 说明：根据 enterprise_database_design.md 设计文档修复数据库表结构

-- ========================================
-- 修复 enterprise_user_relation 表
-- ========================================

-- 1. 添加缺失的字段
ALTER TABLE enterprise_user_relation 
ADD COLUMN status tinyint NOT NULL DEFAULT '1' COMMENT '关系状态 (0:待审批/申请中, 1:有效/已关联, 2:已拒绝, 3:已解除/曾关联)' AFTER relation_type;

ALTER TABLE enterprise_user_relation 
ADD COLUMN request_message varchar(255) DEFAULT NULL COMMENT '申请加入时的留言' AFTER status;

ALTER TABLE enterprise_user_relation 
ADD COLUMN approver_id bigint DEFAULT NULL COMMENT '审批人ID (关联system_users表, 适用于申请加入流程)' AFTER request_message;

ALTER TABLE enterprise_user_relation 
ADD COLUMN approval_time datetime DEFAULT NULL COMMENT '审批时间' AFTER approver_id;

ALTER TABLE enterprise_user_relation 
ADD COLUMN approval_remarks varchar(255) DEFAULT NULL COMMENT '审批备注 (如拒绝原因)' AFTER approval_time;

-- 2. 添加索引
ALTER TABLE enterprise_user_relation 
ADD KEY idx_status (status);

-- 3. 修复唯一索引（包含 status 字段）
ALTER TABLE enterprise_user_relation 
DROP INDEX uk_user_enterprise_store_deleted;

ALTER TABLE enterprise_user_relation 
ADD UNIQUE KEY uk_user_enterprise_store_deleted_status (user_id, enterprise_id, store_id, deleted, status);

-- ========================================
-- 验证脚本
-- ========================================

-- 验证表结构
-- DESCRIBE enterprise_user_relation;

-- 验证索引
-- SHOW INDEX FROM enterprise_user_relation;

-- ========================================
-- 修复完成
-- ========================================

/*
修复总结：
1. ✅ enterprise_info - 企业信息表（已存在，结构正确）
2. ✅ member_personal_auth - 个人实名认证表（已存在，结构正确）
3. ✅ enterprise_auth_record - 企业认证记录表（已存在，结构正确）
4. ✅ enterprise_qualification - 企业资质表（已存在，结构正确）
5. ✅ enterprise_store - 企业门店表（已存在，结构正确）
6. ✅ enterprise_user_relation - 用户企业关系表（已修复，添加缺失字段和索引）
7. ✅ enterprise_audit_log - 企业审核日志表（已存在，结构正确）

所有表结构现在都与设计文档保持一致！
*/ 