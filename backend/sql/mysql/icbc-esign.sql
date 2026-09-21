-- 连接字符集：文件里有中文注释与种子。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 电子签章：平台级配置 + 租户级开通（#92，见 docs/adr/0036-框架收购协议走第三方电子签章.md、
-- docs/adr/0037-卡证识别与电子签章各立出站端口.md）
--
-- 章是**租户级**的：回收企业才是发起方（第三方接口约束，也与 ADR 0001 一致），平台不代盖。
-- 因此配置分两层：
--   * `icbc_esign_config` —— 平台级唯一一份：环境 / 两套 endpoint / 应用标识 / 密钥 /
--     回调地址与验签 / 签署链接渠道 / 平台模板。**密钥只落后端、界面不回显明文**，
--     它是全局表，登记进 yudao.tenant.ignore-tables；
--   * `icbc_esign_tenant` —— 租户级：子客编号（我们生成、持久化、不可变、不可重复）、
--     激活状态、经办人编号、企业印章编号、合同额度。
--
-- 未开通即降级、不阻断建档：签章不可用 → 协议落 signMethod=PAPER，向导照常走完（ADR 0036 决策 7）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS；菜单在 icbc-menu.sql 里幂等维护。
-- ========================================

-- ----------------------------
-- 平台级电子签章参数：全局表，只该有一行；查询取第一条、保存原地更新
-- ----------------------------
CREATE TABLE IF NOT EXISTS `icbc_esign_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `environment` varchar(16) DEFAULT NULL COMMENT '环境：TEST-测试，PROD-生产',
  `api_endpoint` varchar(255) DEFAULT NULL COMMENT '服务端接口地址（两套 endpoint 之一）',
  `console_endpoint` varchar(255) DEFAULT NULL COMMENT '控制台地址（两套 endpoint 之二）：一次性开通链接拼在它上面',
  `app_id` varchar(64) DEFAULT NULL COMMENT '应用标识（第三方分配给平台的 AppId）',
  `secret_id` varchar(128) DEFAULT NULL COMMENT '应用密钥 ID（只写不读，界面不回显明文）',
  `secret_key` varchar(255) DEFAULT NULL COMMENT '应用密钥（只写不读，界面不回显明文）',
  `callback_url` varchar(255) DEFAULT NULL COMMENT '签署状态回调地址（平台外网可达）',
  `callback_sign_key` varchar(255) DEFAULT NULL COMMENT '回调验签密钥（只写不读；验签失败必须明确失败）',
  `sign_link_channel` varchar(32) DEFAULT NULL COMMENT '签署链接渠道：H5 / MINI_PROGRAM / PC',
  `agreement_template_id` varchar(64) DEFAULT NULL COMMENT '平台模板：框架收购协议（租户只能填变量）',
  `notice_template_id` varchar(64) DEFAULT NULL COMMENT '平台模板：反向发票合规告知函',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子签章平台级参数（平台级唯一一份）';

-- ----------------------------
-- 租户级电子签章配置：每个回收企业一行
--
-- 子客编号 `sub_customer_no` 由我们生成（ES + 租户编号左补零到 10 位），持久化、**不可变**、
-- **不可重复**（唯一键兜底）；它是第三方回执里唯一的租户锚点，回调靠它反查出是哪家回收企业。
-- ----------------------------
CREATE TABLE IF NOT EXISTS `icbc_esign_tenant` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sub_customer_no` varchar(64) NOT NULL COMMENT '子客编号：我们生成、持久化、不可变、不可重复',
  `activation_status` tinyint NOT NULL DEFAULT '0' COMMENT '开通状态：0-未开通，1-认证中，2-已激活',
  `operator_no` varchar(64) DEFAULT NULL COMMENT '经办人编号（第三方侧的企业办事人）',
  `seal_no` varchar(64) DEFAULT NULL COMMENT '企业印章编号（为空表示印章未就位）',
  `contract_quota` int NOT NULL DEFAULT '0' COMMENT '合同额度（份数）',
  `contract_used` int NOT NULL DEFAULT '0' COMMENT '已用合同额度（份数）',
  `console_token` varchar(64) DEFAULT NULL COMMENT '最近一次签发的控制台链接令牌（一次性，激活后清空）',
  `console_token_expire_time` datetime DEFAULT NULL COMMENT '控制台链接有效期止',
  `activated_time` datetime DEFAULT NULL COMMENT '激活时间（企业认证通过且印章就位）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_esign_tenant_sub_customer_no` (`sub_customer_no`),
  UNIQUE KEY `uk_esign_tenant_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户级电子签章配置';
