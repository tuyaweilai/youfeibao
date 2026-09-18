-- 合同管理模块数据库表结构脚本
-- 创建日期：2024年12月
-- 版本：V2.0

-- 确保使用了正确的字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建合同类型表
CREATE TABLE IF NOT EXISTS `contract_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '合同类型ID',
  `code` varchar(50) NOT NULL COMMENT '类型编码',
  `name` varchar(100) NOT NULL COMMENT '类型名称',
  `description` text COMMENT '类型描述',
  `system_defined` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否为系统预定义类型',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态',
  `sort` int(11) DEFAULT '0' COMMENT '排序顺序',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`, `deleted`, `tenant_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同类型表';

-- 创建合同模板表
CREATE TABLE IF NOT EXISTS `contract_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(255) NOT NULL COMMENT '模板名称',
  `template_code` varchar(100) NOT NULL COMMENT '模板编码',
  `contract_type_id` bigint(20) NOT NULL COMMENT '合同类型ID',
  `template_content` longtext COMMENT '模板内容',
  `template_file_url` varchar(512) DEFAULT '' COMMENT '模板文件URL',
  `version` varchar(50) DEFAULT '1.0' COMMENT '模板版本',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认模板',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`, `deleted`, `tenant_id`),
  KEY `idx_contract_type_id` (`contract_type_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板表';

-- 创建合同主表
CREATE TABLE IF NOT EXISTS `contracts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `contract_no` varchar(64) NOT NULL COMMENT '合同编号',
  `contract_uuid` varchar(36) NOT NULL COMMENT '合同UUID',
  `contract_name` varchar(255) NOT NULL COMMENT '合同名称',
  `contract_type_id` bigint(20) NOT NULL COMMENT '合同类型ID',
  `template_id` bigint(20) DEFAULT NULL COMMENT '使用的模板ID',
  `current_version_id` bigint(20) DEFAULT NULL COMMENT '当前激活版本ID',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '合同状态(0:草稿,1:待审核,2:审核通过,3:待签署,4:签署中,5:已生效,6:已履行,7:即将到期,8:已到期,9:已解除,10:已作废,11:已归档)',
  `is_electronic` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否为电子合同',
  `primary_owner_enterprise_id` bigint(20) NOT NULL COMMENT '合同主要负责企业ID',
  `total_amount` decimal(15,2) DEFAULT NULL COMMENT '合同总金额',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '币种',
  `priority_level` tinyint(4) DEFAULT '0' COMMENT '优先级(0:普通,1:重要,2:紧急)',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`, `deleted`, `tenant_id`),
  UNIQUE KEY `uk_contract_uuid` (`contract_uuid`),
  KEY `idx_contract_type_id` (`contract_type_id`),
  KEY `idx_status` (`status`),
  KEY `idx_primary_owner_enterprise_id` (`primary_owner_enterprise_id`),
  KEY `idx_current_version_id` (`current_version_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同主表';

-- 创建合同版本表
CREATE TABLE IF NOT EXISTS `contract_versions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `contract_id` bigint(20) NOT NULL COMMENT '合同ID',
  `version_number` varchar(20) NOT NULL DEFAULT '1.0' COMMENT '版本号',
  `effective_date` date DEFAULT NULL COMMENT '生效日期',
  `expiry_date` date DEFAULT NULL COMMENT '失效日期',
  `signing_date` date DEFAULT NULL COMMENT '签署完成日期',
  `termination_date` date DEFAULT NULL COMMENT '终止日期',
  `reason_for_termination` text COMMENT '终止原因',
  `description_of_changes` text COMMENT '版本变更说明',
  `contract_content` longtext COMMENT '合同内容',
  `contract_file_url` varchar(512) DEFAULT '' COMMENT '合同文件URL',
  `esignature_provider` varchar(50) DEFAULT '' COMMENT '电子签章服务商',
  `esignature_process_id` varchar(100) DEFAULT '' COMMENT '第三方签署流程ID',
  `esignature_status` tinyint(4) DEFAULT '0' COMMENT '电子签章状态(0:未发起,1:进行中,2:已完成,3:已失败)',
  `auto_remind_days` varchar(100) DEFAULT '30,15,7' COMMENT '自动提醒天数(逗号分隔)',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_effective_date` (`effective_date`),
  KEY `idx_expiry_date` (`expiry_date`),
  KEY `idx_esignature_status` (`esignature_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同版本表';

-- 创建合同参与方表
CREATE TABLE IF NOT EXISTS `contract_parties` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '参与方ID',
  `version_id` bigint(20) NOT NULL COMMENT '合同版本ID',
  `enterprise_id` bigint(20) NOT NULL COMMENT '参与企业ID',
  `enterprise_name` varchar(255) NOT NULL COMMENT '企业名称',
  `role_in_contract` varchar(100) NOT NULL COMMENT '合同中的角色',
  `signatory_name` varchar(100) DEFAULT '' COMMENT '签署人姓名',
  `signatory_email` varchar(255) DEFAULT '' COMMENT '签署人邮箱',
  `signatory_phone` varchar(20) DEFAULT '' COMMENT '签署人电话',
  `signatory_user_id` bigint(20) DEFAULT NULL COMMENT '签署人用户ID',
  `sign_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '签署状态(0:待签署,1:已签署,2:已拒绝,3:已过期)',
  `signed_at` datetime DEFAULT NULL COMMENT '签署时间',
  `sign_ip` varchar(50) DEFAULT '' COMMENT '签署IP地址',
  `esignature_individual_id` varchar(100) DEFAULT '' COMMENT '电子签章个体ID',
  `order_in_sign_flow` int(11) DEFAULT '0' COMMENT '签署顺序',
  `is_required` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否必须签署',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_sign_status` (`sign_status`),
  KEY `idx_signatory_user_id` (`signatory_user_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同参与方表';

-- 创建合同附件表
CREATE TABLE IF NOT EXISTS `contract_attachments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `version_id` bigint(20) NOT NULL COMMENT '合同版本ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `original_file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(512) NOT NULL COMMENT '文件路径',
  `file_url` varchar(512) NOT NULL COMMENT '文件URL',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(100) DEFAULT '' COMMENT '文件类型(MIME)',
  `attachment_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '附件类型(0:合同正文,1:扫描件,2:补充文件,3:签署凭证)',
  `is_public` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `download_count` int(11) DEFAULT '0' COMMENT '下载次数',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_attachment_type` (`attachment_type`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同附件表';

-- 创建合同关联对象表
CREATE TABLE IF NOT EXISTS `contract_linked_objects` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `contract_id` bigint(20) NOT NULL COMMENT '合同ID',
  `version_id` bigint(20) NOT NULL COMMENT '合同版本ID',
  `object_id` bigint(20) NOT NULL COMMENT '关联对象ID',
  `object_type` varchar(50) NOT NULL COMMENT '关联对象类型',
  `object_no` varchar(100) DEFAULT '' COMMENT '关联对象编号',
  `link_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '关联类型(0:业务关联,1:依赖关联,2:参考关联)',
  `link_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '关联状态(0:有效,1:失效,2:暂停)',
  `link_description` varchar(500) DEFAULT '' COMMENT '关联说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_object_id_type` (`object_id`, `object_type`),
  KEY `idx_link_status` (`link_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同关联对象表';

-- 创建合同操作日志表
CREATE TABLE IF NOT EXISTS `contract_operation_logs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `contract_id` bigint(20) NOT NULL COMMENT '合同ID',
  `version_id` bigint(20) DEFAULT NULL COMMENT '合同版本ID',
  `operation_type` tinyint(4) NOT NULL COMMENT '操作类型(1:创建,2:修改,3:提交审核,4:审核,5:签署,6:激活,7:终止,8:归档)',
  `operation_description` varchar(500) NOT NULL COMMENT '操作描述',
  `old_status` tinyint(4) DEFAULT NULL COMMENT '操作前状态',
  `new_status` tinyint(4) DEFAULT NULL COMMENT '操作后状态',
  `operation_data` text COMMENT '操作数据(JSON)',
  `operator_id` bigint(20) NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) NOT NULL COMMENT '操作人姓名',
  `operator_ip` varchar(50) DEFAULT '' COMMENT '操作IP',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同操作日志表';

-- 插入系统预定义的合同类型
INSERT INTO `contract_type` (`id`, `code`, `name`, `description`, `system_defined`, `status`, `sort`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'WASTE_TRANSFER', '危险废物转移合同', '产废企业与回收企业之间的危险废物转移合同', b'1', 1, 1, 0, 'system', NOW(), 'system', NOW(), b'0'),
(2, 'LOGISTICS_SERVICE', '物流服务合同', '回收企业与物流企业之间的物流服务合同', b'1', 1, 2, 0, 'system', NOW(), 'system', NOW(), b'0'),
(3, 'WASTE_DISPOSAL', '废物处置合同', '回收企业与处置企业之间的废物处置合同', b'1', 1, 3, 0, 'system', NOW(), 'system', NOW(), b'0'),
(4, 'PLATFORM_SERVICE', '平台服务协议', '企业与平台之间的服务协议', b'1', 1, 4, 0, 'system', NOW(), 'system', NOW(), b'0'),
(5, 'PROCUREMENT', '采购合同', '企业间的采购合同', b'1', 1, 5, 0, 'system', NOW(), 'system', NOW(), b'0'),
(6, 'TECHNICAL_SERVICE', '技术服务合同', '技术服务相关合同', b'1', 1, 6, 0, 'system', NOW(), 'system', NOW(), b'0');

-- 插入系统配置信息
INSERT INTO `infra_config` (`id`, `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2001, 'contract', 1, '合同编号前缀', 'contract.number.prefix', 'CT', '0', '合同编号生成前缀', 'admin', NOW(), 'admin', NOW(), b'0'),
(2002, 'contract', 1, '合同到期提醒天数', 'contract.expiry.remind.days', '30,15,7', '0', '合同到期自动提醒天数，逗号分隔', 'admin', NOW(), 'admin', NOW(), b'0'),
(2003, 'contract', 1, '电子签章服务商', 'contract.esignature.provider', 'esign', '0', '默认电子签章服务商', 'admin', NOW(), 'admin', NOW(), b'0'),
(2004, 'contract', 1, '合同文件存储路径', 'contract.file.storage.path', '/contract/files/', '0', '合同文件存储相对路径', 'admin', NOW(), 'admin', NOW(), b'0'),
(2005, 'contract', 1, '合同审核流程启用', 'contract.approval.enabled', 'true', '0', '是否启用合同审核流程', 'admin', NOW(), 'admin', NOW(), b'0');

SET FOREIGN_KEY_CHECKS = 1; 