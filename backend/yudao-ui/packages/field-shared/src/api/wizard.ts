// 请求封装由宿主工程通过 '@' 别名提供（收货员现场端与司机端各有一份，见 README）
//
// 建档向导（#91）：五步壳的**识别**与**落库**两组接口。分步状态只在现场端本地暂存，
// 后端不存草稿；识别是无状态的上传 + 识别，图片识别完即弃、不进文件服务（ADR 0037）。
//
// 请求 / 响应**类型**与自填壳（自然人端）共用一份：见 ./wizardTypes.ts（#94 评审 S-2）。
import { post } from '@/utils/request'
import type {
  BankCardRecognizeReq,
  BankCardRecognizeVO,
  IdCardBackRecognizeReq,
  IdCardBackRecognizeVO,
  IdCardFrontRecognizeReq,
  IdCardFrontRecognizeVO,
  OnboardingWizardSubmitReq,
  OnboardingWizardSubmitVO
} from './wizardTypes'

export type {
  BankCardRecognizeReq,
  BankCardRecognizeVO,
  IdCardBackRecognizeReq,
  IdCardBackRecognizeVO,
  IdCardFrontRecognizeReq,
  IdCardFrontRecognizeVO,
  OnboardingWizardSubmitReq,
  OnboardingWizardSubmitVO
} from './wizardTypes'

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
