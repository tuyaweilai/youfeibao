<template>
  <view class="page" :class="{ 'page--empty': !naturalPersonId }">
    <SellerNoProfile v-if="!naturalPersonId" />

    <template v-else>
      <!-- 底部导航的「交易」：待我确认 / 我的记录 / 收款记录 三个子页签，等宽平分一行 -->
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
          <view v-if="!home.pendingItems?.length" class="muted">
            <template v-if="stationId">
              你在这家场站没有待确认的货。可能是收货员还没建单，或你用的手机号与建档手机号不一致。
            </template>
            <template v-else>暂时没有需要你确认的事。</template>
          </view>
          <button v-if="stationId && !home.pendingItems?.length" class="btn" @click="contactService">
            联系客服
          </button>
          <view
            v-for="item in home.pendingItems"
            :key="`${item.type}-${item.settlementId || item.title}`"
            class="pending"
            :class="{ 'pending--urgent': item.urgent }"
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
            <!-- 真动作入口（#95）：待签协议点一下才现取签署链接；其余待办给一个可点的说明 -->
            <view class="pending__actions">
              <button
                v-if="item.action === 'SIGN_AGREEMENT'"
                class="mini-btn"
                :disabled="signing"
                @click.stop="openPending(item)"
              >
                {{ signing ? '正在打开…' : '去签署' }}
              </button>
              <text v-else-if="item.type === 'SETTLEMENT'" class="link" @click.stop="openPending(item)">
                去确认
              </text>
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
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import SellerNoProfile from '@/components/SellerNoProfile.vue'
import {
  confirmReceived,
  getHome,
  getPayments,
  getRecordGroups,
  PendingItem,
  SellerHome,
  SellerPayment,
  SellerRecordGroup
} from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'
import { useAgreementSign } from '@/composables/useAgreementSign'
import { currentStationId, forgetStationId, takeTabHint } from '@/utils/nav'
import { openHtmlWithAuth } from '@/utils/download'

defineOptions({ name: 'SellerTransaction' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)
/** 场站编号在首页落库（底部导航换页不带 query），进页时重取一次 */
const stationId = ref<number | undefined>(currentStationId())
const { signing, signAgreement } = useAgreementSign()

/** 子页签：与首页「常用服务」的入口对齐（首页点收款记录直接落这里） */
type TabKey = 'pending' | 'records' | 'payments'
const tabs: { key: TabKey; label: string }[] = [
  { key: 'pending', label: '待我确认' },
  { key: 'records', label: '我的记录' },
  { key: 'payments', label: '收款记录' }
]
const activeTab = ref<TabKey>('pending')
const home = reactive<SellerHome>({})
const recordGroups = ref<SellerRecordGroup[]>([])
const payments = ref<SellerPayment[]>([])

onLoad(() => {
  if (!auth.token) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
})

onShow(() => {
  stationId.value = currentStationId()
  // 从首页跳进来时预选的子页签（取一次即清）；存储里可能是别的页写的值，所以先认一遍
  const hint = takeTabHint()
  if (tabs.some((tab) => tab.key === hint) && hint !== activeTab.value) {
    activeTab.value = hint as TabKey
  }
  loadActive()
})

function switchTab(key: TabKey) {
  activeTab.value = key
  loadActive()
}

async function loadActive() {
  if (!naturalPersonId.value) {
    return
  }
  try {
    if (activeTab.value === 'pending') {
      const hint = currentStationId()
      Object.assign(home, await getHome(naturalPersonId.value, hint))
      // 后端认不出这个场站（已删 / 换库）：忘掉它，别再拿它去筛待办
      if (hint && !home.stationId) {
        forgetStationId()
      }
    } else if (activeTab.value === 'records') {
      recordGroups.value = await getRecordGroups(naturalPersonId.value)
    } else if (activeTab.value === 'payments') {
      payments.value = await getPayments(naturalPersonId.value)
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
  if (item.type === 'AGREEMENT' && item.payeeId) {
    signAgreement(naturalPersonId.value, item.payeeId)
    return
  }
  uni.showToast({ title: `${item.typeName}：请到现场与收货员办理`, icon: 'none' })
}

function contactService() {
  uni.showModal({
    title: '如何关联交易记录',
    content: '请联系现场收货员，让对方为你建单，并核对建档手机号是否与当前登录手机号一致。',
    showCancel: false,
    confirmText: '我知道了'
  })
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

function formatTime(time?: string) {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}
</script>

<style lang="scss" scoped>
@import '../../styles/seller-page.scss';
</style>
