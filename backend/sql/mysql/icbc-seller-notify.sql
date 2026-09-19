-- ========================================
-- 出售者触达：短信三条 + 收货员转达（#36，见 docs/adr/0023-触达只靠短信与收货员转达.md）
--
-- 一期触达只有两条路：
--   * 短信（平台 / 租户开关，**默认关闭**；费用与到达率是运营成本）；
--   * 收货员一键把确认链接转达给出售者——首次交易、从未留手机号的场景只有这条通路。
--
-- 短信**只发三条**：结算单待确认 / 付款异常 / 发票已开出，每条都带一次性令牌链接，
-- 收短信的人**不需要注册**。不做 App 推送、不做公众号。
--
-- 幂等：CREATE TABLE IF NOT EXISTS；短信模板用 INSERT ... SELECT ... WHERE NOT EXISTS 维护；
-- 菜单在 icbc-menu.sql 里幂等维护。两张表都是租户表（带 tenant_id），不进 ignore-tables。
-- ========================================

-- ----------------------------
-- 触达记录：每次「本该发的触达」都留一条，无论发没发出去
-- ----------------------------
CREATE TABLE IF NOT EXISTS `icbc_seller_notify` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(32) NOT NULL COMMENT '触达类型：SETTLEMENT_PENDING / PAYMENT_EXCEPTION / INVOICE_ISSUED',
  `biz_key` varchar(128) NOT NULL COMMENT '业务键（幂等键）：同一业务事件只发一次',
  `template_code` varchar(64) NOT NULL COMMENT '短信模板编码',
  `natural_person_id` bigint unsigned DEFAULT NULL COMMENT '自然人主体编号',
  `payee_id` bigint unsigned DEFAULT NULL COMMENT '收方（出售者）档案编号',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名快照',
  `mobile` varchar(32) DEFAULT NULL COMMENT '接收手机号（为空表示未留号）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-未发送（开关关闭），1-未发送（未留手机号），2-未发送（未配置入口），3-已发送，4-发送失败',
  `sms_log_id` bigint unsigned DEFAULT NULL COMMENT '短信发送日志编号',
  `content` varchar(500) DEFAULT NULL COMMENT '短信正文（渲染后的最终文案）',
  `link` varchar(500) DEFAULT NULL COMMENT '一次性令牌链接',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因 / 未发送说明',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seller_notify_dedup` (`tenant_id`, `biz_type`, `biz_key`),
  KEY `idx_seller_notify_payee` (`tenant_id`, `payee_id`),
  KEY `idx_seller_notify_natural` (`tenant_id`, `natural_person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出售者触达记录';

-- ----------------------------
-- 触达设置：短信开关「租户或平台可开」，与平台级配置取或
-- ----------------------------
CREATE TABLE IF NOT EXISTS `icbc_notify_setting` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sms_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '本租户是否开启短信触达：0-关闭，1-开启',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注（谁开的、为什么开）',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notify_setting_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户级触达设置';

-- ----------------------------
-- 短信模板：只三条，内容与 SellerNotifyServiceImpl 的文案保持一致（ADR 0021：只说可核验的事）
-- 真实通道与报备模板号是前置条件：先用 DEBUG 渠道占位，报备通过后由运营改成真实渠道与 api_template_id。
-- ----------------------------
INSERT INTO `system_sms_template`
(`id`, `type`, `status`, `code`, `name`, `content`, `params`, `remark`, `api_template_id`, `channel_id`, `channel_code`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 9001, 2, 0, 'icbc_seller_notify_settlement_pending', '反向开票-结算单待确认',
       '你有一批货待确认：结算单 {settlementNo}，共 {count} 笔、金额 {amount} 元。点此查看并确认：{link}',
       '["settlementNo","count","amount","link"]', '前置条件：短信通道与签名报备；报备后替换 channel_id 与 api_template_id',
       '', COALESCE((SELECT `id` FROM `system_sms_channel` WHERE `code` = 'DEBUG_DING_TALK' AND `deleted` = b'0' ORDER BY `id` LIMIT 1), 4),
       'DEBUG_DING_TALK', 'admin', NOW(), 'admin', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `system_sms_template` WHERE `code` = 'icbc_seller_notify_settlement_pending');

INSERT INTO `system_sms_template`
(`id`, `type`, `status`, `code`, `name`, `content`, `params`, `remark`, `api_template_id`, `channel_id`, `channel_code`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 9002, 2, 0, 'icbc_seller_notify_payment_exception', '反向开票-付款异常',
       '你有一笔货款付款未完成（{status}）：{acquisitionNo}，金额 {amount} 元。点此查看下一步：{link}',
       '["status","acquisitionNo","amount","link"]', '前置条件：短信通道与签名报备；报备后替换 channel_id 与 api_template_id',
       '', COALESCE((SELECT `id` FROM `system_sms_channel` WHERE `code` = 'DEBUG_DING_TALK' AND `deleted` = b'0' ORDER BY `id` LIMIT 1), 4),
       'DEBUG_DING_TALK', 'admin', NOW(), 'admin', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `system_sms_template` WHERE `code` = 'icbc_seller_notify_payment_exception');

INSERT INTO `system_sms_template`
(`id`, `type`, `status`, `code`, `name`, `content`, `params`, `remark`, `api_template_id`, `channel_id`, `channel_code`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 9003, 2, 0, 'icbc_seller_notify_invoice_issued', '反向开票-发票已开出',
       '你的发票已开出：发票号 {invoiceNo}，金额 {amount} 元。点此下载：{link}',
       '["invoiceNo","amount","link"]', '前置条件：短信通道与签名报备；报备后替换 channel_id 与 api_template_id',
       '', COALESCE((SELECT `id` FROM `system_sms_channel` WHERE `code` = 'DEBUG_DING_TALK' AND `deleted` = b'0' ORDER BY `id` LIMIT 1), 4),
       'DEBUG_DING_TALK', 'admin', NOW(), 'admin', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `system_sms_template` WHERE `code` = 'icbc_seller_notify_invoice_issued');

-- ----------------------------
-- 短信日志列宽：三条模板都带一次性令牌链接，链接本身就约 260 字符（payload + HMAC 签名）。
-- 而快照里 `system_sms_log`.`template_content` 与 `template_params` 都只有 varchar(255)：
-- 渲染后的正文（约 320 字符）与参数 JSON 都装不下，插日志即报
-- "Data too long for column 'template_content'"。日志插不进去 → 短信永远发不出去，
-- 「短信转达」只能退化成「复制链接当面给他」，自动触达则落一条发送失败记录。
-- 单测里 SmsSendApi 是 Mock 的（不写这张表），所以只有真库跑得出来这个错。
-- 上游快照保持原样（ADR 0012），在引入长链接的这个文件里幂等加宽。
-- ----------------------------
SET @col_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_sms_log' AND COLUMN_NAME = 'template_content'
);
SET @ddl := IF(@col_len IS NOT NULL AND @col_len < 1024,
  'ALTER TABLE `system_sms_log` MODIFY COLUMN `template_content` varchar(1024) NOT NULL COMMENT ''短信内容''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- template_params 存的是同一批参数的 JSON（也含整条链接），一样会溢出
SET @col_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_sms_log' AND COLUMN_NAME = 'template_params'
);
SET @ddl := IF(@col_len IS NOT NULL AND @col_len < 1024,
  'ALTER TABLE `system_sms_log` MODIFY COLUMN `template_params` varchar(1024) NOT NULL COMMENT ''短信参数''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
