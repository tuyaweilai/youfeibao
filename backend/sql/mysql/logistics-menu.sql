-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 物流域的菜单种子（V2a #77，见 ADR 0032 与 CONTEXT.md「物流与运力」）
--
-- 落一级「物流管理」及其页面菜单，并把它追加进「回收企业套餐」（system_tenant_package.id = 200）。
-- 按钮型权限行**不在这里**：它们由 LogisticsPermissionSyncService 依据 LogisticsRoleEnum
-- 幂等生成（ADR 0026 的物流侧实现），本文件只负责「能被点开的页面」。
--
-- **为什么不用固定 ID 段**（与 icbc 不同）：icbc-menu.sql 用 5100–5399 的固定段，但
-- system_menu 里还有大量**自增 id** 的权限行（同步服务插的）。实测本地库
-- MAX(id) = 5328 而 AUTO_INCREMENT = 5210——显式 id 插入并不推高 InnoDB 的计数器，
-- 于是任何固定段都可能与下一次自增插入撞号。物流是后加的一棵树，因此改为：
--   按标记删（permission LIKE 'logistics:%' OR component LIKE 'logistics/%'）→ 自增插 →
--   用 LAST_INSERT_ID() / 变量串起父子 → 把子树 id 并进租户套餐。可重复导入。
--
-- 幂等：重复导入只会重建自己的这棵树（先按标记把整棵旧树连同角色关联删掉）；
-- 套餐菜单用 JSON 去重、并丢掉已在 system_menu 里不存在的 id 后整体写回。
-- ========================================

-- ---------------------------------------------------------------------
-- 1. 先认领旧树：一级菜单的 permission 是空串、component 是 NULL，
--    光靠 permission / component 会漏掉根节点（漏掉就会越导越多一棵）。
--    而且**必须从全部物流根展开**：历史上重复导入留下的多个根都在 path='/logistics' 下，
--    只认领一个根就会把其它的留在库里，变成越导越多。这样写也顺带能自愈已脏的环境。
-- ---------------------------------------------------------------------
CREATE TEMPORARY TABLE `tmp_logistics_menu` (`id` bigint NOT NULL, PRIMARY KEY (`id`));
INSERT INTO `tmp_logistics_menu` (`id`)
WITH RECURSIVE `old_tree` AS (
    SELECT `m`.`id` FROM `system_menu` `m`
    WHERE `m`.`deleted` = 0 AND `m`.`parent_id` = 0 AND `m`.`path` = '/logistics'
    UNION ALL
    SELECT `c`.`id` FROM `system_menu` `c` INNER JOIN `old_tree` `t` ON `c`.`parent_id` = `t`.`id`
    WHERE `c`.`deleted` = 0
)
SELECT `id` FROM `old_tree`;
-- 同步服务插的按钮权限行带 logistics: 前缀（component 为空，不一定在旧树里）
INSERT IGNORE INTO `tmp_logistics_menu` (`id`)
SELECT `id` FROM `system_menu`
WHERE `permission` LIKE 'logistics:%' OR `component` LIKE 'logistics/%';

DELETE FROM `system_role_menu` WHERE `menu_id` IN (SELECT `id` FROM `tmp_logistics_menu`);
DELETE FROM `system_menu` WHERE `id` IN (SELECT `id` FROM `tmp_logistics_menu`);
DROP TEMPORARY TABLE `tmp_logistics_menu`;

-- ---------------------------------------------------------------------
-- 2. 一级「物流管理」与页面菜单
--    排序放在「回收作业」(60) 与「仓储管理」(70) 之间：派车是收货之前的事
-- ---------------------------------------------------------------------
INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
('物流管理', '', 1, 65, 0, '/logistics', 'ep:van', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @logistics_root_id = LAST_INSERT_ID();

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
('车辆档案', '', 2, 10, @logistics_root_id, 'vehicle', 'ep:truck', 'logistics/vehicle/index', 'LogisticsVehicle',
 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('司机档案', '', 2, 20, @logistics_root_id, 'driver', 'ep:user', 'logistics/driver/index', 'LogisticsDriver',
 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- ---------------------------------------------------------------------
-- 3. 把物流菜单子树并进「回收企业套餐」
--    用 JSON 去重后整体写回，重复导入不产生重复 id，也不动 icbc 已有的部分
-- ---------------------------------------------------------------------
SET @logistics_menu_ids = (
    WITH RECURSIVE `tree` AS (
        SELECT `id` FROM `system_menu` WHERE `deleted` = 0 AND `id` = @logistics_root_id
        UNION ALL
        SELECT `m`.`id` FROM `system_menu` `m` INNER JOIN `tree` `t` ON `m`.`parent_id` = `t`.`id`
        WHERE `m`.`deleted` = 0
    )
    SELECT GROUP_CONCAT(`id` ORDER BY `id`) FROM `tree`
);

SET SESSION group_concat_max_len = 1048576;
SET @package_menu_ids = (SELECT `menu_ids` FROM `system_tenant_package` WHERE `id` = 200);

-- 套餐不存在时（本地库还没导 icbc-menu.sql）跳过：置 NULL 后 UPDATE 不命中任何行。
-- 已经不在 system_menu 里的 id 要丢掉：否则重复导入（菜单 id 每次都新分配）会让套餐
-- 越积越多死 id；这也让本文件顺带能自愈历史遗留的悬挂 id。
SET @merged_menu_ids = IF(@package_menu_ids IS NULL, NULL, (
    SELECT CONCAT('[', GROUP_CONCAT(DISTINCT `id` ORDER BY `id`), ']')
    FROM (
        SELECT `m`.`id` AS `id`
        FROM JSON_TABLE(@package_menu_ids, '$[*]' COLUMNS (`menu_id` INT PATH '$')) AS `jt`
        INNER JOIN `system_menu` `m` ON `m`.`id` = `jt`.`menu_id` AND `m`.`deleted` = 0
        UNION
        SELECT CAST(`jt2`.`id` AS UNSIGNED) AS `id`
        FROM JSON_TABLE(CONCAT('[', @logistics_menu_ids, ']'), '$[*]' COLUMNS (`id` INT PATH '$')) AS `jt2`
    ) AS `merged`
));

UPDATE `system_tenant_package`
SET `menu_ids` = @merged_menu_ids, `updater` = 'admin', `update_time` = NOW()
WHERE `id` = 200 AND @merged_menu_ids IS NOT NULL;
