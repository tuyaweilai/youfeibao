<template>
  <view class="page">
    <!-- 链接无效 / 已过期：给可读提示，不显示任何租户内数据 -->
    <view v-if="linkError" class="card empty">
      <view class="empty__title">链接不可用</view>
      <view class="empty__desc">{{ linkError }}</view>
      <view class="empty__desc">请让收货员重新生成一枚链接。</view>
    </view>

    <template v-else>
      <view class="banner">
        <view class="banner__title">本人自填建档</view>
        <view class="banner__desc">
          用你自己的手机拍身份证与银行卡；照片只用于识别，平台不留存。中途退出不产生任何档案。
        </view>
      </view>

      <!-- 五步的进度条自己画：分步状态只在本页 / 本地草稿里，不落后端 -->
      <view class="steps">
        <view
          v-for="(name, index) in STEP_NAMES"
          :key="name"
          class="steps__item"
          :class="{ 'steps__item--active': draft.step === index + 1, 'steps__item--done': draft.step > index + 1 }"
        >
          <text class="steps__no">{{ index + 1 }}</text>
          <text class="steps__name">{{ name }}</text>
        </view>
      </view>

      <view v-if="resumed && draft.step !== 5" class="resume">
        <view class="resume__title">已带出上次未完成的填写</view>
        <view class="resume__desc">不想接着填？可以清掉重来。</view>
        <button class="link" @click="restart">重新开始（清空）</button>
      </view>

      <!-- 第 1 步：拍身份证（正反面） -->
      <view v-if="draft.step === 1" class="card">
        <view class="card__title">1. 拍身份证（正反面）</view>
        <view class="tip">识别不出也能继续：下一步手工录入就行。照片只用于识别，平台不留存。</view>

        <view class="shot" @click="shootIdFront">
          <image v-if="draft.idFrontImage" class="shot__img" :src="draft.idFrontImage" mode="aspectFit" />
          <view v-else class="shot__empty">拍人像面（带姓名与住址）</view>
        </view>
        <view class="shot__hint">{{ draft.idFrontImage ? '点击重拍人像面' : '未拍摄人像面' }}</view>
        <view v-if="draft.idFrontWarnings.length" class="alerts alerts--warn">
          <view v-for="(word, i) in draft.idFrontWarnings" :key="i">· {{ word }}</view>
        </view>
        <view v-if="draft.idFrontBlockReasons.length" class="alerts alerts--block">
          <view v-for="(word, i) in draft.idFrontBlockReasons" :key="i">· {{ word }}</view>
          <view>人像面这张不能用，请重拍或换一张。</view>
        </view>

        <view class="shot" @click="shootIdBack">
          <image v-if="draft.idBackImage" class="shot__img" :src="draft.idBackImage" mode="aspectFit" />
          <view v-else class="shot__empty">拍国徽面（带有效期）</view>
        </view>
        <view class="shot__hint">{{ draft.idBackImage ? '点击重拍国徽面' : '未拍摄国徽面' }}</view>
        <view v-if="draft.idBackWarnings.length" class="alerts alerts--warn">
          <view v-for="(word, i) in draft.idBackWarnings" :key="i">· {{ word }}</view>
        </view>
        <view v-if="draft.idBackBlockReasons.length" class="alerts alerts--block">
          <view v-for="(word, i) in draft.idBackBlockReasons" :key="i">· {{ word }}</view>
          <view>国徽面这张不能用，请重拍或换一张。</view>
        </view>

        <view class="actions">
          <button class="btn btn--primary" :disabled="!canStep1Next" @click="goStep(2)">下一步：确认信息</button>
        </view>
      </view>

      <!-- 第 2 步：确认身份信息（可改） -->
      <view v-if="draft.step === 2" class="card">
        <view class="card__title">2. 确认识别结果（可改）</view>
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

        <view v-if="idBlocked" class="alerts alerts--block">
          <view v-for="(word, i) in idBlockReasons" :key="i">· {{ word }}</view>
          <view>证件这张不能用，请回上一步重拍或换一张。</view>
        </view>

        <view class="actions">
          <button class="btn btn--ghost" @click="goStep(1)">上一步</button>
          <button class="btn btn--primary" @click="toStep3">下一步：拍银行卡</button>
        </view>
      </view>

      <!-- 第 3 步：拍银行卡 -->
      <view v-if="draft.step === 3" class="card">
        <view class="card__title">3. 拍银行卡</view>
        <view class="tip">拍本人银行卡正面，识别不出就下一步手输。</view>

        <view class="shot" @click="shootBankCard">
          <image v-if="draft.bankImage" class="shot__img" :src="draft.bankImage" mode="aspectFit" />
          <view v-else class="shot__empty">拍银行卡正面</view>
        </view>
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
          <button class="btn btn--primary" :disabled="!canStep3Next" @click="goStep(4)">下一步：确认卡信息</button>
        </view>
      </view>

      <!-- 第 4 步：确认卡信息（可改）+ 协议要素；确认后一次性落库 -->
      <view v-if="draft.step === 4" class="card">
        <view class="card__title">4. 确认卡信息（可改）</view>

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
            识别不到、你也没选时，将按缺省「是工行卡」建档。
          </view>
        </view>

        <view class="divider" />
        <view class="card__title">框架收购协议要素</view>
        <view class="tip">税总要求的合同流证据，留空按缺省值落库。本次协议按纸质签署。</view>
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
          <button class="btn btn--primary" :loading="submitting" @click="onSubmit">确认并提交</button>
        </view>
      </view>

      <!-- 第 5 步：完成（本票协议落 PAPER）+ 接着做实名 -->
      <view v-if="draft.step === 5" class="card">
        <view class="card__title">5. 建档完成</view>
        <view v-if="draft.signMethod === 'PAPER'" class="notice">
          <view class="notice__title">本次协议按纸质签署建档</view>
          <view class="notice__desc">
            电子签章开通后，协议可在你的手机上直接签署；当前版本先按纸质签法建档。
          </view>
        </view>
        <view v-else class="notice">
          <view class="notice__title">协议已按 {{ draft.signMethod || '电子签章' }} 建档</view>
        </view>

        <view class="tip">还差最后一步实名：只有本人能做，做完企业才能给你付款与开票。</view>
        <button class="btn btn--primary" @click="goRealName">去实名</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onHide, onLoad, onUnload } from '@dcloudio/uni-app'
import {
  getWizardContext,
  recognizeBankCard,
  recognizeIdCardBack,
  recognizeIdCardFront,
  submitOnboardingWizard,
  OnboardingWizardSubmitReq
} from '@/api/wizard'
import { getToken } from '@/utils/token'
import { chooseImage, pathToDataUrl, toBase64 } from '@/utils/upload'
import {
  clearWizardDraft,
  emptyWizardDraft,
  loadWizardDraft,
  saveWizardDraft,
  OnboardingWizardDraft
} from '@/utils/wizardDraft'

/**
 * 本人自填建档（#94）：收货员给一枚免注册链接，本人在自己手机上走完与现场端**同一套**五步向导。
 *
 * - 分步状态只在本地暂存（按链接绑定），第 4 步确认后一次性落库；后端不存草稿；
 * - 识别是无状态上传 + 识别，图片识别完即弃、不落库（ADR 0037）；
 * - 结果只在空缺处回填，人工输入优先；提交的是确认页的值；
 * - 建档完成后换一枚实名令牌，本人接着做实名（ADR 0007 补充）。
 */
defineOptions({ name: 'SellerOnboardingWizard' })

const STEP_NAMES = ['拍身份证', '确认身份', '拍银行卡', '确认卡信息', '完成']

const BANK_CARD_RE = /^\d{16,19}$/
const ID_CARD_RE = /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/
const ADDRESS_MIN_CHINESE = 4
const ADDRESS_MIN_CHARS = 7
/** 腾讯要求 base64 后单张不超过 10M；手机原图常超，拍完先在这里拦一道 */
const MAX_BASE64_LENGTH = 10 * 1024 * 1024

const token = ref('')
const draft = reactive<OnboardingWizardDraft>(emptyWizardDraft())
const submitting = ref(false)
const recognizing = ref(false)
const linkError = ref('')
const resumed = ref(false)
/** 建档完成后跳走时不再回写草稿，否则清掉的草稿会被 onUnload 又存回来 */
let finished = false

onLoad((query) => {
  // 入口页带 token 跳进来；直接刷新本页时退回本地存的那枚
  token.value = (query?.token as string) || getToken()
  if (!token.value) {
    linkError.value = '链接不完整'
    return
  }
  const saved = loadWizardDraft(token.value)
  if (saved) {
    Object.assign(draft, saved)
    resumed.value = !!(saved.idCardNo || saved.name || saved.idFrontImage)
  }
  // 已建档（草稿停在第 5 步）：令牌已按一次性作废，但本人仍要能接着做实名，
  // 所以这里不再验令牌，直接用草稿里存的那枚实名令牌。
  if (saved?.step === 5 && saved.onboardingToken) {
    return
  }
  // 打开链接先验一次：过期 / 被收货员作废时立刻给可读提示，而不是拍到一半才失败
  getWizardContext(token.value).catch((e) => {
    linkError.value = (e as Error).message || '链接不可用'
  })
})

// 切走（接电话、切后台）就把当前进度写回本地
onHide(persist)
// 返回键 / 关闭页面走 onUnload，不走 onHide：不补这一下，第 2、4 步刚手输的文字会随退出丢掉
onUnload(() => {
  if (!finished) {
    persist()
  }
})

const accountCodeShown = computed(() => draft.accountCode || draft.accountCodeRecognized)
const idBlockReasons = computed(() => [...draft.idFrontBlockReasons, ...draft.idBackBlockReasons])
const idBlocked = computed(() => idBlockReasons.value.length > 0)
const canStep1Next = computed(() => !!draft.idFrontImage && !!draft.idBackImage && !idBlocked.value)
const canStep3Next = computed(() => !!draft.bankImage && draft.bankBlockReasons.length === 0)

function tips(message: string) {
  uni.showToast({ title: message, icon: 'none', duration: 2600 })
}

function persist() {
  try {
    saveWizardDraft(token.value, draft)
  } catch (e) {
    tips((e as Error).message)
  }
}

function restart() {
  clearWizardDraft(token.value)
  Object.assign(draft, emptyWizardDraft())
  resumed.value = false
}

// ==================== 拍照 + 识别（无状态） ====================

async function pickImage(): Promise<string> {
  const paths = await chooseImage(1)
  if (!paths.length) {
    throw new Error('未选择照片')
  }
  const dataUrl = await pathToDataUrl(paths[0])
  if (toBase64(dataUrl).length > MAX_BASE64_LENGTH) {
    throw new Error('照片太大（识别要求单张不超过 10M），请重拍或换一张')
  }
  return dataUrl
}

async function shootIdFront() {
  if (recognizing.value) return
  recognizing.value = true
  try {
    let image: string
    try {
      image = await pickImage()
    } catch (e) {
      tips((e as Error).message || '未拍到照片，可重拍')
      return
    }
    // 先落图再识别：识别失败（弱网超时）也不丢照片，仍能回下一步手工录入
    draft.idFrontImage = image
    draft.idFrontWarnings = []
    draft.idFrontBlockReasons = []
    persist()
    try {
      const resp = await recognizeIdCardFront(token.value, {
        imageBase64: toBase64(image),
        name: draft.name,
        idCardNo: draft.idCardNo,
        address: draft.address
      })
      draft.name = resp.name || ''
      draft.idCardNo = resp.idCardNo || ''
      draft.address = resp.address || ''
      draft.idFrontWarnings = resp.warnings || []
      draft.idFrontBlockReasons = resp.blockReasons || []
      persist()
    } catch (e) {
      tips((e as Error).message || '识别失败，照片已保留，可继续手工录入')
    }
  } finally {
    recognizing.value = false
  }
}

async function shootIdBack() {
  if (recognizing.value) return
  recognizing.value = true
  try {
    let image: string
    try {
      image = await pickImage()
    } catch (e) {
      tips((e as Error).message || '未拍到照片，可重拍')
      return
    }
    draft.idBackImage = image
    draft.idBackWarnings = []
    draft.idBackBlockReasons = []
    persist()
    try {
      const resp = await recognizeIdCardBack(token.value, {
        imageBase64: toBase64(image),
        idSignDate: draft.idSignDate,
        idValidityPeriod: draft.idValidityPeriod
      })
      draft.idSignDate = resp.idSignDate || ''
      draft.idValidityPeriod = resp.idValidityPeriod || ''
      draft.idBackWarnings = resp.warnings || []
      draft.idBackBlockReasons = resp.blockReasons || []
      persist()
    } catch (e) {
      tips((e as Error).message || '识别失败，照片已保留，可继续手工录入')
    }
  } finally {
    recognizing.value = false
  }
}

async function shootBankCard() {
  if (recognizing.value) return
  recognizing.value = true
  try {
    let image: string
    try {
      image = await pickImage()
    } catch (e) {
      tips((e as Error).message || '未拍到照片，可重拍')
      return
    }
    draft.bankImage = image
    draft.bankWarnings = []
    draft.bankBlockReasons = []
    persist()
    try {
      const resp = await recognizeBankCard(token.value, {
        imageBase64: toBase64(image),
        bankCardNo: draft.bankCardNo,
        bankName: draft.bankName,
        // 只把本人确认过的值给后端：未确认时留空，识别结果才回填得进来
        accountCode: draft.accountCode || undefined
      })
      draft.bankCardNo = resp.bankCardNo || ''
      draft.bankName = resp.bankName || ''
      if (!draft.accountCode) {
        draft.accountCodeRecognized = resp.accountCode || ''
      }
      draft.bankWarnings = resp.warnings || []
      draft.bankBlockReasons = resp.blockReasons || []
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
      accountCode: draft.accountCode || draft.accountCodeRecognized || undefined,
      productName: draft.productName || undefined,
      quantity: draft.quantity || undefined,
      specification: draft.specification || undefined,
      recyclePeriod: draft.recyclePeriod || undefined,
      settlementMethod: draft.settlementMethod || undefined
    }
    const resp = await submitOnboardingWizard(token.value, payload)
    draft.payeeId = resp.payeeId
    draft.signMethod = resp.signMethod || 'PAPER'
    draft.onboardingToken = resp.onboardingToken || ''
    draft.step = 5
    persist()
  } catch (e) {
    // 落库失败（校验不过 / 链接已用尽）：留在第 4 步，已填内容不丢。
    // 已建档不再是一类失败：后端会更新既有档案（#94 修票）。
    tips((e as Error).message || '建档失败')
  } finally {
    submitting.value = false
  }
}

function goRealName() {
  finished = true
  clearWizardDraft(token.value)
  if (draft.onboardingToken) {
    uni.redirectTo({
      url: `/pages/index/index?token=${encodeURIComponent(draft.onboardingToken)}&purpose=ONBOARDING`
    })
    return
  }
  uni.showToast({ title: '建档完成，请让收货员给你实名链接', icon: 'none', duration: 2600 })
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
}

.banner {
  padding: 24rpx 28rpx;
  margin-bottom: 20rpx;
  background-color: #eef4ff;
  border-radius: 16rpx;

  &__title {
    font-size: 32rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 8rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
    line-height: 1.6;
  }
}

.empty {
  margin-top: 160rpx;
  text-align: center;

  &__title {
    font-size: 36rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 16rpx;
    color: $seller-text-secondary;
    line-height: 1.6;
  }
}

.steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24rpx;

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    color: $seller-text-secondary;
    font-size: 22rpx;
  }

  &__no {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44rpx;
    height: 44rpx;
    margin-bottom: 6rpx;
    border-radius: 50%;
    background-color: #e7eaf0;
    font-size: 24rpx;
  }

  &__item--active {
    color: $seller-primary;

    .steps__no {
      color: #ffffff;
      background-color: $seller-primary;
    }
  }

  &__item--done .steps__no {
    color: #ffffff;
    background-color: #1a7f43;
  }
}

.resume {
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
  background-color: #eef4ff;
  border-radius: 12rpx;

  &__title {
    font-size: 28rpx;
    font-weight: 600;
  }

  &__desc {
    margin-top: 8rpx;
    color: $seller-text-secondary;
    font-size: 24rpx;
    line-height: 1.6;
  }
}

.card {
  padding: 32rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__title {
    margin-bottom: 20rpx;
    font-size: 32rpx;
    font-weight: 600;
  }
}

.input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
}

.field {
  margin-bottom: 20rpx;

  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
  }
}

.shot {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300rpx;
  margin-top: 20rpx;
  overflow: hidden;
  background-color: #f5f6f8;
  border: 1rpx dashed #c8cdd8;
  border-radius: 12rpx;

  &__img {
    width: 100%;
    height: 100%;
  }

  &__empty {
    color: $seller-text-secondary;
  }

  &__hint {
    margin-top: 8rpx;
    color: $seller-text-secondary;
    font-size: 24rpx;
  }
}

.divider {
  height: 1rpx;
  margin: 28rpx 0;
  background-color: #eef0f4;
}

.toggle {
  display: flex;
  gap: 16rpx;

  &__item {
    flex: 1;
    padding: 16rpx 0;
    text-align: center;
    background-color: #f5f6f8;
    border-radius: 12rpx;
    font-size: 26rpx;
  }

  &__item--active {
    color: #ffffff;
    background-color: $seller-primary;
  }
}

.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.btn {
  flex: 1;

  &--primary {
    color: #ffffff;
    background-color: $seller-primary;
  }

  &--ghost {
    color: $seller-primary;
    background-color: #ffffff;
    border: 1rpx solid $seller-primary;
  }
}

.notice {
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
  background-color: #fff7e6;
  border-radius: 12rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    color: #b26a00;
  }

  &__desc {
    margin-top: 8rpx;
    color: #b26a00;
    line-height: 1.6;
  }
}

.tip {
  margin-top: 8rpx;
  color: $seller-text-secondary;
  font-size: 24rpx;
  line-height: 1.6;
}

.alerts {
  padding: 16rpx 20rpx;
  margin-top: 20rpx;
  border-radius: 12rpx;
  line-height: 1.6;
  font-size: 24rpx;

  &--warn {
    color: #b26a00;
    background-color: #fff7e6;
  }

  &--block {
    color: #cf1322;
    background-color: #fff1f0;
  }
}

.link {
  display: inline-block;
  padding: 0;
  margin-top: 12rpx;
  color: $seller-primary;
  font-size: 26rpx;
  background-color: transparent;
  text-align: left;
}
</style>
