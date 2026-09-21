#!/usr/bin/env bash
#
# fleet.sh —— 按 GitHub 原生依赖边，把 ready-for-agent 的票按波次自动跑完。
#
# 一件事：算波次 → 每张票开一个 worktree → 起一个无头 pi 会话实现它 →
# 过闸（有提交 / 工作树干净 / 没碰禁区 / 全量测试绿）→ 合并进 main → 关票 → 下一波。
#
#   scripts/fleet.sh plan             只算波次、打印计划，不动任何东西（先跑这个）
#   scripts/fleet.sh run              跑到没有可跑的票为止
#   scripts/fleet.sh status           看每张票的状态
#   scripts/fleet.sh adopt <票> <PID> <分支> <工作树> <日志>   收养一个已在跑的任务
#   scripts/fleet.sh gate <票>        只过闸，不合并
#   scripts/fleet.sh review <票>      只跑独立评审
#   scripts/fleet.sh integrate <票>   只合并（闸门过了才调用）
#
# 环境变量：MAX_PARALLEL=2  MAX_MIN=240  STALL_MIN=30  PUSH=0  REVIEW=0  REVIEW_MIN=40  SKIP=81
#
# 它**不做**的事，别指望：解合并冲突、替代人工验收（真机冒烟之类）、判断验收清单
# 里那些只有人才能验的条目。撞了冲突就停那一张、打 ready-for-human，等人。
set -uo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
FLEET="$ROOT/.fleet"
BRIEFS="$ROOT/scripts/fleet/briefs"
SKILLS="$HOME/.pi/agent/skills"
STATE="$FLEET/state.tsv"

MAX_PARALLEL=${MAX_PARALLEL:-2}
MAX_PARALLEL=${MAX_PARALLEL:-2}
# 两个上限都要：光看墙钟会把「还在认真干活」的杀棹；光看卡死会把「一直绕圈」的一直养着。
# 运行日志只在跑完时写，所以它的 mtime 就是启动时间；会话文件是实时写的，拿它算「卡死」。
MAX_MIN=${MAX_MIN:-240}     # 单票墙钟上限（分钟）
STALL_MIN=${STALL_MIN:-30}  # 多久没有动静算卡死（分钟）
PUSH=${PUSH:-0}
# 独立评审：不过人眼就合，就得有个不同上下文的会话拿着议题去对着 diff 找茬。
# 它只审不改、输出一行机器可读的结论；超时算「没过」——没审完的东西不往 main 上合。
REVIEW=${REVIEW:-0}
REVIEW_MIN=${REVIEW_MIN:-40}
# 父票 / 史诗票不进舰队：它们是若干子票的集合，没有可交付物。用空格分隔。
SKIP=${SKIP:-81}
TEST_CMD=${TEST_CMD:-"mvn -o -pl yudao-module-icbc/yudao-module-icbc-api,yudao-module-icbc/yudao-module-icbc-biz test"}

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null)
PARENT=$(dirname "$ROOT")

mkdir -p "$FLEET/logs" "$FLEET/prompts" "$FLEET/gates"

die()  { echo "✗ $*" >&2; exit 1; }
info() { echo "· $*"; }
ok()   { echo "✓ $*"; }

# ---------------------------------------------------------------- 状态文件
state_set() { # n status branch wt pid ec menu log
  local n=$1 tmp="$STATE.$$"
  shift
  [ -f "$STATE" ] && grep -v "^$n	" "$STATE" > "$tmp" || : > "$tmp"
  ( IFS=$'\t'; echo "$n	$*" ) >> "$tmp"
  mv "$tmp" "$STATE"
}
state_field() { [ -f "$STATE" ] && awk -F'\t' -v n="$1" -v f="$2" '$1==n{print $f}' "$STATE"; }
state_known() { [ -f "$STATE" ] && awk -F'\t' -v n="$1" '$1==n{found=1} END{exit !found}' "$STATE"; }

# 串行化的互斥锁（macOS 没有 flock，用 mkdir 的原子性）
lock()   { local l="$FLEET/$1.lock" i=0; while ! mkdir "$l" 2>/dev/null; do i=$((i+1)); [ $i -gt 1800 ] && die "等锁超时：$1"; sleep 1; done; }
unlock() { rmdir "$FLEET/$1.lock" 2>/dev/null; }

# ---------------------------------------------------------------- 依赖图
fetch_graph() {
  local q='query { repository(owner:"'"${REPO%%/*}"'", name:"'"${REPO##*/}"'") {
      issues(first: 100, states: OPEN, labels: ["ready-for-agent"]) {
        nodes { number title blockedBy(first: 20) { nodes { number state } } } } } }'
  gh api graphql -f query="$q" \
     --jq '[.data.repository.issues.nodes[] | {number, title, blockedBy: [.blockedBy.nodes[]]}]'
}
graph_excluding_skip() { fetch_graph | jq --argjson skip "[$(echo $SKIP | tr ' ' ',')]" '[.[] | select(.number as $n | ($skip | index($n)) | not)]'; }

cmd_plan() {
  local g; g=$(graph_excluding_skip)
  echo "跳过（SKIP）：$(echo $SKIP | tr ' ' ' ')"
  echo
  echo "== 依赖图 =="
  echo "$g" | jq -r '.[] | "  #\(.number)  ← \(if (.blockedBy|length)==0 then "（无前置）" else ([.blockedBy[]|"#\(.number)(\(.state))"]|join("  ")) end)  \(.title[0:34])"'
  echo
  echo "== 波次 =="
  echo "$g" | jq -c -f "$ROOT/scripts/fleet/layers.jq" | jq -r 'to_entries[] | "  波次 \(.key+1)：\(.value | map("#"+(tostring)) | join("  "))"'
  local placed; placed=$(echo "$g" | jq -c -f "$ROOT/scripts/fleet/layers.jq" | jq '[.[][]] | length')
  local total; total=$(echo "$g" | jq 'length')
  [ "$placed" != "$total" ] && echo "  ⚠ 有 $((total-placed)) 张被非 ready-for-agent 的票挡着，不在任何波次里"
  echo
  echo "== 目前马上能起 =="
  for n in $(frontier_now); do echo "  #$n $(brief_status "$n")"; done
  local inflight; inflight=$(running_tickets)
  [ -n "$inflight" ] && for n in $inflight; do echo "  #$n 已在跑（pid $(state_field "$n" 5)）"; done
  echo
  echo "（MAX_PARALLEL=${MAX_PARALLEL}，单票墙钟上限 ${MAX_MIN} 分钟 / 卡死 ${STALL_MIN} 分钟，PUSH=${PUSH}）"
}

# 现在就能起的票：图里无未清前置、不在 SKIP、还没被本舰队碰过（要么在跑）
frontier_now() {
  local running; running=$(running_tickets)
  graph_excluding_skip | jq -c -f "$ROOT/scripts/fleet/layers.jq" | jq -r '.[0][]?' | while read -r n; do
    echo "$running" | grep -qw "$n" && continue
    state_known "$n" && continue
    echo "$n"
  done
}

running_tickets() {
  [ -f "$STATE" ] || return 0
  awk -F'\t' '$2=="running"{print $1}' "$STATE"
}

brief_status() {
  local n=$1
  [ -f "$BRIEFS/$n.md" ] && echo "" || echo "⚠ 缺 brief：scripts/fleet/briefs/$n.md（没有它不启动）"
}

# ---------------------------------------------------------------- 段位分配
# 分配不能只看 main：正在跑的票改在自己工作树的**未提交**文件里。所以扫一遍
# 「main + 所有在跑票的工作树」里的实际值，取最大段 +1。这样无论 state 里记了什么都不撞。
EC_FILE="backend/yudao-module-icbc/yudao-module-icbc-api/src/main/java/cn/iocoder/yudao/module/icbc/enums/ErrorCodeConstants.java"
MENU_FILE="backend/sql/mysql/icbc-menu.sql"

scan_files() { # <相对路径> -> 路径列表
  echo "$ROOT/$1"
  local w
  for w in $(running_tickets); do
    local wt; wt=$(state_field "$w" 4)
    [ -n "$wt" ] && [ -d "$wt" ] && echo "$wt/$1"
  done
}

next_ec_segment() {
  # 两个来源都要看，少一个就会撞：
  #  ① 磁盘上的实际值——收养的票、以及已经动手写过的在跑票
  #  ② state 里刚分配出去的段——同一波次里刚起的票，工作树还是干净的 main，扫不到
  local from_files from_state max
  from_files=$(scan_files "$EC_FILE" | xargs grep -oh '1_030_[0-9][0-9][0-9]_' 2>/dev/null \
               | sed 's/1_030_\([0-9]*\)_/\1/' | sort -n | tail -1)
  from_state=$([ -f "$STATE" ] && awk -F'\t' '{print $6}' "$STATE" \
               | sed -n 's/^1_030_\([0-9][0-9]*\)_xxx$/\1/p' | sort -n | tail -1)
  max=${from_files:-0}
  [ -n "${from_state:-}" ] && [ "$from_state" -gt "$max" ] && max=$from_state
  printf '1_030_%03d_xxx' $((10#$max + 1))
}
next_menu_segment() {
  # 在 5280–5380 里找第一段连续 10 个没人用的 id
  local used from_state
  used=$(scan_files "$MENU_FILE" | xargs grep -ohE '\(5[0-9]{3},' 2>/dev/null | tr -d '(,' | sort -n | uniq)
  from_state=$([ -f "$STATE" ] && awk -F'\t' '{print $7}' "$STATE" \
               | sed -n 's/^\([0-9][0-9]*\)-\([0-9][0-9]*\)$/\1 \2/p' | while read -r a b; do seq "$a" "$b"; done)
  local all; all="$(echo "$used"; echo "$from_state")"
  local cand=5280
  while [ "$cand" -le 5380 ]; do
    local free=1 i=0
    while [ $i -lt 10 ]; do
      if echo "$all" | grep -qx "$((cand+i))"; then free=0; break; fi
      i=$((i+1))
    done
    [ $free = 1 ] && { echo "$cand-$((cand+9))"; return; }
    cand=$((cand+10))
  done
  echo "（无空闲段本票不要加菜单）"
}

# ---------------------------------------------------------------- 启动
cmd_launch() {
  local n=$1
  [ -f "$BRIEFS/$n.md" ] || die "#$n 没有 brief（scripts/fleet/briefs/$n.md），不启动。判断部分没法自动生成。"

  lock alloc
  local ec menu; ec=$(next_ec_segment); menu=$(next_menu_segment)
  local slug; slug=$(gh issue view "$n" --json title -q .title | tr -c 'A-Za-z0-9' '-' | sed 's/-\{2,\}/-/g;s/^-//;s/-$//' | cut -c1-28)
  local branch="i$n-$slug" wt="$PARENT/youfeibao-$n" log="$FLEET/logs/$n.log"
  unlock alloc

  if [ -d "$wt" ]; then info "#$n 工作树已存在，复用：$wt"; elif [ "${RENDER_ONLY:-0}" != 1 ]; then
    git -C "$ROOT" worktree add "$wt" -b "$branch" main >/dev/null 2>&1 || die "建工作树失败：$wt"
  fi

  local forbidden; forbidden=$(sed -n 's/^<!-- *forbidden: *\(.*\) *-->$/\1/p' "$BRIEFS/$n.md")
  local base; base=$(git -C "$ROOT" rev-parse --short main)
  local title; title=$(gh issue view "$n" --json title -q .title)

  local prompt="$FLEET/prompts/$n.md"
  {
    echo "# 派工书：#$n $title"
    echo
    cat "$BRIEFS/$n.md"
    echo
    sed -e "s|@@ISSUE@@|$n|g" -e "s|@@BRANCH@@|$branch|g" -e "s|@@WORKTREE@@|$wt|g" \
        -e "s|@@BASE@@|$base|g" -e "s|@@ROOT@@|$ROOT|g" -e "s|@@BRIEF@@|$BRIEFS/$n.md|g" \
        -e "s|@@SKILLS@@|$SKILLS|g" -e "s|@@EC_SEG@@|$ec|g" -e "s|@@MENU_SEG@@|$menu|g" \
        -e "s|@@TEST_CMD@@|$TEST_CMD|g" -e "s|@@FORBIDDEN@@|${forbidden:-（无）}|g" \
        -e "s|@@COMMIT_PREFIX@@|feat(icbc):|g" \
        "$ROOT/scripts/fleet/common.md"
  } > "$prompt"

  if [ "${RENDER_ONLY:-0}" = 1 ]; then
    ok "只渲染，不启动。派工书：${prompt}（错误码段 ${ec}，菜单段 ${menu}）"
    return 0
  fi

  # 包一层，好在日志尾巴留下退出码——舰队靠它区分「跑完」和「崩了」
  local runner="$FLEET/run-$n.sh"
  cat > "$runner" <<EOF
#!/bin/bash
cd "$wt" || exit 9
env -u PI_SESSION_FILE -u PI_SESSION_ID pi -p "\$(cat "$prompt")" --name "$branch"
echo "PI_EXIT=\$?"
EOF
  chmod +x "$runner"
  nohup "$runner" > "$log" 2>&1 &
  local pid=$!
  sleep 3
  # pi 是 runner 的子进程，记它
  local ppid_pi; ppid_pi=$(pgrep -P "$pid" | head -1)
  state_set "$n" running "$branch" "$wt" "${ppid_pi:-$pid}" "$ec" "$menu" "$log"
  ok "#$n 已启动  pid=${ppid_pi:-$pid}  分支 $branch  错误码段 $ec  菜单段 $menu"
  info "    日志 tail -f $log"
}

cmd_adopt() { # n pid branch wt log [ec] [menu]
  local n=$1 pid=$2 branch=$3 wt=$4 log=$5 ec=${6:-（继承）} menu=${7:-（继承）}
  state_set "$n" running "$branch" "$wt" "$pid" "$ec" "$menu" "$log"
  ok "#$n 已收养 pid=${pid}（错误码段 ${ec}，菜单段 ${menu}）"
}

# 最后一次有动静的时间：运行日志 + 该工作树自己的 pi 会话文件（会话文件是实时的，日志要到跑完才写）
last_activity() {
  local wt=$1 log=$2 d f m newest=0
  d="$HOME/.pi/agent/sessions/--$(echo "$wt" | sed 's|^/||; s|/|-|g')--"
  for f in "$log" $(ls -t "$d"/*.jsonl 2>/dev/null | head -1); do
    [ -f "$f" ] || continue
    m=$(stat -f %m "$f" 2>/dev/null || echo 0)
    [ "$m" -gt "$newest" ] && newest=$m
  done
  echo "$newest"
}

# ---------------------------------------------------------------- 等一波跑完
wait_wave() {
  while :; do
    local alive=0
    for n in $(running_tickets); do
      local pid wt log; pid=$(state_field "$n" 5); wt=$(state_field "$n" 4); log=$(state_field "$n" 8)
      if kill -0 "$pid" 2>/dev/null; then
        alive=$((alive+1))
        local elapsed=$(( ($(date +%s) - $(stat -f %m "$log" 2>/dev/null || echo 0)) / 60 ))
        local idle=$(( ($(date +%s) - $(last_activity "$wt" "$log")) / 60 ))
        local why=""
        [ "$idle" -ge "$STALL_MIN" ] && why="卡死 ${idle} 分钟（上限 ${STALL_MIN}）"
        [ "$elapsed" -ge "$MAX_MIN" ] && why="跑到墙钟上限 ${elapsed} 分钟"
        if [ -n "$why" ]; then
          info "#$n 被杀：${why}"
          kill -TERM "$pid" 2>/dev/null; sleep 3; kill -KILL "$pid" 2>/dev/null
          state_set "$n" timeout "$(state_field "$n" 3)" "$wt" "$pid" \
                    "$(state_field "$n" 6)" "$(state_field "$n" 7)" "$log"
        fi
      fi
    done
    [ "$alive" = 0 ] && break
    info "还有 $alive 个在跑…（$(date '+%H:%M:%S')）"
    sleep 30
  done
}

# surefire 的每个测试类都会打一行 `Tests run:`，真正的总计在 `Results:` 之后那一行。
# 直接 `grep ... | tail -1` 会拿到最后一个测试类的小计，报告里看上去像只有几条测试。
test_summary() { # <日志>
  awk '/^\[INFO\] Results:/{found=1} found && /Tests run:/{sub(/^\[INFO\] /, ""); print; exit}' "$1"
}

# ---------------------------------------------------------------- 闸门
cmd_gate() {
  local n=$1 wt; wt=$(state_field "$n" 4); local branch; branch=$(state_field "$n" 3)
  local report="$FLEET/gates/$n.md"
  local pass=1
  : > "$report"
  { echo "# 闸门报告 #$n"; echo; } >> "$report"

  if [ ! -d "$wt" ]; then echo "✗ 工作树不存在：$wt" | tee -a "$report"; return 1; fi

  local commits; commits=$(git -C "$wt" rev-list --count main..HEAD 2>/dev/null || echo 0)
  if [ "$commits" = 0 ]; then echo "- ✗ 分支上没有任何提交" | tee -a "$report" >&2; pass=0
  else echo "- ✓ 提交数：$commits" >> "$report"; fi

  local dirty; dirty=$(git -C "$wt" status --porcelain | wc -l | tr -d ' ')
  if [ "$dirty" != 0 ]; then echo "- ✗ 工作树不干净（$dirty 个文件没提交）" | tee -a "$report" >&2; pass=0
  else echo "- ✓ 工作树干净" >> "$report"; fi

  local forbidden; forbidden=$(sed -n 's/^<!-- *forbidden: *\(.*\) *-->$/\1/p' "$BRIEFS/$n.md")
  if [ -n "$forbidden" ]; then
    local hit=""
    for f in $forbidden; do
      local m; m=$(git -C "$wt" diff --name-only main...HEAD | grep -F "$f" || true)
      [ -n "$m" ] && hit="$hit$m
"
    done
    if [ -n "$hit" ]; then echo "- ✗ 碰了禁区：
$(echo "$hit" | sed 's/^/    /')" | tee -a "$report" >&2; pass=0
    else echo "- ✓ 没碰禁区（${forbidden}）" >> "$report"; fi
  fi

  # 测试：并行票共用一个 ~/.m2 与一份 CPU，串行跑
  local tlog="$FLEET/logs/$n.tests.log"
  info "#$n 跑全量测试（串行，日志 ${tlog}）…"
  lock test
  ( cd "$wt/backend" && eval "$TEST_CMD" ) > "$tlog" 2>&1
  local rc=$?
  unlock test
  local summary; summary=$(test_summary "$tlog")
  if [ $rc = 0 ] && grep -q 'BUILD SUCCESS' "$tlog"; then
    echo "- ✓ 测试：$summary" >> "$report"
  else
    echo "- ✗ 测试没过（rc=${rc}）：$(grep -E 'Tests run:.*Failures: [1-9]|ERROR\]' "$tlog" | head -5 | sed 's/^/    /')" >> "$report"
    pass=0
  fi

  local exitline; exitline=$(grep -o 'PI_EXIT=[0-9]*' "$(state_field "$n" 8)" 2>/dev/null | tail -1)
  [ -n "$exitline" ] && echo "- $exitline" >> "$report"

  if [ $pass = 1 ]; then ok "#$n 过闸（${summary}）"; return 0; else echo "✗ #$n 没过闸，详见 $report" >&2; return 1; fi
}

# ---------------------------------------------------------------- 合并
cmd_integrate() {
  local n=$1 branch; branch=$(state_field "$n" 3)
  local wt; wt=$(state_field "$n" 4)
  local title; title=$(gh issue view "$n" --json title -q .title)

  if ! git -C "$ROOT" diff --quiet || ! git -C "$ROOT" diff --cached --quiet; then
    die "main 工作树不干净，先处理：$ROOT"
  fi
  git -C "$ROOT" checkout -q main
  if git -C "$ROOT" merge --no-ff "$branch" -q -m "merge(#$n): $title" 2>"$FLEET/logs/$n.merge.log"; then
    ok "#$n 已合并（$commits 个提交）"
  else
    git -C "$ROOT" merge --abort 2>/dev/null
    state_set "$n" conflict "$branch" "$wt" "" "$(state_field "$n" 6)" "$(state_field "$n" 7)" "$(state_field "$n" 8)"
    gh issue edit "$n" --add-label ready-for-human >/dev/null 2>&1
    gh issue comment "$n" --body "自动舰队：合并 \`$branch\` 到 main 时**撞了冲突**，已 abort，没有硬合。冲突文件见 \`$FLEET/logs/$n.merge.log\`。分支留在本地，请人工按 \`/resolving-merge-conflicts\` 的方式按意图解。" >/dev/null 2>&1
    die "#$n 合并冲突，已 abort 并标 ready-for-human（分支保留）"
  fi

  # 人工解完冲突再跑一次时，分支已经合过了，`main..branch` 会数成 0——改从合并提交自身数。
  local merged_sha; merged_sha=$(git -C "$ROOT" rev-parse --short HEAD)
  local commits; commits=$(git -C "$ROOT" rev-list --count "${merged_sha}^1..${merged_sha}^2" 2>/dev/null || echo 0)
  local summary; summary=$(test_summary "$FLEET/logs/$n.tests.log" 2>/dev/null)
  local files; files=$(git -C "$ROOT" diff --name-only "HEAD^1..HEAD" | wc -l | tr -d ' ')

  # 日志路径按 state 里记的来：收养的票日志在仓库外（/tmp），写死 $FLEET/logs/$n.log 会找不到
  local log; log=$(state_field "$n" 8)
  local review_note=""
  if [ -f "$FLEET/gates/$n.review.md" ]; then
    review_note="
- 独立评审：$(grep -o 'REVIEW_VERDICT: [A-Z]*' "$FLEET/gates/$n.review.md" | tail -1 | awk '{print $2}')（报告 \`.fleet/gates/$n.review.md\`）"
  fi

  # 只记一行「合并记录」：实现细节由这张票自己的 handoff 段写（agent 写的），
  # 驱动再凭空追加一整节会变成两段同标题的重复。
  {
    echo
    echo "### 合并记录：#${n}（自动舰队）"
    echo
    echo "- 分支 \`$branch\` → \`$merged_sha\`：$files 个文件、$commits 个提交${review_note}"
    echo "- 闸门：全量 icbc \`$summary\`；报告 \`.fleet/gates/$n.md\`，运行日志 \`$log\`（\`.fleet/\` 与收养票的仓库外日志不入库）"
  } >> "$ROOT/docs/agents/handoff.md"

  git -C "$ROOT" add docs/agents/handoff.md
  git -C "$ROOT" commit -q -m "docs(handoff): #$n 记录（自动舰队）"
  state_set "$n" merged "$branch" "$wt" "" "$(state_field "$n" 6)" "$(state_field "$n" 7)" "$log"
  [ -n "$wt" ] && [ -d "$wt" ] && git -C "$ROOT" worktree remove --force "$wt" >/dev/null 2>&1
  [ "$PUSH" = 1 ] && {
    if git -C "$ROOT" push origin main > "$FLEET/logs/push.log" 2>&1; then
      info "已 push 到 origin/main"
    else
      echo "✗ push 失败！票已合并并关，但远端 main 落后了。看 $FLEET/logs/push.log" >&2
      gh issue comment "$n" --body "⚠ 自动舰队：**本地**已合并（\`$merged_sha\`）并关票，但 \`git push origin main\` **失败**。远端 main 落后于本地，需要人工推一次。" >/dev/null 2>&1
    fi
  }

  local log; log=$(state_field "$n" 8)
  local tail_report; tail_report=$(sed -n '/验收清单/,$p' "$log" 2>/dev/null | head -20)
  gh issue comment "$n" --body "自动舰队已合并到 main：
- 合并提交 \`$merged_sha\`（$commits 个提交，$files 个文件）
- 闸门：全量 icbc \`$summary\`
- 报告：\`.fleet/gates/$n.md\`、复审 \`.fleet/gates/$n.review.md\`（\`.fleet/\` 不入库）

**机器验不了、留给人验收的**（agent 报告里的那一段）：

${tail_report:-（报告里没有这一节，请翻 ${log}）}" >/dev/null 2>&1
  gh issue close "$n" --comment "代码与验收已核，关闭。" >/dev/null 2>&1
  ok "#$n 已关票"
}

# ---------------------------------------------------------------- 带超时地跑一个子进程
# macOS 没有 timeout/gtimeout，自己轮询。返回子进程的退出码；超时返回 2。
run_capped() { # <分钟> <命令...>
  local cap=$1; shift
  "$@" &
  local p=$! waited=0
  while kill -0 "$p" 2>/dev/null; do
    if [ "$waited" -ge $((cap * 60)) ]; then
      kill -TERM "$p" 2>/dev/null; sleep 2; kill -KILL "$p" 2>/dev/null
      wait "$p" 2>/dev/null
      return 2
    fi
    sleep 5; waited=$((waited + 5))
  done
  wait "$p"
  return $?
}

# ---------------------------------------------------------------- 独立评审（可选闸门）
# 不过人眼就合，就得有个没参与过实现、上下文干净的会话拿着议题去找茬。
# 它只审不改；没拿到结论（跑不完 / 没输出结论行）都算不过——没审完的东西不往 main 上合。
cmd_review() {
  local n=$1 wt branch title
  wt=$(state_field "$n" 4); branch=$(state_field "$n" 3)
  title=$(gh issue view "$n" --json title -q .title)
  [ -d "$wt" ] || { echo "✗ 工作树不存在：#${n}" >&2; return 1; }

  local rprompt="$FLEET/prompts/$n.review.md" rlog="$FLEET/logs/$n.review.log"
  cat > "$rprompt" <<EOF
# 独立评审：#$n ${title}

你是一个**独立评审者**，没有参与过这份实现。工作目录：${wt}，分支 \`${branch}\`，基线 \`main\`。

先读 ${SKILLS}/code-review/SKILL.md，按它做**两轴**评审：

- **Standards**：是否符合本仓库的规范（AGENTS.md、CONTEXT.md、相关 ADR、既有写法）。
- **Spec**：\`gh issue view ${n} --comments\` 的验收清单是不是真的做到了。

评审范围：\`git diff main...HEAD\`，配合 \`git log main..HEAD\` 看它是怎么分步做的。相关背景在 ${BRIEFS}/${n}.md。

**你是只读的。**不许改任何文件，不许 git add / commit / checkout / stash / reset。发现要改的地方写进报告，不要自己动手。

尤其盯这几类（它们正好是自动闸门看不住的）：

1. **测试是不是假的**：断言太弱（只断言不报错）、把被测逻辑在测试里重写一遍、跳过了关键分支。
2. **口径是不是对**：字段落在错的表上、状态机少了分支、验收清单里某条被默默跳过。
3. **交付物是不是真的**：声称做了但代码里没有，或者只是个空壳。
4. **有没有踩别人的地**：\`git diff --name-only main...HEAD\` 里有没有冻结的契约、别的票的领地。

报告结尾**必须**是单独一行、不带任何其它字：

\`REVIEW_VERDICT: PASS\` 或 \`REVIEW_VERDICT: BLOCK\`

拿不准就 BLOCK，并在上面写清是哪一条、为什么。错杀的代价是等人看一眼；漏放的代价是 main 上多一块脏东西，而所有下游票都建在它上面。
EOF

  if [ "${RENDER_ONLY:-0}" = 1 ]; then info "只渲染，不启动：$rprompt"; return 0; fi
  info "#$n 独立评审中（上限 ${REVIEW_MIN} 分钟，日志 ${rlog}）…"
  run_capped "$REVIEW_MIN" env -u PI_SESSION_FILE -u PI_SESSION_ID pi -p "$(cat "$rprompt")" --name "review-$n" > "$rlog" 2>&1
  local rc=$?

  cp "$rlog" "$FLEET/gates/$n.review.md" 2>/dev/null
  local verdict
  verdict=$(grep -o 'REVIEW_VERDICT: [A-Z]*' "$rlog" 2>/dev/null | tail -1 | awk '{print $2}')
  if [ "$rc" = 2 ]; then
    echo "✗ #$n 评审没跑完（超过 ${REVIEW_MIN} 分钟），按不过处理" >&2; return 1
  fi
  case "${verdict}" in
    PASS)  ok "#$n 独立评审：PASS"; return 0 ;;
    BLOCK) echo "✗ #$n 独立评审：BLOCK，详见 $FLEET/gates/$n.review.md" >&2; return 1 ;;
    *)     echo "✗ #$n 评审没给出结论行，按不过处理" >&2; return 1 ;;
  esac
}

park() { # <票> <状态> <给议题的说明>
  local n=$1 st=$2 why=$3
  state_set "$n" "$st" "$(state_field "$n" 3)" "$(state_field "$n" 4)" "" \
            "$(state_field "$n" 6)" "$(state_field "$n" 7)" "$(state_field "$n" 8)"
  gh issue edit "$n" --add-label ready-for-human >/dev/null 2>&1
  gh issue comment "$n" --body "$why" >/dev/null 2>&1
  info "#$n 停下等人（不自动重试）"
}

# ---------------------------------------------------------------- 主循环
cmd_run() {
  while :; do
    local started=0
    for n in $(frontier_now | head -n "$MAX_PARALLEL"); do
      cmd_launch "$n"
      started=$((started+1))
    done
    [ "$started" -gt 0 ] && info "本波起了 $started 张"
    if [ -z "$(running_tickets)" ]; then
      [ "$started" = 0 ] && { ok "没有可跑的票了（或都已在跑 / 都被 SKIP 挡着）"; break; }
    fi
    wait_wave
    for n in $(running_tickets); do
      if ! cmd_gate "$n"; then
        park "$n" gated-out "自动舰队：这张票**没过闸**（详见 \`.fleet/gates/$n.md\`，运行日志 \`.fleet/logs/$n.log\`）。分支 \`$(state_field "$n" 3)\` 与工作树保留，等人接手。没有自动重试——同样的失败重跑一遍通常还是同样的失败。"
        continue
      fi
      if [ "$REVIEW" = 1 ] && ! cmd_review "$n"; then
        park "$n" review-blocked "自动舰队：全量测试过了，但**独立评审没过**（报告 \`.fleet/gates/$n.review.md\`）。分支 \`$(state_field "$n" 3)\` 保留。评审者拿不准就会 BLOCK——请看一眼它指的那一条。"
        continue
      fi
      cmd_integrate "$n"
    done
  done
  cmd_status
}

cmd_status() {
  [ -f "$STATE" ] || { echo "（还没有任何票跑过）"; return; }
  printf '%-6s %-11s %-28s %-7s %s\n' 票 状态 分支 PID 备注
  awk -F'\t' '{printf "%-6s %-11s %-28s %-7s %s\n", $1, $2, $3, $5, $8}' "$STATE"
  echo
  echo "日志：$FLEET/logs/   闸门报告：$FLEET/gates/   派工书：$FLEET/prompts/"
}

case "${1:-}" in
  plan)      cmd_plan ;;
  run)       cmd_run ;;
  status)    cmd_status ;;
  launch)    cmd_launch "$2" ;;
  adopt)     cmd_adopt "$2" "$3" "$4" "$5" "$6" "${7:-}" "${8:-}" ;;
  gate)      cmd_gate "$2" ;;
  review)    cmd_review "$2" ;;
  integrate) cmd_integrate "$2" ;;
  *) sed -n '2,20p' "$0" | sed 's/^# \?//' ;;
esac
