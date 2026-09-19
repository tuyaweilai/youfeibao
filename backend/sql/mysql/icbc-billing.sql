-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 平台计费计量（#16）
-- 新建 icbc_billing_ledger：平台自己的账，按「租户 × 月份」记成功开具的报废产品收购发票张数、
-- 被红冲的张数与应计费用。表为全局表（无租户隔离语义），须登记进 yudao.tenant.ignore-tables。
-- 菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_billing_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '被计费的回收企业租户编号',
  `period_month` varchar(7) NOT NULL COMMENT '计费期间（yyyy-MM，按开票日期归属）',
  `issued_count` int NOT NULL DEFAULT '0' COMMENT '本期成功开具的报废产品收购发票张数',
  `reversed_count` int NOT NULL DEFAULT '0' COMMENT '本期蓝票中被成功红冲的张数',
  `billable_count` int NOT NULL DEFAULT '0' COMMENT '计费张数 = issued_count - reversed_count',
  `unit_price` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '计费单价（元/张）',
  `amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '应计费用（元）',
  `generated_time` datetime DEFAULT NULL COMMENT '本次计量时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_billing_tenant_period` (`tenant_id`, `period_month`),
  KEY `idx_billing_period_month` (`period_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台计费计量台账';
