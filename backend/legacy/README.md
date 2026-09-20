# 留库但停编的代码（不在 Maven reactor 里）

这里的两个模块随 `tuya-saas-java` 快照并进本仓库（提交 `fc280f7`）。它们**不是本项目的实现**，也不在 `backend/pom.xml` 的 `<modules>` 里，因此不参与编译与打包；保留在这里只为可回查，不提供任何运行时能力。

| 目录 | 原始口径 | 停编原因 |
|---|---|---|
| `yudao-module-logistics` | 「危废再生资源全流程追溯 SaaS 平台」的物流运输模块：运输任务挂在**危废订单**上、`waste_code` / `waste_name` 是自由文本、含**司机现金代付**（`logistics_cash_advance`）与**临时订单**（`logistics_temporary_order`，司机现场现金收购、可转正式订单） | [ADR 0032](../../docs/adr/0032-物流独立成模块不承载工行语义.md)：与 ADR 0028（品类权威是品类配置）、ADR 0006（不垫资、不代收代付）、ADR 0010（货款只走公对私到出售者本人卡）冲突。本项目按 ADR 0032 **重写**独立的物流模块（`backend/yudao-module-logistics/`），不复用这一份。它的设计文档与建表 SQL 也一并降级为历史参考 |
| `yudao-module-waste` | 危废预约、报价、**竞价对比**、价格基准与回收商配置 | ADR 0004（只做工具不做交易撮合）+ 其品类取舍（危废不做）；另已两次造成 bean 名冲突（`AppointmentServiceImpl`、`appointmentMapper`，见 `docs/agents/handoff.md`） |

## 注意

- **不要把这两个模块加回 `<modules>`**。它们与在编模块存在同名类与同名 bean（`AppointmentServiceImpl` / `AppointmentMapper` / `ContractMapper` 一类），加回去会在整机启动时炸注入冲突——单测发现不了，只有 `spring-boot:run` 才会暴露。
- 需要它们的哪段逻辑时，**按本项目的口径重写**，不要搬运：里面的实体语义（危废订单、现金支付、自由文本品类）与本项目的 ADR 冲突，`waste_code` / `waste_name` 这类字段尤其不能带进来。
- `icbc` 侧为绕开这些冲突而做的显式 bean 命名（`icbcAppointmentServiceImpl` / `icbcAppointmentMapper` / `icbcPurchaseContractMapper`）**保留不回退**：重命名本身是对的。
