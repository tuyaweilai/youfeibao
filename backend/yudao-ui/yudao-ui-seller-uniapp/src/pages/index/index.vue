<template>
  <view v-if="webViewUrl" class="webview">
    <web-view :src="webViewUrl" />
  </view>

  <view v-else class="page">
    <view v-if="!token" class="empty card">
      <view class="empty__title">入口无效</view>
      <view class="empty__desc">请用收购确认书上的链接或二维码打开；令牌一次性有效，过期请让回收企业重新签发。</view>
    </view>

    <template v-else>
      <view class="tabs">
        <view
          v-for="tab in visibleTabs"
          :key="tab.key"
          class="tabs__item"
          :class="{ 'tabs__item--active': activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </view>
      </view>

      <!-- 待办提醒（短信 / 收货员转达的一次性链接打开先看这里，确认时再验手机号） -->
      <view v-if="activeTab === 'notice'" class="card">
        <view v-if="notice" class="quota">
          <view class="quota__name">待办提醒</view>
          <view class="quota__message">{{ notice.message }}</view>
          <view v-for="item in notice.items" :key="`${item.type}-${item.settlementId || item.partnerOrderId}`" class="notice-item">
            <view class="notice-item__top">
              <text class="notice-item__type">{{ item.typeName }}</text>
              <text class="notice-item__status">{{ item.statusName }}</text>
            </view>
            <view class="notice-item__title">{{ item.title }}</view>
            <view v-if="item.amount !== undefined && item.amount !== null" class="notice-item__meta">
              金额 {{ item.amount }} 元（本平台累计）
            </view>
            <view v-if="item.deadlineTime" class="notice-item__meta">
              截止 {{ item.deadlineTime.replace('T', ' ').slice(0, 16) }}
            </view>
            <view v-if="item.nextStep" class="notice-item__next">下一步：{{ item.nextStep }}</view>
          </view>
          <view v-if="!notice.items?.length" class="muted">暂时没有需要你处理的事。</view>
          <button class="btn btn--primary" @click="goVerify">验证手机号后处理</button>
          <view class="scope-note">{{ notice.scopeNote }}</view>
        </view>
        <view v-else-if="loading" class="loading">加载中…</view>
        <view v-else class="error">{{ error }}</view>
      </view>

      <!-- 实名与收方入驻 -->
      <view v-if="activeTab === 'onboarding'" class="card">
        <view v-if="onboarding" class="quota">
          <view class="quota__name">实名与收方入驻</view>
          <view class="quota__message">{{ onboarding.message }}</view>
          <!-- 从工行实名结果页跳回来的落点（#82）：不用再点「查询结果」，打开就是最新状态 -->
          <view v-if="faceReturn === 'face-success'" class="face-return face-return--ok">
            已从工行返回，实名结果已刷新。
          </view>
          <view v-else-if="faceReturn === 'face-fail' || realNameFailed" class="face-return face-return--fail">
            <view class="face-return__title">实名未通过</view>
            <view v-if="onboarding.realNameMsg" class="face-return__reason">{{ onboarding.realNameMsg }}</view>
            <view class="face-return__desc">可以重新发起一次实名；也可以先留个联系方式，让企业联系你。</view>
          </view>
          <view class="kv"><text class="kv__k">实名认证</text><text>{{ onboarding.realNameStatusName || '未认证' }}</text></view>
          <view class="kv"><text class="kv__k">收方入驻</text><text>{{ onboarding.onboardingStateName || '未开始' }}</text></view>
          <view v-if="onboarding.bankCardChangeStatusName" class="kv">
            <text class="kv__k">收款账户变更</text><text>{{ onboarding.bankCardChangeStatusName }}</text>
          </view>
          <view v-if="onboarding.nextStep" class="quota__exempt quota__exempt--warn">下一步：{{ onboarding.nextStep }}</view>
          <!-- 人脸只在微信环境能唤起：不在微信里就不要给一个点了没反应的按钮（#88） -->
          <view v-if="notInWechat && onboarding.step !== 'DONE'" class="wechat-guide">
            <view class="wechat-guide__title">请用微信打开才能做人脸</view>
            <view class="wechat-guide__desc">
              工行实人认证只在微信里能唤起。把本页链接发到微信里打开（或在微信里扫现场的二维码），再完成人脸。
            </view>
            <button class="btn btn--ghost" @click="copyCurrentUrl">复制本页链接</button>
          </view>
          <button v-else-if="onboarding.step !== 'DONE'" class="btn btn--primary" @click="openOnboardingForm">
            {{ realNameFailed ? '重新发起实名认证' : '去工行页面完成实名' }}
          </button>
          <button class="btn btn--ghost" :loading="loading" @click="loadOnboarding">我已完成，刷新</button>
        </view>
        <view v-else-if="loading" class="loading">加载中…</view>
        <view v-else class="error">{{ error }}</view>
      </view>

      <!-- 额度 -->
      <view v-if="activeTab === 'quota'" class="card">
        <view v-if="quota" class="quota">
          <view class="quota__name">{{ quota.name }} <text class="quota__id">{{ quota.idCardMasked }}</text></view>
          <view class="quota__remaining">剩余额度 {{ quota.remainingAmount }} 元</view>
          <view class="quota__message">{{ quota.message }}</view>
          <view class="kv"><text class="kv__k">滚动窗口上限</text><text>{{ quota.capAmount }} 元</text></view>
          <view class="kv"><text class="kv__k">已开票</text><text>{{ quota.issuedAmount }} 元</text></view>
          <view class="kv"><text class="kv__k">在途</text><text>{{ quota.pendingAmount }} 元</text></view>
          <view class="kv"><text class="kv__k">红冲</text><text>{{ quota.redOffsetAmount }} 元</text></view>
          <view class="kv"><text class="kv__k">已用额度</text><text>{{ quota.usedAmount }} 元</text></view>
          <view class="kv"><text class="kv__k">按 1% 计税部分</text><text>{{ quota.amountAtOnePercent }} 元</text></view>
          <view class="kv"><text class="kv__k">按 3% 计税部分</text><text>{{ quota.amountAtThreePercent }} 元</text></view>
          <view class="kv"><text class="kv__k">本月净销售额</text><text>{{ quota.currentMonthAmount }} 元</text></view>
          <view class="quota__exempt" :class="{ 'quota__exempt--warn': quota.currentMonthOverExempt }">
            {{ quota.currentMonthOverExempt
              ? `本月已超 ${quota.monthlyExemptAmount} 元免征线，回收企业须按时代办申报缴款`
              : `本月未超 ${quota.monthlyExemptAmount} 元免征线` }}
          </view>
        </view>
        <view v-else-if="loading" class="loading">加载中…</view>
        <view v-else class="error">{{ error }}</view>
      </view>

      <!-- 发票下载 -->
      <view v-if="activeTab === 'invoice'" class="card">
        <view class="card__title">我的发票</view>
        <view class="tip">发票为 PDF，可直接下载或打印；无需注册、无需关注公众号。</view>
        <button class="btn btn--primary" :loading="downloading" @click="onDownloadInvoice">下载 / 保存发票</button>
        <view v-if="error" class="error">{{ error }}</view>
      </view>

      <!-- 汇算清缴 -->
      <view v-if="activeTab === 'settlement'" class="card">
        <view v-if="settlement" class="quota">
          <view class="quota__name">{{ settlement.sellerName }} <text class="quota__id">{{ settlement.idCardMasked }}</text></view>
          <view class="quota__remaining">{{ settlement.taxYear }} 年度对账单</view>
          <view class="quota__message">{{ settlement.message }}</view>
          <view class="kv"><text class="kv__k">开票张数</text><text>{{ settlement.invoiceCount }}</text></view>
          <view class="kv"><text class="kv__k">开票金额</text><text>{{ settlement.invoicedAmount }} 元</text></view>
          <view class="kv"><text class="kv__k">已预缴税费</text><text>{{ settlement.paidTaxAmount }} 元</text></view>
          <view class="kv"><text class="kv__k">已预缴个税</text><text>{{ settlement.iitAmount }} 元</text></view>
          <view class="kv"><text class="kv__k">截止日</text><text>{{ settlement.deadline }}（剩 {{ settlement.daysLeft }} 天）</text></view>
          <view v-if="settlement.months?.length" class="months">
            <view v-for="month in settlement.months" :key="month.month" class="months__row">
              <text>{{ month.month }}</text>
              <text>{{ month.invoicedAmount }} 元 / 税 {{ month.paidTaxAmount }} 元</text>
            </view>
          </view>
        </view>
        <view v-else-if="loading" class="loading">加载中…</view>
        <view v-else class="error">{{ error }}</view>
      </view>

      <!-- 留联系方式 -->
      <view v-if="activeTab === 'contact'" class="card">
        <view class="card__title">留个联系方式</view>
        <view class="tip">实名或收方入驻没通过时，留下联系方式，平台会尽快联系你。</view>
        <view class="field">
          <text class="field__label">姓名</text>
          <input v-model="contact.name" class="input" placeholder="姓名" />
        </view>
        <view class="field">
          <text class="field__label">手机号</text>
          <input v-model="contact.mobile" class="input" placeholder="手机号" />
        </view>
        <view class="field">
          <text class="field__label">备注</text>
          <input v-model="contact.remark" class="input" placeholder="失败原因 / 备注（选填）" />
        </view>
        <button class="btn btn--primary" :loading="submitting" @click="onSubmitContact">提交</button>
        <view v-if="error" class="error">{{ error }}</view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { queryQuota, queryNotice, querySettlement, submitContactLead, syncOnboarding, onboardingFormUrl, QuotaVO, SettlementVO, OnboardingStatusVO, PublicNoticeVO } from '@/api/public'
import { resolveEntryParams, resolveFaceReturn, resolveStationCode, setPurpose, setToken } from '@/utils/token'
import { getSubject, getToken } from '@/utils/auth'
import { setTenantId } from '@/config/env'
import { downloadInvoicePdf } from '@/utils/download'

defineOptions({ name: 'SellerIndex' })

const PURPOSE_SECTION: Record<string, string> = {
  QUOTA_QUERY: 'quota',
  INVOICE_DOWNLOAD: 'invoice',
  SETTLEMENT_STATEMENT: 'settlement',
  CONTACT_LEAD: 'contact',
  ONBOARDING: 'onboarding',
  SELLER_NOTICE: 'notice'
}

const ALL_TABS = [
  { key: 'notice', label: '待办提醒' },
  { key: 'onboarding', label: '实名与入驻' },
  { key: 'quota', label: '我的额度' },
  { key: 'invoice', label: '我的发票' },
  { key: 'settlement', label: '汇算清缴' },
  { key: 'contact', label: '留联系方式' }
]

const token = ref('')
const purpose = ref('')
const activeTab = ref('quota')
const loading = ref(false)
const downloading = ref(false)
const submitting = ref(false)
const error = ref('')
const quota = ref<QuotaVO | null>(null)
const notice = ref<PublicNoticeVO | null>(null)
const settlement = ref<SettlementVO | null>(null)
const onboarding = ref<OnboardingStatusVO | null>(null)
const webViewUrl = ref('')
const notInWechat = ref(false)
const faceReturn = ref('')
const contact = reactive({ name: '', mobile: '', remark: '' })

// 令牌按用途签发，只放行对应功能；没带用途时给出全部入口
const visibleTabs = computed(() => {
  const section = PURPOSE_SECTION[purpose.value]
  return section ? ALL_TABS.filter((tab) => tab.key === section) : ALL_TABS
})

/** 实名未通过（PayeeRealNameStatusEnum.FAILED = 3）：落点页要说清原因并给重试入口 */
const realNameFailed = computed(() => onboarding.value?.realNameStatus === 3)

onLoad(() => {
  // 非微信环境（H5）：实名页在这里唤不起来，别让本人点了没反应（#88）
  // #ifdef H5
  notInWechat.value = !/MicroMessenger/i.test(navigator.userAgent)
  // #endif
  // 场站二维码：码内不带任何令牌，只编码场站码；先看公开信息再登录
  const station = resolveStationCode()
  if (station) {
    uni.redirectTo({ url: `/pages/station/index?station=${encodeURIComponent(station)}` })
    return
  }
  const params = resolveEntryParams()
  token.value = params.token
  purpose.value = params.purpose
  faceReturn.value = resolveFaceReturn()
  if (params.token) {
    setToken(params.token)
    setPurpose(params.purpose)
    activeTab.value = PURPOSE_SECTION[params.purpose] || 'quota'
    loadActive()
    return
  }
  // 没有一次性令牌：这是自然人端正式入口，按登录态路由
  if (getToken() && getSubject()) {
    uni.redirectTo({ url: '/pages/home/index' })
  } else {
    uni.redirectTo({ url: '/pages/login/index' })
  }
})

function switchTab(key: string) {
  activeTab.value = key
  error.value = ''
  loadActive()
}

async function loadActive() {
  if (!token.value) return
  error.value = ''
  if (activeTab.value === 'onboarding') {
    await loadOnboarding()
  } else if (activeTab.value === 'quota') {
    await loadQuota()
  } else if (activeTab.value === 'settlement') {
    await loadSettlement()
  } else if (activeTab.value === 'notice') {
    await loadNotice()
  }
}

async function loadNotice() {
  loading.value = true
  try {
    notice.value = await queryNotice(token.value)
    // 租户从令牌解析结果拿：登录页用它定位回收企业
    if (notice.value.tenantId) {
      setTenantId(notice.value.tenantId)
    }
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

/** 看通知不需要注册；要确认 / 处理时再用手机号验证（登录即注册） */
function goVerify() {
  uni.redirectTo({ url: '/pages/login/index' })
}

async function loadOnboarding() {
  loading.value = true
  try {
    onboarding.value = await syncOnboarding(token.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

/** 复制当前页链接：本人要在微信里重新打开同一条带令牌的链接 */
function copyCurrentUrl() {
  let url = ''
  // #ifdef H5
  url = window.location.href
  // #endif
  if (!url) {
    return
  }
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '已复制，请到微信里打开', icon: 'none' })
  })
}

function trxChannel(): string {
  // #ifdef MP-WEIXIN
  return '05'
  // #endif
  // #ifndef MP-WEIXIN
  return '03'
  // #endif
}

function openOnboardingForm() {
  const url = onboardingFormUrl(token.value, trxChannel())
  // #ifdef H5
  window.open(url, '_blank')
  // #endif
  // #ifndef H5
  webViewUrl.value = url
  // #endif
}

async function loadQuota() {
  loading.value = true
  try {
    quota.value = await queryQuota(token.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function loadSettlement() {
  loading.value = true
  try {
    settlement.value = await querySettlement(token.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function onDownloadInvoice() {
  downloading.value = true
  error.value = ''
  try {
    await downloadInvoicePdf(token.value, '我的发票.pdf')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    downloading.value = false
  }
}

async function onSubmitContact() {
  if (!contact.name || !contact.mobile) {
    error.value = '请填写姓名与手机号'
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await submitContactLead({ token: token.value, ...contact })
    uni.showToast({ title: '已提交，等待联系', icon: 'success' })
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    submitting.value = false
  }
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
    margin-bottom: 20rpx;
    font-size: 32rpx;
    font-weight: 600;
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

.tabs {
  display: flex;
  margin-bottom: 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;

  &__item {
    flex: 1;
    padding: 24rpx 0;
    text-align: center;
    color: $seller-text-secondary;

    &--active {
      color: $seller-primary;
      font-weight: 600;
      border-bottom: 4rpx solid $seller-primary;
    }
  }
}

.quota {
  &__name {
    font-size: 34rpx;
    font-weight: 700;
  }

  &__id {
    margin-left: 12rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
    font-weight: 400;
  }

  &__remaining {
    margin-top: 12rpx;
    font-size: 40rpx;
    font-weight: 700;
    color: $seller-primary;
  }

  &__message {
    margin: 16rpx 0 8rpx;
    color: $seller-text-secondary;
    line-height: 1.6;
  }

  &__exempt {
    margin-top: 16rpx;
    padding: 16rpx 20rpx;
    background-color: #e8f7ee;
    color: #1a7f43;
    border-radius: 12rpx;
    line-height: 1.6;

    &--warn {
      background-color: #fff7e6;
      color: #b26a00;
    }
  }
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
  gap: 24rpx;

  &__k {
    color: $seller-text-secondary;
  }
}

.months {
  margin-top: 20rpx;
  border-top: 1rpx solid #eef0f3;
  padding-top: 16rpx;

  &__row {
    display: flex;
    justify-content: space-between;
    padding: 8rpx 0;
    font-size: 26rpx;
    color: $seller-text-secondary;
  }
}

.field {
  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
  }
}

.input {
  height: 80rpx;
  padding: 0 20rpx;
  margin-bottom: 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
}

.btn {
  width: 100%;
  margin-top: 8rpx;
  color: #ffffff;
  background-color: $seller-primary;
}

.btn--ghost {
  color: $seller-primary;
  background-color: #ffffff;
  border: 1rpx solid $seller-primary;
}

.webview {
  width: 100%;
  height: 100vh;
}

.tip {
  margin-bottom: 20rpx;
  color: $seller-text-secondary;
  line-height: 1.6;
}

.loading,
.error {
  padding: 20rpx 0;
  color: $seller-text-secondary;
  text-align: center;
}

.error {
  color: #cf1322;
}

.muted {
  padding: 20rpx 0;
  color: $seller-text-secondary;
}

.notice-item {
  padding: 20rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__top {
    display: flex;
    justify-content: space-between;
  }

  &__type {
    font-weight: 600;
  }

  &__status {
    color: $seller-text-secondary;
  }

  &__title {
    margin-top: 8rpx;
  }

  &__meta {
    margin-top: 6rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
  }

  &__next {
    margin-top: 8rpx;
    color: #b26a00;
    line-height: 1.6;
  }
}

.scope-note {
  margin-top: 16rpx;
  color: $seller-text-secondary;
  font-size: 24rpx;
  line-height: 1.6;
}

.wechat-guide {
  margin-top: 16rpx;
  padding: 20rpx 24rpx;
  background-color: #fff7e6;
  border-radius: 12rpx;

  &__title {
    font-weight: 600;
    color: #b26a00;
  }

  &__desc {
    margin-top: 8rpx;
    color: #b26a00;
    font-size: 26rpx;
    line-height: 1.6;
  }
}

.face-return {
  margin-top: 16rpx;
  padding: 20rpx 24rpx;
  border-radius: 12rpx;
  line-height: 1.6;

  &--ok {
    background-color: #e8f7ee;
    color: #1a7f43;
  }

  &--fail {
    background-color: #fdecec;
    color: #cf1322;
  }

  &__title {
    font-weight: 600;
  }

  &__reason {
    margin-top: 8rpx;
  }

  &__desc {
    margin-top: 8rpx;
    font-size: 26rpx;
  }
}
</style>
