# 本地开发库：建表与种子数据（MySQL 8）

用于本地把后端跑起来。目标库名 `ruoyi-vue-pro`。

## 导入顺序

1. `ruoyi-vue-pro.sql` —— yudao 全量建表 + 演示数据（管理员、菜单、租户等）。**ERP 的菜单（id 2563–2702）与 `erp:*` 权限也在其中**
2. `erp.sql` —— ERP 建表（33 张表，来自上游导出，已删演示数据）。没建这几张表时，放开 `yudao-module-erp` 后任何 ERP 页面一查库就报错
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
23. `icbc-menu.sql` —— 反向开票与租户开票就绪的管理后台菜单（依赖 `system_menu`，最后导）

> enterprise 的菜单与字典已包含在 `ruoyi-vue-pro.sql` 中，不要再单独导入 `enterprise-menu.sql` / `module-enterprise-dict.sql`（会主键冲突）。

## 一键导入

```bash
cd backend/sql/mysql
MYSQL="mysql -h 127.0.0.1 -P 13308 -uroot -p --default-character-set=utf8mb4"
$MYSQL -e "CREATE DATABASE IF NOT EXISTS \`ruoyi-vue-pro\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
for f in ruoyi-vue-pro erp quartz 02-initial-data yudao-module-enterprise-auth-flow member-2024-01-18 \
         icbc_payee_info icbc_payer_info icbc_invoice_tables icbc_payment_order \
         icbc_invoice_download icbc_api_log_callback enterprise icbc-readiness icbc-evidence icbc-public-token icbc-jobs icbc-seller-onboarding icbc-acquisition icbc-invoice-application icbc-payment icbc-invoice-issuance icbc-red-invoice icbc-quota icbc-tax-declaration icbc-billing icbc-natural-person icbc-settlement icbc-station icbc-seller-portal icbc-appointment icbc-seller-notify icbc-bank-card-change icbc-menu; do
  $MYSQL ruoyi-vue-pro < "$f.sql"
done
```

端口 `13308` 对应 `backend/docker-compose.yml` 里 MySQL 的宿主映射。

> **别用没有 LANG 的 `docker exec` 导**：`docker exec -i <mysql容器> mysql` 的连接字符集是 **latin1**，
> 中文会被双重编码存进库，界面上就是乱码。要用就必须带 `--default-character-set=utf8mb4`。
> 现在这些文件自己以 `SET NAMES utf8mb4;` 开头，已经能顶住这种客户端；老库已脏的见下一节。

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

## 来源与脱敏

- `erp.sql` 的来源另计：`sql/erp-2026-04-18.sql.zip`（项目外的上游导出），建表语句逐字保留，仅删演示数据。
- 其余来源：`tuyaweilai/tuya-saas-java` 归档的 `ruoyi-vue-pro/sql/mysql`。
- 并入时已**脱敏**：阿里云 / 腾讯云示例 AccessKey、短信 `api_secret` 等替换为 `REPLACE_ME`。详见 ADR 0012。
- icbc 表在本快照里已带 `tenant_id`；`backend/doc/icbc/sql/tenant-scope.sql` 保留，用于进一步把出售者唯一键收敛为「租户内唯一」。
