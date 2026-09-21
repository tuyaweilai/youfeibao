# 机械部分（每张票都一样的那些话，由 fleet.sh 追加在 brief 后面）

上面的 brief 是这张票的**判断部分**；下面是机械部分。

## 你在哪里

- 工作树：`@@WORKTREE@@`，分支：`@@BRANCH@@`，基线：`@@BASE@@`（main 的提交，已含所有前置票）
- **不要合并、不要切分支、不要 `git push`。** 做完就提交到 `@@BRANCH@@`，剩下的交给舰队。
- 仓库根在 `@@ROOT@@`，派工书写在 `@@BRIEF@@`。

## 流程

读并遵循：

- `@@SKILLS@@/implement/SKILL.md`（总流程）
- `@@SKILLS@@/tdd/SKILL.md`（一片红一片绿地推进，@seams 见 brief）
- `@@SKILLS@@/code-review/SKILL.md`（收尾自查，两轴：Standards + Spec）

先把上下文读齐再动手：`AGENTS.md`、`CONTEXT.md`、`gh issue view @@ISSUE@@ --comments`（**议题正文即 spec**，评论是最新的口径修订）、议题引用的 ADR、以及 `docs/agents/handoff.md` 里相关段落。

## 段位（并行票靠它才不撞车）

- **错误码段：本票用 `@@EC_SEG@@`**（写进 `ErrorCodeConstants` 时按这个段；只追加、不重排既有码）。
- **菜单段：本票用 `@@MENU_SEG@@`**（写进 `backend/sql/mysql/icbc-menu.sql`；**不需要菜单就别加**。注意该文件会先删 5100–5399 再重建，行必须写进文件才不会被清掉）。
- 脊柱文件（`backend/sql/mysql/README.md`、测试 `src/test/resources/sql/create_tables.sql` · `clean.sql`、`docs/agents/handoff.md`）一律**只追加、不重排**。

## 禁区

以下路径本票**不许改**（别的票正在那边干活，或者那是已经冻结的契约）：

```
@@FORBIDDEN@@
```

## 构建与测试

- **不要跑 `mvn install`**，也不要把 `-api` 与 `-biz` 拆成两次跑——所有并行票共用 `~/.m2`，`install` 会互相覆盖 SNAPSHOT，表现为「刚编译过、现在找不到符号」。一律用一条 reactor 命令：
  - 单测：`cd backend && @@TEST_CMD@@ -Dtest=你的测试类`
  - 收尾全量：`cd backend && @@TEST_CMD@@`
- 前端：这个工作树里**没有 `node_modules`**。先 `pnpm install --prefer-offline`；装不动就从主工作树软链（`ln -s @@ROOT@@/backend/yudao-ui/<app>/node_modules node_modules`）。PC 后台额外需要 `src/types/auto-imports.d.ts` / `auto-components.d.ts`（vite 生成物，被 gitignore，可从主工作树拷贝）与 `NODE_OPTIONS=--max_old_space_size=8192`。
- **PC 后台本来就有 1254 条既存类型错误**：判断标准是「新增文件零错误」，不是 `ts:check` 全绿；至少 `pnpm build:local` 要通过。

## 提交前自己过一遍这几面透镜

单测全绿 + 验收逐条核，盖不住「默认配置下真实会发生什么」。三轮独立评审被拦下的问题，共同点都是这个。提交前请自己回答，并写进报告：

1. **默认配置**（`application.yaml`，不是 `application-local.yaml`）下，这条新路径的哪些开关是**开着**的？平台的 `ApiAccessLogFilter` 默认记录所有 `/admin-api` 的 JSON 请求体，而 `SANITIZE_KEYS` 只脱敏 `password/token`——所以请求体里**不能出现**影像字节、身份证号、银行卡号、手机号、住址。带这些的接口要 `@ApiAccessLog(requestEnable = false)`，并留一条能钉住它的测试。
2. **PII 去哪了**：请求体 / 查询串 / 路径参数 / 日志 / 异常栈 / 新建的表 / 文件服务，有没有不该留的？ADR 0037 定了证件影像识别完即弃。
3. **异常路径会不会丢输入或卡住**：弱网、超时、重复提交、第三方报错时，用户已经输入的东西还在不在？状态会不会推到回不来的地方（拿不到链接、回不到上一步）？现场端的主要故障模式就是弱网。
4. **只有人才能验的**（真机相机、第三方联调）单独列一节，**不要记成「已验」**。
5. **有没有把「临时态 / 预留 / 等下游」写成「已经做到」**？注释、枚举文档、报告里都算。

## 交付

- **只 `git add` 你自己改的文件，不要 `git add -A`。**
- 提交到 `@@BRANCH@@`，标题 `@@COMMIT_PREFIX@@ <一句话>`，正文中文：做了什么、口径取舍、测试数字，末尾 `Refs #@@ISSUE@@`。
- 结束前跑一次全量测试，把数字写进提交信息。
- 最后回一份简短报告，必须包含三段：**改了什么**、**测试数字**、**验收清单逐条对账**（哪几条没做到、为什么、留给谁）。舰队按这份报告和测试结果决定要不要合并——**报告里不要含糊其辞，做不到就直说，含糊只会让下一票踩到。**
