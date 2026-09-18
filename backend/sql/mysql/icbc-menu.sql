-- 反向开票（icbc）与租户开票就绪的管理后台菜单
-- 幂等：先按固定 id 范围删除，再插入。
-- 组件路径对应前端 backend/yudao-ui/yudao-ui-admin-vue3/src/views/icbc/* 与 views/enterprise/*

DELETE FROM `system_menu` WHERE `id` BETWEEN 5100 AND 5150;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
-- ===== 反向开票 =====
(5100, '反向开票', '', 1, 60, 0, '/icbc', 'ep:tickets', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5101, '出售者档案', 'icbc:payee-info:query', 2, 1, 5100, 'payee', '', 'icbc/payee/index', 'IcbcPayee', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5102, '出售者新增', 'icbc:payee-info:create', 3, 1, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5103, '出售者修改', 'icbc:payee-info:update', 3, 2, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5104, '出售者删除', 'icbc:payee-info:delete', 3, 3, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5105, '出售者导出', 'icbc:payee-info:export', 3, 4, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5106, '付方档案', 'icbc:payer-info:query', 2, 2, 5100, 'payer', '', 'icbc/payer/index', 'IcbcPayer', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5107, '付方新增', 'icbc:payer-info:create', 3, 1, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5108, '付方修改', 'icbc:payer-info:update', 3, 2, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5109, '付方删除', 'icbc:payer-info:delete', 3, 3, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5110, '开票申请', 'icbc:invoice-order:query', 2, 3, 5100, 'invoice', '', 'icbc/invoice/index', 'IcbcInvoice', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5111, '发起开票', 'icbc:invoice-order:create', 3, 1, 5110, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5112, '付款', 'icbc:payment:query', 2, 4, 5100, 'payment', '', 'icbc/payment/index', 'IcbcPayment', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5113, '发起付款', 'icbc:payment:create', 3, 1, 5112, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5118, '发票下载与证据', 'icbc:invoice-download:query', 2, 5, 5100, 'download', '', 'icbc/download/index', 'IcbcDownload', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5119, '执行下载', 'icbc:invoice-download:download', 3, 1, 5118, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5120, '重试下载', 'icbc:invoice-download:retry', 3, 2, 5118, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5121, '下载文件', 'icbc:invoice-download:download-file', 3, 3, 5118, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- ===== 租户开票就绪 =====
(5114, '租户开票就绪', '', 1, 61, 0, '/readiness', 'ep:document-checked', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5115, '企业信息', 'enterprise:info:query', 2, 1, 5114, 'enterprise-info', '', 'enterprise/info/index', 'Info', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5116, '企业资质', 'enterprise:cert:query', 2, 2, 5114, 'enterprise-qualification', '', 'enterprise/qualification/index', 'EnterpriseQualification', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5117, '开票就绪自检', 'icbc:test:query', 2, 3, 5114, 'self-check', '', 'icbc/readiness/index', 'IcbcReadiness', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 三层资质
(5122, '三层资质', 'icbc:qualification:query', 2, 4, 5114, 'qualification', '', 'icbc/qualification/index', 'IcbcQualification', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5123, '资质新增', 'icbc:qualification:create', 3, 1, 5122, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5124, '资质修改', 'icbc:qualification:update', 3, 2, 5122, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5125, '资质删除', 'icbc:qualification:delete', 3, 3, 5122, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 编码配置
(5126, '编码配置', 'icbc:goods-config:query', 2, 5, 5114, 'goods-config', '', 'icbc/goodsConfig/index', 'IcbcGoodsConfig', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5127, '品类新增', 'icbc:goods-config:create', 3, 1, 5126, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5128, '品类修改', 'icbc:goods-config:update', 3, 2, 5126, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5129, '品类删除', 'icbc:goods-config:delete', 3, 3, 5126, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 企业授权
(5130, '企业授权', 'icbc:enterprise-auth:query', 2, 6, 5114, 'enterprise-auth', '', 'icbc/enterpriseAuth/index', 'IcbcEnterpriseAuth', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5131, '发起授权', 'icbc:enterprise-auth:init', 3, 1, 5130, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5132, '回填授权结果', 'icbc:enterprise-auth:update', 3, 2, 5130, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- ===== 平台运营 =====
(5140, '平台运营', '', 1, 62, 0, '/platform', 'ep:data-analysis', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5141, '资质核实', 'icbc:platform:qualification:query', 2, 1, 5140, 'qualification', '', 'icbc/platformQualification/index', 'IcbcPlatformQualification', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5142, '核实资质', 'icbc:platform:qualification:audit', 3, 1, 5141, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5143, '报废产品编码表', 'icbc:scrap-code:query', 2, 2, 5140, 'scrap-code', '', 'icbc/scrapCode/index', 'IcbcScrapCode', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5144, '编码新增', 'icbc:scrap-code:create', 3, 1, 5143, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5145, '编码修改', 'icbc:scrap-code:update', 3, 2, 5143, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5146, '编码删除', 'icbc:scrap-code:delete', 3, 3, 5143, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
