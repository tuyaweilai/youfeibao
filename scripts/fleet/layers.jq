# 输入：开放且 ready-for-agent 的票，每张带 blockedBy（含 blocker 的 state）
# 输出：波次数组，每波是票号数组；不在任何波次里的就是「被非 ready-for-agent 的票挡着」
#
# 注意：GraphQL 的 state 是枚举名（OPEN / CLOSED），REST 是小写——这里按 GraphQL 比较。
def ready($satisfied):
  [ .[] | select( ([.blockedBy[] | select(.state=="OPEN") | .number]
                  | map(select(. as $b | $satisfied | index($b) | not)) | length) == 0)
        | .number ];

def wave($remaining; $satisfied):
  ($remaining | ready($satisfied)) as $this
  | if ($this | length) == 0 then []
    else [ $this ] + wave([ $remaining[] | select(.number as $n | $this | index($n) | not) ]; $satisfied + $this)
    end;

. as $all
| ([ $all[] | .blockedBy[] | select(.state != "OPEN") | .number ] | unique) as $closed
| wave($all; $closed)
