-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- 工行付方支付订单表
CREATE TABLE `icbc_payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '支付订单号',
  `partner_order_id` varchar(35) NOT NULL COMMENT '合作方订单ID',
  `acquisition_id` bigint DEFAULT NULL COMMENT '来源收购单编号',
  `invoice_order_id` bigint DEFAULT NULL COMMENT '来源开票订单编号',
  `icbc_order_no` varchar(64) DEFAULT NULL COMMENT '工行订单号',
  `payee_no` varchar(40) DEFAULT NULL COMMENT '收方编号',
  `payer_no` varchar(20) DEFAULT NULL COMMENT '付方编号',
  `payment_amount` decimal(10,2) DEFAULT '0.00' COMMENT '支付金额',
  `payment_status` tinyint DEFAULT '0' COMMENT '支付状态：0-待支付，1-支付中，2-支付成功，3-支付失败，4-订单关闭，5-已冲正，6-已退汇，7-他行已扣款本行未入账，8-已支付待签收，9-部分成功',
  `pay_status` varchar(8) DEFAULT NULL COMMENT '工行原始支付状态码：-1/00/01/02/03/04/05/06/07/12/25',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `payment_serial_no` varchar(64) DEFAULT NULL COMMENT '支付流水号',
  `actually_received_amount` decimal(14,2) DEFAULT NULL COMMENT '实际到账金额',
  `receipt_no` varchar(64) DEFAULT NULL COMMENT '转账回单号',
  `receipt_time` datetime DEFAULT NULL COMMENT '转账回单归档时间',
  `receipt_file_url` varchar(500) DEFAULT NULL COMMENT '转账回单文件地址',
  `retry_count` int DEFAULT '0' COMMENT '重新发起次数',
  `verified_code` varchar(30) DEFAULT NULL COMMENT '机构编码（场景支付时必输）',
  `ukey_id` varchar(24) DEFAULT NULL COMMENT 'U盾ID（场景支付时必输）',
  `redirect_url` text COMMENT '支付页面重定向URL',
  `msg_id` varchar(32) DEFAULT NULL COMMENT '消息通讯唯一编号',
  `error_code` varchar(20) DEFAULT NULL COMMENT '错误码',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_partner_order_id` (`partner_order_id`,`deleted`,`tenant_id`) COMMENT '合作方订单ID唯一索引',
  UNIQUE KEY `uk_order_no` (`order_no`,`deleted`,`tenant_id`) COMMENT '支付订单号唯一索引',
  KEY `idx_icbc_order_no` (`icbc_order_no`) COMMENT '工行订单号索引',
  KEY `idx_payment_status` (`payment_status`) COMMENT '支付状态索引',
  KEY `idx_payment_acquisition_id` (`acquisition_id`) COMMENT '收购单索引',
  KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行付方支付订单表'; 