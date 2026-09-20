-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- 回收企业经营作业系统的管理后台菜单骨架（#41 T03）。
--
-- 本文件是菜单的最终形态，排在 `ruoyi-vue-pro.sql` 之后导入：
--   1. 清场：删掉已禁用模块（报表 / 商城 / CRM / 公众号 / AI / IoT）与脚手架
--      （支付 / 工作流程 / 会员中心）的死菜单、三条外链（作者动态 / Boot 文档 /
--      Cloud 文档）与演示菜单；ERP 系统那一棵先停用（status=1）保留行，等 #39 T01
--      放开 ERP、后续票把 stock 域收敛进「仓储管理」时再启用。
--   2. 骨架：一级菜单为 工作台 / 基础资料 / 交易对方 / 采购管理 / 回收作业 /
--      仓储管理 / 结算管理 / 财务票务 / 业务追溯 / 经营报表（外加系统管理、基础设施）。
--      既有 icbc 页面挂到对应骨架下；「租户开票就绪」一级菜单取消，配置项落「基础资料」。
--   3. 套餐：落一个「回收企业套餐」（system_tenant_package.id = 200），菜单集合覆盖骨架。
--
-- 幂等：先删所有 icbc 自有菜单（permission / component 前缀）与固定的 5100–5299 区间，再插入；
-- 死亡菜单用递归 CTE 找齐后删除；套餐先按 id 删再插入。组件路径对应前端
-- backend/yudao-ui/yudao-ui-admin-vue3/src/views/icbc/* 与 views/enterprise/*。

-- =====================================================================
-- 1. 菜单清场：删除已禁用模块 / 外链 / 演示菜单
-- =====================================================================
-- 先删关联，再删菜单。子节点靠递归 CTE 找齐（含按钮型权限行）。
CREATE TEMPORARY TABLE `tmp_dead_menu` (`id` bigint NOT NULL, PRIMARY KEY (`id`));
INSERT INTO `tmp_dead_menu` (`id`)
WITH RECURSIVE `tree` AS (
    SELECT `id` FROM `system_menu`
    WHERE `deleted` = 0 AND `id` IN (
        1254, -- 作者动态（演示 / 外链）
        2159, -- Boot 开发文档（外链）
        2160, -- Cloud 开发文档（外链）
        1070, -- 基础设施 / 代码生成案例（演示）
        1117, -- 支付管理（脚手架）
        1185, -- 工作流程（脚手架，回收域不用 BPM）
        1281, -- 报表管理（已禁用模块）
        2084, -- 公众号管理（已禁用模块）
        2262, -- 会员中心（已禁用模块）
        2362, -- 商城系统（已禁用模块）
        2397, -- CRM 系统（已禁用模块）
        2758, -- AI 大模型（已禁用模块）
        4000  -- IoT 物联网（已禁用模块）
    )
    UNION ALL
    SELECT `m`.`id` FROM `system_menu` `m` INNER JOIN `tree` `t` ON `m`.`parent_id` = `t`.`id`
    WHERE `m`.`deleted` = 0
)
SELECT `id` FROM `tree`;
DELETE FROM `system_role_menu` WHERE `menu_id` IN (SELECT `id` FROM `tmp_dead_menu`);
DELETE FROM `system_menu` WHERE `id` IN (SELECT `id` FROM `tmp_dead_menu`);
DROP TEMPORARY TABLE `tmp_dead_menu`;

-- ERP 系统那一棵（id 2563–2702）先整棵停用，不出现在任何租户下；
-- 菜单行与 `erp:*` 权限保留，供 T01 放开 ERP 后由后续票收敛进采购 / 仓储骨架。
UPDATE `system_menu` SET `status` = 1, `updater` = 'admin', `update_time` = NOW() WHERE `id` = 2563;

-- =====================================================================
-- 2. 骨架与 icbc 菜单
-- =====================================================================
-- 清掉所有 icbc 自有菜单（含历史增量 SQL 用自增 id 落下的、如 icbc_invoice_tables.sql / icbc-invoice-application.sql
-- 里的行），再按固定 id 重建，避免重复。父级目录没有 permission，用 component 前缀兵底。
DELETE FROM `system_role_menu` WHERE `menu_id` IN (SELECT `id` FROM `system_menu` WHERE `permission` LIKE 'icbc:%' OR `component` LIKE 'icbc/%');
DELETE FROM `system_menu` WHERE `permission` LIKE 'icbc:%' OR `component` LIKE 'icbc/%';
DELETE FROM `system_role_menu` WHERE `menu_id` BETWEEN 5100 AND 5299;
DELETE FROM `system_menu` WHERE `id` BETWEEN 5100 AND 5299;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
-- ===== 一级骨架 =====
(5200, '工作台', '', 2, 5, 0, '/workbench', 'ep:home-filled', 'icbc/workbench/index', 'IcbcWorkbench', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5201, '基础资料', '', 1, 30, 0, '/basedata', 'ep:notebook', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5202, '交易对方', '', 1, 40, 0, '/counterparty', 'ep:user', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5203, '采购管理', '', 2, 50, 0, '/purchase', 'ep:shopping-cart', 'icbc/purchase/index', 'IcbcPurchase', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5204, '回收作业', '', 1, 60, 0, '/recycling', 'ep:van', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5205, '仓储管理', '', 1, 70, 0, '/warehouse', 'ep:box', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5206, '结算管理', '', 1, 80, 0, '/settlement', 'ep:money', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5207, '财务票务', '', 1, 90, 0, '/finance', 'ep:tickets', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5208, '业务追溯', '', 1, 100, 0, '/trace', 'ep:connection', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5209, '经营报表', '', 2, 110, 0, '/report', 'ep:data-analysis', 'icbc/report/index', 'IcbcReport', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 平台运营：系统租户专有，不属回收企业套餐
(5140, '平台运营', '', 1, 120, 0, '/platform', 'ep:data-analysis', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),

-- ===== 基础资料 =====
(5115, '企业信息', 'enterprise:info:query', 2, 1, 5201, 'enterprise-info', '', 'enterprise/info/index', 'Info', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5116, '企业资质', 'enterprise:cert:query', 2, 2, 5201, 'enterprise-qualification', '', 'enterprise/qualification/index', 'EnterpriseQualification', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5122, '三层资质', 'icbc:qualification:query', 2, 3, 5201, 'qualification', '', 'icbc/qualification/index', 'IcbcQualification', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5123, '资质新增', 'icbc:qualification:create', 3, 1, 5122, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5124, '资质修改', 'icbc:qualification:update', 3, 2, 5122, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5125, '资质删除', 'icbc:qualification:delete', 3, 3, 5122, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5126, '编码配置', 'icbc:goods-config:query', 2, 4, 5201, 'goods-config', '', 'icbc/goodsConfig/index', 'IcbcGoodsConfig', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5127, '品类新增', 'icbc:goods-config:create', 3, 1, 5126, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5128, '品类修改', 'icbc:goods-config:update', 3, 2, 5126, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5129, '品类删除', 'icbc:goods-config:delete', 3, 3, 5126, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5117, '开票就绪自检', 'icbc:test:query', 2, 5, 5201, 'self-check', '', 'icbc/readiness/index', 'IcbcReadiness', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5177, '场站', 'icbc:station:query', 2, 6, 5201, 'station', '', 'icbc/station/index', 'IcbcStation', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5178, '维护场站', 'icbc:station:manage', 3, 1, 5177, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5106, '付方档案', 'icbc:payer-info:query', 2, 7, 5201, 'payer', '', 'icbc/payer/index', 'IcbcPayer', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5107, '付方新增', 'icbc:payer-info:create', 3, 1, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5108, '付方修改', 'icbc:payer-info:update', 3, 2, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5109, '付方删除', 'icbc:payer-info:delete', 3, 3, 5106, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),

-- ===== 交易对方 =====
(5101, '出售者档案', 'icbc:payee-info:query', 2, 1, 5202, 'payee', '', 'icbc/payee/index', 'IcbcPayee', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5102, '出售者新增', 'icbc:payee-info:create', 3, 1, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5103, '出售者修改', 'icbc:payee-info:update', 3, 2, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5104, '出售者删除', 'icbc:payee-info:delete', 3, 3, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5105, '出售者导出', 'icbc:payee-info:export', 3, 4, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5133, '出售者建档', 'icbc:seller-onboarding:execute', 2, 2, 5202, 'payee-onboarding', '', 'icbc/payeeOnboarding/index', 'IcbcPayeeOnboarding', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5134, '框架收购协议', 'icbc:seller-agreement:manage', 3, 1, 5133, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5135, '首次授权', 'icbc:seller-authorization:manage', 3, 2, 5133, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5130, '企业授权', 'icbc:enterprise-auth:query', 2, 3, 5202, 'enterprise-auth', '', 'icbc/enterpriseAuth/index', 'IcbcEnterpriseAuth', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5131, '发起授权', 'icbc:enterprise-auth:init', 3, 1, 5130, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5132, '回填授权结果', 'icbc:enterprise-auth:update', 3, 2, 5130, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5181, '触达记录', 'icbc:seller-notify:query', 2, 4, 5202, 'seller-notify', '', 'icbc/sellerNotify/index', 'IcbcSellerNotify', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5182, '转达与短信开关', 'icbc:seller-notify:manage', 3, 1, 5181, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 单位供货方（#44 T06）：与自然人出售者并列的第二种交易对方，档案落 ERP 供应商表，
-- 权限字符串是 erp:supplier:*，页面在 views/erp/purchase/supplier 下。
(5193, '单位供货方', 'erp:supplier:query', 2, 5, 5202, 'supplier', '', 'erp/purchase/supplier/index', 'ErpSupplier', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5194, '单位供货方新增', 'erp:supplier:create', 3, 1, 5193, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5195, '单位供货方修改', 'erp:supplier:update', 3, 2, 5193, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5196, '单位供货方删除', 'erp:supplier:delete', 3, 3, 5193, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5197, '单位供货方导出', 'erp:supplier:export', 3, 4, 5193, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),

-- ===== 回收作业 =====
(5179, '到站预约', 'icbc:appointment:query', 2, 1, 5204, 'appointment', '', 'icbc/appointment/index', 'IcbcAppointment', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5180, '标记到场与未到场', 'icbc:appointment:manage', 3, 1, 5179, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5136, '收购登记', 'icbc:acquisition:query', 2, 2, 5204, 'acquisition', '', 'icbc/acquisition/index', 'IcbcAcquisition', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5137, '登记收购', 'icbc:acquisition:create', 3, 1, 5136, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5138, '修正识别结果', 'icbc:acquisition:update', 3, 2, 5136, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5139, '导出收购确认书', 'icbc:acquisition:export', 3, 3, 5136, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
-- 交接批次与有效磅次（#50 T12）：一个交易对方的一次物理交接记为一个批次；过磅保留每一次原始读数，
-- 只有被选定的那一次参与计量，其余留档不参与。同一车同一天两次送货是两个批次（不去重）。
(5240, '交接批次', 'icbc:handover-batch:query', 2, 3, 5204, 'handover-batch', '', 'icbc/handoverBatch/index', 'IcbcHandoverBatch', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5241, '登记交接批次', 'icbc:handover-batch:manage', 3, 1, 5240, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5242, '新增磅次', 'icbc:handover-batch:manage', 3, 2, 5240, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5243, '指定有效磅次', 'icbc:handover-batch:manage', 3, 3, 5240, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),

-- ===== 仓储管理 =====
-- 库位 / 批次 / 库存查询落 ERP 的 stock 域（ADR 0027）：权限字符串是 erp:*，页面在 views/erp/stock 下；
-- 菜单挂到回收企业骨架「仓储管理」（5205），随套餐递归进回收企业套餐。
(5183, '库位维护', 'erp:stock-location:query', 2, 1, 5205, 'location', '', 'erp/stock/location/index', 'ErpStockLocation', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5184, '库位新增', 'erp:stock-location:create', 3, 1, 5183, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5185, '库位修改', 'erp:stock-location:update', 3, 2, 5183, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5186, '库位删除', 'erp:stock-location:delete', 3, 3, 5183, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5187, '批次维护', 'erp:stock-batch:query', 2, 2, 5205, 'batch', '', 'erp/stock/batch/index', 'ErpStockBatch', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5188, '批次新增', 'erp:stock-batch:create', 3, 1, 5187, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5189, '批次修改', 'erp:stock-batch:update', 3, 2, 5187, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5190, '批次删除', 'erp:stock-batch:delete', 3, 3, 5187, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5191, '库存查询', 'erp:stock:query', 2, 3, 5205, 'inventory', '', 'erp/stock/stock/index', 'ErpStock', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5192, '库存导出', 'erp:stock:export', 3, 1, 5191, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),

-- ===== 结算管理 =====
(5175, '结算单', 'icbc:settlement-confirm:query', 2, 1, 5206, 'list', '', 'icbc/settlement/index', 'IcbcSettlement', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5176, '生成与处理异议', 'icbc:settlement-confirm:manage', 3, 1, 5175, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),

-- ===== 财务票务 =====
(5110, '开票申请', 'icbc:invoice-order:query', 2, 1, 5207, 'invoice', '', 'icbc/invoice/index', 'IcbcInvoice', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5111, '发起开票', 'icbc:invoice-order:create', 3, 1, 5110, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5156, '开票申请（按收购单）', 'icbc:invoice-application:query', 2, 2, 5207, 'invoice-application', '', 'icbc/invoiceApplication/index', 'IcbcInvoiceApplication', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5157, '发起开票申请', 'icbc:invoice-application:apply', 3, 1, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5158, '发起红冲', 'icbc:red-invoice:apply', 3, 2, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5159, '撤销红字确认单', 'icbc:red-invoice:revoke', 3, 3, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5160, '取消预开票', 'icbc:invoice-order:cancel', 3, 4, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5161, '查询红冲', 'icbc:red-invoice:query', 3, 5, 5156, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5112, '付款', 'icbc:payment:query', 2, 3, 5207, 'payment', '', 'icbc/payment/index', 'IcbcPayment', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5113, '发起付款', 'icbc:payment:create', 3, 1, 5112, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5118, '发票下载与证据', 'icbc:invoice-download:query', 2, 4, 5207, 'download', '', 'icbc/download/index', 'IcbcDownload', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5119, '执行下载', 'icbc:invoice-download:download', 3, 1, 5118, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5120, '重试下载', 'icbc:invoice-download:retry', 3, 2, 5118, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5121, '下载文件', 'icbc:invoice-download:download-file', 3, 3, 5118, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5164, '代办税费申报', 'icbc:tax-declaration:query', 2, 5, 5207, 'tax', '', 'icbc/tax/index', 'IcbcTax', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5165, '生成申报清单', 'icbc:tax-declaration:manage', 3, 1, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5166, '报送报告表', 'icbc:tax-declaration:manage', 3, 2, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5167, '缴款归档', 'icbc:tax-declaration:manage', 3, 3, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5168, '登记补缴', 'icbc:tax-declaration:manage', 3, 4, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5169, '缴清补缴', 'icbc:tax-supplement:manage', 3, 5, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5170, '查看汇算清缴', 'icbc:settlement:query', 3, 6, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5171, '生成汇算提醒', 'icbc:settlement:remind', 3, 7, 5164, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5162, '额度台账', 'icbc:quota:query', 2, 6, 5207, 'quota', '', 'icbc/quota/index', 'IcbcQuota', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5163, '处理引导', 'icbc:quota:guidance:handle', 3, 1, 5162, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),

-- ===== 业务追溯 =====
(5151, '一票一档', 'icbc:evidence:query', 2, 1, 5208, 'evidence', '', 'icbc/evidence/index', 'IcbcEvidence', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5152, '补录证据', 'icbc:evidence:attach', 3, 1, 5151, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5153, '删除证据', 'icbc:evidence:delete', 3, 2, 5151, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5154, '导出证据包与台账', 'icbc:evidence:export', 3, 3, 5151, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5155, '生成公开令牌', 'icbc:public-token:create', 3, 4, 5151, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),

-- ===== 平台运营（系统租户专有）=====
(5141, '资质核实', 'icbc:platform:qualification:query', 2, 1, 5140, 'qualification', '', 'icbc/platformQualification/index', 'IcbcPlatformQualification', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5142, '核实资质', 'icbc:platform:qualification:audit', 3, 1, 5141, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5143, '报废产品编码表', 'icbc:scrap-code:query', 2, 2, 5140, 'scrap-code', '', 'icbc/scrapCode/index', 'IcbcScrapCode', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5144, '编码新增', 'icbc:scrap-code:create', 3, 1, 5143, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5145, '编码修改', 'icbc:scrap-code:update', 3, 2, 5143, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5146, '编码删除', 'icbc:scrap-code:delete', 3, 3, 5143, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5147, '通知监控', 'icbc:platform:callback:query', 2, 3, 5140, 'callback', '', 'icbc/platformCallback/index', 'IcbcPlatformCallback', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5148, '重放通知', 'icbc:platform:callback:retry', 3, 1, 5147, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5149, '全平台证据与异常票', 'icbc:platform:evidence:query', 2, 4, 5140, 'evidence', '', 'icbc/platformEvidence/index', 'IcbcPlatformEvidence', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5150, '计费计量', 'icbc:platform:billing:query', 2, 5, 5140, 'billing', '', 'icbc/platformBilling/index', 'IcbcPlatformBilling', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5172, '重新计量', 'icbc:platform:billing:manage', 3, 1, 5150, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5173, '自然人主体', 'icbc:platform:natural-person:query', 2, 6, 5140, 'natural-person', '', 'icbc/naturalPerson/index', 'IcbcNaturalPerson', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5174, '身份认领与解绑', 'icbc:platform:natural-person:manage', 3, 1, 5173, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- =====================================================================
-- 3. 回收企业租户套餐
-- =====================================================================
-- menu_ids 覆盖骨架全部节点（含子孙与按钮权限），外加系统管理 / 基础设施；
-- 不含平台运营（那是系统租户自己的前台）。
SET SESSION group_concat_max_len = 1048576;

DELETE FROM `system_tenant_package` WHERE `id` = 200;
INSERT INTO `system_tenant_package`
(`id`, `name`, `status`, `remark`, `menu_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
    200,
    '回收企业套餐',
    0,
    '回收企业经营作业系统骨架：工作台 / 基础资料 / 交易对方 / 采购管理 / 回收作业 / 仓储管理 / 结算管理 / 财务票务 / 业务追溯 / 经营报表（外加系统管理与基础设施）。',
    CONCAT('[', GROUP_CONCAT(`id` ORDER BY `id`), ']'),
    'admin', NOW(), 'admin', NOW(), b'0'
FROM (
    WITH RECURSIVE `tree` AS (
        SELECT `id` FROM `system_menu`
        WHERE `deleted` = 0 AND `id` IN (1, 2, 5200, 5201, 5202, 5203, 5204, 5205, 5206, 5207, 5208, 5209)
        UNION ALL
        SELECT `m`.`id` FROM `system_menu` `m` INNER JOIN `tree` `t` ON `m`.`parent_id` = `t`.`id`
        WHERE `m`.`deleted` = 0
    )
    SELECT `id` FROM `tree`
) AS `pkg_menu`;
