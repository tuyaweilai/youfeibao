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

      <!-- 实名与收方入驻 -->
      <view v-if="activeTab === 'onboarding'" class="card">
        <view v-if="onboarding" class="quota">
          <view class="quota__name">实名与收方入驻</view>
          <view class="quota__message">{{ onboarding.message }}</view>
          <view class="kv"><text class="kv__k">实名认证</text><text>{{ onboarding.realNameStatusName || '未认证' }}</text></view>
          <view class="kv"><text class="kv__k">收方入驻</text><text>{{ onboarding.onboardingStateName || '未开始' }}</text></view>
          <view v-if="onboarding.nextStep" class="quota__exempt quota__exempt--warn">下一步：{{ onboarding.nextStep }}</view>
          <button v-if="onboarding.step !== 'DONE'" class="btn btn--primary" @click="openOnboardingForm">
            去工行页面（实名 / 绑卡）
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
import { queryQuota, querySettlement, submitContactLead, syncOnboarding, onboardingFormUrl, QuotaVO, SettlementVO, OnboardingStatusVO } from '@/api/public'
import { resolveEntryParams, setPurpose, setToken } from '@/utils/token'
import { downloadInvoicePdf } from '@/utils/download'

defineOptions({ name: 'SellerIndex' })

const PURPOSE_SECTION: Record<string, string> = {
  QUOTA_QUERY: 'quota',
  INVOICE_DOWNLOAD: 'invoice',
  SETTLEMENT_STATEMENT: 'settlement',
  CONTACT_LEAD: 'contact',
  ONBOARDING: 'onboarding'
}

const ALL_TABS = [
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
const settlement = ref<SettlementVO | null>(null)
const onboarding = ref<OnboardingStatusVO | null>(null)
const webViewUrl = ref('')
const contact = reactive({ name: '', mobile: '', remark: '' })

// 令牌按用途签发，只放行对应功能；没带用途时给出全部入口
const visibleTabs = computed(() => {
  const section = PURPOSE_SECTION[purpose.value]
  return section ? ALL_TABS.filter((tab) => tab.key === section) : ALL_TABS
})

onLoad(() => {
  const params = resolveEntryParams()
  token.value = params.token
  purpose.value = params.purpose
  if (params.token) {
    setToken(params.token)
    setPurpose(params.purpose)
  }
  activeTab.value = PURPOSE_SECTION[params.purpose] || 'quota'
  loadActive()
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
  }
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
</style>
