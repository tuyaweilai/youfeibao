/**
 * 自填建档（#94）的**本地暂存**。
 *
 * 分步状态不落后端草稿表——中间态（有证件没卡）没有业务价值，落库只会让收方档案带上
 * 「半成品」语义（#81 决策 2）。切走再回来时前面填的还在，靠这里把字段与照片 blob 存本地。
 *
 * 存储 key 与**这枚链接**绑定（`…:<token>`）：同一台手机上换一枚链接打开时不会捡到上一枚的
 * 字段与照片（现场端草稿按身份证号绑定是同一个道理，见 `field-uniapp/utils/wizardDraft.ts`）。
 * 同一链接的草稿只留一份，`loadWizardDraft` 取到的就是本次未完成的建档。
 */
export interface OnboardingWizardDraft {
  step: number
  /** 证件 / 银行卡照片（dataURL）：识别完即弃、不落库，这里只为「切走再回来还在」 */
  idFrontImage?: string
  idBackImage?: string
  bankImage?: string
  name: string
  idCardNo: string
  mobile: string
  address: string
  idSignDate: string
  idValidityPeriod: string
  bankCardNo: string
  bankName: string
  /** 是否我行卡的人工确认值：'' = 本人还没确认（识别结果见 accountCodeRecognized） */
  accountCode: string
  /** 是否我行卡的识别结果：只在本人未确认时，作为提交时的第二顺位 */
  accountCodeRecognized: string
  idFrontWarnings: string[]
  idFrontBlockReasons: string[]
  idBackWarnings: string[]
  idBackBlockReasons: string[]
  bankWarnings: string[]
  bankBlockReasons: string[]
  productName: string
  quantity: string
  specification: string
  recyclePeriod: string
  settlementMethod: string
  payeeId?: number
  signMethod?: string
  /** 建档完成后换发的实名令牌（ONBOARDING） */
  onboardingToken?: string
  /** 实名令牌有效期至（页面要把它显示给本人，别让它成为没人用的字段） */
  onboardingExpiresTime?: string
}

const DRAFT_KEY_PREFIX = 'seller_onboarding_wizard_draft'

/** 草稿 key 带上链接：换一枚链接不会捡到上一枚的半成品 */
function draftKey(token: string) {
  return `${DRAFT_KEY_PREFIX}:${(token || '').slice(0, 64)}`
}

function existingDraftKeys(): string[] {
  try {
    const info = uni.getStorageInfoSync()
    return (info?.keys || []).filter((key: string) => key.startsWith(`${DRAFT_KEY_PREFIX}:`))
  } catch {
    return []
  }
}

export function emptyWizardDraft(): OnboardingWizardDraft {
  return {
    step: 1,
    idFrontImage: '',
    idBackImage: '',
    bankImage: '',
    name: '',
    idCardNo: '',
    mobile: '',
    address: '',
    idSignDate: '',
    idValidityPeriod: '',
    bankCardNo: '',
    bankName: '',
    accountCode: '',
    accountCodeRecognized: '',
    idFrontWarnings: [],
    idFrontBlockReasons: [],
    idBackWarnings: [],
    idBackBlockReasons: [],
    bankWarnings: [],
    bankBlockReasons: [],
    productName: '',
    quantity: '',
    specification: '',
    recyclePeriod: '',
    settlementMethod: '',
    payeeId: undefined,
    signMethod: '',
    onboardingToken: '',
    onboardingExpiresTime: ''
  }
}

export function loadWizardDraft(token: string): OnboardingWizardDraft | null {
  const saved = uni.getStorageSync(draftKey(token))
  if (!saved || typeof saved !== 'object') {
    return null
  }
  // 与空草稿合并：旧版本草稿少字段时不至于把页面弄崩
  return { ...emptyWizardDraft(), ...(saved as OnboardingWizardDraft) }
}

/**
 * 存草稿。照片是 base64，可能撑爆本地配额——撑爆时**退回只存字段**并如实告诉本人，
 * 不静默丢整份草稿（最怕的是「填完一半，回来什么都没了」）。
 */
export function saveWizardDraft(token: string, draft: OnboardingWizardDraft) {
  const key = draftKey(token)
  const write = (value: OnboardingWizardDraft) => {
    // 同一时刻只留当前链接这一份，其它链接的草稿清掉
    existingDraftKeys()
      .filter((item) => item !== key)
      .forEach((item) => uni.removeStorageSync(item))
    uni.setStorageSync(key, value)
  }
  try {
    write(draft)
  } catch {
    try {
      write({ ...draft, idFrontImage: '', idBackImage: '', bankImage: '' })
    } catch {
      throw new Error('本地暂存失败：存储空间不足，请先清理后再继续')
    }
  }
}

/** 清干净：建档结束、「重新开始」或链接失效时都要调它 */
export function clearWizardDraft(token: string) {
  if (token) {
    uni.removeStorageSync(draftKey(token))
    return
  }
  existingDraftKeys().forEach((key) => uni.removeStorageSync(key))
}
