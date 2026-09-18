## 代码库

平台代码在本仓库的 `backend/` 下，是一个 yudao 多模块 Maven 工程（原 `tuyaweilai/tuya-saas-java` 的 `ruoyi-vue-pro`，见 `docs/adr/0008-代码并入本仓库自持.md`）。代码、文档、票据同仓管理；`tuya-saas-java` 已转为只读归档。构建产物 `target/` 不入库，密钥走环境变量 `ICBC_*`。

常用命令（在 `backend/` 下）：

```bash
mvn -DskipTests compile                 # 全量编译
mvn -pl yudao-module-icbc/yudao-module-icbc-biz -am -DskipTests install   # 安装上游依赖
mvn -pl yudao-module-icbc/yudao-module-icbc-biz test                      # 运行 ICBC 模块测试
```

首次编译前需把 `backend/yudao-module-icbc/lib/` 下的私有 jar 安装进本地 Maven 仓库（`mvn install:install-file`）。

前端（PC 管理后台）在 `backend/yudao-ui/yudao-ui-admin-vue3/`（vue3 + vite5 + element-plus，pnpm；见 `docs/adr/0011-前端形态与终端矩阵.md`）。依赖的构建脚本在 `pnpm-workspace.yaml` 放行，Node 需 >= 16、pnpm >= 8.6。

```bash
cd backend/yudao-ui/yudao-ui-admin-vue3
pnpm install        # 首次
pnpm dev            # 本地开发（端口 3100，联调 http://localhost:48080）
pnpm build:local    # 生产构建
```

## 本地运行后端

`local` profile 需要 MySQL 与 Redis；敏感配置一律走环境变量（模板见根目录 `.env.example`）。建表与种子数据在 `backend/sql/mysql/`（已脱敏，见 ADR 0012）。

```bash
cp .env.example .env && $EDITOR .env   # 填 MYSQL_PASSWORD、YUDAO_ENCRYPTOR_PASSWORD 等；值含 & 要加引号
set -a; source .env; set +a

cd backend
docker compose up -d                   # MySQL(13308) + Redis(16382)，避免与常见端口冲突
# 首次：按 backend/sql/mysql/README.md 导入建表与种子数据
mvn -pl yudao-server -am -DskipTests install   # 首次：安装依赖
mvn -pl yudao-server spring-boot:run   # 端口 48080，默认 icbc.gateway.mode=fake 不触网
```

默认登录：请求头 `tenant-id: 1`，账号 `admin` / `admin123`。

## Agent skills

### Issue tracker

Issues and specs live as GitHub issues in `tuyaweilai/youfeibao`, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, using the default label strings (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: a root `CONTEXT.md` plus ADRs under `docs/adr/`. See `docs/agents/domain.md`.
