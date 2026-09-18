# 本地开发库：建表与种子数据（MySQL 8）

用于本地把后端跑起来。目标库名 `ruoyi-vue-pro`。

## 导入顺序

1. `ruoyi-vue-pro.sql` —— yudao 全量建表 + 演示数据（管理员、菜单、租户等）
2. `quartz.sql` —— Quartz 调度表
3. `02-initial-data.sql`、`yudao-module-enterprise-auth-flow.sql`、`member-2024-01-18.sql` —— 模块补充
4. `icbc_*.sql` —— 反向开票（工行）业务表
5. `enterprise.sql` —— 企业管理（企业信息 / 资质 / 门店 / 用户关系）表
6. `icbc-readiness.sql` —— 租户开票就绪：三层资质、编码配置、企业授权表；平台级报废产品编码表；到期预警表
7. `icbc-evidence.sql` —— 一票一档证据表（人工补录的合同流 / 货物流证据）
8. `icbc-public-token.sql` —— 公开令牌表与收方入驻失败留联系方式表（自然人免登录端点）
9. `icbc-jobs.sql` —— icbc 定时任务种子（资质到期提醒）
10. `icbc-menu.sql` —— 反向开票与租户开票就绪的管理后台菜单（依赖 `system_menu`，最后导）

> enterprise 的菜单与字典已包含在 `ruoyi-vue-pro.sql` 中，不要再单独导入 `enterprise-menu.sql` / `module-enterprise-dict.sql`（会主键冲突）。

## 一键导入

```bash
cd backend/sql/mysql
MYSQL="mysql -h 127.0.0.1 -P 13308 -uroot -p"
$MYSQL -e "CREATE DATABASE IF NOT EXISTS \`ruoyi-vue-pro\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
for f in ruoyi-vue-pro quartz 02-initial-data yudao-module-enterprise-auth-flow member-2024-01-18 \
         icbc_payee_info icbc_payer_info icbc_invoice_tables icbc_payment_order \
         icbc_invoice_download icbc_api_log_callback enterprise icbc-readiness icbc-evidence icbc-public-token icbc-jobs icbc-menu; do
  $MYSQL ruoyi-vue-pro < "$f.sql"
done
```

端口 `13308` 对应 `backend/docker-compose.yml` 里 MySQL 的宿主映射。

## 默认登录

- 租户：`芋道源码`（请求头 `tenant-id: 1`）
- 账号：`admin` / `admin123`

## 来源与脱敏

- 来源：`tuyaweilai/tuya-saas-java` 归档的 `ruoyi-vue-pro/sql/mysql`。
- 并入时已**脱敏**：阿里云 / 腾讯云示例 AccessKey、短信 `api_secret` 等替换为 `REPLACE_ME`。详见 ADR 0012。
- icbc 表在本快照里已带 `tenant_id`；`backend/doc/icbc/sql/tenant-scope.sql` 保留，用于进一步把出售者唯一键收敛为「租户内唯一」。
