-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- ⚠️ 必须先指定数据库！用法：
--     mysql -uroot -p <库名> < icbc-api-log-menu-revoke.sql
--     docker exec -i <mysql容器> mysql -uroot -p <库名> < icbc-api-log-menu-revoke.sql
--
-- 为什么强调：本文件靠 `system_tenant` 关联决定删哪些行，**不带库名**时 `system_role_menu` / `system_tenant`
-- 都解析不到，MySQL 会报 1046（No database selected）——它不会静默误删，只会失败。这里加一道与
-- icbc-payee-unique-key.sql 同款的硬失败守卫，把「必须带库名」的跑法摆在文件头，也避免将来被改成
-- 依赖 information_schema 的写法时重蹈「不带库名就静默走 SELECT 1」的覆辙。
-- ========================================
SET @guard := IF(DATABASE() IS NULL,
    'SELECT 1 FROM `__请指定数据库：mysql -uroot -p 库名 < 本文件` . `t`',
    'DO 0');
PREPARE guard_stmt FROM @guard; EXECUTE guard_stmt; DEALLOCATE PREPARE guard_stmt;

-- ========================================
-- 收回「API 日志」授权（#102，D 边的**既存租户**补救）
--
-- 背景（#102 的验收答案，也是必须写出来的限制）：
--   可见性由 `system_role_menu` 决定，不是运行时读套餐。`icbc-menu.sql` 只重建
--   `system_tenant_package.id = 200` 的 `menu_ids` —— 新租户开箱即得正确集合，
--   但**不重算既存租户的 `system_role_menu`**：那些行还在，已经开出来的回收企业租户的
--   admin 仍然看得见 `infra:api-access-log:*` / `infra:api-error-log:*`。本文件补这一步。
--
-- 跑法：
--   1. 先重导 `backend/sql/mysql/icbc-menu.sql`（让套餐 200 的 `menu_ids` 本身不含这 8 个 id）。
--      不先做这步的话，下一次任何触发 `updateTenantRoleMenu` 的动作（后台改套餐、改角色菜单）
--      都会按套餐把授权**重新加回来**。
--   2. 执行本文件。第 1 段是预览（每个租户将删多少行，含软删行，与第 2 段的 DELETE 口径一致），
--      第 2 段删除并打印实际删除行数（`ROW_COUNT()`），第 3 段复核（套餐 200 的租户应当剩 0 行）。
--   3. 收接口权限缓存（见下「Redis 权限缓存」——**不做这步接口最坏还能用 1 小时**）。
--   4. 让租户 admin 退出重新登录（菜单树另缓在 localStorage 的 `roleRouters`，不重登页面还在）。
--
-- 影响面（这是共享表，逐条说清）：
--   * 只删 `system_role_menu`（角色-菜单关联）行；不删 `system_menu` 行、不改 `system_tenant_package`、
--     不动任何业务表。删的是物理行（不是 `deleted = 1`），与 `icbc-menu.sql` 的做法一致，**不可逆**。
--   * 作用域**只限套餐 200 的租户**（`system_tenant.package_id = 200`）：
--     系统租户（`package_id = 0`，平台运营）与其它套餐的租户**一行都不动** —— 平台运营必须留着这两个页面。
--     这里**不过滤 `system_tenant.deleted`**：已软删的租户也一并清（本地实测里就有这么一户，
--     `deleted = 1`）。它们眼下不生效，但将来被恢复时不该默默重新看到这两个页面；多清一行无副作用。
--   * 被删的授权对套餐 200 租户内的**所有角色**生效（含租户管理员与自建角色）：这正是「收口到平台运营」
--     的目的。以后要给某个租户开，走「后台改套餐 / 改角色菜单」重新授权，不要指望本文件回滚。
--
-- 幂等：删「存在才删」的行，重复跑第二次影响 0 行。
-- 新库：直接导 `icbc-menu.sql` 即可（那之后建的租户拿到的 `system_role_menu` 本身就不含这 8 个 id），
--       不需要跑本文件。
--
-- ============================================================================
-- Redis 权限缓存（**本脚本不 evict，这是它的实际边界，别读成「重登即可」**）
-- ============================================================================
-- 判权不只读 `system_role_menu`，还读两层 Redis 缓存（`PermissionServiceImpl.hasAnyPermission`）：
--   * `menu_role_ids:<tenantId>:<menuId>` —— 拥有菜单的角色集合。租户维度（`menu_role_ids` 不在
--     `application.yaml` 的 `ignore-caches` 里，`TenantRedisCacheManager` 会拼租户后缀），
--     由 `PermissionServiceImpl#getMenuRoleIdListByMenuIdFromCache` 的 `@Cacheable` 写入。
--   * `permission_menu_ids:<permission>` —— 权限标识对应的菜单集合。全局（在 `ignore-caches` 里，无租户后缀），
--     由 `MenuServiceImpl#getMenuIdListByPermissionFromCache` 写入。
-- 两者 TTL 都是 `1h`（`yudao-server/.../application.yaml` 的 `spring.cache.redis.time-to-live`）。
-- **本文件只删 MySQL 行，不 evict 这两层缓存**：删完后最坏的 1 小时内，页面（前端路由）退了，
-- 但租户 admin 仍能直接调 `/admin-api/infra/api-error-log/page`（`@ss.hasPermission` 缓存命中仍放行）。
--
-- 清缓存（可核对；本地是 docker-compose 的 16382 端口，生产按实际端口 / 库号）：
--   redis-cli -p 16382 --scan --pattern 'menu_role_ids:*'        # 先看条数
--   redis-cli -p 16382 --scan --pattern 'menu_role_ids:*' | xargs -r redis-cli -p 16382 DEL
--   redis-cli -p 16382 --scan --pattern 'permission_menu_ids:*' | xargs -r redis-cli -p 16382 DEL
--   两条 --scan 再跑一次应当看不到 key；或等 TTL（≤1h）自然过期。
--   **重启应用不清 Redis**，别把重启当清缓存。
-- 更省事的替代：改走后台编辑套餐 200 / 编辑租户管理员角色菜单并保存，会触发
--   `PermissionServiceImpl.assignRoleMenu`（带 `@CacheEvict(allEntries = true)` 清上面两层），无需手工 redis-cli。
--   注意 `updateTenantPackage` 只在套餐菜单确实变化时才触发 `updateTenantRoleMenu`（原样保存不算）。
-- ============================================================================
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

-- 1. 预览：套餐 200 的租户各自还有多少行「API 日志」授权（租户管理员角色各 8 行；若有自建角色也被
--    授过这 8 个菜单，会更多）。**这里故意不只筛 `deleted = 0`**：第 2 段的 DELETE 会连软删行一起物理删，
--    预览数要与实际删除的 `revoked_rows` 对得上（以前预览带 `deleted = 0`、DELETE 不带，两个数会对不上）。
SELECT `n`.`id` AS `tenant_id`, `n`.`name` AS `tenant_name`, COUNT(*) AS `revoke_rows`
FROM `system_role_menu` `rm`
INNER JOIN `system_tenant` `n`
    ON `n`.`id` = `rm`.`tenant_id` AND `n`.`package_id` = 200
WHERE `rm`.`menu_id` IN (1078, 1082, 1083, 1084, 1085, 1086, 1088, 1089)
GROUP BY `n`.`id`, `n`.`name`
ORDER BY `n`.`id`;

-- 2. 收回授权（幂等；`ROW_COUNT()` 是本次实际删除的行数）
DELETE `rm`
FROM `system_role_menu` `rm`
INNER JOIN `system_tenant` `n`
    ON `n`.`id` = `rm`.`tenant_id` AND `n`.`package_id` = 200
WHERE `rm`.`menu_id` IN (1078, 1082, 1083, 1084, 1085, 1086, 1088, 1089);
SELECT ROW_COUNT() AS `revoked_rows`;

-- 3. 复核：套餐 200 的租户应当一行都不剩（预期 0；口径同第 2 段，含软删行）
SELECT COUNT(*) AS `remaining_api_log_rows`
FROM `system_role_menu` `rm`
INNER JOIN `system_tenant` `n`
    ON `n`.`id` = `rm`.`tenant_id` AND `n`.`package_id` = 200
WHERE `rm`.`menu_id` IN (1078, 1082, 1083, 1084, 1085, 1086, 1088, 1089);
