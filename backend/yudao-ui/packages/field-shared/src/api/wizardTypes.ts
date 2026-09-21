// 建档向导（#91）的请求 / 响应**类型**——两个壳共用一份。
//
// 为什么单独一个只有类型、没有任何 import 的文件：现场端壳走 admin 通道（`@/utils/request` + 登录态），
// 自填壳走免登录公开端点（令牌作为查询参数），**请求层不共享**；但两边调的是同一个后端
// `OnboardingWizardService`，字段名必须逐字一致。手抄两遍会静默漂移（#94 评审 S-2），所以类型只留一份，
// 各自的 `api/wizard.ts` 只负责拼 URL 与调宿主请求封装。
//
// 这里不放任何运行时代码，也不 import 宿主的 `@/...`，这样自然人端可以只 `import type` 引它
// （`import type` 会被完全擦除，不把现场端的请求层带过去）。

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
  /** ELECTRONIC-电子签章，PAPER-纸质签署（未开通电子签章时恒为 PAPER） */
  signMethod?: string
  /** 建档完成后换发的实名令牌（ONBOARDING），只有本人自填壳有；代录壳为空 */
  onboardingToken?: string
  onboardingExpiresTime?: string
}
