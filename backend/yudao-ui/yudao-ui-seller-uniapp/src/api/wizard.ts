// 免注册自填建档（#94）：本人用收货员给的链接打开，走与现场端代录壳**同一套**向导接口。
//
// 请求封装由宿主（`@/utils/request`）提供：这条通道是免登录的公开端点，令牌作为查询参数带上，
// 不带登录态、不带 tenant-id（端点在白名单里，租户由令牌解析）。
import { get, post } from '@/utils/request'

const withToken = (path: string, token: string) => `${path}?token=${encodeURIComponent(token)}`

/** 打开链接先验一次令牌：有效返回用途与有效期，失效给可读错误 */
export interface WizardContextVO {
  purpose?: string
  purposeName?: string
  expiresTime?: string
}

export const getWizardContext = (token: string) =>
  get<WizardContextVO>(withToken('/icbc/public/onboarding-wizard/context', token))

export interface IdCardFrontRecognizeReq {
  /** 压缩后的身份证人像面照片（base64，不含 dataURL 前缀） */
  imageBase64: string
  /** 当前表单里的值：只在空缺处回填识别结果，人工输入优先 */
  name?: string
  idCardNo?: string
  address?: string
}

export interface IdCardFrontRecognizeVO {
  name?: string
  idCardNo?: string
  address?: string
  qualityScore?: number
  warnings?: string[]
  blockReasons?: string[]
}

export interface IdCardBackRecognizeReq {
  imageBase64: string
  idSignDate?: string
  idValidityPeriod?: string
}

export interface IdCardBackRecognizeVO {
  idSignDate?: string
  idValidityPeriod?: string
  qualityScore?: number
  warnings?: string[]
  blockReasons?: string[]
}

export interface BankCardRecognizeReq {
  imageBase64: string
  bankCardNo?: string
  bankName?: string
  /** 0-非我行用户，1-我行用户 */
  accountCode?: string
}

export interface BankCardRecognizeVO {
  bankCardNo?: string
  bankName?: string
  accountCode?: string
  qualityScore?: number
  warnings?: string[]
  blockReasons?: string[]
}

export interface OnboardingWizardSubmitReq {
  name: string
  idCardNo: string
  mobile: string
  address?: string
  idSignDate?: string
  idValidityPeriod?: string
  bankCardNo?: string
  bankName?: string
  bankBranch?: string
  accountCode?: string
  // 协议要素（税总 5 号公告第十七条），留空由后端给缺省
  productName?: string
  quantity?: string
  specification?: string
  recyclePeriod?: string
  settlementMethod?: string
}

export interface OnboardingWizardSubmitVO {
  payeeId?: number
  naturalPersonId?: number
  agreementId?: number
  /** ELECTRONIC-电子签章，PAPER-纸质签署（本票未开通电子签章时恒为 PAPER） */
  signMethod?: string
  /** 建档完成后换发的实名令牌（ONBOARDING），本人接着做实名用 */
  onboardingToken?: string
  onboardingExpiresTime?: string
}

/** 识别身份证人像面（无状态，图片不留存） */
export const recognizeIdCardFront = (token: string, data: IdCardFrontRecognizeReq) =>
  post<IdCardFrontRecognizeVO>(withToken('/icbc/public/onboarding-wizard/id-card/front', token), data)

/** 识别身份证国徽面（无状态，图片不留存） */
export const recognizeIdCardBack = (token: string, data: IdCardBackRecognizeReq) =>
  post<IdCardBackRecognizeVO>(withToken('/icbc/public/onboarding-wizard/id-card/back', token), data)

/** 识别银行卡（无状态，图片不留存） */
export const recognizeBankCard = (token: string, data: BankCardRecognizeReq) =>
  post<BankCardRecognizeVO>(withToken('/icbc/public/onboarding-wizard/bank-card', token), data)

/** 提交建档：一次性落库（与代录壳同一个向导 Service） */
export const submitOnboardingWizard = (token: string, data: OnboardingWizardSubmitReq) =>
  post<OnboardingWizardSubmitVO>(withToken('/icbc/public/onboarding-wizard/submit', token), data)
