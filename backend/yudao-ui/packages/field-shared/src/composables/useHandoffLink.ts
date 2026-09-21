/**
 * 「交给出售者本人」的转达入口（#91 复审 ST-B）。
 *
 * 现场端与司机端是同一套：签发一枚一次性令牌（ADR 0023），拼出本人要打开的链接，出示二维码，
 * 并把「有效期到什么时候」如实写出来。以前 `pages/payee/index.vue` 与 `pages/payee/wizard.vue`
 * 各抄了一份，向导那份复制时把模板串的插值丢了（`链接 24 小时内有效` 后面没有时间），收进这里
 * 就不会再有第二份、也不会再漂。
 *
 * **二维码渲染由宿主传入**（`renderQr`）：`qrcode` 是宿主的依赖，这个共享包不引它
 * （`field-shared` 没有 `node_modules`，裸依赖解析会落到宿主工程，见 README 的坑）。
 */
import { reactive, ref } from 'vue'
import { createPublicToken, revokePublicToken } from '../api/publicToken'
import { SELLER_APP_URL } from '@/config/env'

export interface HandoffState {
  token: string
  link: string
  qr: string
  expiresText: string
}

/** 令牌 / 链接 / 二维码 / 有效期是一组，重置与清空都用同一份，不能只清一半。 */
function emptyHandoff(): HandoffState {
  return { token: '', link: '', qr: '', expiresText: '' }
}

function resetHandoffState(handoff: HandoffState) {
  Object.assign(handoff, emptyHandoff())
}

/** 拼本人要打开的链接：自然人端地址 + 一次性令牌 + 用途；地址或令牌为空时返回空串（只出示令牌）。 */
export function buildSellerHandoffLink(sellerAppUrl: string, token: string, purpose: string): string {
  if (!token || !sellerAppUrl) {
    return ''
  }
  return `${sellerAppUrl.replace(/\/$/, '')}/#/?token=${encodeURIComponent(token)}&purpose=${purpose}`
}

/** 令牌有效期的展示文案：24 小时是后端签发口径，至具体到期时间。 */
export function handoffExpiresText(expiresTime?: number): string {
  return expiresTime ? `链接 24 小时内有效，至 ${new Date(expiresTime).toLocaleString()}` : ''
}

/**
 * 转达链接的状态与动作，两端共用。
 *
 * @param payeeId 当前收方档案编号（取值为空时不签发）
 * @param renderQr 把链接渲染成二维码 dataURL；返回空串表示不展示二维码
 * @param tips     失败时的现场提示（两端的 toast 封装不同）
 */
export function useHandoffLink(
  payeeId: () => number | undefined,
  renderQr: (text: string) => Promise<string>,
  tips: (message: string) => void
) {
  const issuing = ref(false)
  const handoff = reactive<HandoffState>(emptyHandoff())

  /** 签发 ONBOARDING 一次性令牌，拼出本人要打开的执行链接（令牌 24 小时内有效）。 */
  async function issueLink() {
    const id = payeeId()
    if (!id) {
      return
    }
    issuing.value = true
    try {
      const resp = await createPublicToken({ purpose: 'ONBOARDING', payeeId: id })
      handoff.token = resp.token || ''
      handoff.link = buildSellerHandoffLink(SELLER_APP_URL, handoff.token, 'ONBOARDING')
      handoff.expiresText = handoffExpiresText(resp.expiresTime)
      handoff.qr = await renderQr(handoff.link)
    } catch (e) {
      tips((e as Error).message || '生成链接失败')
    } finally {
      issuing.value = false
    }
  }

  function copyLink() {
    const text = handoff.link || handoff.token
    if (!text) {
      return
    }
    uni.setClipboardData({ data: text, success: () => tips('已复制，请交给出售者本人打开') })
  }

  /** 换人 / 重新开始时清干净：令牌、链接、二维码、有效期是一组，不能只清一半。 */
  function resetHandoff() {
    resetHandoffState(handoff)
  }

  return { issuing, handoff, issueLink, copyLink, resetHandoff }
}

/**
 * 本人自填建档链接（#94）：收货员把链接交给自然人本人，他在自己手机上走完同一套五步向导。
 *
 * 与 {@link useHandoffLink} 同一套一次性令牌机制，差别只有两点：
 * <ul>
 *   <li>用途是 {@code ONBOARDING_WIZARD}：<b>已建档</b>（传了 `payeeId`）时链接锁到那个人身上，
 *       <b>待建档</b>时才绑链接本身（#94 修票 ST-1 结构根因）；</li>
 *   <li>链接可**作废**：本人中途放弃、或换一枚新的时，收货员点一下就把旧的废掉。</li>
 * </ul>
 * 二维码渲染仍由宿主传入（`field-shared` 不引 `qrcode`，见 README）。
 *
 * @param payeeId 当前收方档案编号；有值 = 已建档（链接锁到人），无值 = 待建档（绑定链接本身）
 */
export function useWizardInviteLink(
  payeeId: () => number | undefined,
  renderQr: (text: string) => Promise<string>,
  tips: (message: string) => void
) {
  const issuing = ref(false)
  const revoking = ref(false)
  const handoff = reactive<HandoffState>(emptyHandoff())

  async function issueLink() {
    issuing.value = true
    try {
      const id = payeeId()
      const resp = await createPublicToken({ purpose: 'ONBOARDING_WIZARD', payeeId: id || undefined })
      handoff.token = resp.token || ''
      handoff.link = buildSellerHandoffLink(SELLER_APP_URL, handoff.token, 'ONBOARDING_WIZARD')
      handoff.expiresText = handoffExpiresText(resp.expiresTime)
      handoff.qr = await renderQr(handoff.link)
    } catch (e) {
      tips((e as Error).message || '生成链接失败')
    } finally {
      issuing.value = false
    }
  }

  /** 作废当前链接：链接立刻失效，收货员可重新生成一枚。 */
  async function revokeLink() {
    if (!handoff.token) {
      return
    }
    revoking.value = true
    try {
      await revokePublicToken(handoff.token)
      resetHandoffState(handoff)
      tips('链接已作废，可重新生成')
    } catch (e) {
      tips((e as Error).message || '作废链接失败')
    } finally {
      revoking.value = false
    }
  }

  function copyLink() {
    const text = handoff.link || handoff.token
    if (!text) {
      return
    }
    uni.setClipboardData({ data: text, success: () => tips('已复制，请交给本人打开') })
  }

  function resetHandoff() {
    resetHandoffState(handoff)
  }

  return { issuing, revoking, handoff, issueLink, revokeLink, copyLink, resetHandoff }
}
