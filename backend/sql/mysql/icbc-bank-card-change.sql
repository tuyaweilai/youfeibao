-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 换银行卡（#37，见 docs/adr/0010-付款走公对私直付到银行卡.md）
--
-- 「卡过期 / 换行 / 卡丢失」是必然会发生的事。工行收方入驻绑的是本人**一张**卡，
-- 换卡必须重走收方入驻（同一 outUserId + outVendorId 在工行侧是「修改」，auditStatus=3 修改审核中），
-- 因此：
--   * 不新造流程——复用已有的 ONBOARDING 令牌与后端输出表单机制；
--   * 不允许多张卡——收方档案（icbc_payee_info.bank_card_no）只保留生效中的那一张，
--     待变更的新卡只活在本表里，审核通过才「搬」过去；
--   * 审核期间新交易的付款挂起——企业侧的预下单照旧，但**付款发起**被拦下，避免钱打到废卡（退汇）。
--
-- 本表是**流水与在途状态**：每发起一次换卡就是一条，终态（已生效 / 已拒绝 / 已取消）后就地留痕，
-- 不删行。同一收方同一时刻最多一条「银行审核中」（服务层校验，DB 不建部分唯一索引以兼容 H2）。
-- 迁移幂等：CREATE TABLE IF NOT EXISTS。租户表（带 tenant_id），不进 ignore-tables。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_payee_bank_card_change` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `change_no` varchar(64) NOT NULL COMMENT '变更单号（租户内唯一）',
  `payee_id` bigint unsigned NOT NULL COMMENT '收方（出售者）档案编号',
  `natural_person_id` bigint unsigned DEFAULT NULL COMMENT '自然人主体编号（平台级身份）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-银行审核中，1-已生效，2-已拒绝，9-已取消',
  `old_card_tail` varchar(8) DEFAULT NULL COMMENT '原卡尾号快照（钱原本要打到的卡）',
  `new_bank_card_no` varchar(64) NOT NULL COMMENT '待变更的新银行卡号（审核通过前不生效）',
  `new_bank_name` varchar(100) DEFAULT NULL COMMENT '新卡开户银行',
  `new_bank_branch` varchar(100) DEFAULT NULL COMMENT '新卡开户支行',
  `id_sign_date` varchar(20) DEFAULT NULL COMMENT '证件签发日期 yyyy-MM-dd（收方入驻入参快照）',
  `id_validity_period` varchar(20) DEFAULT NULL COMMENT '证件截止日期 yyyy-MM-dd（收方入驻入参快照）',
  `icbc_openacct_status` varchar(8) DEFAULT NULL COMMENT '工行侧开户状态（原样透传，只属于本次变更）',
  `audit_result` varchar(16) DEFAULT NULL COMMENT '工行审核结果（原样透传）：pass / reject',
  `icbc_medium_id` varchar(64) DEFAULT NULL COMMENT '工行返回的账户标识（mediumId，原样透传）',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '审核拒绝原因 / 取消原因',
  `request_source` varchar(32) DEFAULT NULL COMMENT '发起来源：SELLER_PORTAL-自然人端，FIELD-现场端，ADMIN-管理后台',
  `request_ip` varchar(64) DEFAULT NULL COMMENT '发起 IP（留痕）',
  `requested_at` datetime NOT NULL COMMENT '发起时间',
  `resolved_at` datetime DEFAULT NULL COMMENT '有结果时间（生效 / 拒绝 / 取消）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_card_change_no` (`change_no`),
  KEY `idx_bank_card_change_payee` (`tenant_id`, `payee_id`, `status`),
  KEY `idx_bank_card_change_natural` (`tenant_id`, `natural_person_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出售者换银行卡（收款账户变更）';
