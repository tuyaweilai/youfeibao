/**
 * 建档向导（#91）的**本地暂存**。
 *
 * 分步状态不落后端草稿表——中间态（有证件没卡）没有业务价值，落库只会让收方档案带上
 * 「半成品」语义（#81 决策 2）。切走再回来时前面填的还在，靠这里把字段与照片 blob 存本地。
 */
export interface OnboardingWizardDraft {
  step: number
  /** 身份证人像面 / 国徽面照片（dataURL，仅本地暂存与识别用，识别完即弃、不落库） */
  idFrontImage?: string
  idBackImage?: string
  /** 银行卡照片（dataURL，同上） */
  bankImage?: string
  name: string
  idCardNo: string
  mobile: string
  address: string
  idSignDate: string
  idValidityPeriod: string
  bankCardNo: string
  bankName: string
  accountCode: string
  // 识别反馈：质量分告警（提示类不拦）与硬拦原因，随表单一起带过确认页
  idWarnings: string[]
  idBlockReasons: string[]
  bankWarnings: string[]
  bankBlockReasons: string[]
  // 协议要素（税总 5 号公告第十七条），留空由后端给缺省
  productName: string
  quantity: string
  specification: string
  recyclePeriod: string
  settlementMethod: string
  // 落库结果（第 4 步提交后写入）：档案编号与本次的签署方式
  payeeId?: number
  signMethod?: string
}

const DRAFT_KEY = 'field_onboarding_wizard_draft'

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
    accountCode: '1',
    idWarnings: [],
    idBlockReasons: [],
    bankWarnings: [],
    bankBlockReasons: [],
    productName: '',
    quantity: '',
    specification: '',
    recyclePeriod: '',
    settlementMethod: '',
    payeeId: undefined,
    signMethod: ''
  }
}

export function loadWizardDraft(): OnboardingWizardDraft | null {
  const saved = uni.getStorageSync(DRAFT_KEY)
  if (!saved || typeof saved !== 'object') {
    return null
  }
  // 与空草稿合并：旧版本草稿少字段时不至于把页面弄崩
  return { ...emptyWizardDraft(), ...(saved as OnboardingWizardDraft) }
}

/**
 * 存草稿。照片是 base64，可能撑爆本地配额——撑爆时**退回只存字段**并如实告诉收货员，
 * 不静默丢整份草稿（现场最怕的是「填完一半，回来什么都没了」）。
 */
export function saveWizardDraft(draft: OnboardingWizardDraft) {
  try {
    uni.setStorageSync(DRAFT_KEY, draft)
  } catch {
    try {
      uni.setStorageSync(DRAFT_KEY, { ...draft, idFrontImage: '', idBackImage: '', bankImage: '' })
    } catch {
      throw new Error('本地暂存失败：存储空间不足，请先清理已补传的草稿')
    }
  }
}

export function clearWizardDraft() {
  uni.removeStorageSync(DRAFT_KEY)
}
