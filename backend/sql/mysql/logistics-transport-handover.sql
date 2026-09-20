-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 物流域：交接登记（V6 #73，见 ADR 0030 / 0031 与 CONTEXT.md「物流与运力」）
--
-- 司机在**提货点**就一个停靠点登记交接事实：品类、参考量、参考单价与凭证照片。
-- 它是「现场谈好的事」的留痕，**不是收购单**：
--   * **现场不产生金额、不产生收购单**（ADR 0031）——结算重量回场复磅才定稿，
--     收购单那时才由 icbc 侧按本登记生成；
--   * 参考量 / 参考单价是**现场约定值**，不是计量事实，收购时会被复磅结果与收货员的
--     修正覆盖（修正必须留原因）；
--   * 现场参考量 / 照片凭证要给**磅房**看得到（icbc 侧经 logistics-api 读取面取）。
--
-- 缺身份证或银行卡时登记为**待补档**（document_status = PENDING）：事实照记，
-- 但回场生成的收购单会被付款与开票门禁拦住，不进开票申请、不进台账口径、不计入额度。
--
-- 品类存 icbc 侧的 `goods_config_id` + 名称快照（ADR 0028：品类权威是 goods_config_id，
-- 不用自由文本）；出售者同理只存 icbc 侧编号 + 姓名 / 手机号快照。物流不引用 icbc 的类
-- （ADR 0032），编号在物流这里只是数字。
--
-- `handover_batch_id` 故意**不放在这里**：两侧的挂接点在 icbc 侧（`icbc_handover_batch.logistics_handover_id`）。
-- 物流只提供读取面（ADR 0032），不写 icbc 的状态；反过来 icbc 按 `logistics_handover_id` 反查本表，
-- 方向恒为 icbc → 物流。重复回场复磅时 icbc 侧按该编号幂等，不会为同一次交接建出两个批次。
--
-- 幂等：ADD COLUMN 按 information_schema 判存在再执行；建表用 CREATE TABLE IF NOT EXISTS。
-- 注意 `--` 后必须跟空格才是注释：中文全角括号紧跟 `--` 会被 MySQL 当成语句，报 1064。
-- ========================================

CREATE TABLE IF NOT EXISTS `logistics_transport_handover` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `handover_no` varchar(64) NOT NULL COMMENT '交接登记单号（平台生成，租户内唯一）',
  `task_id` bigint unsigned NOT NULL COMMENT '运输任务编号',
  `task_no` varchar(64) DEFAULT NULL COMMENT '运输任务单号（冗余，便于按单号展示）',
  `stop_id` bigint unsigned NOT NULL COMMENT '停靠点编号（一个停靠点一次交接登记）',
  `address` varchar(255) DEFAULT NULL COMMENT '提货地址快照（上门提货的实际提货地址，落到收购单的交易地址）',
  `payee_id` bigint unsigned DEFAULT NULL COMMENT '出售者编号（icbc 侧编号，临时散户建档后回填）',
  `payee_name` varchar(64) DEFAULT NULL COMMENT '出售者姓名快照',
  `payee_mobile` varchar(32) DEFAULT NULL COMMENT '出售者手机号快照',
  `goods_config_id` bigint unsigned DEFAULT NULL COMMENT '品类配置编号（icbc 侧编号；权威品类，ADR 0028）',
  `category_name` varchar(64) DEFAULT NULL COMMENT '品类名称快照',
  `unit` varchar(16) DEFAULT NULL COMMENT '计量单位快照',
  `reference_quantity` decimal(16,4) DEFAULT NULL COMMENT '参考量（现场约定值，不是计量事实）',
  `reference_unit_price` decimal(16,4) DEFAULT NULL COMMENT '参考单价（现场约定值，收购时可修正并留原因）',
  `photos` text COMMENT '凭证照片 URL 列表（JSON 数组文本）',
  `driver_id` bigint unsigned DEFAULT NULL COMMENT '司机编号（引用 + 快照并存）',
  `driver_name` varchar(64) DEFAULT NULL COMMENT '司机姓名快照',
  `driver_mobile` varchar(32) DEFAULT NULL COMMENT '司机手机号快照',
  `vehicle_id` bigint unsigned DEFAULT NULL COMMENT '车辆编号（引用 + 快照并存）',
  `plate_no` varchar(32) DEFAULT NULL COMMENT '车牌号快照',
  `occur_time` datetime DEFAULT NULL COMMENT '交接发生时间',
  `document_status` varchar(16) NOT NULL DEFAULT 'COMPLETE' COMMENT '要件状态：COMPLETE-已齐，PENDING-待补档（缺身份证或银行卡）',
  `document_gap` varchar(128) DEFAULT NULL COMMENT '缺什么（待补档时说明，如「缺身份证」）',
  `client_request_id` varchar(64) DEFAULT NULL COMMENT '客户端请求号（弱网重复提交的幂等键）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_handover_tenant_no` (`tenant_id`, `handover_no`),
  UNIQUE KEY `uk_handover_tenant_client` (`tenant_id`, `client_request_id`),
  KEY `idx_handover_tenant_task` (`tenant_id`, `task_id`),
  KEY `idx_handover_tenant_stop` (`tenant_id`, `stop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 交接登记（司机在提货点登记的交接事实，不是收购单）';
