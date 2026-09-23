# 正式服务器部署（47.99.49.104）

有废宝一期四端 + 后端 + 独立数据库，跑在阿里云 ECS `47.99.49.104` 的 Docker 里，
由宿主机的宝塔 nginx 反代四个域名。

首次落地时间：2026-09-23。本目录下的 `prod-configs/` 是**服务器上实际生效的配置副本**，
改线上配置时应同时改这边，否则两边会漂移。

---

## 1. 拓扑

```
                        ┌──────────────── 宿主机 nginx (宝塔, 80) ────────────────┐
                        │                                                         │
 yfbadmin.baibaitan.com ─┤ /            → /opt/youfeibao/dist/admin  (vue3)      │
 yfbwuliu.baibaitan.com ─┤ /            → /opt/youfeibao/dist/driver (uni-app)   │
 yfbgeren.baibaitan.com ─┤ /            → /opt/youfeibao/dist/seller (uni-app)   │
 yfbqiye.baibaitan.com  ─┤ /            → /opt/youfeibao/dist/field  (uni-app)   │
                        │                                                         │
   四个域名共同的          │ /admin-api/  ┐                                          │
   后端入口               │ /app-api/    ├→ 127.0.0.1:18080                        │
                        │ /infra/ws    ┘        │                                 │
                        └───────────────────────┼─────────────────────────────────┘
                                                │
                                    ┌───────────▼───────────┐  docker network youfeibao_yfb-net
                                    │  yfb-backend :48080   │  (eclipse-temurin:17-jre-jammy)
                                    └────┬─────────────┬────┘
                                         │             │
                              yfb-mysql :3306    yfb-redis :6379
                              (127.0.0.1:13318)  (127.0.0.1:16392)
```

四个域名都已在 DNS 解析到 `47.99.49.104`，不需要再配域名。

### 目录

| 路径 | 内容 |
|---|---|
| `/opt/youfeibao/docker-compose.yml` | compose 编排 |
| `/opt/youfeibao/.env.prod` | 生产口令（600 权限），**不入库** |
| `/opt/youfeibao/Dockerfile` | 后端镜像 |
| `/opt/youfeibao/app/yudao-server.jar` | 后端 fat jar（212M） |
| `/opt/youfeibao/config/application-prod.yaml` | 外部化配置，改完重启即生效，不用重打包 |
| `/opt/youfeibao/dist/{admin,driver,seller,field}` | 四端静态产物 |
| `/opt/youfeibao/backup/` | 数据库备份（每日 3:30，保留 14 天） |
| `/opt/youfeibao/deploy/backup-db.sh` | 备份脚本 |
| `/opt/youfeibao/nginx/` | vhost 副本（正式位置在 `/www/server/panel/vhost/nginx/`） |
| `/www/wwwlogs/yfb*.baibaitan.com*.log` | nginx 日志 |

---

## 2. 日常操作

> **走不通 SSH 时看 [§6 Workbench CLI](#6-备用通道workbench-cli不依赖-22-端口)** ——
> 22 端口受安全组限制，且被云盾 aegis 封过。下文所有 `ssh ... '<cmd>'` 都可以换成
> `workbench exec -i i-bp16czox2yme6947ewrm -c '<cmd>'`。

```bash
ssh root@47.99.49.104
cd /opt/youfeibao

docker compose --env-file .env.prod ps                 # 状态
docker compose --env-file .env.prod logs -f backend    # 后端日志
docker compose --env-file .env.prod restart backend    # 重启后端
docker compose --env-file .env.prod down && \
  docker compose --env-file .env.prod up -d            # 全量重启
```

### 更新后端

```bash
# 本地
cd backend && mvn -T 1C -DskipTests clean package -pl yudao-server -am

# 上传（二选一）
scp yudao-server/target/yudao-server.jar root@47.99.49.104:/opt/youfeibao/app/     # ~30s
workbench upload yudao-server/target/yudao-server.jar \
  /opt/youfeibao/app/yudao-server.jar -i i-bp16czox2yme6947ewrm                   # ~58s，但不依赖 22

# 服务器（或用 workbench exec 执行同样命令）
cd /opt/youfeibao
docker compose --env-file .env.prod up -d --build backend
```

启动约 45–90 秒。探活：`curl -s localhost:18080/actuator/health`。

> 用 `workbench upload` 覆盖已存在的 jar 时，它会**交互式问是否覆盖**（默认 No）。
> 自动化场景先 `workbench exec -i <id> -c "ls -la /opt/youfeibao/app/"` 确认一下。

### 更新前端（不用重建镜像）

```bash
# 本地：admin 用 build:prod，三个 uniapp 用 build:h5
cd backend/yudao-ui/yudao-ui-admin-vue3 && pnpm build:prod      # → dist-prod
cd ../yudao-ui-driver-uniapp && pnpm build:h5                   # → dist/build/h5
# field / seller 同理

# 打包上传（注意：三个 uniapp 产物都叫 h5/，必须先改名再打，否则 tar 会互相覆盖）
mkdir -p /tmp/stage/{admin,driver,seller,field}
cp -a yudao-ui-admin-vue3/dist-prod/.        /tmp/stage/admin/
cp -a yudao-ui-driver-uniapp/dist/build/h5/. /tmp/stage/driver/
cp -a yudao-ui-field-uniapp/dist/build/h5/.  /tmp/stage/field/
cp -a yudao-ui-seller-uniapp/dist/build/h5/. /tmp/stage/seller/
tar czf /tmp/dist.tar.gz -C /tmp/stage admin driver seller field
scp /tmp/dist.tar.gz root@47.99.49.104:/tmp/     # 或 workbench upload /tmp/dist.tar.gz /tmp/ -i <id>
ssh root@47.99.49.104 'tar xzf /tmp/dist.tar.gz -C /opt/youfeibao/dist && chmod -R a+rX /opt/youfeibao/dist'

# 装完必须跑一次真实浏览器验证（见 scripts/deploy/README.md）
node scripts/deploy/verify-deploy.mjs
```

静态文件直接落盘，不需要 reload nginx。

---

## 3. 部署时踩过的坑（都已在当前配置里解决）

### 3.1 后端不能用 JRE 8

`pom.xml` 声明 `java.version=1.8`，但 `yudao-module-icbc/erp/enterprise` 里实际用了
**112 处 `Set.of`/`List.of`/`Map.of`**（Java 9+ API）。本地只有 JDK 21，编译时
`-source/-target 1.8` 不拦截新 API，于是产物是「Java 8 字节码 + Java 9+ 调用」。

在 JRE 8 上启动直接崩：

```
java.lang.NoSuchMethodError: java.util.Set.of(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Set;
  at ...SellerPortalServiceImpl.<clinit>(SellerPortalServiceImpl.java:76)
```

容器基底因此用 `eclipse-temurin:17-jre-jammy`（对齐本地 JDK 21 的行为，取 LTS 17）。
副作用收益：jammy 镜像自带 DejaVu 字体，验证码的 `load font error: Problem reading font data`
不再出现（`openjdk:8-jre-slim` 没字体）。

> 根治办法是给 maven-compiler-plugin 加 `<release>8</release>` 并清掉这 112 处调用，
> 属独立议题；在修掉之前**不要**把容器基底换回 JRE 8。

### 3.2 磁盘被单个容器日志撑满

接手时根分区 99G 已用 83G（89%）。`/var/lib/docker/containers` 占 **29G**，
全部来自 `kaojun-server` 一个容器的 json-file 日志（无上限）。
清理 build cache + 悬挂镜像 + 截断该日志后，可用空间 11G → **49G**。

已做的防复发措施：

- `/etc/docker/daemon.json` 加 `log-opts: {max-size: 100m, max-file: 3}`（对**新建**容器生效）
- `/etc/logrotate.d/docker-containers` 每日轮转并压缩，兜住已存在的容器
- 本项目 compose 里每个服务都显式限了日志大小

### 3.3 `tar` 打四端产物会互相覆盖

三个 uniapp 的 H5 产物路径都是 `dist/build/h5/`，直接 `tar czf x.tar.gz -C a dist/build/h5 -C b dist/build/h5 ...`
会让**同名条目互相覆盖**，只有最后一个能解出来。
必须像上面那样先复制到 `admin/driver/seller/field` 暂存目录再打包。

### 3.4 司机端 `manifest.json` 的 H5 base 曾是 `/driver/`

`yudao-ui-driver-uniapp/src/manifest.json` 里 `h5.router.base` 原本是 `/driver/`
（`field` / `seller` 两个端都是 `/`），构建出来的 `index.html` 因此引用
`/driver/assets/xxx.js`。放到 `yfbwuliu.baibaitan.com` 的根路径下会整站 404。

已改成 `/` 并重新构建。此后改造端请**确认 base 是 `/`**，三个 uniapp 端应保持一致。

### 3.5 登录页把默认口令打进了线上 JS（已修）

基础 `.env` 里有脚手架自带的默认账号：

```
VITE_APP_DEFAULT_LOGIN_USERNAME = admin
VITE_APP_DEFAULT_LOGIN_PASSWORD = admin123
```

`--mode prod` 也会读 `.env`，这两个值被 Vite 替换成字面量进产物，登录表单拿它**预填**——
任何人打开 `yfbadmin.baibaitan.com` 都是填好的一键登录，`grep admin123` 还能直接从 JS 里挖出来。

更麻的是 `/social-login` 这个路由在 `src/permission.ts` 的**免登录白名单**里，
`SocialLogin.vue` 又硬编码了 `'芋道源码' / 'admin' / 'admin123'`（上游脚手架原样），
等于把一个带口令的登录表单挂在公网上。

修法：`.env.prod` 里把三个变量覆盖为空（`.env.prod` 优先级高于 `.env`，故本地开发不受影响），
`SocialLogin.vue` 改成与 `LoginForm.vue` 一致地读 env、空串兜底。

**验证口径**：`grep -rl admin123 dist-prod/` 必须无输出；改前端登录相关代码后请重跑这条。

### 3.6 前端把开发机地址打进了产物（`VITE_BASE_URL`）

`.env.prod` 里 `VITE_BASE_URL` 原本是脚手架默认的 `http://localhost:48080`，
而 `src/config/axios/config.ts` 是这么拼的：

```ts
base_url: import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL
```

于是产物里就写死了 `http://localhost:48080/admin-api`——**浏览器里所有接口都打向访问者自己的
localhost**，后台完全用不了（表现为 `http://localhost:48080/admin-api/system/tenant/get-by-website...` 失败）。

**已置为空**，走同源 `/admin-api` 由 nginx 反代。四个域名共用一套后端，空值也是唯一不需要
写死域名的选项。同时受影响的还有 `useUpload`、各导入表单、`/infra/ws`、Knife4j 等。

> **教训**：静态资源 200 和“直接 curl 后端接口 200”**都不能**证明前端在浏览器里是好的。
> 必须真的用浏览器跑一遍。仓库里已放 `scripts/deploy/verify-deploy.mjs`，前端每次部署后必跑。

### 3.7 脚手架遗留的百度统计在往百度上报数据

基础 `.env` 的 `VITE_APP_BAIDU_CODE` 是脚手架作者的统计 ID，`src/plugins/tongji/index.ts` 会
引入 `hm.baidu.com` 并上报页面访问。`.env.prod` 已置空（该插件值为空时自动不加载）。

同类顺手清掉的还有 `VITE_MALL_H5_DOMAIN`（指向 yudao 演示商城）与 `VITE_GOVIEW_URL`。

**低于低优先级遗留**：`DiyEditor`（商城装修组件）的默认示例图标还指向 `mall.yudao.iocoder.cn`，
只在有人打开装修编辑器时才会发请求，不影响本项目功能，暂未动。

### 3.8 图形验证码不能全局开（会拦死 uniapp 端）

`yudao.captcha.enable` 的校验挂在 **`/admin-api/system/auth/login`** 上，而司机端与企业收货端
（`yudao-ui-driver-uniapp` / `yudao-ui-field-uniapp` 的 `src/api/auth.ts`）用的**就是同一个接口**，
且它们没有任何验证码 UI：

```java
// AdminAuthServiceImpl.doValidateCaptcha
if (!captchaEnable) { return ResponseModel.success(); }
ValidationUtils.validate(validator, reqVO, CodeEnableGroup.class);
```
```java
// CaptchaVerificationReqVO
@NotEmpty(message = "验证码不能为空", groups = CodeEnableGroup.class)
private String captchaVerification;
```

所以线上把 `captcha.enable` 置 true 后，这两端登录全部报 `请求参数不正确:验证码不能为空`。
本地是 `false`，所以本地不复现。

**当前取值：`YUDAO_CAPTCHA_ENABLE` 默认 `false`（后端） + `VITE_APP_CAPTCHA_ENABLE=false`（后台前端）**，
两边保持一致，避免后台用户白解一次滑块。

> `yudao-ui-seller-uniapp` 的“验证码”是**短信验证码**（6 位数字），与这里无关。
>
> 如果以后确实要给后台单独加图形验证码，**不能只改 `yudao.captcha.enable`**，
> 得先给 uniapp 拆出独立的登录接口（或在 `AdminAuthServiceImpl` 里按 login 来源区分）。

### 3.9 短信验证码恒为 `9999`（**已决策：演示期保留，勿改**）

`backend/yudao-server/src/main/resources/application.yaml`：

```yaml
yudao:
  sms-code:
    begin-code: 9999 # 这里配置 9999 的原因是，测试方便。
    end-code: 9999   # 这里配置 9999 的原因是，测试方便。
```

`SmsCodeServiceImpl.createSmsCode()` 用 `randomInt(beginCode, endCode+1)` 生成，两者相等
→ 验证码恒为 `9999`。线上 `system_sms_channel` 是空表（无真实下发通道），这个值就是唯一有效码。

> ⚠️ **2026-09-23 决策：保留 `9999`。** 它是演示流程的直接依赖（现场给客户演示时要能随输随进）。
> 请勿以「安全」为由改成随机码 —— 那会让演示当场做不了。
> 想知道风险边界，见下一段；想改，先确认演示不再需要。

**风险边界（已实测，记录备查）**：任何人只要能访问公网，就能

```bash
# ① 匿名给任意手机号触发发码，无需任何凭证
curl -X POST -H 'tenant-id: 1' -H 'Content-Type: application/json' \
  -d '{"mobile":"<目标手机号>"}' https://yfbgeren.baibaitan.com/app-api/icbc/seller/auth/sms-send
# ② 用 9999 登录，拿到该手机号主人的令牌 → 可读其结算单 / 发票 / 收款记录
curl -X POST -H 'tenant-id: 1' -H 'Content-Type: application/json' \
  -d '{"mobile":"<目标手机号>","code":"9999"}' https://yfbgeren.baibaitan.com/app-api/icbc/seller/auth/sms-login
```

因此生效范围要控制住：**只放演示手机号、只放演示数据**。
在真实自然人出售者开始自主使用之前，必须完成下面两件事之一，否则就是明面上的账号接管。

**上线闸门（真实用户接入前）**：二选一

1. 配真实短信渠道（`system_sms_channel` + 模板报备），并把 `9999` 挪出 prod：

   ```yaml
   # application-prod.yaml
   yudao:
     sms-code:
       begin-code: ${YUDAO_SMS_CODE_BEGIN:1000}
       end-code: ${YUDAO_SMS_CODE_END:9999}
   ```

   本地/演示环境继续用 9999，只有 prod 随机化。

2. 若一时配不上短信渠道，就把演示期的固定码收敛到**指定手机号白名单**（需改
   `SellerAuthServiceImpl`，属代码改动）。

内部测试需要读码时：

```bash
set -a; . /opt/youfeibao/.env.prod; set +a
docker exec yfb-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 -e "
SELECT mobile, code, create_time FROM \`ruoyi-vue-pro\`.system_sms_code
WHERE used=0 ORDER BY id DESC LIMIT 5;"
```

### 3.10 自然人端的租户上下文来自「场站二维码」

自然人端的 `/app-api` 请求必须带 **`tenant-id` 请求头**（值 = 他扫码那家回收企业的租户），
否则后端报 `请求的租户标识未传递，请进行排查`。

关键点：**这个头不是手填的，只能靠走场站二维码入口拿到。**
`WebFrameworkUtils.getTenantId()` 只读 HTTP 头，没有 query 参数兜底：

```java
String tenantId = request.getHeader(HEADER_TENANT_ID);
return NumberUtil.isNumber(tenantId) ? Long.valueOf(tenantId) : null;
```

正确入口（`icbc.station.entry-url` 拼出来的二维码内容）：

```
https://yfbgeren.baibaitan.com/#/?station=<场站码>
```

流程：`解析场站 → resolveStation(code) → 得到 tenantId → setTenantId() 写 localStorage`
（`seller_tenant_id`）→ 之后所有 `/app-api` 请求由 `src/utils/request.ts` 自动带上该头。
同时 `stationId` 也来自这里，会出现在 `portal/home?naturalPersonId=..&stationId=..` 里。

所以直接打开 `https://yfbgeren.baibaitan.com/` 而**不带 `?station=`**，一定会拿到
`请求的租户标识未传递`——这是设计如此（同一个人可能给多家回收企业卖货，
租户必须由「他扫的那家」决定），不是 bug。

**线上验证用的二维码链接**（场站 `STATION_TEST`，id=1，tenant=1）：

```
http://yfbgeren.baibaitan.com/#/?station=STATION_TEST
```

### 3.11 工行网关连不通（与本地一致，待工行加白）

```
apipcs4.dccnet.com.cn -> 219.143.240.50:443  Connection timed out
```

服务器与本地现象一致，所以保持 `ICBC_GATEWAY_MODE=fake`。
要真连需把 `47.99.49.104` 报给工行加白名单。IPv6 无出口，强制走 A 记录也一样超时。

---

## 4. 安全现状与待办

**已处理**

- `yudao.security.mock-enable: false`（本地是 `true`，线上留着等于登录态可伪造）
- Druid 控制台、`/actuator/*` 对外 404（只在 `127.0.0.1:18080` 内网可见）
- MySQL / Redis 只绑 `127.0.0.1`，不对公网暴露
- 图形验证码**关闭**（`YUDAO_CAPTCHA_ENABLE` 默认 false）—— 详见 §3.8，开了会拦死两个 uniapp 端
- 每日数据库备份

**待办**

- [ ] **建运营账号**：`recycling_receiver`（收货员）/ `recycling_invoicer`（开票员）/
      `recycling_finance`（财务）/ `logistics_dispatcher`（调度）**四个角色目前 0 个用户**，
      只有 `driver01` 挂了 `logistics_driver`（司机）。因此现在没人能正常使用企业收货端
      （用 admin 登能跑通：收购单 10 笔 / 待确认结算 1 单；用 driver01 登则概览全是「暂不可用」，
      因为该角色没有 `icbc:acquisition:query` / `icbc:settlement:query` 权限——这是正确行为）。
- [ ] **改 admin 口令**：当前仍是 `admin123`（`admin` 与 `driver01` 共用同一 bcrypt 哈希）。
- [ ] **清dev 账号 / 测试租户**：线上库是本地开发库整体搬迁的，带着一堆脚手架示范账号。
      租户 1（有废宝）13 个账号里只有 `admin` 是真实运营者，其余 12 个是
      `yudao` / `yuanma` / `test` / `newobject` / `hrmgr` / `aotemane` / `admin123` /
      `goudan` / `hh` / `wwbwwb` / `admin1` / `driver01`。
      另有 4 个**孤儿账号**（所属租户根本不存在）：`admin107`(118) / `admin108`(119) /
      `admin109`(120) / `recycling001`(162)；以及 `121 小租户` / `122 测试租户` 两个测试租户。
      上正式运营前建议逐个确认、禁用或删除。
- [ ] **SSL 证书**：四端目前只有 HTTP，宝塔申请后自动写 443 段。
      申请完要把 `.env.prod` 里 4 个 `http://yfbgeren.baibaitan.com` 改成 `https://`，
      否则自然人端链接、场站二维码、工行回跳仍是 http。
- [ ] **改 root 密码**：部署期间密码以明文形式传递过，建议换成密钥登录。
      sshd 当前是 `PermitRootLogin yes` + `PasswordAuthentication yes`。
- [ ] **轮换 Workbench CLI 用的 AccessKey**：部署期间该 AK/SK 以明文形式传递过。
      它对应的 RAM 用户是 `power-application-user`，只读探测显示权限**跨 region 且覆盖
      ECS/安全组/VPC/镜像**，比这个用途需要的宽（详见 §6.6）。建议另建专用 RAM 用户，
      把 `Resource` 收窄到 `i-bp16czox2yme6947ewrm` 这一台。
- [ ] 工行网关加白名单。
- [ ] 短信渠道报备（要用 ICBC 短信触达时；当前 `ICBC_NOTIFY_SMS_ENABLED=false`）。
- [ ] 三方登录密钥（钉钉/企微/微信，用到再填）。

---

## 5. 关键配置值与本地的一致性

从本地库整体迁到线上独立库（228 张表，`ruoyi-vue-pro`），以下两项**必须**与本地保持同值，
否则库里已加密/已签名的数据解不开：

| 变量 | 值 | 说明 |
|---|---|---|
| `YUDAO_ENCRYPTOR_PASSWORD` | `devonly0123456789` | `mybatis-plus.encryptor.password`，`EncryptTypeHandler` 用 |
| `ICBC_PUBLIC_TOKEN_SECRET` | 与本地同值 | `icbc_public_token` 表已用它签过令牌 |

本地库 `infra_data_source_config` 是空表，所以想换强口令的话清空该表后即可轮换。

---

## 6. 备用通道：Workbench CLI（不依赖 22 端口）

来源：阿里云 Skills 门户的 `alibabacloud-workbench-cli`（Apache-2.0）。
它通过 OpenAPI + WebSocket 走**云助手通道**操作 ECS，**完全不碰 22 端口、也不需要公网 IP**。

### 6.1 为什么需要它

这台机器的 22 端口本来就不顺：

- 安全组只对部分来源放行 —— 全球探测节点（check-host.net）打 `47.99.49.104:22` 全是 timeout
- 我们部署期间被**云盾 aegis 的暴力破解防护**封过至少两次
  （症状是 `kex_exchange_identification: Connection closed by remote host`，等几分钟自动解封）

Workbench 走 443 到 `*.aliyuncs.com`，与安全组、aegis、fail2ban 都无关。
它是**断了 SSH 还能进屋**的那条路，也是换网络 / 换人之后不用重新报白名单的路。

### 6.2 服务端零改动（本来就绪）

```
aliyun.service (Aliyun Assist 云助手)   active，已运行数月
/usr/bin/ecs_config_instance_connect    存在
instance-id  i-bp16czox2yme6947ewrm
region       cn-hangzhou
```

设备信息：ECS 实例 `i-bp16czox2yme6947ewrm`（`launch-advisor-20240802`），
`ecs.u1-c1m4.xlarge`，账号 ID `1245722240329268`。

### 6.3 安装与配置

```bash
# 安装（脚本会校验 sha256，可先下下来审一眼再跑）
curl -fsSL https://workbench-cli.oss-cn-hangzhou.aliyuncs.com/install.sh -o /tmp/wb-install.sh
bash /tmp/wb-install.sh                      # 默认装到 /usr/local/bin（要 sudo）
bash /tmp/wb-install.sh -d "$HOME/.local/bin"  # 没有 sudo 就装用户目录

export PATH="$HOME/.local/bin:$PATH"
workbench version
```

凭证放 `~/.workbench/config.json`（**必须 0600**）：

```json
{
  "current": "default",
  "profiles": {
    "default": {
      "mode": "AK",
      "access_key_id": "<AccessKeyId>",
      "access_key_secret": "<AccessKeySecret>",
      "region": "cn-hangzhou"
    }
  }
}
```

四种认证模式：`AK` / `StsToken` / `RamRoleArn`（自动刷新 STS）/ `CredentialsCmd` / `CredentialsURI`。
生产建议 `RamRoleArn`。

### 6.4 常用命令

```bash
I=i-bp16czox2yme6947ewrm

workbench list ecs --region cn-hangzhou
workbench exec -i $I -c "docker compose -f /opt/youfeibao/docker-compose.yml ps"
workbench exec -i $I -c "df -h / | tail -1"
workbench exec -i $I -c "date '+%F %T'"

workbench upload ./yudao-server.jar /opt/youfeibao/app/yudao-server.jar -i $I
workbench download /opt/youfeibao/logs/yudao-server.log ./ -i $I

workbench daemon status
workbench session list
workbench session close --all
```

**注意**：每次 `exec` 都是**独立 shell**，`cd` / `export` 不保留。有上下文依赖就用 `&&` 串起来。
`upload` 遇到远端同名文件会**交互式询问是否覆盖**（默认 No），自动化场景要先 `ls` 确认。

### 6.5 与 SSH 的取舍（2026-09-23 实测）

| 场景 | Workbench | SSH / scp |
|---|---|---|
| 跑一条命令（冷启动） | 6.5s | ~1–2s |
| 跑一条命令（热身后） | **0.06s** | ~1–2s（每次都要握手） |
| 传 5MB | 2.2s / 2.0s（上传/下载） | 相当 |
| 传 212MB jar | 58s（经 OSS 中转） | **~30s** |
| 抗 aegis 封禁 / 安全组限制 | **完全不受影响** | 会被拦 |
| 断网兜底能力 | **有** | 无 |

结论：**日常跑命令、看日志、改配置优先用 Workbench；超大文件用 scp 更快。两条路都留着。**

文件传输经 OSS 中转 —— 敏感文件要意识到它们经过阿里云 OSS。

### 6.6 权限要求

文档给的最小 RAM 策略（`Resource` 应**收窄到单实例**）：

```json
{
  "Version": "1",
  "Statement": [
    { "Effect": "Allow",
      "Action": ["ecs-workbench:LoginECSInstance", "ecs-workbench:ChatMessages"],
      "Resource": "acs:ecs:cn-hangzhou:<账号ID>:ecs/i-bp16czox2yme6947ewrm" },
    { "Effect": "Allow",
      "Action": ["ecs:DescribeInstances", "ecs:DescribeCloudAssistantStatus", "ecs:StartTerminalSession"],
      "Resource": "acs:ecs:cn-hangzhou:<账号ID>:instance/i-bp16czox2yme6947ewrm" },
    { "Effect": "Allow",
      "Action": "ram:CreateServiceLinkedRole",
      "Resource": "*",
      "Condition": { "StringEquals": { "ram:ServiceName": "workbench.ecs.aliyuncs.com" } } }
  ]
}
```

**不要用主账号 AK。** 也不要把这个 CLI 装到**被它管理的服务器上** ——
那等于把「能控制阿里云 ECS 的凭证」放进被控制的机器里，一旦服务器被拿下就是账号级失陷。
装在运维机上。

### 6.7 已知问题

- CLI 目前是 v1.0.1（2026-08 构建），比较新，遇到问题先 `workbench version` 确认版本。
- macOS 上装到 `/usr/local/bin` 需要 sudo；`sudo` 不可用时用 `-d "$HOME/.local/bin"`。
- 退出码有意义（见官方文档）：`4`=鉴权失败、`5`=网络、`6`=daemon 未起、`7`=会话被占用；
  `exec` 还会把远端命令的退出码透传出来。
