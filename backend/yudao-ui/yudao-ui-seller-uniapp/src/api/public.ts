import { get, post } from '@/utils/request'
import { API_BASE_URL } from '@/config/env'

export interface QuotaMonthVO {
  month?: string
  issuedAmount?: number
  pendingAmount?: number
  redOffsetAmount?: number
  netAmount?: number
  amountAtOnePercent?: number
  amountAtThreePercent?: number
  otherAmount?: number
  overMonthlyExempt?: boolean
}

export interface QuotaVO {
  name?: string
  idCardMasked?: string
  capAmount?: number
  issuedAmount?: number
  pendingAmount?: number
  redOffsetAmount?: number
  usedAmount?: number
  remainingAmount?: number
  amountAtOnePercent?: number
  amountAtThreePercent?: number
  monthlyExemptAmount?: number
  currentMonthAmount?: number
  currentMonthOverExempt?: boolean
  quotaExceeded?: boolean
  message?: string
  months?: QuotaMonthVO[]
}

export interface SettlementMonthVO {
  month?: string
  invoiceCount?: number
  invoicedAmount?: number
  paidTaxAmount?: number
  iitAmount?: number
}

export interface SettlementVO {
  sellerName?: string
  idCardMasked?: string
  taxYear?: number
  deadline?: string
  daysLeft?: number
  overdue?: boolean
  invoiceCount?: number
  invoicedAmount?: number
  paidTaxAmount?: number
  iitAmount?: number
  months?: SettlementMonthVO[]
  message?: string
}

const withToken = (path: string, token: string) => `${path}?token=${encodeURIComponent(token)}`

export const queryQuota = (token: string) => get<QuotaVO>(withToken('/icbc/public/quota', token))

export const querySettlement = (token: string) =>
  get<SettlementVO>(withToken('/icbc/public/settlement', token))

export const submitContactLead = (data: { token: string; name: string; mobile: string; remark?: string }) =>
  post<boolean>('/icbc/public/contact-lead', data)

export interface OnboardingStatusVO {
  step?: string
  realNameStatusName?: string
  onboardingStateName?: string
  nextStep?: string
  invoiceEligible?: boolean
  message?: string
}

/** 刷新建档状态（后端按当前步骤向工行主动查询一次） */
export const syncOnboarding = (token: string) =>
  post<OnboardingStatusVO>(`/icbc/public/onboarding/sync?token=${encodeURIComponent(token)}`)

/** 当前该做的工行建档页面 URL（后端输出自动提交表单 HTML，前端只负责承载） */
export const onboardingFormUrl = (token: string, trxChannel: string) =>
  `${API_BASE_URL}/icbc/public/onboarding/form?token=${encodeURIComponent(token)}&trxChannel=${trxChannel}`
