-- 反向开票（icbc）管理后台菜单
-- 幂等：先按固定 id 删除，再插入。
-- 组件路径对应前端 backend/yudao-ui/yudao-ui-admin-vue3/src/views/icbc/payee/index.vue

DELETE FROM `system_menu` WHERE `id` BETWEEN 5100 AND 5109;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(5100, '反向开票', '', 1, 60, 0, '/icbc', 'ep:tickets', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5101, '出售者档案', 'icbc:payee-info:query', 2, 1, 5100, 'payee', '', 'icbc/payee/index', 'IcbcPayee', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5102, '出售者新增', 'icbc:payee-info:create', 3, 1, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5103, '出售者修改', 'icbc:payee-info:update', 3, 2, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5104, '出售者删除', 'icbc:payee-info:delete', 3, 3, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5105, '出售者导出', 'icbc:payee-info:export', 3, 4, 5101, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
