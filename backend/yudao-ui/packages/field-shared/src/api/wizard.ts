// 请求封装由宿主工程通过 '@' 别名提供（收货员现场端与司机端各有一份，见 README）
//
// 建档向导（#91）：五步壳的**识别**与**落库**两组接口。分步状态只在现场端本地暂存，
// 后端不存草稿；识别是无状态的上传 + 识别，图片识别完即弃、不进文件服务（ADR 0037）。
import { post } from '@/utils/request'

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
  /** 提示类告警（可读），不拦继续 */
  warnings?: string[]
  /** 硬拦原因（可读），非空即不可继续 */
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
}

/** 识别身份证人像面（无状态，图片不留存） */
export const recognizeIdCardFront = (data: IdCardFrontRecognizeReq) =>
  post<IdCardFrontRecognizeVO>('/icbc/onboarding-wizard/id-card/front', data)

/** 识别身份证国徽面（无状态，图片不留存） */
export const recognizeIdCardBack = (data: IdCardBackRecognizeReq) =>
  post<IdCardBackRecognizeVO>('/icbc/onboarding-wizard/id-card/back', data)

/** 识别银行卡（无状态，图片不留存） */
export const recognizeBankCard = (data: BankCardRecognizeReq) =>
  post<BankCardRecognizeVO>('/icbc/onboarding-wizard/bank-card', data)

/** 提交建档：一次性落库并落框架收购协议 */
export const submitOnboardingWizard = (data: OnboardingWizardSubmitReq) =>
  post<OnboardingWizardSubmitVO>('/icbc/onboarding-wizard/submit', data)
