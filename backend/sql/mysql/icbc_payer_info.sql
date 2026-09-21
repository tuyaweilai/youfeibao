-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- 工行付方信息表
CREATE TABLE IF NOT EXISTS `icbc_payer_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payer_no` varchar(64) DEFAULT NULL COMMENT '工行付方编号（建档时为空）',
  `partner_payer_id` varchar(64) NOT NULL COMMENT '合作方付方编号',
  `name` varchar(100) NOT NULL COMMENT '企业名称',
  `credit_code` varchar(18) NOT NULL COMMENT '统一社会信用代码',
  `tax_no` varchar(20) NOT NULL COMMENT '纳税人识别号',
  `bank_account` varchar(32) DEFAULT NULL COMMENT '银行账户',
  `bank_name` varchar(100) DEFAULT NULL COMMENT '开户行名称',
  `address` varchar(500) DEFAULT NULL COMMENT '企业地址',
  `telephone` varchar(20) DEFAULT NULL COMMENT '企业电话',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人姓名',
  `contact_mobile` varchar(11) DEFAULT NULL COMMENT '联系人手机号',
  `taxpayer_type` varchar(2) DEFAULT NULL COMMENT '纳税人类型：01-一般纳税人，02-小规模纳税人',
  `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-待审核，1-审核通过，2-审核拒绝',
  `audit_msg` varchar(500) DEFAULT NULL COMMENT '审核消息',
  `icbc_payer_status` varchar(1) DEFAULT NULL COMMENT '工行付方状态：0-不可用，1-可用',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payer_no` (`payer_no`),
  UNIQUE KEY `uk_partner_payer_id` (`partner_payer_id`),
  UNIQUE KEY `uk_credit_code` (`credit_code`),
  UNIQUE KEY `uk_tax_no` (`tax_no`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行付方信息表'; 