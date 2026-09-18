import request from '@/config/axios'

// 出售者建档相关 VO
export interface SellerStepVO {
  payeeId?: number
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
  createTime?: number
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
  agreementHistory?: FrameworkAgreementVO[]
}

// 出售者建档 API
export const OnboardingApi = {
  // 建档总览
  getOnboarding: async (payeeId: number) => {
    return await request.get({ url: `/icbc/seller-onboarding/get?payeeId=` + payeeId })
  },

  // 回头客带档
  findReturningCustomer: async (params: { idCardNo?: string; mobile?: string }) => {
    return await request.get({ url: `/icbc/seller-onboarding/returning-customer`, params })
  },

  // 发起实人认证
  startRealName: async (payeeId: number) => {
    return await request.post({ url: `/icbc/seller-onboarding/real-name/start`, data: { payeeId } })
  },

  // 查询实人认证结果
  syncRealName: async (payeeId: number) => {
    return await request.post({ url: `/icbc/seller-onboarding/real-name/sync?payeeId=` + payeeId })
  },

  // 发起收方入驻
  submitOnboarding: async (data: any) => {
    return await request.post({ url: `/icbc/seller-onboarding/onboarding/submit`, data })
  },

  // 查询收方入驻结果
  syncOnboarding: async (payeeId: number) => {
    return await request.post({ url: `/icbc/seller-onboarding/onboarding/sync?payeeId=` + payeeId })
  },

  // 留联系方式
  leaveContactFallback: async (data: { payeeId: number; mobile: string; remark?: string }) => {
    return await request.post({ url: `/icbc/seller-onboarding/contact-fallback`, data })
  },

  // 签署 / 更新框架收购协议
  saveAgreement: async (data: FrameworkAgreementVO) => {
    return await request.post({ url: `/icbc/seller-onboarding/agreement/create`, data })
  },

  // 生效中的框架收购协议
  getAgreement: async (payeeId: number) => {
    return await request.get({ url: `/icbc/seller-onboarding/agreement/get?payeeId=` + payeeId })
  },

  // 记录首次授权
  authorize: async (data: SellerAuthorizationVO) => {
    return await request.post({ url: `/icbc/seller-onboarding/authorization/create`, data })
  }
}
