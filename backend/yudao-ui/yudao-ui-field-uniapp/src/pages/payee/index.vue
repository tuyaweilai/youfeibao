<template>
  <view class="page">
    <!-- 第一步：认出人 / 新建档（只登记，不代做手续） -->
    <template v-if="!payeeId">
      <view class="card">
        <view class="card__title">回头客带档</view>
        <view class="row">
          <input v-model="lookup.idCardNo" class="input" placeholder="身份证号" />
          <input v-model="lookup.mobile" class="input" placeholder="手机号" />
        </view>
        <button class="btn btn--ghost" :loading="looking" @click="onLookup">带出档案</button>
        <view v-if="lookedUp && !foundSeller" class="hint hint--warn">
          没查到档案，请在下方建档。
        </view>
        <view v-if="foundSeller" class="seller">
          <view class="seller__name">{{ foundSeller.name }}</view>
          <view class="seller__meta">{{ foundSeller.mobile || foundSeller.idCardNo }}</view>
          <button class="link" @click="openOnboarding(foundSeller.id!, foundSeller.name)">
            查看进度 / 再给一次链接
          </button>
        </view>
      </view>

      <view class="card">
        <view class="card__title">建档</view>
        <view class="tip">
          用五步向导建档：拍身份证正反面与银行卡 → 识别 → 确认 → 落库 → 纸质协议。
          识别不出时手工录入也能走完；未开通电子签章时协议落纸质签法。
        </view>
        <button class="btn btn--primary" @click="startWizard">开始建档向导</button>
      </view>
    </template>

    <!-- 第二步：只读准入进度 + 转达链接 -->
    <template v-else>
      <view class="card status">
        <view class="status__top">
          <view class="status__name">{{ overview?.name || payeeName || '出售者' }}</view>
          <text class="tag" :class="overview?.invoiceEligible ? 'tag--ok' : 'tag--warn'">
            {{ overview?.invoiceEligible ? '可用于开票' : '暂不可开票' }}
          </text>
        </view>

        <view v-if="awaitingRealName" class="pending">
          <view class="pending__title">待本人实名</view>
          <view class="pending__desc">
            实名只有本人能做：让他用微信扫下面的码，或打开链接完成人脸。
          </view>
        </view>
        <view v-else-if="overview?.rejectReason" class="status__warn">入驻被拒：{{ overview.rejectReason }}</view>
        <view v-else-if="overview?.realNameMsg" class="status__warn">实名信息：{{ overview.realNameMsg }}</view>

        <view class="grid">
          <view class="grid__item">
            <view class="grid__label">实人认证</view>
            <view class="grid__value">{{ overview?.realNameStatusName || '未认证' }}</view>
          </view>
          <view class="grid__item">
            <view class="grid__label">收方入驻</view>
            <view class="grid__value">{{ overview?.onboardingStateName || '未开始' }}</view>
          </view>
          <view class="grid__item">
            <view class="grid__label">框架收购协议</view>
            <view class="grid__value">{{ overview?.frameworkAgreement ? '已签' : '未签' }}</view>
          </view>
          <view class="grid__item">
            <view class="grid__label">首次授权</view>
            <view class="grid__value">{{ overview?.authorization ? '已授权' : '未授权' }}</view>
          </view>
        </view>

        <view v-if="overview?.nextStep" class="status__next">下一步：{{ overview.nextStep }}</view>
        <view v-if="overview?.invoiceBlockReason" class="status__warn">{{ overview.invoiceBlockReason }}</view>
        <view class="status__refresh">
          <text class="link" @click="refresh">刷新进度</text>
        </view>
      </view>

      <view class="card">
        <view class="card__title">交给本人用微信办理</view>
        <view class="tip">
          人脸只能在微信里唤起。请让本人用微信扫这个码，或把链接发到他微信里打开。
        </view>
        <view v-if="handoff.qr" class="qr">
          <image class="qr__img" :src="handoff.qr" mode="aspectFit" />
        </view>
        <view class="link-box">
          <view class="link-box__url">{{ handoff.link || handoff.token || '（尚未生成）' }}</view>
          <button v-if="handoff.link || handoff.token" class="link" @click="copyLink">
            复制{{ handoff.link ? '链接' : '令牌' }}
          </button>
        </view>
        <view v-if="!handoff.link && handoff.token" class="hint hint--warn">
          未配置自然人端地址（VITE_APP_SELLER_URL），只能把上面的令牌交给本人。
        </view>
        <button class="btn btn--ghost" :loading="issuing" @click="issueLink">
          {{ handoff.link ? '重新生成链接' : '生成链接' }}
        </button>
        <view v-if="handoff.expiresText" class="tip">{{ handoff.expiresText }}</view>
      </view>

      <button class="btn btn--ghost" @click="backToSeller">换一位出售者</button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import QRCode from 'qrcode'
import { findReturningCustomer, PayeeVO } from '@/api/payee'
import { isRealNamePassed, useHandoffLink, useSellerOnboarding } from '@youfeibao/field-shared'
import { clearWizardDraft } from '@/utils/wizardDraft'

/**
 * 现场端「自然人建档」（#88 只登记与转达，#91 换上五步向导）。
 *
 * 建档改走五步向导（`pages/payee/wizard`）：拍身份证正反面与银行卡 → 识别 → 确认 → 落库 →
 * 纸质协议。这一页仍做两件事：回头客带档，以及建档后的**只读准入进度** +
 * 二维码 / 可复制链接（实名只能本人做，现场只负责把入口交给他）。
 *
 * 四步的接口与状态走共享 composable（同一份也在司机端用，见 packages/field-shared/README.md）。
 */
defineOptions({ name: 'FieldPayee' })

const payeeId = ref<number>()
const payeeName = ref('')
const looking = ref(false)
const lookedUp = ref(false)
const foundSeller = ref<PayeeVO | null>(null)

const lookup = reactive({ idCardNo: '', mobile: '' })

// 只读准入进度：composable 在 payeeId 变化时自动拉取（watch immediate）
const { overview, load, tips } = useSellerOnboarding(() => payeeId.value)

// 「交给本人」的入口：令牌 / 链接 / 二维码 / 有效期一起生成、一起清空（#91 复审 ST-B 收进共享包）
const { issuing, handoff, issueLink, copyLink, resetHandoff } = useHandoffLink(
  () => payeeId.value,
  renderQr,
  tips
)

/** 实名未通过（含未认证 / 认证中 / 未通过）就算「待本人实名」；进度未加载完不下结论 */
const awaitingRealName = computed(() => !!overview.value && !isRealNamePassed(overview.value.realNameStatus))

onShow(() => {
  // 出售者可能刚在自己手机上做完实名：回到这一页就重新读一次，现场不用点任何按钮
  if (payeeId.value) {
    load()
  }
})

/** 向导走完跳回来时带上 payeeId：直接进只读进度并准备好「交给本人」的入口 */
onLoad((query) => {
  const id = Number(query?.payeeId || 0)
  if (id) {
    openOnboarding(id)
  }
})

function startWizard() {
  uni.navigateTo({ url: '/pages/payee/wizard' })
}

async function onLookup() {
  if (!lookup.idCardNo && !lookup.mobile) {
    tips('请填身份证号或手机号')
    return
  }
  looking.value = true
  try {
    foundSeller.value = await findReturningCustomer({
      idCardNo: lookup.idCardNo || undefined,
      mobile: lookup.mobile || undefined
    })
    lookedUp.value = true
    if (!foundSeller.value?.id) {
      foundSeller.value = null
    }
  } catch (e) {
    tips((e as Error).message || '带档失败')
  } finally {
    looking.value = false
  }
}

/** 进入某位出售者：拉一次只读进度，并立刻把「交给本人」的入口准备好 */
async function openOnboarding(id: number, name?: string) {
  payeeId.value = id
  payeeName.value = name || ''
  await issueLink()
}

async function refresh() {
  await load()
  tips('已刷新')
}

/**
 * 把链接渲染成二维码（`qrcode` 是宿主依赖，不引到共享包，见 `useHandoffLink` 的注释）。
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

function backToSeller() {
  // 「换一位出售者」：把上一位未完成的向导草稿一并清干净，下一位不会看到他/她的照片与字段（#91 评审 SP-5）
  clearWizardDraft()
  payeeId.value = undefined
  payeeName.value = ''
  foundSeller.value = null
  lookedUp.value = false
  resetHandoff()
  lookup.idCardNo = ''
  lookup.mobile = ''
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
}

.card {
  padding: 32rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__title {
    margin-bottom: 24rpx;
    font-size: 32rpx;
    font-weight: 600;
  }
}

.row {
  display: flex;
  gap: 16rpx;
}

.input {
  flex: 1;
  height: 80rpx;
  padding: 0 20rpx;
  margin-bottom: 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
}

.field {
  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
  }
}

.seller {
  margin-top: 24rpx;
  padding: 24rpx;
  background-color: #eef4ff;
  border-radius: 12rpx;

  &__name {
    font-size: 32rpx;
    font-weight: 600;
  }

  &__meta {
    margin-top: 8rpx;
    color: $field-text-secondary;
  }
}

.status {
  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__name {
    font-size: 36rpx;
    font-weight: 700;
  }

  &__warn {
    margin-top: 12rpx;
    color: #cf1322;
    line-height: 1.6;
  }

  &__next {
    margin-top: 12rpx;
    color: #b26a00;
    line-height: 1.6;
  }

  &__refresh {
    margin-top: 20rpx;
    text-align: right;
  }
}

.pending {
  margin-top: 20rpx;
  padding: 20rpx 24rpx;
  background-color: #fff7e6;
  border-radius: 12rpx;

  &__title {
    font-size: 32rpx;
    font-weight: 700;
    color: #b26a00;
  }

  &__desc {
    margin-top: 8rpx;
    color: #b26a00;
    line-height: 1.6;
  }
}

.grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20rpx;

  &__item {
    width: 50%;
    padding: 12rpx 0;
  }

  &__label {
    color: $field-text-secondary;
    font-size: 24rpx;
  }

  &__value {
    margin-top: 4rpx;
    font-size: 28rpx;
  }
}

.qr {
  display: flex;
  justify-content: center;
  padding: 24rpx 0;

  &__img {
    width: 360rpx;
    height: 360rpx;
  }
}

.tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  background-color: #eef4ff;
  color: $field-primary;

  &--ok {
    background-color: #e8f7ee;
    color: #1a7f43;
  }

  &--warn {
    background-color: #fff7e6;
    color: #b26a00;
  }
}

.link-box {
  margin-top: 16rpx;
  padding: 16rpx 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
  word-break: break-all;

  &__url {
    color: $field-text-secondary;
    font-size: 24rpx;
    line-height: 1.6;
  }
}

.hint {
  margin-top: 20rpx;
  line-height: 1.6;
  color: $field-text-secondary;

  &--warn {
    color: #b26a00;
  }
}

.tip {
  margin-top: 16rpx;
  color: $field-text-secondary;
  font-size: 24rpx;
  line-height: 1.6;
}

.link {
  display: inline-block;
  padding: 0;
  margin-top: 12rpx;
  color: $field-primary;
  font-size: 26rpx;
  background-color: transparent;
  text-align: left;
}

.btn {
  flex: 1;
  margin-top: 8rpx;

  &--primary {
    color: #ffffff;
    background-color: $field-primary;
  }

  &--ghost {
    color: $field-primary;
    background-color: #ffffff;
    border: 1rpx solid $field-primary;
  }
}
</style>
