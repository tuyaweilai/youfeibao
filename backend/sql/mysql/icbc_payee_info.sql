-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 工商银行反向开票收方信息表
-- ========================================

-- 收方信息表（个人销售方）
CREATE TABLE `icbc_payee_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payee_no` varchar(64) DEFAULT NULL COMMENT '收方编号（工行返回，建档时为空）',
  `partner_payee_id` varchar(64) NOT NULL COMMENT '合作方收方编号（我方生成的收方档案编号）',
  `natural_person_id` bigint DEFAULT NULL COMMENT '自然人主体编号（平台级身份，ADR 0017）',
  `name` varchar(100) NOT NULL COMMENT '收方姓名',
  `id_card_no` varchar(18) NOT NULL COMMENT '身份证号码',
  `mobile` varchar(11) NOT NULL COMMENT '手机号码',
  `bank_card_no` varchar(32) DEFAULT NULL COMMENT '银行卡号',
  `bank_name` varchar(100) DEFAULT NULL COMMENT '开户银行',
  `bank_branch` varchar(200) DEFAULT NULL COMMENT '开户支行',
  `address` varchar(500) DEFAULT NULL COMMENT '地址',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-待审核，1-审核通过，2-审核拒绝',
  `audit_msg` varchar(500) DEFAULT NULL COMMENT '审核信息',
  `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型：RECYCLE-再生资源等',
  `icbc_receiver_status` varchar(1) DEFAULT NULL COMMENT '工行收方状态：0-不可用，1-可用',
  `occupation` varchar(3) DEFAULT NULL COMMENT '职业',
  `company_name` varchar(60) DEFAULT NULL COMMENT '关联企业名称',
  `real_name_status` tinyint unsigned DEFAULT '0' COMMENT '实人认证状态：0-未认证，1-认证中，2-认证通过，3-认证未通过',
  `real_name_msg` varchar(500) DEFAULT NULL COMMENT '实人认证失败原因',
  `real_name_time` datetime DEFAULT NULL COMMENT '实人认证通过时间',
  `audit_result` varchar(10) DEFAULT NULL COMMENT '收方审核结果：pass-通过，reject-拒绝',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '审核拒绝原因',
  `onboarding_state` varchar(20) DEFAULT NULL COMMENT '收方入驻结果：READY/REJECTED/OPENACCT_FAILED/FAILED_AND_REJECTED',
  `id_sign_date` varchar(10) DEFAULT NULL COMMENT '证件签发日期 yyyy-MM-dd',
  `id_validity_period` varchar(10) DEFAULT NULL COMMENT '证件截止日期 yyyy-MM-dd，永久有效传 9999-12-30',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payee_no` (`payee_no`),
  UNIQUE KEY `uk_partner_payee_id` (`partner_payee_id`),
  KEY `idx_payee_natural_person` (`natural_person_id`),
  UNIQUE KEY `uk_id_card_no` (`id_card_no`, `deleted`),
  UNIQUE KEY `uk_mobile` (`mobile`, `deleted`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行收方信息表'; 