<template>
  <view class="page">
    <view class="wizard-heading">
      <view><view class="wizard-heading__eyebrow">自然人建档</view><view class="wizard-heading__title">{{ STEP_NAMES[draft.step - 1] }}</view></view>
      <view class="wizard-heading__count"><text>{{ draft.step }}</text> / 5</view>
    </view>
    <!-- 五步的进度条自己画：分步状态只在本页 / 本地草稿里，不落后端 -->
    <view class="steps">
      <view
        v-for="(name, index) in STEP_NAMES"
        :key="name"
        class="steps__item"
        :class="{ 'steps__item--active': draft.step === index + 1, 'steps__item--done': draft.step > index + 1 }"
      >
        <text class="steps__no">{{ draft.step > index + 1 ? '✓' : index + 1 }}</text>
        <text class="steps__name">{{ name }}</text>
      </view>
    </view>

    <view v-if="resumed" class="resume">
      <view class="resume__title">已带出上次未完成的建档{{ resumeLabel ? `：${resumeLabel}` : '' }}</view>
      <view class="resume__desc">不是同一位出售者？请先清掉再开始。</view>
      <button class="link" @click="restart">重新开始（清空草稿）</button>
    </view>

    <!-- 第 1 步：拍身份证（正反面） -->
    <view v-if="draft.step === 1" class="card">
      <view class="card__title">上传身份证照片</view>
      <view class="tip">请拍摄清晰、完整的证件。识别失败时，可在下一步手动填写。照片只用于识别，平台不留存。</view>

      <button class="shot" :disabled="recognizing" aria-label="拍摄人像面" @click="shootIdFront">
        <image v-if="draft.idFrontImage" class="shot__img" :src="draft.idFrontImage" mode="aspectFit" />
        <view v-else class="shot__empty">
          <image class="shot__illustration" :src="documentArt.front" mode="aspectFit" aria-hidden="true" />
          <view class="shot__title">拍摄人像面</view>
          <view class="shot__subtitle">姓名、身份证号码所在面</view>
          <view class="shot__capture">{{ recognizing ? '正在识别…' : '拍照 / 从相册选择' }}</view>
        </view>
      </button>
      <view class="shot__hint">{{ draft.idFrontImage ? '点击重拍人像面' : '未拍摄人像面' }}</view>
      <view v-if="draft.idFrontWarnings.length" class="alerts alerts--warn">
        <view v-for="(word, i) in draft.idFrontWarnings" :key="i">· {{ word }}</view>
      </view>
      <view v-if="draft.idFrontBlockReasons.length" class="alerts alerts--block">
        <view v-for="(word, i) in draft.idFrontBlockReasons" :key="i">· {{ word }}</view>
        <view>人像面这张不能用，请重拍或换一张。</view>
      </view>
      <view
        v-if="idFrontQuality"
        class="alerts"
        :class="idFrontQuality.low ? 'alerts--warn' : 'alerts--info'"
      >
        {{ idFrontQuality.text }}
      </view>

      <button class="shot" :disabled="recognizing" aria-label="拍摄国徽面" @click="shootIdBack">
        <image v-if="draft.idBackImage" class="shot__img" :src="draft.idBackImage" mode="aspectFit" />
        <view v-else class="shot__empty">
          <image class="shot__illustration" :src="documentArt.back" mode="aspectFit" aria-hidden="true" />
          <view class="shot__title">拍摄国徽面</view>
          <view class="shot__subtitle">签发机关、有效期限所在面</view>
          <view class="shot__capture">{{ recognizing ? '正在识别…' : '拍照 / 从相册选择' }}</view>
        </view>
      </button>
      <view class="shot__hint">{{ draft.idBackImage ? '点击重拍国徽面' : '未拍摄国徽面' }}</view>
      <view v-if="draft.idBackWarnings.length" class="alerts alerts--warn">
        <view v-for="(word, i) in draft.idBackWarnings" :key="i">· {{ word }}</view>
      </view>
      <view v-if="draft.idBackBlockReasons.length" class="alerts alerts--block">
        <view v-for="(word, i) in draft.idBackBlockReasons" :key="i">· {{ word }}</view>
        <view>国徽面这张不能用，请重拍或换一张。</view>
      </view>
      <view
        v-if="idBackQuality"
        class="alerts"
        :class="idBackQuality.low ? 'alerts--warn' : 'alerts--info'"
      >
        {{ idBackQuality.text }}
      </view>

      <view class="action-note">请先完成人像面和国徽面拍摄，再确认信息</view>
      <view class="actions">
        <button class="btn btn--primary" :disabled="!canStep1Next || recognizing" @click="goStep(2)">下一步：确认信息</button>
      </view>
    </view>

    <!-- 第 2 步：确认身份信息（可改） -->
    <view v-if="draft.step === 2" class="card">
      <view class="card__title">核对身份信息</view>
      <view class="tip">以你改过的为准。读不出来的项请手工补上。</view>

      <view class="field">
        <text class="field__label">姓名</text>
        <input v-model="draft.name" class="input" placeholder="与身份证一致" />
      </view>
      <view class="field">
        <text class="field__label">身份证号</text>
        <input v-model="draft.idCardNo" class="input" placeholder="身份证号" />
      </view>
      <view class="field">
        <text class="field__label">住址（工行必输）</text>
        <input v-model="draft.address" class="input" placeholder="常住地址" />
      </view>
      <view class="field">
        <text class="field__label">证件签发日期（yyyy-MM-dd）</text>
        <input v-model="draft.idSignDate" class="input" placeholder="如 2020-01-01" />
      </view>
      <view class="field">
        <text class="field__label">证件有效期至（长期填 9999-12-30）</text>
        <input v-model="draft.idValidityPeriod" class="input" placeholder="如 2030-01-01" />
      </view>
      <view class="field">
        <text class="field__label">手机号（本人实名要用）</text>
        <input v-model="draft.mobile" class="input" type="number" placeholder="手机号" />
      </view>

      <view v-if="draft.idFrontWarnings.length" class="alerts alerts--warn">
        <view v-for="(word, i) in draft.idFrontWarnings" :key="`f${i}`">· {{ word }}</view>
      </view>
      <view v-if="draft.idBackWarnings.length" class="alerts alerts--warn">
        <view v-for="(word, i) in draft.idBackWarnings" :key="`b${i}`">· {{ word }}</view>
      </view>
      <view v-if="idBlocked" class="alerts alerts--block">
        <view v-for="(word, i) in idBlockReasons" :key="i">· {{ word }}</view>
        <view>证件这张不能用，请回上一步重拍或换一张。</view>
      </view>
      <view
        v-if="idFrontQuality"
        class="alerts"
        :class="idFrontQuality.low ? 'alerts--warn' : 'alerts--info'"
      >
        人像面：{{ idFrontQuality.text }}
      </view>
      <view
        v-if="idBackQuality"
        class="alerts"
        :class="idBackQuality.low ? 'alerts--warn' : 'alerts--info'"
      >
        国徽面：{{ idBackQuality.text }}
      </view>

      <view class="actions">
        <button class="btn btn--ghost" @click="goStep(1)">上一步</button>
        <button class="btn btn--primary" @click="toStep3">下一步：拍银行卡</button>
      </view>
    </view>

    <!-- 第 3 步：拍银行卡 -->
    <view v-if="draft.step === 3" class="card">
      <view class="card__title">上传银行卡照片</view>
      <view class="tip">拍本人银行卡正面，识别不出就下一步手输。</view>

      <button class="shot" :disabled="recognizing" aria-label="拍摄银行卡正面" @click="shootBankCard">
        <image v-if="draft.bankImage" class="shot__img" :src="draft.bankImage" mode="aspectFit" />
        <view v-else class="shot__empty">
          <image class="shot__illustration" :src="documentArt.bank" mode="aspectFit" aria-hidden="true" />
          <view class="shot__title">拍摄银行卡正面</view>
          <view class="shot__subtitle">请使用本人的收款银行卡</view>
          <view class="shot__capture">{{ recognizing ? '正在识别…' : '拍照 / 从相册选择' }}</view>
        </view>
      </button>
      <view class="shot__hint">{{ draft.bankImage ? '点击重拍银行卡' : '未拍摄银行卡' }}</view>

      <view v-if="draft.bankWarnings.length" class="alerts alerts--warn">
        <view v-for="(word, i) in draft.bankWarnings" :key="i">· {{ word }}</view>
      </view>
      <view v-if="draft.bankBlockReasons.length" class="alerts alerts--block">
        <view v-for="(word, i) in draft.bankBlockReasons" :key="i">· {{ word }}</view>
        <view>这张不能用，请重拍或换一张。</view>
      </view>

      <view class="actions">
        <button class="btn btn--ghost" @click="goStep(2)">上一步</button>
        <button class="btn btn--primary" :disabled="!canStep3Next || recognizing" @click="goStep(4)">下一步：确认卡信息</button>
      </view>
    </view>

    <!-- 第 4 步：确认卡信息（可改）+ 协议要素；确认后一次性落库 -->
    <view v-if="draft.step === 4" class="card">
      <view class="card__title">核对收款账户</view>

      <view class="field">
        <text class="field__label">银行卡号</text>
        <input v-model="draft.bankCardNo" class="input" type="number" placeholder="本人银行卡号" />
      </view>
      <view class="field">
        <text class="field__label">开户银行</text>
        <input v-model="draft.bankName" class="input" placeholder="如：中国工商银行" />
      </view>
      <view class="field">
        <text class="field__label">是否本人工行卡</text>
        <view class="toggle">
          <view
            class="toggle__item"
            :class="{ 'toggle__item--active': accountCodeShown === '1' }"
            @click="draft.accountCode = '1'"
          >
            是工行卡
          </view>
          <view
            class="toggle__item"
            :class="{ 'toggle__item--active': accountCodeShown === '0' }"
            @click="draft.accountCode = '0'"
          >
            非工行卡
          </view>
        </view>
        <view v-if="!draft.accountCode && draft.accountCodeRecognized" class="tip">
          识别为「{{ draft.accountCodeRecognized === '1' ? '是工行卡' : '非工行卡' }}」，请点一下确认。
        </view>
        <view v-else-if="!draft.accountCode" class="tip">
          识别不到、本人也没选时，将按缺省「是工行卡」建档。
        </view>
        <view
          v-if="bankQuality"
          class="alerts"
          :class="bankQuality.low ? 'alerts--warn' : 'alerts--info'"
        >
          {{ bankQuality.text }}
        </view>
      </view>

      <view class="divider" />
      <view class="card__title">框架收购协议要素</view>
      <view class="tip">税总要求的合同流证据，留空按缺省值落库（电子签 / 纸质签由企业是否开通电子签章决定）。</view>
      <view class="field">
        <text class="field__label">货物名称</text>
        <input v-model="draft.productName" class="input" placeholder="留空按「报废产品」" />
      </view>
      <view class="field">
        <text class="field__label">数量</text>
        <input v-model="draft.quantity" class="input" placeholder="留空按「以实际交货为准」" />
      </view>
      <view class="field">
        <text class="field__label">规格</text>
        <input v-model="draft.specification" class="input" placeholder="留空按「以实际交货为准」" />
      </view>
      <view class="field">
        <text class="field__label">回收期次</text>
        <input v-model="draft.recyclePeriod" class="input" placeholder="留空按「长期」" />
      </view>
      <view class="field">
        <text class="field__label">结算方式</text>
        <input v-model="draft.settlementMethod" class="input" placeholder="留空按「银行转账」" />
      </view>

      <view class="actions">
        <button class="btn btn--ghost" @click="goStep(3)">上一步</button>
        <button class="btn btn--primary" :loading="submitting" :disabled="submitting" @click="onSubmit">确认并发起签署</button>
      </view>
    </view>

    <!-- 第 5 步：签署（电子签 = 待签署，纸质 = 当场生效）+ 二维码 / 可复制链接 -->
    <view v-if="draft.step === 5" class="card">
      <view class="card__title">签署框架收购协议</view>

      <view v-if="draft.signMethod === 'PAPER'" class="notice">
        <view class="notice__title">本企业未开通电子签章：协议走纸质签署</view>
        <view class="notice__desc">
          {{ draft.signMessage
            || '请打印框架收购协议与反向发票合规告知函，请本人当场签字；系统已按纸质签法建档。' }}
        </view>
      </view>
      <view v-else class="notice">
        <view class="notice__title">协议待签署（电子签章）</view>
        <view class="notice__desc">
          {{ draft.signMessage
            || '签署已发起，协议还没生效。请把下面的二维码或链接交给本人。' }}
        </view>
      </view>
      <view class="hint">协议状态：{{ agreementStatusName }}</view>

      <view class="handoff__title">
        {{ draft.signMethod === 'PAPER' ? '交给本人用微信办理实名' : '交给本人用微信打开去签署' }}
      </view>
      <view class="tip">
        {{ draft.signMethod === 'PAPER'
          ? '建档已完成。实名只能本人做：让他用微信扫下面的码，或把链接发到他微信里打开。'
          : '签署只能本人做：让他用微信扫下面的码或打开链接，在打开的页面里点「去签署」；点一下才会现取签署链接，现生成现用。' }}
      </view>
      <view v-if="handoff.qr" class="qr">
        <image class="qr__img" :src="handoff.qr" mode="aspectFit" />
      </view>
      <view class="link-box">
        <view class="link-box__url">{{ handoff.link || handoff.token || '（尚未生成）' }}</view>
        <button v-if="handoff.link || handoff.token" class="link" @click="copyLink">复制链接</button>
      </view>
      <view v-if="!handoff.link && handoff.token" class="hint hint--warn">
        未配置自然人端地址，只能把上面的令牌交给本人。
      </view>
      <button class="btn btn--ghost" :loading="issuing" @click="issueLink">重新生成链接</button>
      <view v-if="handoff.expiresText" class="tip">{{ handoff.expiresText }}</view>

      <view class="actions">
        <button class="btn btn--primary" @click="finish">完成，返回进度</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onHide, onLoad, onUnload } from '@dcloudio/uni-app'
import QRCode from 'qrcode'
import {
  recognizeBankCard,
  recognizeIdCardBack,
  recognizeIdCardFront,
  submitOnboardingWizard,
  OnboardingWizardSubmitReq
} from '@/api/wizard'
import { useHandoffLink } from '@youfeibao/field-shared'
import { chooseImage, compressDataUrl, pathToDataUrl } from '@/utils/upload'
import {
  clearWizardDraft,
  emptyWizardDraft,
  loadWizardDraft,
  saveWizardDraft,
  OnboardingWizardDraft
} from '@/utils/wizardDraft'

/**
 * 建档向导（#91）：拍身份证正反面 → 确认 → 拍银行卡 → 确认 → 签署协议。
 *
 * - 分步状态**只在本地暂存**（`wizardDraft`），第 4 步确认后一次性落库；
 * - 识别是「上传 + 识别」的无状态调用，图片识别完即弃、不落库、不进文件服务；
 * - 结果只在空缺处回填，人工输入的值优先；确认后以确认后的为准（提交的是确认页的值）；
 * - 协议签署方式由后端定：租户开通电子签章则落「待签署」并发起合同组签署（本人在自己手机
 *   上点「去签署」），未开通则降级纸质当场生效。
 */
defineOptions({ name: 'FieldPayeeWizard' })

function documentSvg(content: string) {
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="120" height="76" viewBox="0 0 120 76" fill="none"><rect x="2" y="2" width="116" height="72" rx="9" fill="#fff" stroke="#a3c7b3" stroke-width="2"/>' + content + '</svg>')
}
const documentArt = {
  front: documentSvg('<rect x="12" y="15" width="34" height="45" rx="5" fill="#e9f3ed"/><circle cx="29" cy="30" r="8" fill="#86b59d"/><path d="M17 52a12 12 0 0 1 24 0" fill="#86b59d"/><path d="M58 24h44M58 36h30M58 48h38M14 65h88" stroke="#a3c7b3" stroke-width="3" stroke-linecap="round"/>'),
  back: documentSvg('<circle cx="60" cy="26" r="12" fill="#e9f3ed" stroke="#a3c7b3" stroke-width="2"/><path d="M54 26l4 4 8-8M26 48h68M36 59h48" stroke="#86b59d" stroke-width="3" stroke-linecap="round"/>'),
  bank: documentSvg('<path d="M3 18h114v13H3z" fill="#d4e7dc"/><rect x="15" y="41" width="20" height="14" rx="3" fill="#e9deb7"/><path d="M16 64h18M43 64h18M70 64h18M93 45h11" stroke="#a3c7b3" stroke-width="3" stroke-linecap="round"/>')
}

const STEP_NAMES = ['拍身份证', '确认身份', '拍银行卡', '确认卡信息', '签署协议']

/** 工行收方账号：16-19 位数字（与后端 PayeeInfoSaveReqVO 同一条规则） */
const BANK_CARD_RE = /^\d{16,19}$/
const ID_CARD_RE = /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/
const ADDRESS_MIN_CHINESE = 4
const ADDRESS_MIN_CHARS = 7
/** 腾讯要求 base64 后单张不超过 10M；现场原图常超，拍完先压缩再在这里拦一道 */
const MAX_BASE64_LENGTH = 10 * 1024 * 1024
/** 质量分低于它就在确认页提示重拍，但**不硬拦**（#93 验收）。 */
const QUALITY_WARN_THRESHOLD = 60

const draft = reactive<OnboardingWizardDraft>(emptyWizardDraft())
const submitting = ref(false)
const recognizing = ref(false)
/** 本次是否从上次未完成的草稿继续（#91 评审 SP-5：带上一位的草稿时必须让人看得见） */
const resumed = ref(false)
/** 建档完成（跳回进度页）后不再回写草稿，否则 `finish` 清掉的草稿会被 `onUnload` 又存回来 */
let finished = false
// 转达入口（令牌 / 链接 / 二维码 / 有效期）走共享 composable，与 `pages/payee/index.vue` 同一份
// （#91 复审 ST-B：以前是整段复制，复制时把有效期文案的插值丢了）
const { issuing, handoff, issueLink, copyLink, resetHandoff } = useHandoffLink(
  () => draft.payeeId,
  renderQr,
  tips
)

onLoad(() => {
  const saved = loadWizardDraft()
  if (saved) {
    Object.assign(draft, saved)
    resumed.value = !!(saved.idCardNo || saved.name || saved.idFrontImage)
  }
  // 停在结果页时重进：档案已建好，只要把二维码 / 链接再取一次
  if (draft.step === 5 && draft.payeeId) {
    issueLink()
  }
})

// 切走（接电话、切后台）就把当前进度写回本地
onHide(persist)
// 返回键 / 关闭页面走 onUnload，不走 onHide：不补这一下，第 2、4 步刚手输的文字会随退出丢掉
onUnload(() => {
  if (!finished) {
    persist()
  }
})

/** 「是否我行卡」界面上显示的那个值：本人确认过的优先，否则显示识别结果（都不清空重拍） */
const accountCodeShown = computed(() => draft.accountCode || draft.accountCodeRecognized)
const idBlockReasons = computed(() => [...draft.idFrontBlockReasons, ...draft.idBackBlockReasons])
const idBlocked = computed(() => idBlockReasons.value.length > 0)
const resumeLabel = computed(() => [draft.name, draft.idCardNo].filter(Boolean).join(' / '))

/** 协议状态展示：后端回带了就以它为准，没回带（旧草稿 / 重进页面）就按签署方式推断（#95） */
const agreementStatusName = computed(() => {
  if (draft.agreementStatus === 0) {
    return '待签署'
  }
  if (draft.agreementStatus === 1) {
    return '已生效'
  }
  return draft.signMethod === 'ELECTRONIC' ? '待签署' : '已生效'
})

const canStep1Next = computed(
  () => !!draft.idFrontImage && !!draft.idBackImage && !idBlocked.value
)
const canStep3Next = computed(() => !!draft.bankImage && draft.bankBlockReasons.length === 0)

/** 质量分提示：低分提示重拍，高分只报数；都没有就不显示。 */
interface QualityInfo {
  text: string
  low: boolean
}

function qualityInfo(score?: number): QualityInfo | null {
  if (score === undefined || score === null) {
    return null
  }
  const low = score < QUALITY_WARN_THRESHOLD
  return { text: low ? `图片质量分 ${score}（偏低，建议重拍）` : `图片质量分 ${score}`, low }
}

const idFrontQuality = computed(() => qualityInfo(draft.idFrontQualityScore))
const idBackQuality = computed(() => qualityInfo(draft.idBackQualityScore))
const bankQuality = computed(() => qualityInfo(draft.bankQualityScore))

function tips(message: string) {
  uni.showToast({ title: message, icon: 'none', duration: 2600 })
}

function persist() {
  try {
    saveWizardDraft(draft)
  } catch (e) {
    tips((e as Error).message)
  }
}

/** 不是同一位出售者：把上一位的草稿清干净（#91 评审 SP-5） */
function restart() {
  clearWizardDraft()
  Object.assign(draft, emptyWizardDraft())
  resumed.value = false
  resetHandoff()
}

// ==================== 拍照 + 识别（无状态） ====================

/** 拍照 / 选图并读成 dataURL；H5 靠 chooseImage 的 compressed 模式压，另加一道 10M 上限拦截 */
async function pickImage(): Promise<string> {
  const paths = await chooseImage(1)
  if (!paths.length) {
    throw new Error('未选择照片')
  }
  const dataUrl = await pathToDataUrl(paths[0])
  // 前端必须先压缩：腾讯要求 base64 后单张不超过 10M，手机原图常超（#93 验收）
  const compressed = await compressDataUrl(dataUrl, MAX_BASE64_LENGTH)
  if (toBase64(compressed).length > MAX_BASE64_LENGTH) {
    throw new Error('照片太大（识别要求单张不超过 10M），请重拍或换一张')
  }
  return compressed
}

/** 去掉 dataURL 前缀：识别接口要的是纯 base64（图片随请求进来、识别完即弃） */
function toBase64(dataUrl: string): string {
  const index = dataUrl.indexOf(',')
  return index >= 0 ? dataUrl.slice(index + 1) : dataUrl
}

async function shootIdFront() {
  if (recognizing.value) {
    return
  }
  recognizing.value = true
  try {
    let image: string
    try {
      image = await pickImage()
    } catch (e) {
      tips((e as Error).message || '未拍到照片，可重拍')
      return
    }
    // **先落图再识别**：识别失败（弱网超时）也不丢照片，收货员仍能回第 2 步手工录入（#91 复审 SP-C）
    draft.idFrontImage = image
    draft.idFrontWarnings = []
    draft.idFrontBlockReasons = []
    draft.idFrontQualityScore = undefined
    persist()
    try {
      const resp = await recognizeIdCardFront({
        imageBase64: toBase64(image),
        name: draft.name,
        idCardNo: draft.idCardNo,
        address: draft.address
      })
      draft.name = resp.name || ''
      draft.idCardNo = resp.idCardNo || ''
      draft.address = resp.address || ''
      // 正反面各留各的：同一对字段会被后拍的那张覆盖，硬拦就能被绕过（#91 评审 SP-3）
      draft.idFrontWarnings = resp.warnings || []
      draft.idFrontBlockReasons = resp.blockReasons || []
      draft.idFrontQualityScore = resp.qualityScore
      persist()
    } catch (e) {
      tips((e as Error).message || '识别失败，照片已保留，可继续手工录入')
    }
  } finally {
    recognizing.value = false
  }
}

async function shootIdBack() {
  if (recognizing.value) {
    return
  }
  recognizing.value = true
  try {
    let image: string
    try {
      image = await pickImage()
    } catch (e) {
      tips((e as Error).message || '未拍到照片，可重拍')
      return
    }
    // 先落图再识别（#91 复审 SP-C）
    draft.idBackImage = image
    draft.idBackWarnings = []
    draft.idBackBlockReasons = []
    draft.idBackQualityScore = undefined
    persist()
    try {
      const resp = await recognizeIdCardBack({
        imageBase64: toBase64(image),
        idSignDate: draft.idSignDate,
        idValidityPeriod: draft.idValidityPeriod
      })
      draft.idSignDate = resp.idSignDate || ''
      draft.idValidityPeriod = resp.idValidityPeriod || ''
      draft.idBackWarnings = resp.warnings || []
      draft.idBackBlockReasons = resp.blockReasons || []
      draft.idBackQualityScore = resp.qualityScore
      persist()
    } catch (e) {
      tips((e as Error).message || '识别失败，照片已保留，可继续手工录入')
    }
  } finally {
    recognizing.value = false
  }
}

async function shootBankCard() {
  if (recognizing.value) {
    return
  }
  recognizing.value = true
  try {
    let image: string
    try {
      image = await pickImage()
    } catch (e) {
      tips((e as Error).message || '未拍到照片，可重拍')
      return
    }
    // 先落图再识别（#91 复审 SP-C）
    draft.bankImage = image
    draft.bankWarnings = []
    draft.bankBlockReasons = []
    draft.bankQualityScore = undefined
    persist()
    try {
      const resp = await recognizeBankCard({
        imageBase64: toBase64(image),
        bankCardNo: draft.bankCardNo,
        bankName: draft.bankName,
        // 只把**本人确认过**的值给后端：未确认时留空，识别结果才回填得进来（#91 评审 SP-2）
        accountCode: draft.accountCode || undefined
      })
      draft.bankCardNo = resp.bankCardNo || ''
      draft.bankName = resp.bankName || ''
      // 确认过的值不动；未确认时把识别结果单独存下来，供确认页展示与提交时作为第二顺位
      if (!draft.accountCode) {
        draft.accountCodeRecognized = resp.accountCode || ''
      }
      draft.bankWarnings = resp.warnings || []
      draft.bankBlockReasons = resp.blockReasons || []
      draft.bankQualityScore = resp.qualityScore
      persist()
    } catch (e) {
      tips((e as Error).message || '识别失败，照片已保留，可继续手工录入')
    }
  } finally {
    recognizing.value = false
  }
}

// ==================== 分步 ====================

function goStep(step: number) {
  draft.step = step
  persist()
}

function toStep3() {
  if (!draft.name || !draft.idCardNo) {
    tips('姓名与身份证号必填')
    return
  }
  if (!ID_CARD_RE.test(draft.idCardNo)) {
    tips('身份证号格式不正确')
    return
  }
  if (!draft.mobile) {
    tips('手机号必填：本人实名要用它')
    return
  }
  const address = (draft.address || '').trim()
  const chineseCount = (address.match(/[\u4e00-\u9fa5]/g) || []).length
  if (chineseCount < ADDRESS_MIN_CHINESE && address.length < ADDRESS_MIN_CHARS) {
    tips(`住址至少 ${ADDRESS_MIN_CHINESE} 个汉字（或不少于 ${ADDRESS_MIN_CHARS} 个字符），工行才收`)
    return
  }
  goStep(3)
}

// ==================== 第 4 步确认 → 一次性落库 ====================

async function onSubmit() {
  if (!BANK_CARD_RE.test(draft.bankCardNo)) {
    tips('银行卡号应为 16-19 位数字')
    return
  }
  submitting.value = true
  try {
    const payload: OnboardingWizardSubmitReq = {
      name: draft.name,
      idCardNo: draft.idCardNo,
      mobile: draft.mobile,
      address: draft.address,
      idSignDate: draft.idSignDate || undefined,
      idValidityPeriod: draft.idValidityPeriod || undefined,
      bankCardNo: draft.bankCardNo,
      bankName: draft.bankName || undefined,
      // 人工确认 → 识别结果 → 留空（后端按缺省「是工行卡」兜底）（#91 评审 SP-2）
      accountCode: draft.accountCode || draft.accountCodeRecognized || undefined,
      productName: draft.productName || undefined,
      quantity: draft.quantity || undefined,
      specification: draft.specification || undefined,
      recyclePeriod: draft.recyclePeriod || undefined,
      settlementMethod: draft.settlementMethod || undefined
    }
    const resp = await submitOnboardingWizard(payload)
    draft.payeeId = resp.payeeId
    draft.signMethod = resp.signMethod || 'PAPER'
    // 消费后端回带的两件事（#95）：协议状态与「本人接下来做什么」的说明。以前只读了 signMethod，
    // 结果状态与现场话术都由前端自己猜；现在直接展示后端组装的那份，避免页面与事实相反。
    draft.agreementStatus = resp.agreementStatus
    draft.signMessage = resp.message || ''
    draft.step = 5
    persist()
    await issueLink()
  } catch (e) {
    tips((e as Error).message || '建档失败')
  } finally {
    submitting.value = false
  }
}

// ==================== 二维码 ====================

/**
 * 把链接渲染成二维码（`qrcode` 是宿主依赖，不引到共享包；令牌 / 链接 / 有效期在 `useHandoffLink`）。
 */
async function renderQr(text: string) {
  if (!text) {
    return ''
  }
  try {
    return await QRCode.toDataURL(text, { width: 240, margin: 1, errorCorrectionLevel: 'M' })
  } catch {
    return ''
  }
}

function finish() {
  finished = true
  clearWizardDraft()
  const payeeId = draft.payeeId
  if (payeeId) {
    uni.redirectTo({ url: `/pages/payee/index?payeeId=${payeeId}` })
  } else {
    uni.navigateBack()
  }
}
</script>

<style lang="scss" scoped>
.page { box-sizing: border-box; width: 100%; max-width: 480px; min-height: 100vh; margin: 0 auto; padding: 24px 16px calc(108px + env(safe-area-inset-bottom)); color: #203b2e; background: #f3f7f5; }
button { cursor: pointer; &::after { border: none; } &:focus-visible { outline: 3px solid #80b69a; outline-offset: 3px; } }
.wizard-heading { display: flex; justify-content: space-between; align-items: center; padding: 0 4px; margin-bottom: 24px;
  &__eyebrow { color: #64796c; font-size: 12px; margin-bottom: 7px; }
  &__title { font-size: 26px; font-weight: 700; }
  &__count { color: #6c8174; font-size: 14px; text { color: #176b4c; font-size: 28px; font-weight: 600; } }
}
.steps { display: flex; margin: 0 0 24px;
  &__item { position: relative; display: flex; flex-direction: column; align-items: center; flex: 1; min-width: 0; color: #697d70; font-size: 10px;
    &:not(:last-child)::after { content: ''; position: absolute; top: 14px; left: calc(50% + 18px); width: calc(100% - 36px); height: 2px; background: #dce7df; }
  }
  &__no { display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; margin-bottom: 8px; border-radius: 50%; background: #e4ece7; font-size: 13px; font-weight: 600; }
  &__item--active { color: #176b4c; font-weight: 600; .steps__no { background: #176b4c; color: #fff; box-shadow: 0 0 0 4px #e0eee5; } }
  &__item--done { color: #176b4c; .steps__no { background: #d5eadd; color: #176b4c; } &:not(:last-child)::after { background: #83b899; } }
}
.card { padding: 20px 16px; background: #fff; border: 1px solid #e4ece7; border-radius: 20px; box-shadow: 0 4px 16px rgba(23,78,59,.025);
  &__title { margin-bottom: 10px; font-size: 19px; font-weight: 600; line-height: 1.5; }
}
.tip { margin: 8px 0 14px; color: #63766a; font-size: 13px; line-height: 1.7; }
.field { margin-top: 20px; &__label { display: block; margin-bottom: 9px; color: #354d3f; font-size: 14px; line-height: 1.5; } }
.input { box-sizing: border-box; width: 100%; height: 52px; padding: 0 14px; font-size: 16px; background: #f7f9f7; border: 1px solid #dce6df; border-radius: 11px; &:focus-within { border-color: #278158; box-shadow: 0 0 0 3px #e8f2eb; } }
.shot { box-sizing: border-box; display: flex; align-items: center; justify-content: center; width: 100%; min-height: 204px; padding: 18px 12px; margin-top: 18px; overflow: hidden; background: #f6faf7; border: 1px dashed #9ebfad; border-radius: 15px; line-height: 1.5; &[disabled] { background: #f1f5f2; } &:active { background: #eaf4ee; }
  &__img { width: 100%; height: 170px; }
  &__empty { display: flex; flex-direction: column; align-items: center; }
  &__illustration { width: 100px; height: 63px; margin-bottom: 12px; }
  &__title { color: #254b36; font-size: 16px; font-weight: 600; }
  &__subtitle { margin-top: 4px; font-size: 12px; color: #6b8071; }
  &__capture { margin-top: 13px; padding: 6px 14px; color: #176b4c; border: 1px solid #c2dacb; border-radius: 8px; background: #fff; font-size: 12px; }
  &__hint { margin: 8px 0 0; color: #687d6f; font-size: 12px; text-align: center; }
}
.actions { position: fixed; z-index: 10; bottom: 0; left: 50%; transform: translateX(-50%); box-sizing: border-box; display: flex; gap: 12px; width: 100%; max-width: 480px; padding: 14px 20px calc(14px + env(safe-area-inset-bottom)); border-top: 1px solid #e5ece7; background: #fff; box-shadow: 0 -4px 20px rgba(23,78,59,.04); .btn--ghost { flex: 0 0 88px; } }
.action-note { margin-top: 18px; color: #687d6f; font-size: 12px; text-align: center; line-height: 1.6; }
.btn { flex: 1; min-width: 0; min-height: 50px; padding: 0 12px; margin: 0; border-radius: 12px; font-size: 15px; font-weight: 600; line-height: 50px;
  &--primary { background: #176b4c; color: #fff; &[disabled] { background: #e4ebe6; color: #6d7d72; } }
  &--ghost { background: #f3f8f5; color: #176b4c; border: 1px solid #cddfd3; }
}
.toggle { display: flex; gap: 10px; &__item { flex: 1; padding: 14px 6px; text-align: center; background: #f3f7f4; border: 1px solid #dce6df; border-radius: 11px; font-size: 14px; cursor: pointer; } &__item--active { color: #176b4c; font-weight: 600; border-color: #278158; background: #e5f2ea; } }
.divider { height: 1px; margin: 28px 0 22px; background: #e6ede8; }
.resume { padding: 16px; margin-bottom: 18px; background: #edf4ef; border: 1px solid #cddfd3; border-radius: 14px; overflow-wrap: anywhere; &__title { font-size: 14px; font-weight: 600; line-height: 1.6; } &__desc { margin-top: 6px; font-size: 13px; color: #63766a; line-height: 1.6; } }
.notice { padding: 16px; margin: 16px 0; background: #fff6e5; border: 1px solid #f0dfbc; border-radius: 14px; &__title { color: #805824; font-size: 16px; font-weight: 600; line-height: 1.6; } &__desc { margin-top: 8px; color: #805824; font-size: 14px; line-height: 1.7; } }
.alerts { padding: 12px; margin-top: 12px; border-radius: 10px; font-size: 13px; line-height: 1.7; &--warn { color: #805824; background: #fff6e5; } &--block { color: #a23434; background: #fff0ef; } &--info { color: #466353; background: #edf5ef; } }
.handoff__title { margin: 24px 0 8px; font-size: 17px; font-weight: 600; line-height: 1.6; }
.qr { display: flex; justify-content: center; padding: 20px 0; &__img { width: 200px; height: 200px; } }
.link-box { padding: 14px; margin-bottom: 16px; background: #f3f7f4; border: 1px solid #e1eae3; border-radius: 12px; overflow-wrap: anywhere; &__url { font-size: 12px; line-height: 1.7; color: #536c5d; } }
.link { display: inline-block; min-height: 44px; padding: 10px 0; margin: 4px 0 0; background: transparent; color: #176b4c; font-size: 13px; text-align: left; line-height: 24px; }
.hint { margin: 14px 0; color: #63766a; font-size: 13px; line-height: 1.7; &--warn { color: #805824; } }
</style>
