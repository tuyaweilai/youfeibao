<template>
  <view class="page">
    <!-- 第一步：找到或新建出售者 -->
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
          <button class="link" @click="openOnboarding(foundSeller.id!)">继续建档</button>
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
        <button class="btn btn--primary" :loading="creating" @click="onCreateSeller">建档并开始手续</button>
        <view class="tip">建档后依次完成实人认证 → 收方入驻 → 框架协议 → 首次授权。</view>
      </view>
    </template>

    <!-- 第二步：一次性手续 -->
    <template v-else>
      <view class="card status">
        <view class="status__top">
          <view class="status__name">{{ overview.name || '出售者' }}</view>
          <text class="tag" :class="overview.invoiceEligible ? 'tag--ok' : 'tag--warn'">
            {{ overview.invoiceEligible ? '可用于开票' : '暂不可开票' }}
          </text>
        </view>
        <view v-if="!overview.invoiceEligible" class="status__warn">
          {{ overview.invoiceBlockReason || '建档未完成' }}
        </view>
        <view class="grid">
          <view class="grid__item">
            <view class="grid__label">实人认证</view>
            <view class="grid__value">{{ overview.realNameStatusName || '未认证' }}</view>
          </view>
          <view class="grid__item">
            <view class="grid__label">开户状态</view>
            <view class="grid__value">{{ overview.icbcOpenacctStatus || '未开始' }}</view>
          </view>
          <view class="grid__item">
            <view class="grid__label">审核结果</view>
            <view class="grid__value">{{ overview.auditResult || '未开始' }}</view>
          </view>
          <view class="grid__item">
            <view class="grid__label">入驻结果</view>
            <view class="grid__value">{{ overview.onboardingStateName || '未开始' }}</view>
          </view>
        </view>
        <view v-if="overview.nextStep" class="status__next">下一步：{{ overview.nextStep }}</view>
        <view v-if="overview.rejectReason" class="status__warn">拒绝原因：{{ overview.rejectReason }}</view>
        <view v-if="overview.realNameMsg" class="status__warn">实名信息：{{ overview.realNameMsg }}</view>
      </view>

      <!-- 1. 实人认证 -->
      <view class="card">
        <view class="card__title">1. 实人认证</view>
        <view class="actions">
          <button class="btn btn--primary" :loading="startingRealName" @click="onStartRealName">发起实名认证</button>
          <button class="btn btn--ghost" :loading="syncing" @click="onSyncRealName">查询结果</button>
        </view>
        <view class="tip">发起后在新窗口完成工行实人认证；完成后回到本页点「查询结果」。</view>
      </view>

      <!-- 2. 收方入驻 -->
      <view class="card">
        <view class="card__title">2. 收方入驻（绑定本人银行卡）</view>
        <view class="field">
          <text class="field__label">证件签发日期</text>
          <input v-model="onboarding.idSignDate" class="input" placeholder="yyyy-MM-dd" />
        </view>
        <view class="field">
          <text class="field__label">证件截止日期</text>
          <input v-model="onboarding.idValidityPeriod" class="input" placeholder="永久有效填 9999-12-30" />
        </view>
        <view class="field">
          <text class="field__label">开户银行</text>
          <input v-model="onboarding.bankName" class="input" placeholder="银行卡识别结果" />
        </view>
        <view class="field">
          <text class="field__label">开户支行</text>
          <input v-model="onboarding.bankBranch" class="input" placeholder="银行卡识别结果" />
        </view>
        <view class="actions">
          <button class="btn btn--primary" :loading="submittingOnboarding" @click="onSubmitOnboarding">发起收方入驻</button>
          <button class="btn btn--ghost" :loading="syncing" @click="onSyncOnboarding">查询结果</button>
        </view>

        <view v-if="overview.onboardingState && !overview.invoiceEligible" class="fallback">
          <view class="fallback__title">入驻未通过？留下联系方式等待联系</view>
          <view class="row">
            <input v-model="lead.mobile" class="input" placeholder="手机号" />
            <input v-model="lead.remark" class="input" placeholder="备注" />
          </view>
          <button class="btn btn--ghost" :loading="leavingContact" @click="onLeaveContact">提交</button>
        </view>
      </view>

      <!-- 3. 框架收购协议 -->
      <view class="card">
        <view class="card__title">3. 框架收购协议</view>
        <view v-if="overview.frameworkAgreement" class="agreement">
          已签：{{ overview.frameworkAgreement.agreementNo }} · {{ overview.frameworkAgreement.productName }}
        </view>
        <view class="field">
          <text class="field__label">货物名称</text>
          <input v-model="agreement.productName" class="input" placeholder="如 废钢" />
        </view>
        <view class="field">
          <text class="field__label">数量</text>
          <input v-model="agreement.quantity" class="input" placeholder="如 5 吨" />
        </view>
        <view class="field">
          <text class="field__label">规格</text>
          <input v-model="agreement.specification" class="input" placeholder="如 重型废钢" />
        </view>
        <view class="field">
          <text class="field__label">回收期次</text>
          <input v-model="agreement.recyclePeriod" class="input" placeholder="如 2026 年 9 月第 1 期" />
        </view>
        <view class="field">
          <text class="field__label">结算方式</text>
          <input v-model="agreement.settlementMethod" class="input" placeholder="如 银行转账，过磅后 3 日内结清" />
        </view>
        <button class="btn btn--primary" :loading="savingAgreement" @click="onSaveAgreement">
          {{ overview.frameworkAgreement ? '重签协议' : '签署协议' }}
        </button>
      </view>

      <!-- 4. 首次授权 -->
      <view class="card">
        <view class="card__title">4. 首次授权</view>
        <view class="switch-row">
          <text>授权反向开票</text>
          <switch :checked="authorization.reverseInvoiceAuthorized" @change="onReverseChange" />
        </view>
        <view class="switch-row">
          <text>授权代办税费</text>
          <switch :checked="authorization.taxAgencyAuthorized" @change="onTaxChange" />
        </view>
        <button class="btn btn--primary" :loading="savingAuthorization" @click="onAuthorize">保存授权</button>
        <view v-if="overview.authorization" class="tip">已于 {{ formatTime(overview.authorization.authorizedAt) }} 授权</view>
      </view>

      <!-- 5. 交给出售者自助办理 -->
      <view class="card">
        <view class="card__title">交给出售者自助办理（可选）</view>
        <view class="tip">出售者也可在自己手机上完成实名 / 绑卡：生成链接发给本人，用微信打开即可。</view>
        <button class="btn btn--ghost" :loading="creatingLink" @click="onCreateSelfServiceLink">生成自助链接</button>
        <view v-if="selfService" class="link-box">
          <view class="link-box__url">{{ selfService.link || selfService.token }}</view>
          <button class="link" @click="copyLink">复制</button>
        </view>
      </view>

      <button class="btn btn--ghost" @click="backToSeller">换一位出售者</button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  createPayee,
  findReturningCustomer,
  getPayee,
  PayeeVO
} from '@/api/payee'
import {
  authorizeSeller,
  getOnboarding,
  leaveContactFallback,
  saveAgreement,
  SellerOnboardingVO,
  startRealName,
  submitOnboarding,
  syncOnboarding,
  syncRealName
} from '@/api/onboarding'
import { openIcbcFormHtml } from '@/utils/icbcForm'
import { createPublicToken } from '@/api/publicToken'
import { SELLER_APP_URL } from '@/config/env'

defineOptions({ name: 'FieldPayee' })

const payeeId = ref<number | undefined>(undefined)
const overview = ref<SellerOnboardingVO>({})
const looking = ref(false)
const lookedUp = ref(false)
const foundSeller = ref<PayeeVO | null>(null)
const creating = ref(false)
const startingRealName = ref(false)
const submittingOnboarding = ref(false)
const syncing = ref(false)
const leavingContact = ref(false)
const savingAgreement = ref(false)
const savingAuthorization = ref(false)
const creatingLink = ref(false)
const selfService = ref<{ link: string; token: string } | null>(null)

const lookup = reactive({ idCardNo: '', mobile: '' })
const newSeller = reactive({ name: '', idCardNo: '', mobile: '', bankCardNo: '', address: '' })
const onboarding = reactive({ idSignDate: '', idValidityPeriod: '', bankName: '', bankBranch: '' })
const lead = reactive({ mobile: '', remark: '' })
const agreement = reactive({
  productName: '',
  quantity: '',
  specification: '',
  recyclePeriod: '',
  settlementMethod: ''
})
const authorization = reactive({ reverseInvoiceAuthorized: false, taxAgencyAuthorized: false })

async function onLookup() {
  if (!lookup.idCardNo && !lookup.mobile) {
    uni.showToast({ title: '请填身份证号或手机号', icon: 'none' })
    return
  }
  looking.value = true
  try {
    foundSeller.value = await findReturningCustomer({
      idCardNo: lookup.idCardNo || undefined,
      mobile: lookup.mobile || undefined
    })
    lookedUp.value = true
  } catch (e) {
    showError(e)
  } finally {
    looking.value = false
  }
}

async function onCreateSeller() {
  if (!newSeller.name || !newSeller.idCardNo || !newSeller.mobile) {
    uni.showModal({ title: '还差一点', content: '姓名、身份证号与手机号必填', showCancel: false })
    return
  }
  creating.value = true
  try {
    const id = await createPayee({ ...newSeller, businessType: 'RECYCLE' })
    await openOnboarding(id)
  } catch (e) {
    showError(e)
  } finally {
    creating.value = false
  }
}

async function openOnboarding(id: number) {
  payeeId.value = id
  try {
    const payee = await getPayee(id)
    onboarding.bankName = payee.bankName || ''
    onboarding.bankBranch = payee.bankBranch || ''
    lead.mobile = payee.mobile || ''
    await refreshOverview()
  } catch (e) {
    showError(e)
  }
}

async function refreshOverview() {
  if (!payeeId.value) return
  overview.value = await getOnboarding(payeeId.value)
}

async function onStartRealName() {
  if (!payeeId.value) return
  startingRealName.value = true
  try {
    const step = await startRealName(payeeId.value)
    openIcbcFormHtml(step.formHtml || '', '实人认证')
    await refreshOverview()
  } catch (e) {
    showError(e)
  } finally {
    startingRealName.value = false
  }
}

async function onSyncRealName() {
  if (!payeeId.value) return
  syncing.value = true
  try {
    overview.value = await syncRealName(payeeId.value)
    uni.showToast({ title: '已刷新', icon: 'none' })
  } catch (e) {
    showError(e)
  } finally {
    syncing.value = false
  }
}

async function onSubmitOnboarding() {
  if (!payeeId.value) return
  submittingOnboarding.value = true
  try {
    const step = await submitOnboarding({
      payeeId: payeeId.value,
      idSignDate: onboarding.idSignDate || undefined,
      idValidityPeriod: onboarding.idValidityPeriod || undefined,
      bankName: onboarding.bankName || undefined,
      bankBranch: onboarding.bankBranch || undefined,
      trxChannel: '03'
    })
    openIcbcFormHtml(step.formHtml || '', '收方入驻')
    await refreshOverview()
  } catch (e) {
    showError(e)
  } finally {
    submittingOnboarding.value = false
  }
}

async function onSyncOnboarding() {
  if (!payeeId.value) return
  syncing.value = true
  try {
    overview.value = await syncOnboarding(payeeId.value)
    if (!overview.value.invoiceEligible && overview.value.nextStep) {
      uni.showModal({ title: '还差一步', content: overview.value.nextStep, showCancel: false })
    }
  } catch (e) {
    showError(e)
  } finally {
    syncing.value = false
  }
}

async function onLeaveContact() {
  if (!payeeId.value) return
  if (!lead.mobile) {
    uni.showToast({ title: '请填写联系方式', icon: 'none' })
    return
  }
  leavingContact.value = true
  try {
    await leaveContactFallback({ payeeId: payeeId.value, mobile: lead.mobile, remark: lead.remark })
    uni.showToast({ title: '已留下联系方式', icon: 'success' })
  } catch (e) {
    showError(e)
  } finally {
    leavingContact.value = false
  }
}

async function onSaveAgreement() {
  if (!payeeId.value) return
  if (!agreement.productName || !agreement.quantity || !agreement.specification
    || !agreement.recyclePeriod || !agreement.settlementMethod) {
    uni.showModal({ title: '还差一点', content: '协议的名称、数量、规格、回收期次、结算方式都要填', showCancel: false })
    return
  }
  savingAgreement.value = true
  try {
    await saveAgreement({ ...agreement, payeeId: payeeId.value, signMethod: 'PAPER' })
    uni.showToast({ title: '协议已保存', icon: 'success' })
    await refreshOverview()
  } catch (e) {
    showError(e)
  } finally {
    savingAgreement.value = false
  }
}

async function onAuthorize() {
  if (!payeeId.value) return
  savingAuthorization.value = true
  try {
    await authorizeSeller({
      payeeId: payeeId.value,
      reverseInvoiceAuthorized: authorization.reverseInvoiceAuthorized,
      taxAgencyAuthorized: authorization.taxAgencyAuthorized,
      channel: 'ONSITE'
    })
    uni.showToast({ title: '授权已留痕', icon: 'success' })
    await refreshOverview()
  } catch (e) {
    showError(e)
  } finally {
    savingAuthorization.value = false
  }
}

function onReverseChange(event: any) {
  authorization.reverseInvoiceAuthorized = event.detail.value
}
function onTaxChange(event: any) {
  authorization.taxAgencyAuthorized = event.detail.value
}

function backToSeller() {
  payeeId.value = undefined
  overview.value = {}
  foundSeller.value = null
  lookedUp.value = false
  selfService.value = null
  lookup.idCardNo = ''
  lookup.mobile = ''
}

async function onCreateSelfServiceLink() {
  if (!payeeId.value) return
  creatingLink.value = true
  try {
    const resp = await createPublicToken({ purpose: 'ONBOARDING', payeeId: payeeId.value })
    const token = resp.token || ''
    const link = SELLER_APP_URL
      ? `${SELLER_APP_URL.replace(/\/$/, '')}/#/?token=${encodeURIComponent(token)}&purpose=ONBOARDING`
      : ''
    selfService.value = { link, token }
    if (link) {
      copyLink()
    }
  } catch (e) {
    showError(e)
  } finally {
    creatingLink.value = false
  }
}

function copyLink() {
  const text = selfService.value?.link || selfService.value?.token || ''
  if (!text) return
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '已复制', icon: 'none' })
  })
}

function formatTime(ts?: number) {
  return ts ? new Date(ts).toLocaleString() : ''
}

function showError(e: unknown) {
  uni.showModal({ title: '操作失败', content: (e as Error).message || '请重试', showCancel: false })
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

.agreement {
  margin-bottom: 16rpx;
  padding: 16rpx 20rpx;
  background-color: #e8f7ee;
  color: #1a7f43;
  border-radius: 12rpx;
}

.actions {
  display: flex;
  gap: 16rpx;
}

.fallback {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #eef0f3;

  &__title {
    margin-bottom: 16rpx;
    color: #b26a00;
  }
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
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
