-- ========================================
-- 反向开票（icbc）定时任务种子
-- 幂等：按 handler_name 判重后再插入
-- ========================================

-- 资质到期提醒：每日 08:00 扫描，按租户逐个执行（JobHandler 上标 @TenantJob）
INSERT INTO `infra_job`
(`name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '资质到期提醒 Job', 1, 'qualificationExpiryReminderJob', NULL, '0 0 8 * * ?', 0, 0, 0, 'admin', NOW(), 'admin', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'qualificationExpiryReminderJob' AND `deleted` = b'0'
);

-- 代办税费申报提醒：每月申报期是次月 15 日，每日 09:00 刷新上月清单并统计预警
INSERT INTO `infra_job`
(`name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '代办税费申报提醒 Job', 1, 'taxDeclarationReminderJob', NULL, '0 0 9 * * ?', 0, 0, 0, 'admin', NOW(), 'admin', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'taxDeclarationReminderJob' AND `deleted` = b'0'
);

-- 出售者汇算清缴提醒：每月 1 日 09:30 为当年有开票记录的出售者生成提醒与对账单
INSERT INTO `infra_job`
(`name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '出售者汇算清缴提醒 Job', 1, 'annualSettlementReminderJob', NULL, '0 30 9 1 * ?', 0, 0, 0, 'admin', NOW(), 'admin', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'annualSettlementReminderJob' AND `deleted` = b'0'
);
