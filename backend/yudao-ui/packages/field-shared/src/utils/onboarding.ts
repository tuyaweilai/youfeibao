/**
 * 收方入驻状态的判定（#90 顺带修的同类问题）。
 *
 * 后端 `PayeeOnboardingOutcomeEnum`：PENDING 审核中 / READY 入驻完成 / REJECTED 审核拒绝。
 * 司机端 #74 曾用 `onboardingState === 'SUCCESS'` 判断「入驻完成」——不存在这个取值，
 * 标签永远显示成待办色。判定的字面量只放这一处，两端共用。
 */
export const ONBOARDING_STATE = {
  PENDING: 'PENDING',
  READY: 'READY',
  REJECTED: 'REJECTED'
} as const

/** 收方入驻是否已完成（只有 READY 算；审核中 / 拒绝 / 未开始都不算） */
export function isOnboardingReady(state?: string | null): boolean {
  return state === ONBOARDING_STATE.READY
}
