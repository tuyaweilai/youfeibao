/**
 * 实人认证状态的判定（#90）。
 *
 * 后端 `PayeeRealNameStatusEnum`：0 未认证 / 1 认证中 / 2 认证通过 / 3 认证未通过。
 * **1 是「认证中」，不是「已认证」**——司机端 #74 曾把 `=== 1` 当成已认证，
 * 结果是「认证中」被标绿、按钮被藏起来，而真正通过时反而显示成待办。
 *
 * 判定只有这一份：现场端与司机端都从这里取，不再各写一遍魔数。
 */
export const REAL_NAME_STATUS = {
  NOT_STARTED: 0,
  PENDING: 1,
  PASSED: 2,
  FAILED: 3
} as const

/** 实名是否已通过（只有 2 算通过；未认证 / 认证中 / 未通过都不算） */
export function isRealNamePassed(status?: number | null): boolean {
  return status === REAL_NAME_STATUS.PASSED
}
