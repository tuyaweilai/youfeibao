-- 工行反向开票订单表
CREATE TABLE `icbc_invoice_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号（我方生成）',
  `partner_order_id` varchar(35) NOT NULL COMMENT '合作方订单ID（传给工行）',
  `acquisition_id` bigint DEFAULT NULL COMMENT '来源收购单编号（一个收购单对应一张票）',
  `payee_id` bigint DEFAULT NULL COMMENT '收方ID',
  `payee_no` varchar(40) NOT NULL COMMENT '收方编号',
  `payer_id` bigint DEFAULT NULL COMMENT '付方ID',
  `payer_no` varchar(20) NOT NULL COMMENT '付方编号',
  `total_amount` decimal(14,2) NOT NULL COMMENT '订单总金额（元）',
  `invoice_type` tinyint NOT NULL DEFAULT '1' COMMENT '发票类型：1-增值税普通发票，2-增值税专用发票',
  `business_type` varchar(20) NOT NULL COMMENT '业务类型：AGRICULTURAL-农产品收购，SCRAP-报废产品收购',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待确认，1-已确认，2-已支付，3-已开票，4-已完成，9-已取消',
  `invoice_status` tinyint NOT NULL DEFAULT '0' COMMENT '开票状态：0-未开票，1-开票中，2-已开票，3-开票失败',
  `payment_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态：0-未支付，1-支付中，2-支付成功，3-支付失败',
  `tax_status` tinyint NOT NULL DEFAULT '0' COMMENT '缴税状态：0-未缴税，1-缴税中，2-缴税成功，3-缴税失败，4-异常（金额不一致），5-异常（未知），6-无需缴税',
  `upload_status` tinyint DEFAULT NULL COMMENT '发票上传状态：0-未上传，1-处理中，2-已受理，3-上传中，4-上传成功，5-上传失败',
  `tax_real_amount` decimal(14,2) DEFAULT NULL COMMENT '实缴税额（工行 taxRealAmount）',
  `tax_time` datetime DEFAULT NULL COMMENT '缴税时间（工行 tradeTime）',
  `tax_payment_method` varchar(4) DEFAULT NULL COMMENT '税费缴纳方式：0-自然人自行办理，1-企业委托扣缴',
  `tax_voucher_no` varchar(64) DEFAULT NULL COMMENT '应征凭证序号（缴税凭证编号）',
  `confirm_status` tinyint NOT NULL DEFAULT '0' COMMENT '自然人确认状态：0-未确认，1-自然人确认完成，2-全部确认完成',
  `pre_invoice_status` tinyint NOT NULL DEFAULT '0' COMMENT '预开票状态：0-初始，1-预开票中，2-预开票成功，3-预开票失败，4-预开票取消',
  `pre_order_time` datetime DEFAULT NULL COMMENT '预下单发起时间',
  `invoice_no` varchar(20) DEFAULT NULL COMMENT '发票号码',
  `invoice_code` varchar(20) DEFAULT NULL COMMENT '发票代码',
  `invoice_date` datetime DEFAULT NULL COMMENT '开票日期',
  `invoice_amount` decimal(14,2) DEFAULT NULL COMMENT '发票金额',
  `tax_amount` decimal(14,2) DEFAULT NULL COMMENT '税额',
  `invoice_file_url` varchar(500) DEFAULT NULL COMMENT '发票文件URL',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_partner_order_id` (`partner_order_id`),
  KEY `idx_acquisition_id` (`acquisition_id`),
  KEY `idx_payee_no` (`payee_no`),
  KEY `idx_payer_no` (`payer_no`),
  KEY `idx_invoice_no` (`invoice_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行反向开票订单表';

-- 工行订单商品明细表
CREATE TABLE `icbc_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `item_name` varchar(300) NOT NULL COMMENT '商品名称',
  `item_code` varchar(50) DEFAULT NULL COMMENT '商品编码',
  `specification` varchar(100) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(10) NOT NULL COMMENT '单位',
  `quantity` decimal(14,4) NOT NULL COMMENT '数量',
  `unit_price` decimal(14,2) NOT NULL COMMENT '单价（元）',
  `amount` decimal(14,2) NOT NULL COMMENT '金额（元）',
  `tax_rate` decimal(4,2) NOT NULL COMMENT '税率',
  `tax_amount` decimal(14,2) DEFAULT NULL COMMENT '税额（元）',
  `category` varchar(50) DEFAULT NULL COMMENT '商品分类',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行订单商品明细表';

-- 插入菜单权限
INSERT INTO system_menu (
    name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    '工行反向开票', '', 2, 3, 2000, 'icbc-invoice', 'ep:document', 'icbc/invoice/index', 'IcbcInvoice', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
);

-- 获取刚插入的菜单ID
SET @menu_id = LAST_INSERT_ID();

-- 插入按钮权限
INSERT INTO system_menu (
    name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES 
('工行反向开票创建', 'icbc:invoice-order:create', 3, 1, @menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
('工行反向开票查询', 'icbc:invoice-order:query', 3, 2, @menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
('工行反向开票更新', 'icbc:invoice-order:update', 3, 3, @menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
('工行反向开票删除', 'icbc:invoice-order:delete', 3, 4, @menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'); 