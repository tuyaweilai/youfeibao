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
  /** 实名状态值（0 未认证 / 1 认证中 / 2 认证通过 / 3 认证未通过）；3 时落点页给重试入口 */
  realNameStatus?: number
  /** 实名未通过的原因（未通过时非空） */
  realNameMsg?: string
  onboardingStateName?: string
  /** 收款账户变更状态名（换卡在途时非空，例如「银行审核中」，#37） */
  bankCardChangeStatusName?: string
  nextStep?: string
  invoiceEligible?: boolean
  message?: string
}

export interface PublicStationVO {
  stationCode?: string
  stationId?: number
  tenantId?: number
  enterpriseName?: string
  stationName?: string
  address?: string
  open?: boolean
  openStatusName?: string
  contactMobile?: string
  guide?: string[]
}

/** 触达通知条目（#36，ADR 0023）：打开链接就能看到的事，不需要先注册 */
export interface NoticeItemVO {
  type?: string
  typeName?: string
  title?: string
  statusName?: string
  settlementId?: number
  partnerOrderId?: string
  amount?: number
  acquisitionCount?: number
  invoiceNo?: string
  deadlineTime?: string
  nextStep?: string
}

export interface PublicNoticeVO {
  purpose?: string
  tenantId?: number
  payeeId?: number
  sellerName?: string
  message?: string
  items?: NoticeItemVO[]
  scopeNote?: string
}

/** 用令牌看触达通知（待确认结算 / 付款异常），打开即可看 */
export const queryNotice = (token: string) =>
  get<PublicNoticeVO>('/icbc/public/notice', { token })

/** 解析场站二维码：只编码场站码，返回公开信息（不含个人数据） */
export const resolveStation = (code: string) =>
  get<PublicStationVO>('/icbc/public/station', { code })

/** 刷新建档状态（后端按当前步骤向工行主动查询一次） */
export const syncOnboarding = (token: string) =>
  post<OnboardingStatusVO>(`/icbc/public/onboarding/sync?token=${encodeURIComponent(token)}`)

/** 当前该做的工行建档页面 URL（后端输出自动提交表单 HTML，前端只负责承载） */
export const onboardingFormUrl = (token: string) =>
  `${API_BASE_URL}/icbc/public/onboarding/form?token=${encodeURIComponent(token)}`
