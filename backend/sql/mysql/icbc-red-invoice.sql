-- ========================================
-- 红冲与发票取消（#14）
-- 红字发票表：一张蓝票最多一张生效中的红票，红蓝一一对应
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_red_invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `red_offset_no` varchar(35) NOT NULL COMMENT '红冲流水号（工行 outRedOffsetId，幂等键）',
  `invoice_order_id` bigint DEFAULT NULL COMMENT '蓝票开票订单编号',
  `partner_order_id` varchar(35) NOT NULL COMMENT '原蓝字合作方订单编号（工行 outOrderId）',
  `acquisition_id` bigint DEFAULT NULL COMMENT '来源收购单编号',
  `reason` varchar(2) NOT NULL COMMENT '红冲原因：01 开票有误，02 销货退回，03 服务中止，04 销售折让',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '红字冲销金额',
  `tax_amount` decimal(14,2) DEFAULT NULL COMMENT '红字冲销税额',
  `red_offset_status` tinyint NOT NULL DEFAULT '0' COMMENT '平台侧红冲状态：0 初始，1 申请中，2 申请成功，3 申请失败，4 上传处理中，5 上传已受理，6 上传中，7 红冲成功，8 上传失败，9 撤销中，10 已撤销，11 撤销失败',
  `red_offset_status_code` varchar(2) DEFAULT NULL COMMENT '工行原始红字确认单状态码（00–11）',
  `red_invoice_no` varchar(20) DEFAULT NULL COMMENT '红冲发票号（工行 redOffsetInvoiceCode）',
  `red_invoice_date` datetime DEFAULT NULL COMMENT '红票开具日期',
  `revoke_status` varchar(2) DEFAULT NULL COMMENT '撤销结果（10 撤销成功 / 11 撤销失败）',
  `revoke_time` datetime DEFAULT NULL COMMENT '撤销发起时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_red_offset_no` (`red_offset_no`),
  KEY `idx_partner_order_id` (`partner_order_id`),
  KEY `idx_invoice_order_id` (`invoice_order_id`),
  KEY `idx_red_invoice_no` (`red_invoice_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='红字发票（红冲）表';
