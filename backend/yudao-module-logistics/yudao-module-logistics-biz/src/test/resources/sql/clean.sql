-- 每个单元测试结束后清理测试库（BaseDbUnitTest 的 @Sql 会执行本文件）。
--
-- 新增表的票必须在这里补一条 DELETE，否则表数据会跨测试残留
--（H2 的库名按测试上下文唯一，但同一上下文内的多个测试方法共用）。
DELETE FROM logistics_vehicle;
DELETE FROM logistics_driver;
