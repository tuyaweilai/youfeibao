# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

Edit the right-hand column to match whatever vocabulary you actually use.

## 辅助标签（不在五类 triage role 内）

- `blocked` —— 编码已完成，但卡在**外部输入**（供应商 / 第三方 / 运营），交付未闭环。与 `ready-for-human` 并用：`ready-for-human` 说明需要人，`blocked` 说明人不一定能立刻推进（要等对方）。例：#36（短信通道与签名报备）、#37（工行子商户隔离答复）。父票 #30 的「剩余外部依赖」小节跟踪这些。
