# 部署验证脚本

前端每次部署后**必跑**。它们用真实浏览器（Playwright + 系统 Chrome）打开线上四个域名做断言。

## 为什么必须用浏览器验证

2026-09-23 踩过一次：`VITE_BASE_URL` 留成了 `http://localhost:48080`，被 Vite 打进产物，
浏览器里所有接口都打向**访问者自己的 localhost**，后台全废。而当时：

- 静态资源全部 200 ✅
- 直接 `curl` 后端接口全部 200 ✅

**两条都过，但前端在实际浏览器里是坏的。** 静态资源不执行 JS，curl 又绕过了前端。
所以只有真跑一遍浏览器才算验完。

## 一次性准备

脚本不把 playwright 装进仓库，只做一次临时安装：

```bash
mkdir -p /tmp/yfb-e2e && cd /tmp/yfb-e2e && npm i playwright
```

脚本会按 `$PLAYWRIGHT_HOME/node_modules/playwright` → `/tmp/yfb-e2e/node_modules/playwright`
→ 裸 `playwright` 的顺序找。放在别处就 `PLAYWRIGHT_HOME=<含 node_modules 的目录> node ...`。

## verify-deploy.mjs —— 日常用这个

```bash
node scripts/deploy/verify-deploy.mjs
```

四端逐个断言：

1. 没有请求打到 `localhost` / `127.0.0.1`
2. 所有请求同源
3. 没有失败请求

输出形如：

```
✅ yfbadmin.baibaitan.com     有废宝 - 登录    请求=33 跨域=0 localhost=0 失败=0
✅ yfbwuliu.baibaitan.com     司机登录        请求=11 跨域=0 localhost=0 失败=0
✅ yfbgeren.baibaitan.com     登录            请求=20 跨域=0 localhost=0 失败=0
✅ yfbqiye.baibaitan.com      登录            请求=14 跨域=0 localhost=0 失败=0
✅ 四端浏览器视角全部干净（同源、无 localhost、无失败请求）
```

## verify-deploy-full.mjs —— 改动登录相关时才跑

```bash
node scripts/deploy/verify-deploy-full.mjs
```

在上面基础上多做一次**真实登录**：填表 → 点登录 → 断言跳到 `/index` 控制台、
关键接口全 200、登录框无预填、无页面 JS 报错，并 dump 左侧菜单文本留证。

⚠️ 它要求**临时关掉图形验证码**（自动化过不了滑块）：

```bash
# 服务器上
cd /opt/youfeibao
echo 'YUDAO_CAPTCHA_ENABLE=false' >> .env.prod
docker compose --env-file .env.prod up -d backend   # 等 /actuator/health 返回 200

node scripts/deploy/verify-deploy-full.mjs

# 验完务必恢复
sed -i '/^YUDAO_CAPTCHA_ENABLE=false$/d' .env.prod
docker compose --env-file .env.prod up -d backend
```

## verify-login-uniapp.mjs —— 验司机端 / 企业收货端登录

```bash
node scripts/deploy/verify-login-uniapp.mjs
```

用 `driver01` 分别登司机端与企业收货端，断言 `/auth/login` 返回 200 且已离开登录页。

> 注意：`driver01` 挂的是 `logistics_driver`（司机）角色，登企业收货端后概览会显示
> 「暂不可用」—— 那是权限不足的正确表现，不是 bug。要看收货端完整数据请用 `admin`。

## 网络说明

本机 DNS 被代理的 fake-ip（`198.18.0.x`）劫持，脚本用 Chrome 的
`--host-resolver-rules` 把四个域名强制指到 `47.99.49.104`，绕过本地 DNS。
所以脚本里硬编码了这个 IP —— 换服务器要同步改。

服务器 IP 变了的话，三个脚本顶部都有 `47.99.49.104` / `DOMAINS` 需要更新。
