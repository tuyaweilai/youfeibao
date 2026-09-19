-- 反向开票（icbc）与租户开票就绪的管理后台菜单
-- 幂等：先按固定 id 范围删除，再插入。
-- 组件路径对应前端 backend/yudao-ui/yudao-ui-admin-vue3/src/views/icbc/* 与 views/enterprise/*

DELETE FROM `system_menu` WHERE `id` BETWEEN 5100 AND 5199;

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
(5146, '编码删除', 'icbc:scrap-code:delete', 3, 3, 5143, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 通知监控与全平台证据（#15）
(5147, '通知监控', 'icbc:platform:callback:query', 2, 3, 5140, 'callback', '', 'icbc/platformCallback/index', 'IcbcPlatformCallback', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5148, '重放通知', 'icbc:platform:callback:retry', 3, 1, 5147, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5149, '全平台证据与异常票', 'icbc:platform:evidence:query', 2, 4, 5140, 'evidence', '', 'icbc/platformEvidence/index', 'IcbcPlatformEvidence', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 计费计量（#16）
(5150, '计费计量', 'icbc:platform:billing:query', 2, 5, 5140, 'billing', '', 'icbc/platformBilling/index', 'IcbcPlatformBilling', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5172, '重新计量', 'icbc:platform:billing:manage', 3, 1, 5150, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 一票一档证据链
(5151, '一票一档', 'icbc:evidence:query', 2, 6, 5100, 'evidence', '', 'icbc/evidence/index', 'IcbcEvidence', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5152, '补录证据', 'icbc:evidence:attach', 3, 1, 5151, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5153, '删除证据', 'icbc:evidence:delete', 3, 2, 5151, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5154, '导出证据包与台账', 'icbc:evidence:export', 3, 3, 5151, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5155, '生成公开令牌', 'icbc:public-token:create', 3, 4, 5151, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 出售者建档（实名 / 入驻 / 框架协议 / 首次授权）
(5133, '出售者建档', 'icbc:seller-onboarding:execute', 2, 2, 5100, 'payee-onboarding', '', 'icbc/payeeOnboarding/index', 'IcbcPayeeOnboarding', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5134, '框架收购协议', 'icbc:seller-agreement:manage', 3, 1, 5133, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5135, '首次授权', 'icbc:seller-authorization:manage', 3, 2, 5133, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 收购登记（#7）
(5136, '收购登记', 'icbc:acquisition:query', 2, 7, 5100, 'acquisition', '', 'icbc/acquisition/index', 'IcbcAcquisition', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5137, '登记收购', 'icbc:acquisition:create', 3, 1, 5136, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5138, '修正识别结果', 'icbc:acquisition:update', 3, 2, 5136, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5139, '导出收购确认书', 'icbc:acquisition:export', 3, 3, 5136, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 开票申请（#8：预下单与自然人确认）
(5156, '开票申请（按收购单）', 'icbc:invoice-application:query', 2, 8, 5100, 'invoice-application', '', 'icbc/invoiceApplication/index', 'IcbcInvoiceApplication', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5157, '发起开票申请', 'icbc:invoice-application:apply', 3, 1, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 红冲与发票取消（#14）
(5158, '发起红冲', 'icbc:red-invoice:apply', 3, 2, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5159, '撤销红字确认单', 'icbc:red-invoice:revoke', 3, 3, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5160, '取消预开票', 'icbc:invoice-order:cancel', 3, 4, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5161, '查询红冲', 'icbc:red-invoice:query', 3, 5, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 额度台账与经营主体登记引导（#12）
(5162, '额度台账', 'icbc:quota:query', 2, 9, 5100, 'quota', '', 'icbc/quota/index', 'IcbcQuota', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5163, '处理引导', 'icbc:quota:guidance:handle', 3, 1, 5162, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 代办税费申报（#13）
(5164, '代办税费申报', 'icbc:tax-declaration:query', 2, 10, 5100, 'tax', '', 'icbc/tax/index', 'IcbcTax', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5165, '生成申报清单', 'icbc:tax-declaration:manage', 3, 1, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5166, '报送报告表', 'icbc:tax-declaration:manage', 3, 2, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5167, '缴款归档', 'icbc:tax-declaration:manage', 3, 3, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5168, '登记补缴', 'icbc:tax-declaration:manage', 3, 4, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5169, '缴清补缴', 'icbc:tax-supplement:manage', 3, 5, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5170, '查看汇算清缴', 'icbc:settlement:query', 3, 6, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5171, '生成汇算提醒', 'icbc:settlement:remind', 3, 7, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 自然人主体（#31：平台级身份层，跨租户）
(5173, '自然人主体', 'icbc:platform:natural-person:query', 2, 6, 5140, 'natural-person', '', 'icbc/naturalPerson/index', 'IcbcNaturalPerson', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5174, '身份认领与解绑', 'icbc:platform:natural-person:manage', 3, 1, 5173, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
