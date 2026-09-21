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
          没查到档案，请在下方为新出售者建档。
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
        <view class="card__title">新出售者建档</view>
        <view class="field">
          <text class="field__label">姓名</text>
          <input v-model="newSeller.name" class="input" placeholder="与身份证一致" />
        </view>
        <view class="field">
          <text class="field__label">身份证号</text>
          <input v-model="newSeller.idCardNo" class="input" placeholder="身份证号" />
        </view>
        <view class="field">
          <text class="field__label">手机号</text>
          <input v-model="newSeller.mobile" class="input" placeholder="手机号" />
        </view>
        <view class="field">
          <text class="field__label">银行卡号（本人）</text>
          <input v-model="newSeller.bankCardNo" class="input" type="number" placeholder="出售者本人银行卡" />
        </view>
        <view class="field">
          <text class="field__label">地址</text>
          <input v-model="newSeller.address" class="input" placeholder="常住地址" />
        </view>
        <button class="btn btn--primary" :loading="creating" @click="onCreateSeller">建档并交给本人实名</button>
        <view class="tip">
          建档后把二维码或链接交给本人：实名由他本人在微信里做，收方入驻随后自动完成，
          现场不需要再点任何手续。
        </view>
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
import { onShow } from '@dcloudio/uni-app'
import QRCode from 'qrcode'
import { createPayee, findReturningCustomer, PayeeVO } from '@/api/payee'
import { createPublicToken } from '@/api/publicToken'
import { useSellerOnboarding } from '@youfeibao/field-shared'
import { SELLER_APP_URL } from '@/config/env'

/**
 * 现场端「自然人建档」（#88）：**只登记与转达，不做实名**。
 *
 * 建档后显示「待本人实名」+ 二维码 / 可复制链接 + **只读**的准入进度；
 * 实名只能本人做（工行活体，我们替代不了），他做完之后收方入驻由平台自动发起（#85）。
 * 因此这一页**没有**「发起实名认证 / 查询结果 / 发起收方入驻」这类按钮——
 * 谁来做、什么时候做，都由本人决定，现场只负责把入口交给他。
 *
 * 四步的接口与状态走共享 composable（同一份也在司机端用，见 packages/field-shared/README.md）。
 */
defineOptions({ name: 'FieldPayee' })

const payeeId = ref<number>()
const payeeName = ref('')
const looking = ref(false)
const lookedUp = ref(false)
const foundSeller = ref<PayeeVO | null>(null)
const creating = ref(false)
const issuing = ref(false)
/** 「交给本人」的入口：令牌、链接、二维码与有效期一起生成、一起清空 */
const handoff = reactive({ token: '', link: '', qr: '', expiresText: '' })

const lookup = reactive({ idCardNo: '', mobile: '' })
const newSeller = reactive({ name: '', idCardNo: '', mobile: '', bankCardNo: '', address: '' })

/** 工行收方账号：16-19 位数字（与后端 PayeeInfoSaveReqVO 同一条规则） */
const BANK_CARD_RE = /^\d{16,19}$/
/** 工行住址规则：不少于 4 个汉字，或不少于 7 个字符 */
const ADDRESS_MIN_CHINESE = 4
const ADDRESS_MIN_CHARS = 7

// 只读准入进度：composable 在 payeeId 变化时自动拉取（watch immediate）
const { overview, load, tips } = useSellerOnboarding(() => payeeId.value)

/** 实名通过的枚举值是 2（PayeeRealNameStatusEnum.PASSED）；其余都算「待本人实名」。进度未加载完不下结论 */
const REAL_NAME_PASSED = 2
const awaitingRealName = computed(() => !!overview.value && overview.value.realNameStatus !== REAL_NAME_PASSED)

onShow(() => {
  // 出售者可能刚在自己手机上做完实名：回到这一页就重新读一次，现场不用点任何按钮
  if (payeeId.value) {
    load()
  }
})

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

async function onCreateSeller() {
  const invalid = validateNewSeller()
  if (invalid) {
    uni.showModal({ title: '还差一点', content: invalid, showCancel: false })
    return
  }
  creating.value = true
  try {
    const id = await createPayee({ ...newSeller, businessType: 'RECYCLE' })
    await openOnboarding(id, newSeller.name)
  } catch (e) {
    tips((e as Error).message || '建档失败')
  } finally {
    creating.value = false
  }
}

/**
 * 建档前的现场校验：拦下「现场能填进去、工行不收」的值（#84）。
 *
 * 银行卡与住址都是工行收方入驻的必输 / 有格式要求的字段，到了工行才被驳回就晚了：
 * 现场要当场说清缺什么。返回空字符串表示通过。
 */
function validateNewSeller(): string {
  if (!newSeller.name || !newSeller.idCardNo || !newSeller.mobile) {
    return '姓名、身份证号与手机号必填'
  }
  if (!newSeller.bankCardNo) {
    return '请填出售者本人的银行卡号（收方入驻要用）'
  }
  if (!BANK_CARD_RE.test(newSeller.bankCardNo)) {
    return '银行卡号应为 16-19 位数字，请核对后重填'
  }
  const address = (newSeller.address || '').trim()
  const chineseCount = (address.match(/[\u4e00-\u9fa5]/g) || []).length
  if (chineseCount < ADDRESS_MIN_CHINESE && address.length < ADDRESS_MIN_CHARS) {
    return `住址至少 ${ADDRESS_MIN_CHINESE} 个汉字（或不少于 ${ADDRESS_MIN_CHARS} 个字符），工行才收`
  }
  return ''
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
 * 签发一枚 ONBOARDING 一次性令牌，拼出本人要打开的执行链接。
 *
 * 链接是「再次展示」的核心：出售者当时没做，回头还能从这里再拿一次（令牌 24 小时内有效）。
 */
async function issueLink() {
  if (!payeeId.value) {
    return
  }
  issuing.value = true
  try {
    const resp = await createPublicToken({ purpose: 'ONBOARDING', payeeId: payeeId.value })
    handoff.token = resp.token || ''
    handoff.link = handoff.token && SELLER_APP_URL
      ? `${SELLER_APP_URL.replace(/\/$/, '')}/#/?token=${encodeURIComponent(handoff.token)}&purpose=ONBOARDING`
      : ''
    handoff.expiresText = resp.expiresTime ? `链接 24 小时内有效，至 ${formatTime(resp.expiresTime)}` : ''
    handoff.qr = await renderQr(handoff.link)
  } catch (e) {
    tips((e as Error).message || '生成链接失败')
  } finally {
    issuing.value = false
  }
}

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

function copyLink() {
  const text = handoff.link || handoff.token
  if (!text) {
    return
  }
  uni.setClipboardData({
    data: text,
    success: () => tips('已复制，请交给出售者本人打开')
  })
}

function backToSeller() {
  payeeId.value = undefined
  payeeName.value = ''
  foundSeller.value = null
  lookedUp.value = false
  handoff.token = ''
  handoff.link = ''
  handoff.qr = ''
  handoff.expiresText = ''
  lookup.idCardNo = ''
  lookup.mobile = ''
}

function formatTime(ts?: number) {
  return ts ? new Date(ts).toLocaleString() : ''
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
