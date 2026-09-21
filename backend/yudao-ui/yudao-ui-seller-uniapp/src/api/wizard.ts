// 免注册自填建档（#94）：本人用收货员给的链接打开，走与现场端代录壳**同一套**向导接口。
//
// 请求封装由宿主（`@/utils/request`）提供：这条通道是免登录的公开端点，令牌作为查询参数带上，
// 不带登录态、不带 tenant-id（端点在白名单里，租户由令牌解析）。
//
// 识别与落库的请求 / 响应**类型**与现场端共用 `@youfeibao/field-shared/src/api/wizardTypes`
// 那一份（#94 评审 S-2）：字段名两边逐字一致，不会静默漂移。这里只拼 URL、调宿主请求封装。
import { get, post } from '@/utils/request'
import type {
  BankCardRecognizeReq,
  BankCardRecognizeVO,
  IdCardBackRecognizeReq,
  IdCardBackRecognizeVO,
  IdCardFrontRecognizeReq,
  IdCardFrontRecognizeVO,
  OnboardingWizardSubmitReq,
  OnboardingWizardSubmitVO
} from '@youfeibao/field-shared/src/api/wizardTypes'

export type {
  BankCardRecognizeReq,
  BankCardRecognizeVO,
  IdCardBackRecognizeReq,
  IdCardBackRecognizeVO,
  IdCardFrontRecognizeReq,
  IdCardFrontRecognizeVO,
  OnboardingWizardSubmitReq,
  OnboardingWizardSubmitVO
} from '@youfeibao/field-shared/src/api/wizardTypes'

const withToken = (path: string, token: string) => `${path}?token=${encodeURIComponent(token)}`

/** 打开链接先验一次令牌：有效返回用途与有效期，失效给可读错误 */
export interface WizardContextVO {
  purpose?: string
  purposeName?: string
  expiresTime?: string
}

export const getWizardContext = (token: string) =>
  get<WizardContextVO>(withToken('/icbc/public/onboarding-wizard/context', token))

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
