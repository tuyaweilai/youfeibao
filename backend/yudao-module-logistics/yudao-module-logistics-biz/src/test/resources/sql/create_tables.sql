-- 物流模块的测试建表脚本（H2，MODE=MYSQL）。
--
-- #68（V1）只立模块骨架，还没有业务表。车辆、司机、承运商与承运合同、运输任务与节点、
-- 运费对账的建表语句由 #69（V2 一趟活跑通）起逐票追加到这里，并与 backend/sql/mysql/ 下的
-- 增量脚本保持同步（先有测试表，再有真实表）。
--
-- 下面这条 SELECT 是**占位**，不是业务语句：Spring 的脚本执行器会拒绝只含注释的 SQL 文件
-- （报 'script' must not be null or empty），而 UnitTestConfiguration 必须加载本文件来证明
-- 测试底座可用。**首次真实建表时把这条 SELECT 删掉**，它没有别的作用。
SELECT 1;
