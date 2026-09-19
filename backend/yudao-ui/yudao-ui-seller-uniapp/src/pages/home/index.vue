<template>
  <view class="page">
    <!-- 身份未匹配：明确空态，不给假列表 -->
    <view v-if="!naturalPersonId" class="card empty">
      <view class="empty__title">你在这家场站没有待确认的货</view>
      <view class="empty__desc">
        可能是收货员还没建单，或你用的手机号与建档手机号不一致。
      </view>
      <button class="btn btn--ghost" @click="contactService">联系客服</button>
      <button class="btn btn--ghost" @click="signOut">换手机号登录</button>
    </view>

    <template v-else>
      <view class="tabs">
        <view
          v-for="tab in tabs"
          :key="tab.key"
          class="tabs__item"
          :class="{ 'tabs__item--active': activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </view>
      </view>

      <!-- 待我确认 -->
      <template v-if="activeTab === 'pending'">
        <view class="card">
          <view class="card__title">待我确认</view>
          <view v-if="home.pendingCount" class="pending-count">
            共 {{ home.pendingCount }} 项需要你处理
          </view>
          <view v-if="!home.pendingItems?.length" class="muted">暂时没有需要你确认的事。</view>
          <view
            v-for="item in home.pendingItems"
            :key="`${item.type}-${item.settlementId || item.title}`"
            class="pending"
            :class="{ 'pending--urgent': item.urgent }"
            @click="openPending(item)"
          >
            <view class="pending__top">
              <text class="pending__type">{{ item.typeName }}</text>
              <text class="pending__status">{{ item.statusName }}</text>
            </view>
            <view class="pending__title">{{ item.title }}</view>
            <view class="pending__ent">{{ item.enterpriseName }}</view>
            <view v-if="item.deadlineTime" class="pending__deadline">
              截止 {{ formatTime(item.deadlineTime) }}
            </view>
          </view>
        </view>
      </template>

      <!-- 我的记录 -->
      <template v-if="activeTab === 'records'">
        <view v-if="!recordGroups.length" class="card muted">还没有卖货记录。</view>
        <view v-for="group in recordGroups" :key="group.tenantId" class="card">
          <view class="card__title">{{ group.enterpriseName }}</view>
          <view class="group-summary">
            {{ group.count }} 笔 · 本平台累计 {{ group.totalAmount ?? 0 }} 元 · 结算重量
            {{ group.totalSettlementWeight ?? 0 }}
          </view>
          <view v-for="record in group.records" :key="record.acquisitionId" class="record">
            <view class="record__row">
              <text class="record__cat">{{ record.categoryName }}</text>
              <text class="record__amount">{{ record.amount ?? 0 }} 元</text>
            </view>
            <view class="record__meta">
              结算重量 {{ record.settlementWeight ?? 0 }} {{ record.unit || '' }} · 单价
              {{ record.unitPrice ?? 0 }} · {{ record.statusName }}
            </view>
            <view v-if="record.cancelReason" class="record__cancel">作废原因：{{ record.cancelReason }}</view>
            <view class="record__actions">
              <text class="link" @click="printAcquisition(record.acquisitionId)">打印确认书</text>
            </view>
          </view>
        </view>
        <view class="scope-note">金额为本平台累计，不含你在其他渠道的交易。</view>
      </template>

      <!-- 收款记录 -->
      <template v-if="activeTab === 'payments'">
        <view v-if="!payments.length" class="card muted">还没有收款记录。</view>
        <view v-for="payment in payments" :key="payment.paymentOrderId" class="card">
          <view class="record__row">
            <text class="record__cat">{{ payment.categoryName || payment.acquisitionNo }}</text>
            <text class="record__amount">{{ payment.paymentAmount ?? 0 }} 元</text>
          </view>
          <view class="record__meta">{{ payment.acquirerName }} · {{ payment.orderNo }}</view>
          <view class="status-line">
            <text class="status-line__label">付款</text>
            <text :class="statusClass(payment.status)">{{ payment.statusName }}</text>
          </view>
          <view v-if="payment.receiptNo" class="record__meta">回单号：{{ payment.receiptNo }}</view>
          <view v-if="payment.actuallyReceivedAmount !== undefined && payment.actuallyReceivedAmount !== null
            && payment.actuallyReceivedAmount !== payment.paymentAmount" class="record__warn">
            银行回执金额 {{ payment.actuallyReceivedAmount }} 元，与付款金额不一致
          </view>
          <view v-if="payment.nextStep" class="record__warn">下一步：{{ payment.nextStep }}</view>
          <view class="record__actions">
            <text v-if="payment.sellerReceivedConfirmed" class="muted">
              你已确认收到（{{ formatTime(payment.sellerReceivedConfirmedAt) }}）
            </text>
            <button
              v-else-if="payment.canConfirmReceive"
              class="mini-btn"
              @click="onConfirmReceived(payment.paymentOrderId)"
            >
              我收到了
            </button>
          </view>
        </view>
        <view class="scope-note">
          「银行已受理」是我们能核验的银行状态；「我收到了」是你自己的确认，不改动银行状态。
          不使用「已到账」的说法。
        </view>
      </template>

      <!-- 发票与税费 -->
      <template v-if="activeTab === 'invoices'">
        <view class="card">
          <view class="card__title">发票与税费</view>
          <view class="year-row">
            <text class="link" @click="changeYear(-1)">上一年</text>
            <text class="year">{{ invoiceYear }} 年</text>
            <text class="link" @click="changeYear(1)">下一年</text>
          </view>
          <view class="group-summary">
            开票 {{ invoiceSummary.invoiceCount ?? 0 }} 张 · 金额 {{ invoiceSummary.totalInvoiceAmount ?? 0 }} 元
            · 税额 {{ invoiceSummary.totalTaxAmount ?? 0 }} 元
          </view>
          <view class="scope-note">{{ invoiceSummary.taxScopeNote }}</view>
        </view>
        <view v-if="!invoiceSummary.invoices?.length" class="card muted">这一年还没有开票记录。</view>
        <view v-for="invoice in invoiceSummary.invoices" :key="invoice.invoiceOrderId" class="card">
          <view class="record__row">
            <text class="record__cat">{{ invoice.acquirerName }}</text>
            <text class="record__amount">{{ invoice.invoiceAmount ?? 0 }} 元</text>
          </view>
          <view class="record__meta">发票号：{{ invoice.invoiceNo || '—' }}</view>
          <view class="three-lines">
            <view class="status-line">
              <text class="status-line__label">开票</text><text>{{ invoice.invoiceStatusName }}</text>
            </view>
            <view class="status-line">
              <text class="status-line__label">税费</text><text>{{ invoice.taxStatusName }}</text>
            </view>
            <view class="status-line">
              <text class="status-line__label">上传</text><text>{{ invoice.uploadStatusName }}</text>
            </view>
          </view>
          <view class="record__actions">
            <text class="link" @click="downloadInvoice(invoice.invoiceOrderId)">下载发票 PDF</text>
          </view>
        </view>
      </template>

      <!-- 我的资料 -->
      <template v-if="activeTab === 'profile'">
        <view class="card">
          <view class="card__title">我的资料</view>
          <view class="kv"><text class="kv__k">姓名</text><text>{{ profile.name || '—' }}</text></view>
          <view class="kv"><text class="kv__k">手机号</text><text>{{ profile.mobileMasked || '—' }}</text></view>
          <view class="kv"><text class="kv__k">身份证号</text><text>{{ profile.idCardMasked || '—' }}</text></view>
          <view class="kv"><text class="kv__k">实名认证</text><text>{{ profile.realNameStatusName || '—' }}</text></view>
          <view class="kv"><text class="kv__k">客服电话</text><text>{{ profile.serviceMobile }}</text></view>
          <view class="record__actions">
            <text class="link" @click="goAppointments">我的预约到站</text>
          </view>
        </view>

        <view class="card">
          <view class="card__title">收款账户</view>
          <view v-for="(card, i) in profile.bankCards" :key="i" class="kv">
            <text class="kv__k">{{ card.enterpriseName }}</text>
            <text>{{ card.bankName || '—' }} 尾号 {{ card.cardTail || '—' }}</text>
          </view>
          <view v-if="!profile.bankCards?.length" class="muted">还没有登记收款账户。</view>
          <view class="record__actions">
            <text class="link" @click="onChangeCard">变更银行卡</text>
          </view>
        </view>

        <view class="card">
          <view class="card__title">企业授权</view>
          <view class="scope-note">撤销只拦未来的开票与代办税费，已开出的票不追溯。</view>
          <view v-for="item in authorizations" :key="item.tenantId" class="auth">
            <view class="record__row">
              <text class="record__cat">{{ item.enterpriseName }}</text>
              <text :class="item.revoked ? 'closed' : 'open'">
                {{ item.revoked ? '已撤销' : (item.reverseInvoiceAuthorized && item.taxAgencyAuthorized ? '已授权' : '未授权') }}
              </text>
            </view>
            <view class="record__meta">反向开票 {{ item.reverseInvoiceAuthorized ? '已授权' : '未授权' }} · 代办税费
              {{ item.taxAgencyAuthorized ? '已授权' : '未授权' }}</view>
            <view class="record__actions">
              <text v-if="!item.revoked" class="link link--danger" @click="onRevoke(item)">撤销授权</text>
            </view>
          </view>
        </view>

        <view class="card">
          <view class="logout-note">{{ profile.logoutNote }}</view>
          <button class="btn btn--ghost" @click="signOut">退出登录</button>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  confirmReceived,
  getAuthorizations,
  getHome,
  getInvoices,
  getPayments,
  getProfile,
  getRecordGroups,
  revokeAuthorization,
  SellerAuthorization,
  SellerHome,
  SellerInvoiceSummary,
  SellerPayment,
  SellerProfile,
  SellerRecordGroup,
  PendingItem
} from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'
import { downloadWithAuth, openHtmlWithAuth } from '@/utils/download'

defineOptions({ name: 'SellerHome' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)

const tabs = [
  { key: 'pending', label: '待我确认' },
  { key: 'records', label: '我的记录' },
  { key: 'payments', label: '收款记录' },
  { key: 'invoices', label: '发票税费' },
  { key: 'profile', label: '我的资料' }
]
const activeTab = ref('pending')
const home = reactive<SellerHome>({})
const recordGroups = ref<SellerRecordGroup[]>([])
const payments = ref<SellerPayment[]>([])
const invoiceYear = ref(new Date().getFullYear())
const invoiceSummary = reactive<SellerInvoiceSummary>({})
const authorizations = ref<SellerAuthorization[]>([])
const profile = reactive<SellerProfile>({})

onLoad((query) => {
  if (!auth.token) {
    uni.redirectTo({ url: '/pages/login/index' })
    return
  }
  if (!auth.subject) {
    return
  }
  loadActive()
})

onShow(() => {
  if (auth.subject && activeTab.value) {
    loadActive()
  }
})

function switchTab(key: string) {
  activeTab.value = key
  loadActive()
}

async function loadActive() {
  if (!naturalPersonId.value) return
  try {
    if (activeTab.value === 'pending') {
      Object.assign(home, await getHome(naturalPersonId.value))
    } else if (activeTab.value === 'records') {
      recordGroups.value = await getRecordGroups(naturalPersonId.value)
    } else if (activeTab.value === 'payments') {
      payments.value = await getPayments(naturalPersonId.value)
    } else if (activeTab.value === 'invoices') {
      Object.assign(invoiceSummary, await getInvoices(naturalPersonId.value, invoiceYear.value))
    } else if (activeTab.value === 'profile') {
      Object.assign(profile, await getProfile(naturalPersonId.value))
      authorizations.value = await getAuthorizations(naturalPersonId.value)
    }
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  }
}

function openPending(item: PendingItem) {
  if (item.type === 'SETTLEMENT' && item.settlementId) {
    uni.navigateTo({ url: `/pages/settlement/detail?id=${item.settlementId}` })
    return
  }
  uni.showToast({ title: `${item.typeName}：请到现场与收货员办理`, icon: 'none' })
}

function changeYear(delta: number) {
  invoiceYear.value += delta
  loadActive()
}

function statusClass(status?: number) {
  if (status === 2) return 'open'
  if (status === 3 || status === 9 || status === 5 || status === 6) return 'closed'
  return ''
}

async function onConfirmReceived(paymentOrderId: number) {
  try {
    await confirmReceived(naturalPersonId.value, paymentOrderId)
    uni.showToast({ title: '已记录你的确认', icon: 'none' })
    loadActive()
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  }
}

function printAcquisition(acquisitionId: number) {
  openHtmlWithAuth(
    `/icbc/seller/portal/acquisition/confirmation?naturalPersonId=${naturalPersonId.value}&acquisitionId=${acquisitionId}`
  ).catch((e) => uni.showToast({ title: (e as Error).message, icon: 'none' }))
}

function downloadInvoice(invoiceOrderId: number) {
  downloadWithAuth(
    `/icbc/seller/portal/invoice/download?naturalPersonId=${naturalPersonId.value}&invoiceOrderId=${invoiceOrderId}`,
    '发票.pdf'
  ).catch((e) => uni.showToast({ title: (e as Error).message, icon: 'none' }))
}

function onChangeCard() {
  uni.showModal({
    title: '变更银行卡',
    content: '换卡需要重新走工行收方入驻（S7）。请先联系客服或现场收货员协助办理。',
    showCancel: false
  })
}

function onRevoke(item: SellerAuthorization) {
  uni.showModal({
    title: '撤销授权',
    content: `确定撤销对「${item.enterpriseName}」的开票与代办税费授权？已开出的票不受影响。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await revokeAuthorization(naturalPersonId.value, item.tenantId, '自然人自助撤销')
        uni.showToast({ title: '已撤销', icon: 'none' })
        loadActive()
      } catch (e) {
        uni.showToast({ title: (e as Error).message, icon: 'none' })
      }
    }
  })
}

function goAppointments() {
  uni.navigateTo({ url: '/pages/appointment/index' })
}

function contactService() {
  uni.showModal({
    title: '联系客服',
    content: `请拨打 ${profile.serviceMobile || '客服电话'}，或让现场收货员为你建单。`,
    showCancel: false
  })
}

function signOut() {
  auth.signOut()
  uni.redirectTo({ url: '/pages/login/index' })
}

function formatTime(time?: string) {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
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

.tabs {
  display: flex;
  margin-bottom: 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;

  &__item {
    flex: 1;
    padding: 22rpx 0;
    text-align: center;
    font-size: 26rpx;
    color: $seller-text-secondary;

    &--active {
      color: $seller-primary;
      font-weight: 600;
      border-bottom: 4rpx solid $seller-primary;
    }
  }
}

.pending {
  padding: 20rpx;
  margin-bottom: 16rpx;
  border: 1rpx solid #eef0f3;
  border-radius: 12rpx;

  &--urgent {
    border-color: #ffd6d6;
    background-color: #fff5f5;
  }

  &__top {
    display: flex;
    justify-content: space-between;
  }

  &__type {
    color: $seller-primary;
    font-weight: 600;
  }

  &__status {
    color: $seller-text-secondary;
    font-size: 26rpx;
  }

  &__title {
    margin-top: 8rpx;
    font-size: 30rpx;
    font-weight: 600;
  }

  &__ent,
  &__deadline {
    margin-top: 6rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
  }
}

.pending-count {
  margin-bottom: 16rpx;
  color: #b26a00;
}

.group-summary {
  margin-bottom: 12rpx;
  color: $seller-text-secondary;
}

.record {
  padding: 16rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__row {
    display: flex;
    justify-content: space-between;
    gap: 16rpx;
  }

  &__cat {
    font-weight: 600;
  }

  &__amount {
    color: $seller-primary;
    font-weight: 600;
  }

  &__meta {
    margin-top: 6rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
    line-height: 1.6;
  }

  &__cancel,
  &__warn {
    margin-top: 6rpx;
    color: #b26a00;
    font-size: 26rpx;
  }

  &__actions {
    display: flex;
    align-items: center;
    margin-top: 12rpx;
    gap: 20rpx;
  }
}

.status-line {
  display: flex;
  justify-content: space-between;
  margin-top: 6rpx;

  &__label {
    color: $seller-text-secondary;
  }
}

.three-lines {
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #eef0f3;
}

.year-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.year {
  font-weight: 600;
}

.auth {
  padding: 16rpx 0;
  border-top: 1rpx solid #eef0f3;
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 10rpx 0;

  &__k {
    color: $seller-text-secondary;
  }
}

.scope-note {
  margin-top: 12rpx;
  color: $seller-text-secondary;
  font-size: 24rpx;
  line-height: 1.7;
}

.muted {
  color: $seller-text-secondary;
  line-height: 1.7;
}

.link {
  color: $seller-primary;

  &--danger {
    color: #cf1322;
  }
}

.mini-btn {
  display: inline-block;
  padding: 0 24rpx;
  height: 60rpx;
  line-height: 60rpx;
  font-size: 26rpx;
  color: #ffffff;
  background-color: $seller-primary;
  border-radius: 30rpx;
}

.btn {
  width: 100%;
  margin-top: 12rpx;
  color: $seller-primary;
  background-color: #ffffff;
  border: 1rpx solid $seller-primary;
}

.open {
  color: #1a7f43;
}

.closed {
  color: #cf1322;
}

.empty {
  margin-top: 80rpx;
  text-align: center;

  &__title {
    font-size: 34rpx;
    font-weight: 700;
  }

  &__desc {
    margin: 16rpx 0 24rpx;
    color: $seller-text-secondary;
    line-height: 1.7;
  }
}

.logout-note {
  margin-bottom: 16rpx;
  color: $seller-text-secondary;
  font-size: 26rpx;
  line-height: 1.7;
}
</style>
