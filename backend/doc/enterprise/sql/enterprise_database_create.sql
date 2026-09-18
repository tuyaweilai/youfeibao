-- 企业模块数据库创建脚本
-- 基于 enterprise_database_design.md 设计文档
-- 创建日期：2024-12-02

-- ========================================
-- 1. 企业信息表 (enterprise_info)
-- ========================================
CREATE TABLE IF NOT EXISTS `enterprise_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '企业名称',
  `credit_code` varchar(18) NOT NULL COMMENT '统一社会信用代码',
  `enterprise_type` tinyint(4) NOT NULL COMMENT '企业类型 (1:产废, 2:回收, 3:处置, 4:物流, 99:其他)',
  `legal_person_name` varchar(64) NOT NULL COMMENT '法定代表人姓名',
  `legal_person_id_card_no` varchar(18) DEFAULT NULL COMMENT '法定代表人身份证号 (脱敏存储或仅用于认证过程)',
  `registered_capital` decimal(18,2) DEFAULT NULL COMMENT '注册资本 (万元)',
  `establishment_date` date DEFAULT NULL COMMENT '成立日期',
  `business_scope` text COMMENT '经营范围',
  `registered_address_province_code` varchar(20) DEFAULT NULL COMMENT '注册地址-省编码',
  `registered_address_city_code` varchar(20) DEFAULT NULL COMMENT '注册地址-市编码',
  `registered_address_district_code` varchar(20) DEFAULT NULL COMMENT '注册地址-区编码',
  `registered_address_detail` varchar(255) DEFAULT NULL COMMENT '注册地址-详细地址',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '企业联系人姓名',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '企业联系人电话',
  `business_license_file` varchar(500) DEFAULT NULL COMMENT '营业执照附件URL',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '企业状态 (0:待认证, 1:认证中, 2:已认证/审核通过, 3:认证失败/审核拒绝, 4:已禁用)',
  `audit_remarks` varchar(500) DEFAULT NULL COMMENT '最新审核备注 (冗余字段，主要记录在audit_log和auth_record)',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除 (0:未删除, 1:已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credit_code_tenant_id` (`credit_code`, `tenant_id`, `deleted`),
  KEY `idx_name` (`name`),
  KEY `idx_enterprise_type` (`enterprise_type`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业信息表';

-- ========================================
-- 2. 个人实名认证表 (member_personal_auth)
-- ========================================
CREATE TABLE IF NOT EXISTS `member_personal_auth` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID (认证记录ID)',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID (关联 system_users 或 member_users 表)',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `id_card_no` varchar(18) NOT NULL COMMENT '身份证号码 (加密存储)',
  `id_card_front_file_id` bigint(20) DEFAULT NULL COMMENT '身份证正面照片附件ID',
  `id_card_back_file_id` bigint(20) DEFAULT NULL COMMENT '身份证反面照片附件ID',
  `auth_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '认证状态 (0:待认证, 1:认证中, 2:认证通过, 3:认证失败)',
  `auth_channel` varchar(50) DEFAULT NULL COMMENT '认证渠道 (例如: eSign, Manual)',
  `channel_request_id` varchar(100) DEFAULT NULL COMMENT '渠道方请求ID (如e签宝)',
  `channel_flow_id` varchar(100) DEFAULT NULL COMMENT '渠道方流程ID (如e签宝)',
  `failure_reason` varchar(255) DEFAULT NULL COMMENT '认证失败原因',
  `auth_time` datetime DEFAULT NULL COMMENT '认证通过/失败时间',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者 (提交认证的用户)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间 (提交认证时间)',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者 (审核员或系统)',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除 (0:未删除, 1:已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_id_card_no` (`id_card_no`),
  KEY `idx_auth_status` (`auth_status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人实名认证表';

-- ========================================
-- 3. 企业认证记录表 (enterprise_auth_record)
-- ========================================
CREATE TABLE IF NOT EXISTS `enterprise_auth_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID (认证记录ID)',
  `enterprise_id` bigint(20) NOT NULL COMMENT '企业ID (关联 enterprise_info 表)',
  `submitter_user_id` bigint(20) DEFAULT NULL COMMENT '提交认证的用户ID',
  `auth_type` tinyint(4) NOT NULL COMMENT '认证类型 (1:企业三要素验证, 2:法人实名认证, 3:营业执照OCR/签章核验, 4:人工审核)',
  `legal_person_personal_auth_id` bigint(20) DEFAULT NULL COMMENT '法人个人实名认证ID (关联 member_personal_auth 表)',
  `submitted_business_license_file_id` bigint(20) DEFAULT NULL COMMENT '提交的营业执照附件ID',
  `submitted_operating_permit_file_id` bigint(20) DEFAULT NULL COMMENT '提交的经营许可证附件ID (可选)',
  `auth_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '认证状态 (0:待处理, 1:处理中, 2:成功, 3:失败)',
  `audit_user_id` bigint(20) DEFAULT NULL COMMENT '审核员ID (关联 system_users 表, 人工审核时)',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间 (人工审核时)',
  `audit_remarks` varchar(500) DEFAULT NULL COMMENT '审核备注/失败原因',
  `esign_auth_channel` varchar(50) DEFAULT NULL COMMENT 'e签宝认证渠道 (如果使用)',
  `esign_channel_request_id` varchar(100) DEFAULT NULL COMMENT 'e签宝请求ID',
  `esign_channel_flow_id` varchar(100) DEFAULT NULL COMMENT 'e签宝流程ID',
  `blockchain_tx_hash` varchar(100) DEFAULT NULL COMMENT '区块链存证哈希',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者 (发起认证操作的用户或系统)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间 (认证发起时间)',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除 (0:未删除, 1:已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_auth_type` (`auth_type`),
  KEY `idx_auth_status` (`auth_status`),
  KEY `idx_legal_person_personal_auth_id` (`legal_person_personal_auth_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业认证记录表';

-- ========================================
-- 4. 企业资质表 (enterprise_qualification)
-- ========================================
CREATE TABLE IF NOT EXISTS `enterprise_qualification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enterprise_id` bigint(20) NOT NULL COMMENT '企业ID (关联 enterprise_info 表)',
  `qualification_type` tinyint(4) NOT NULL COMMENT '资质类型 (字典管理, 例如: 1:危废经营许可证, 2:道路运输许可证, 3:回收资质证明, 99:其他)',
  `qualification_name` varchar(100) NOT NULL COMMENT '资质名称 (冗余或根据类型自动生成)',
  `qualification_code` varchar(50) DEFAULT NULL COMMENT '资质编号',
  `issue_date` date DEFAULT NULL COMMENT '发证日期',
  `expiry_date` date DEFAULT NULL COMMENT '到期日期',
  `issuing_authority` varchar(100) DEFAULT NULL COMMENT '发证机关',
  `qualification_file_id` bigint(20) NOT NULL COMMENT '资质文件附件ID (关联infra_file表)',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '资质状态 (0:待审核, 1:有效, 2:已过期, 3:审核拒绝, 4:已作废)',
  `audit_remarks` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除 (0:未删除, 1:已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_qualification_type` (`qualification_type`),
  KEY `idx_expiry_date` (`expiry_date`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业资质表';

-- ========================================
-- 5. 企业门店表 (enterprise_store)
-- ========================================
CREATE TABLE IF NOT EXISTS `enterprise_store` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID (门店ID)',
  `enterprise_id` bigint(20) NOT NULL COMMENT '所属企业ID (关联 enterprise_info 表)',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '上级门店ID (用于分层结构, 总店/区域中心等, 关联自身)',
  `name` varchar(100) NOT NULL COMMENT '门店名称',
  `store_code` varchar(50) DEFAULT NULL COMMENT '门店编码 (可选, 自定义或系统生成)',
  `address_province_code` varchar(20) DEFAULT NULL COMMENT '门店地址-省编码',
  `address_city_code` varchar(20) DEFAULT NULL COMMENT '门店地址-市编码',
  `address_district_code` varchar(20) DEFAULT NULL COMMENT '门店地址-区编码',
  `address_detail` varchar(255) DEFAULT NULL COMMENT '门店地址-详细地址',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '门店联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '门店联系电话',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '门店状态 (0:正常, 1:停用)',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除 (0:未删除, 1:已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业门店表';

-- ========================================
-- 6. 用户企业关系表 (enterprise_user_relation)
-- ========================================
CREATE TABLE IF NOT EXISTS `enterprise_user_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID (关联 system_users 或 member_users 表)',
  `enterprise_id` bigint(20) NOT NULL COMMENT '企业ID (关联 enterprise_info 表)',
  `store_id` bigint(20) DEFAULT NULL COMMENT '门店ID (关联 enterprise_store 表, 可选, 表示用户属于某门店)',
  `relation_type` tinyint(4) NOT NULL COMMENT '关系类型 (1:企业管理员, 2:企业员工, 3:企业法人代表, 99:其他)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '关系状态 (0:待审批/申请中, 1:有效/已关联, 2:已拒绝, 3:已解除/曾关联)',
  `request_message` varchar(255) DEFAULT NULL COMMENT '申请加入时的留言',
  `approver_id` bigint(20) DEFAULT NULL COMMENT '审批人ID (关联system_users表, 适用于申请加入流程)',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approval_remarks` varchar(255) DEFAULT NULL COMMENT '审批备注 (如拒绝原因)',
  `is_primary_contact` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否企业主联系人 (0:否, 1:是)',
  `is_default_enterprise` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否用户默认操作企业 (0:否, 1:是, 用户可能关联多个企业时使用)',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者 (用户ID或system)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间 (关系建立或申请发起时间)',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除 (0:未删除, 1:已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_enterprise_store_deleted_status` (`user_id`, `enterprise_id`, `store_id`, `deleted`, `status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_relation_type` (`relation_type`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户企业关系表';

-- ========================================
-- 7. 企业审核日志表 (enterprise_audit_log)
-- ========================================
CREATE TABLE IF NOT EXISTS `enterprise_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `target_entity_type` tinyint(4) NOT NULL COMMENT '目标实体类型 (1:企业信息, 2:企业认证, 3:企业资质, 4:门店信息)',
  `target_entity_id` bigint(20) NOT NULL COMMENT '目标实体ID (例如 enterprise_info_id, enterprise_auth_record_id 等)',
  `enterprise_id` bigint(20) DEFAULT NULL COMMENT '关联的企业ID (方便查询)',
  `audit_user_id` bigint(20) DEFAULT NULL COMMENT '审核人ID (关联 system_users 表, 系统自动审核时可为空)',
  `audit_action` varchar(50) NOT NULL COMMENT '审核动作 (例如: Create, Update, Submit, Approve, Reject, Enable, Disable)',
  `previous_status` varchar(50) DEFAULT NULL COMMENT '操作前状态',
  `current_status` varchar(50) DEFAULT NULL COMMENT '操作后状态',
  `audit_remarks` varchar(500) DEFAULT NULL COMMENT '审核备注或原因',
  `request_details` text DEFAULT NULL COMMENT '请求详情 (JSON格式, 可选, 用于追溯)',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者 (触发审核日志的用户名或系统标识)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间 (审核时间)',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除 (0:未删除, 1:已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_target_entity` (`target_entity_type`, `target_entity_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_audit_user_id` (`audit_user_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业审核日志表';

-- ========================================
-- 创建完成
-- ========================================

/*
企业模块数据库表创建完成！

包含以下表：
1. enterprise_info - 企业信息表
2. member_personal_auth - 个人实名认证表
3. enterprise_auth_record - 企业认证记录表
4. enterprise_qualification - 企业资质表
5. enterprise_store - 企业门店表
6. enterprise_user_relation - 用户企业关系表
7. enterprise_audit_log - 企业审核日志表

所有表结构都严格按照 enterprise_database_design.md 设计文档创建。
*/ 