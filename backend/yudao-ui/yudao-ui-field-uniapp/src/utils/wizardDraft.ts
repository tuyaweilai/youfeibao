/**
 * 建档向导（#91）的**本地暂存**。
 *
 * 分步状态不落后端草稿表——中间态（有证件没卡）没有业务价值，落库只会让收方档案带上
 * 「半成品」语义（#81 决策 2）。切走再回来时前面填的还在，靠这里把字段与照片 blob 存本地。
 *
 * 存储 key 与**本次建档的人**绑定（`…:<身份证号>`，尚未拍到证件时用 `pending`）：
 * 单一 key 的草稿会在中途放弃后留给下一位出售者，让人看到上一位的照片与字段（#91 评审 SP-5）。
 * 同一时刻只保留一份草稿，`loadWizardDraft` 取到的「唯一那份」就是本次未完成的建档。
 */
export interface OnboardingWizardDraft {
  step: number
  /**
   * 身份证人像面 / 国徽面照片（dataURL）。后端识别完即弃、不落库（ADR 0037）；
   * 这里存的是**本机草稿**，只为「切走再回来还在」，建档完成 / 换人时随草稿一起清掉。
   */
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
  /** 是否我行卡的人工确认值：'' = 本人还没确认（识别结果见 accountCodeRecognized） */
  accountCode: string
  /** 是否我行卡的识别结果：只在本人未确认时，作为提交时的第二顺位（#91 评审 SP-2） */
  accountCodeRecognized: string
  // 识别反馈：质量分告警（提示类不拦）与硬拦原因，随表单一起带过确认页。
  // 正反面各留一份：同一对字段会让后拍的那张覆盖前一张，硬拦就能靠「再拍一张干净的」绕过（#91 评审 SP-3）
  idFrontWarnings: string[]
  idFrontBlockReasons: string[]
  idBackWarnings: string[]
  idBackBlockReasons: string[]
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

const DRAFT_KEY_PREFIX = 'field_onboarding_wizard_draft'
/** 尚未认出身份的草稿：key 里还没有身份证号可用 */
const PENDING_SCOPE = 'pending'

/** 草稿 key 带上「是谁」：下一位出售者不会捡到上一位的草稿 */
function draftKey(idCardNo?: string) {
  const scope = (idCardNo || '').trim() || PENDING_SCOPE
  return `${DRAFT_KEY_PREFIX}:${scope}`
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
    signMethod: ''
  }
}

/**
 * 读草稿：优先取已认出身份的那一份，再退回尚未拍到证件的 `pending` 草稿。
 *
 * 同一时刻只留一份，所以这不构成「按人切换草稿」——要换人请先清干净（`clearWizardDraft`）。
 */
export function loadWizardDraft(): OnboardingWizardDraft | null {
  const keys = existingDraftKeys()
  const key =
    keys.find((item) => !item.endsWith(`:${PENDING_SCOPE}`)) ||
    keys.find((item) => item.endsWith(`:${PENDING_SCOPE}`))
  if (!key) {
    return null
  }
  const saved = uni.getStorageSync(key)
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
  const key = draftKey(draft.idCardNo)
  const write = (value: OnboardingWizardDraft) => {
    // 同一时刻只留一份：换了人（身份证号变了）就把上一位的草稿清掉
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
      throw new Error('本地暂存失败：存储空间不足，请先清理已补传的草稿')
    }
  }
}

/** 清干净：建档结束、换一位出售者、或「重新开始」时都要调它 */
export function clearWizardDraft() {
  existingDraftKeys().forEach((key) => uni.removeStorageSync(key))
}
