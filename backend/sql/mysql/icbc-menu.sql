-- 反向开票（icbc）管理后台菜单
-- 幂等：先按固定 id 范围删除，再插入。
-- 组件路径对应前端 backend/yudao-ui/yudao-ui-admin-vue3/src/views/icbc/*

DELETE FROM `system_menu` WHERE `id` BETWEEN 5100 AND 5119;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
-- 目录
(5100, '反向开票', '', 1, 60, 0, '/icbc', 'ep:tickets', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 出售者档案
(5101, '出售者档案', 'icbc:payee-info:query', 2, 1, 5100, 'payee', '', 'icbc/payee/index', 'IcbcPayee', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5102, '出售者新增', 'icbc:payee-info:create', 3, 1, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5103, '出售者修改', 'icbc:payee-info:update', 3, 2, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5104, '出售者删除', 'icbc:payee-info:delete', 3, 3, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5105, '出售者导出', 'icbc:payee-info:export', 3, 4, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 付方档案（回收企业自身）
(5106, '付方档案', 'icbc:payer-info:query', 2, 2, 5100, 'payer', '', 'icbc/payer/index', 'IcbcPayer', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5107, '付方新增', 'icbc:payer-info:create', 3, 1, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5108, '付方修改', 'icbc:payer-info:update', 3, 2, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5109, '付方删除', 'icbc:payer-info:delete', 3, 3, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 开票申请
(5110, '开票申请', 'icbc:invoice-order:query', 2, 3, 5100, 'invoice', '', 'icbc/invoice/index', 'IcbcInvoice', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5111, '发起开票', 'icbc:invoice-order:create', 3, 1, 5110, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 付款
(5112, '付款', 'icbc:payment:query', 2, 4, 5100, 'payment', '', 'icbc/payment/index', 'IcbcPayment', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5113, '发起付款', 'icbc:payment:create', 3, 1, 5112, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
