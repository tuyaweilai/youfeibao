# 本地开发库：建表与种子数据（MySQL 8）

用于本地把后端跑起来。目标库名 `ruoyi-vue-pro`。

## 导入顺序

1. `ruoyi-vue-pro.sql` —— yudao 全量建表 + 演示数据（管理员、菜单、租户等）。**ERP 的菜单（id 2563–2702）与 `erp:*` 权限也在其中**
2. `erp.sql` —— ERP 建表（33 张表，来自上游导出，已删演示数据）。没建这几张表时，放开 `yudao-module-erp` 后任何 ERP 页面一查库就报错
2b. `erp-stock-goods-config.sql` —— T04（#42）：12 张表 `product_id`/`product_unit_id` → `goods_config_id`、`erp_warehouse.station_id`、`erp_stock` 唯一约束，并删除 `erp_product*` 三张表。必须在 `erp.sql` 之后
2c. `erp-stock-location-batch.sql` —— T05（#43）：库位（`erp_stock_location`）与批次（`erp_stock_batch`）两张表；`erp_stock` / `erp_stock_record` 加 `location_id` / `batch_id`，`erp_stock` 唯一约束扩成 `(goods_config_id, warehouse_id, location_id, batch_id)`。必须在 `erp-stock-goods-config.sql` 之后
2d. `erp-supplier.sql` —— T06（#44）：单位供货方档案，给 `erp_supplier` 补 `subject_type`（主体类型六态）/ `taxpayer_qualification`（纳税人资格）/ `address`。必须在 `erp.sql` 之后
3. `quartz.sql` —— Quartz 调度表
4. `02-initial-data.sql`、`yudao-module-enterprise-auth-flow.sql`、`member-2024-01-18.sql` —— 模块补充
5. `icbc_*.sql` —— 反向开票（工行）业务表
6. `enterprise.sql` —— 企业管理（企业信息 / 资质 / 门店 / 用户关系）表
7. `icbc-readiness.sql` —— 租户开票就绪：三层资质、编码配置、企业授权表；平台级报废产品编码表；到期预警表
8. `icbc-evidence.sql` —— 一票一档证据表（人工补录的合同流 / 货物流证据）
9. `icbc-public-token.sql` —— 公开令牌表与收方入驻失败留联系方式表（自然人免登录端点）
10. `icbc-jobs.sql` —— icbc 定时任务种子（资质到期提醒）
11. `icbc-seller-onboarding.sql` —— 出售者建档：框架收购协议、首次授权表（实人认证 / 收方入驻字段已并入 `icbc_payee_info.sql`）
12. `icbc-acquisition.sql` —— 收购登记单（合同流 / 货物流 / 信息流骨架）
13. `icbc-invoice-application.sql`、`icbc-payment.sql`、`icbc-invoice-issuance.sql` —— 开票申请、付款、开票 / 缴税 / 上传状态的增量字段
14. `icbc-red-invoice.sql` —— 红字发票（红冲）表
15. `icbc-quota.sql` —— 额度风控：开票订单适用征收率、出售者额度超限的经营主体登记引导表
16. `icbc-billing.sql` —— 平台计费计量台账（成功开具张数 / 红冲扣减 / 应计费用）
17. `icbc-natural-person.sql` —— 自然人身份层：平台级自然人主体表、自然人主体与登录凭证绑定表、收方档案挂靠与既有数据回填、通知幂等键加宽（#31）
18. `icbc-settlement.sql` —— 结算单与版本快照（#33）：一次到场批次一张结算单、确认留痕与线下签字、异议与版本留痕
19. `icbc-station.sql` —— 场站与场站二维码（#34）：一码一场站，码内不带令牌，只编码场站码；公开信息读取限流
20. `icbc-seller-portal.sql` —— 自然人端（#34）：企业授权自助撤销留痕、收款记录「我收到了」自行确认列
21. `icbc-appointment.sql` —— 到站预约（#35）：预约不是订单，不占额度、不产生开票、不进五流
22. `icbc-seller-notify.sql` —— 出售者触达（#36）：短信三条 + 收货员转达；触达记录与租户级短信开关表、短信模板
22b. `icbc-purchase-contract.sql` —— 采购合同（#45 T07，ADR 0027）：合同主体、适用品类与版本快照三张表。采购履约链建在 `icbc` 模块（#38 规格第 1 条），对手方用「主体类型六态 + 双可空 id」承载自然人出售者与单位供货方
22c. `icbc-handover-batch.sql` —— 交接批次与有效磅次（#50 T12）：`icbc_handover_batch`（一次物理交接一个批次）+ `icbc_weighing`（每次过磅一条原始读数，只有被选定的那一次参与计量）；`icbc_acquisition` 加 `handover_batch_id` / `weighing_id` / `weighing_seq_no`（计量结果引用有效磅次的值与版本）。必须在 `icbc-acquisition.sql` 之后
22d. `icbc-purchase-order.sql` —— 采购订单（#46 T08，ADR 0027）：订单主体、品类明细、交货日价格表、成交价格快照四张表。采购执行依据（一个合同 → 多个订单 → 多次收货），对手方同样用「主体类型六态 + 双可空 id」；合同关联走 `PurchaseContractService` 门禁，可选执行场站
22e. `icbc-input-invoice.sql` —— 进项收票登记与勾稽（#49 T11，ADR 0029）：`icbc_input_invoice`（单位供货方开给回收企业的进项票，按「销方 + 发票号码」唯一）+ `icbc_input_invoice_link`（通用勾稽关联表：`biz_type` / `biz_id` / `biz_no` / `biz_amount` / `linked_amount`）。自然人出售者不在本链路（他们走反向开票）
22f. `icbc-purchase-order-progress.sql` —— 采购订单履约五口径与执行进度（#47 T09，ADRs 0027/0028）：`icbc_purchase_setting`（租户级单行：完成比例采用哪个口径、超量 / 过期 / 跨场站交货按拦截还是提交授权审核）+ `icbc_purchase_exception`（履约异常授权单：提交 → 审核 → 已通过的授权成为交货门禁的放行依据，只放宽被授权的那一件事）。五口径的取数在 `PurchaseOrderService#getProgress` 一处；入库口径等 #52 的入库单落地后接入，现在标「待接入」不出数字
22g. `icbc-acquisition-purchase-link.sql` —— 收购单关联采购安排与「直接收购」（#51 T13，ADR 0027）：`icbc_acquisition` 加 `purchase_order_id` / `purchase_order_item_id`（`NOT NULL DEFAULT 0`，0 = 未关联即「直接收购」）。必须在 `icbc-acquisition.sql` 与 `icbc-purchase-order.sql` 之后
22h. `icbc-stock-in.sql` —— 待入库 → 入库单 → 库存流水（#52 T14，ADR 0027）：`icbc_stock_in`（入库单：待过账 / 已过账 / 已作废；只有过账才经 `StockApi` 写 `RECEIPT_IN(90)`）与 `icbc_stock_in_item`（明细：仓库 / 库位 / 批次 + 数量，一张收购单可拆多个库位、分多次入库）。icbc 不直接碰 `erp_stock*`；可入库实物量的唯一取数点在 `StockInService#resolveAvailableQuantity`
22i. `icbc-acquisition-acceptance.sql` —— 收购接收结论与称量差异（#53 T15，ADR 0028）：`icbc_acquisition` 加 `accepted_weight`（接收量）/ `rejected_weight`（退回量）/ `residual_weight`（余货出场量）/ `reject_reason`（拒收原因）/ `weight_diff`（称量差异 = 实物量 − 结算重量，不静默抹平）。拒收部分不进应付、不进正常库存。必须在 `icbc-acquisition.sql` 之后
22j. `icbc-stock-ops.sql` —— 非销售出库 / 跨仓调拨 / 盘点调整 / 期初导入（#54 T16，ADR 0025 / 0027）：`icbc_stock_out`（报损 / 退货出库 / 内部领用，不挂客户）+ `icbc_stock_move`（源减目标加）+ `icbc_stock_check`（把余额对齐到实盘数）+ `icbc_stock_opening`（一个维度一行，导入即过账）各带明细。四类都只经 `StockApi` 写 `erp_stock*`，icbc 不直接碰库存表；「当前库存」的前提是四项能力齐备**且已导期初**
23. `icbc-menu.sql` —— 菜单清场与租户套餐骨架（#41）：删掉已禁用模块 / 外链 / 演示菜单，停用待启用的 ERP 菜单树，落 工作台 / 基础资料 / 交易对方 / 采购管理 / 回收作业 / 仓储管理 / 结算管理 / 财务票务 / 业务追溯 / 经营报表 一级骨架并把既有 icbc 页面挂进去，再落「回收企业套餐」（`system_tenant_package.id = 200`）。幂等，**必须最后导**（依赖前面所有 `system_menu` / `system_tenant_package` 种子）

> enterprise 的菜单与字典已包含在 `ruoyi-vue-pro.sql` 中，不要再单独导入 `enterprise-menu.sql` / `module-enterprise-dict.sql`（会主键冲突）。

## 一键导入

```bash
cd backend/sql/mysql
MYSQL="mysql -h 127.0.0.1 -P 13308 -uroot -p --default-character-set=utf8mb4"
$MYSQL -e "CREATE DATABASE IF NOT EXISTS \`ruoyi-vue-pro\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
for f in ruoyi-vue-pro erp erp-stock-goods-config erp-stock-location-batch erp-supplier quartz 02-initial-data yudao-module-enterprise-auth-flow member-2024-01-18 \
         icbc_payee_info icbc_payer_info icbc_invoice_tables icbc_payment_order \
         icbc_invoice_download icbc_api_log_callback enterprise icbc-readiness icbc-evidence icbc-public-token icbc-jobs icbc-seller-onboarding icbc-acquisition icbc-invoice-application icbc-payment icbc-invoice-issuance icbc-red-invoice icbc-quota icbc-tax-declaration icbc-billing icbc-natural-person icbc-settlement icbc-station icbc-seller-portal icbc-appointment icbc-seller-notify icbc-handover-batch icbc-purchase-contract icbc-purchase-order icbc-input-invoice icbc-purchase-order-progress icbc-acquisition-purchase-link icbc-stock-in icbc-acquisition-acceptance icbc-stock-ops icbc-bank-card-change icbc-menu; do
  $MYSQL ruoyi-vue-pro < "$f.sql"
done
```

端口 `13308` 对应 `backend/docker-compose.yml` 里 MySQL 的宿主映射。

> **别用没有 LANG 的 `docker exec` 导**：`docker exec -i <mysql容器> mysql` 的连接字符集是 **latin1**，
> 中文会被双重编码存进库，界面上就是乱码。要用就必须带 `--default-character-set=utf8mb4`。
> 现在这些文件自己以 `SET NAMES utf8mb4;` 开头，已经能顶住这种客户端；老库已脏的见下一节。

## 构建与启动：ERP 已启用后的两个坑（#39）

从 #39 起 `backend/pom.xml` 的 `yudao-module-erp` 与 `yudao-server/pom.xml` 的 `yudao-module-erp-biz` 不再注释，ERP 成为默认模块。两条命令上的坑：

1. **先 `-am install` 再起服务**。`erp-biz` 是新模块，本地 `.m2` 里没有，直接 `mvn -pl yudao-server spring-boot:run` 会因找不到它失败。改完任何模块（尤其 `erp` / `icbc`）后：

   ```bash
   cd backend
   mvn -pl yudao-server -am -DskipTests install
   mvn -pl yudao-server spring-boot:run
   ```

2. **`-pl yudao-module-erp -am` 只构建父 pom，不进子模块**。要单独构建 ERP 时写全子模块路径：

   ```bash
   mvn -pl yudao-module-erp/yudao-module-erp-biz -am -DskipTests install
   ```

   （`icbc` 同理是 `-pl yudao-module-icbc/yudao-module-icbc-biz`，不要写 `-pl yudao-module-icbc`。）

起服务后冒烟：用租户 1 的 `admin` 调 `GET /admin-api/erp/warehouse/page`，应返回 `{"list":[],"total":0}`；报 SQL 错就说明 `erp.sql` 没导。

## 排查：菜单 / 种子数据中文乱码

症状是库里存着 `åå‘å¼€ç¥¨` 而不是 `反向开票`（`select hex(name)` 是 `C3A5C28F…` 而不是 `E58F8DE59091…`）。
用 [repair-mojibake.sql](repair-mojibake.sql) 修，它扫全库、只改**能证明**是双重编码的值（英文 / 正常中文 / `é`、`·` 这类 Latin-1 字符都不动），幂等：

```bash
MYSQL="mysql -h 127.0.0.1 -P 13308 -uroot -p --default-character-set=utf8mb4"
$MYSQL ruoyi-vue-pro < repair-mojibake.sql   # 只打印「表 / 列 / 修复行数」，没脏数据就什么都不打印
```

**修完还看到乱码？那是缓存，不是没修好：**

- **后台菜单**：前端把菜单树缓在 localStorage 的 `roleRouters`（登录时写，`src/hooks/web/useCache.ts`）。**退出重新登录**即可。
- **短信模板**：`SmsTemplateServiceImpl` 把 `system_sms_template` 缓在 Redis（`sms_template:<code>`）。
  `docker exec -i youfeibao-redis redis-cli -n 0 --scan --pattern 'sms_template:*'` 看一下，删掉即可。

## 默认登录

- 租户：`芋道源码`（请求头 `tenant-id: 1`）
- 账号：`admin` / `admin123`

> 改了 `icbc-menu.sql` 后菜单没变化？后台把菜单树缓在 localStorage 的 `roleRouters`（登录时写）。**退出重新登录**即可；菜单数据本身每次从库里现读，不用清 Redis。

## 来源与脱敏

- `erp.sql` 的来源另计：`sql/erp-2026-04-18.sql.zip`（项目外的上游导出），建表语句逐字保留，仅删演示数据。
- 其余来源：`tuyaweilai/tuya-saas-java` 归档的 `ruoyi-vue-pro/sql/mysql`。
- 并入时已**脱敏**：阿里云 / 腾讯云示例 AccessKey、短信 `api_secret` 等替换为 `REPLACE_ME`。详见 ADR 0012。
- icbc 表在本快照里已带 `tenant_id`；`backend/doc/icbc/sql/tenant-scope.sql` 保留，用于进一步把出售者唯一键收敛为「租户内唯一」。
