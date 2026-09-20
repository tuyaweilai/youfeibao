# 引入 yudao ERP 作为采购与库存底座，只启用 stock 域

> **2026-09-20 修订（#38 / #39 落地）**：本文原定启用 `product` / `purchase` / `stock` 三个域。规格 #38 改为**只启用 `stock` 域**：品类主数据权威留在 `icbc_goods_config`（`product` 域排除，见 ADR 0028），采购履约链（采购合同 / 订单 / 交接批次 / 入库单 / 进项收票）建在 `icbc` 模块，因为采购订单必须与收购单、结算单在同一模块内做关联追溯；启用 `purchase` 域会让 `erp` 反向依赖 `icbc`。其余结论不变。

> **2026-09-20 修订（#54 T16 落地）**：决策细则第 1 条原话「`stock_out` 保留但只用于非销售出库（报损 / 退货出库 / 内部领用），`customerId` 留空」与第 2 条「重造调拨 / 盘点没有意义」**作部分修订**。非销售出库 / 跨仓调拨 / 盘点调整 / 期初四类**业务单据**改在 `icbc` 侧（`icbc_stock_out` / `icbc_stock_move` / `icbc_stock_check` / `icbc_stock_opening`），理由：ERP 的 `erp_stock_out/move/check` 单据服务与前端页面自 #42 删除 `product` 域后已不可达（前端仍在用 `productId`），且 `erp_stock_out` 强制挂客户、缺「报损 / 退货出库 / 内部领用」类型，与验收「不挂客户」直接冲突。**第 2 条的核心结论不变**：余额 + 流水的记账机制仍在 ERP（`erp_stock` / `erp_stock_record`），icbc 不建自己的余额表、不直接碰 `erp_stock*`，全部经 `StockApi`（#54 追加 `move` / `adjustTo` 两个原语与出库 / 期初的业务类型）。菜单段与权限见 handoff 的「#54」小节。

平台从"反向开票工具"扩成回收企业的经营作业系统后，缺的那一层是**采购履约与库存台账**。仓库里已经躺着 `yudao-module-erp`（33 张表、23 个 Controller、前端 63 个页面），我们**放开它、按需收敛**，而不是自建一套同形的采购/库存。一期只启用 `stock` 域，`product` / `purchase` / `sale` / `finance` / `statistics` 都不分配菜单。

**背景**

- 现状只有 `icbc_*` 域，覆盖"收购事实 + 计价 + 开票 + 付款 + 税费"，没有采购订单、没有仓库、没有库存。
- `yudao-module-erp` 在本仓库的 `pom.xml` 里被注释掉，注释原话是"默认注释，保证编译速度"——不是授权限制。
- 已实测：放开两个 pom 注释后 `mvn -pl yudao-server -am -DskipTests install` 成功、服务启动成功（`Started YudaoServerApplication in 10.483s`）。ERP 源码与本仓库的 framework 版本兼容。
- 建表 SQL 见 `backend/sql/mysql/erp.sql`（33 张表，全部带 `tenant_id`）。

**Considered Options**

- **自建采购与库存**。未采纳：ERP 已有余额表 + 流水表 + 调拨 + 盘点 + 采购履约的完整闭环，重造一遍只换来确定性更差的实现。
- **整体引入 ERP 六个域**。未采纳：`sale` 域与 ADR 0004（不做交易撮合、正向开票不做）直接冲突；`finance` 域的收付款单会与 `icbc_payment_order` 形成**两个金额事实**，而平台的卖点正是单一口径可核验（ADR 0021）。
- **放开 pom + 收敛到 `stock` 域**。采纳。

**决策细则**

1. **域取舍**：启用 `stock` 全域（含出库 / 调拨 / 盘点）。**不启用** `purchase`、`product`、`sale`、`finance`、`statistics`（采购履约链在 `icbc` 模块，见上方修订注）。`stock_out` 保留但只用于**非销售出库**（报损 / 退货出库 / 内部领用），`customerId` 留空——规划 M06 明确"只有入库记录时只能称累计入库，不能称当前库存"。
2. **库存归 ERP**，`erp_stock` + `erp_stock_record` 是实物库存的事实源。重造调拨/盘点没有意义。
3. **库存维度用 `goods_config_id`，不引入 `erp_product` 域**。品类主数据权威留在 `icbc_goods_config`（它带税率、计税方法、税收分类编码，且已被现场端、自然人端、开票三处消费）；等级/规格作为 `icbc_goods_config` 的一行，而不是新开一张等级表。**代价：`erp_*_item`、`erp_stock`、`erp_stock_record` 等约 12 张表把 `product_id` / `product_unit_id` 换成 `goods_config_id bigint`；`erp_product` / `erp_product_category` / `erp_product_unit` 三张表不导入。**这是对上游的一处**故意偏离**，升级 yudao ERP 时要维护。
4. **收购单是唯一事实源，入库是它的派生动作**：

   ```
   icbc_acquisition ──┬─ 计价：settlement_weight ──→ 结算/确认/付款/开票（icbc）
                      └─ 实物：仓管确认入库 ──────→ erp_stock_in → erp_stock_record → erp_stock
   ```

   ERP 不反向写收购金额，收购单也不直接写库存余额。关联用 `erp_stock_record` 已有的 `biz_type/biz_id/biz_item_id/biz_no`，新增一个"收货入库"业务类型（现有枚举里没有这一类）。
5. **不把 `ErpPurchaseIn` 当主载体**：它的代码硬校验"必须挂已审核的采购订单"（`validatePurchaseOrder`），与规划要求的"支持无采购订单的零散收购"冲突。有订单的到货可用它，零散收购直接走 `erp_stock_in`。
6. **菜单清场同时做**：`system_menu` 只在租户侧保留回收业务菜单 + 真正在用的 yudao 菜单（系统管理 / 基础设施）。清掉已禁用模块的死菜单（报表 / 商城 / CRM / 公众号 / AI / IoT）、三条外链（作者动态、Boot 文档、Cloud 文档）与演示菜单。实测当前系统租户 `admin` 的一级菜单有 18 个，其中只有 3 个是回收业务；放开 ERP 会变成 19 个。

**Consequences**

- 本地起服务的流程要补一步：`erp-biz` 是新模块，`mvn -pl yudao-server spring-boot:run` 会因 m2 里没有它而失败，必须先 `mvn -pl yudao-server -am -DskipTests install`。
- 导入顺序加一项：`backend/sql/mysql/erp.sql` 排在 `ruoyi-vue-pro.sql` 之后（ERP 菜单与 `erp:*` 权限本来就在 `ruoyi-vue-pro.sql` 里，共 140 行菜单 / 112 条权限码）。
- **6 张单据表的 `no` 唯一索引不含 `tenant_id`**（`erp_purchase_in/order/return`、`erp_sale_order/out/return`）。当前单号由 `ErpNoRedisDAO` 用 Redis 全局自增生成（key 不含租户），所以跨租户不会撞号。若将来要给每个租户独立的单号序列，**必须同时**改唯一索引为 `(tenant_id, no)` 和 Redis key；只改一边会立刻冲突。现在是唯一便宜的改造时机。
- `erp_stock` 对 `(product_id, warehouse_id)` 只有普通索引、没有唯一约束，而服务层按"查不到就插一行 0"维护余额——并发下可能产生重复余额行。需要补唯一约束（含租户）。
- ERP 没有任何单元测试（`yudao-module-erp` 下 `src/test` 为空），且 `ErpSaleOrderController` 的权限码错写成 `erp:sale-out:*`。引入即继承这些瑕疵。
- 上游 `deleted` 用 `bit(1)`，而 `icbc_*` 表用 `tinyint unsigned`。两者都能工作，但同一个库里并存两种写法，写 SQL 时要留意。
