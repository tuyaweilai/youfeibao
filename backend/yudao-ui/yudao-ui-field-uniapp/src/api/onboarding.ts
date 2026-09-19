import { get, post } from '@/utils/request'

export interface SellerStepVO {
  payeeId?: number
  /** 工行页面自动提交表单 HTML，交给 openIcbcFormHtml() 新窗口打开 */
  formHtml?: string
  step?: string
}

export interface FrameworkAgreementVO {
  id?: number
  payeeId?: number
  agreementNo?: string
  productName?: string
  quantity?: string
  specification?: string
  recyclePeriod?: string
  settlementMethod?: string
  signMethod?: string
  signedAt?: number
  fileUrl?: string
  status?: number
  remark?: string
}

export interface SellerAuthorizationVO {
  id?: number
  payeeId?: number
  reverseInvoiceAuthorized?: boolean
  taxAgencyAuthorized?: boolean
  authorizedAt?: number
  channel?: string
  operator?: string
  evidenceUrl?: string
  remark?: string
}

export interface SellerOnboardingVO {
  payeeId?: number
  name?: string
  idCardNo?: string
  mobile?: string
  realNameStatus?: number
  realNameStatusName?: string
  realNameMsg?: string
  onboardingState?: string
  onboardingStateName?: string
  nextStep?: string
  auditResult?: string
  rejectReason?: string
  status?: number
  icbcOpenacctStatus?: string
  icbcReceiverStatus?: string
  icbcMediumId?: string
  frameworkAgreement?: FrameworkAgreementVO
  authorization?: SellerAuthorizationVO
  invoiceEligible?: boolean
  invoiceBlockReason?: string
}

export interface SellerOnboardingSubmitReq {
  payeeId: number
  idSignDate?: string
  idValidityPeriod?: string
  trxChannel?: string
  bankName?: string
  bankBranch?: string
}

/** 建档总览 */
export const getOnboarding = (payeeId: number) =>
  get<SellerOnboardingVO>('/icbc/seller-onboarding/get', { payeeId })

/** 发起实人认证（返回工行表单） */
export const startRealName = (payeeId: number) =>
  post<SellerStepVO>('/icbc/seller-onboarding/real-name/start', { payeeId })

/** 查询实人认证结果并返回最新建档总览 */
export const syncRealName = (payeeId: number) =>
  post<SellerOnboardingVO>(`/icbc/seller-onboarding/real-name/sync?payeeId=${payeeId}`)

/** 发起收方入驻（返回工行表单） */
export const submitOnboarding = (data: SellerOnboardingSubmitReq) =>
  post<SellerStepVO>('/icbc/seller-onboarding/onboarding/submit', data)

/** 查询收方入驻结果并返回最新建档总览 */
export const syncOnboarding = (payeeId: number) =>
  post<SellerOnboardingVO>(`/icbc/seller-onboarding/onboarding/sync?payeeId=${payeeId}`)

/** 入驻失败时留联系方式等待联系 */
export const leaveContactFallback = (data: { payeeId: number; mobile: string; remark?: string }) =>
  post<boolean>('/icbc/seller-onboarding/contact-fallback', data)

/** 签署 / 更新框架收购协议 */
export const saveAgreement = (data: FrameworkAgreementVO) =>
  post<number>('/icbc/seller-onboarding/agreement/create', data)

/** 记录首次反向开票与代办税费授权 */
export const authorizeSeller = (data: SellerAuthorizationVO) =>
  post<number>('/icbc/seller-onboarding/authorization/create', data)
