<template>
  <view class="page">
    <!-- 结束本次收货：生成后不得再加单，要加只能新建 -->
    <view class="card">
      <view class="card__title">结束本次收货</view>
      <view class="card__hint">
        聚合同一出售者尚未归组的收购单，生成一张结算单交给他确认。生成后不得再往这张单里加收购单。
      </view>
      <view class="field">
        <text class="field__label">出售者手机号或身份证号</text>
        <input v-model="lookup" class="input" placeholder="用于找出这位出售者的档案" />
      </view>
      <button class="btn btn--ghost" :loading="finding" @click="onFind">查出售者</button>
      <view v-if="payee" class="found">
        <view class="found__name">{{ payee.name }} {{ payee.mobile || '' }}</view>
        <view class="field">
          <text class="field__label">离线批次键（选填）</text>
          <input v-model="batchKey" class="input" placeholder="现场同一批用同一个值" />
        </view>
        <button class="btn btn--primary" :loading="generating" @click="onGenerate">生成结算单</button>
      </view>
      <view v-else-if="lookedUp" class="empty">没有查到这位出售者，请先建档。</view>
    </view>

    <!-- 确认状态筛选 -->
    <view class="tabs">
      <view
        v-for="tab in statusTabs"
        :key="tab.value ?? 'all'"
        class="tabs__item"
        :class="{ 'tabs__item--active': confirmStatus === tab.value }"
        @click="switchStatus(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view v-if="!loading && !list.length" class="empty">还没有结算单。</view>

    <view v-for="item in list" :key="item.id" class="card">
      <view class="card__top" @click="goDetail(item.id)">
        <text class="card__no">{{ item.settlementNo }}</text>
        <text class="tag" :class="statusClass(item.confirmStatus)">{{ item.confirmStatusName }}</text>
      </view>
      <view class="card__meta" @click="goDetail(item.id)">
        {{ item.sellerName }} · {{ item.acquisitionCount }} 笔 · 合计 {{ item.totalAmount ?? 0 }} 元
      </view>
      <view v-if="item.deadlineTime" class="card__deadline" @click="goDetail(item.id)">
        确认截止 {{ formatTime(item.deadlineTime) }}
      </view>
      <view v-if="item.disputeReasonName" class="card__dispute" @click="goDetail(item.id)">
        异议：{{ item.disputeReasonName }}（{{ item.disputeCount }} 次）
        <text v-if="item.enterpriseNotReplied"> · 企业尚未回复</text>
      </view>

      <view class="actions">
        <button v-if="!isConfirmed(item.confirmStatus)" class="mini-btn" @click="onForward(item)">
          复制确认链接
        </button>
        <button
          v-if="!isConfirmed(item.confirmStatus) && item.sellerMobile"
          class="mini-btn"
          @click="onForwardBySms(item)"
        >
          短信转达
        </button>
        <button class="mini-btn mini-btn--plain" @click="goDetail(item.id)">明细</button>
      </view>
    </view>

    <view class="scope-note">
      链接打开即可查看，不需要注册；要确认时他用手机号验证一次即可。确认是我们的账被出售者认可的唯一凭据。
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { findReturningCustomer, PayeeVO } from '@/api/payee'
import {
  forwardSettlementLink,
  generateSettlement,
  getSettlementPage,
  SettlementVO
} from '@/api/settlement'

defineOptions({ name: 'FieldSettlement' })

const statusTabs = [
  { label: '全部', value: undefined as number | undefined },
  { label: '待确认', value: 0 },
  { label: '有异议', value: 2 },
  { label: '需线下签字', value: 3 }
]

const list = ref<SettlementVO[]>([])
const loading = ref(false)
const confirmStatus = ref<number | undefined>(undefined)

const lookup = ref('')
const finding = ref(false)
const lookedUp = ref(false)
const payee = ref<PayeeVO | null>(null)
const batchKey = ref('')
const generating = ref(false)

onShow(() => {
  load()
})

async function load() {
  loading.value = true
  try {
    const page = await getSettlementPage({
      pageNo: 1,
      pageSize: 50,
      confirmStatus: confirmStatus.value
    })
    list.value = page.list || []
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

function switchStatus(value?: number) {
  confirmStatus.value = value
  load()
}

async function onFind() {
  if (!lookup.value) {
    uni.showToast({ title: '请输入手机号或身份证号', icon: 'none' })
    return
  }
  finding.value = true
  lookedUp.value = false
  payee.value = null
  try {
    const isMobile = /^1\d{10}$/.test(lookup.value)
    payee.value = await findReturningCustomer(
      isMobile ? { mobile: lookup.value } : { idCardNo: lookup.value }
    )
    lookedUp.value = true
  } catch (e) {
    showError(e)
  } finally {
    finding.value = false
  }
}

async function onGenerate() {
  if (!payee.value?.id) return
  generating.value = true
  try {
    await generateSettlement({
      payeeId: payee.value.id,
      batchKey: batchKey.value || undefined
    })
    uni.showToast({ title: '结算单已生成', icon: 'success' })
    payee.value = null
    lookup.value = ''
    batchKey.value = ''
    lookedUp.value = false
    await load()
  } catch (e) {
    showError(e)
  } finally {
    generating.value = false
  }
}

/** 一键转达：生成/复用一次性链接并复制到剪贴板，收货员当面或微信发给他 */
async function onForward(item: SettlementVO) {
  if (!item.id) return
  try {
    const resp = await forwardSettlementLink({ settlementId: item.id, sendSms: false })
    copyText(resp.link)
    uni.showModal({
      title: '链接已复制',
      content: `${resp.message || ''}\n\n${resp.notificationText || ''}\n${resp.link || ''}`,
      showCancel: false
    })
  } catch (e) {
    showError(e)
  }
}

/** 短信转达：由收货员显式触发，不受自动开关限制 */
async function onForwardBySms(item: SettlementVO) {
  if (!item.id) return
  try {
    const resp = await forwardSettlementLink({ settlementId: item.id, sendSms: true })
    if (resp.link) {
      copyText(resp.link)
    }
    uni.showModal({
      title: resp.smsSent ? '短信已发送' : '未发送短信',
      content: resp.message || '请复制链接当面转达。',
      showCancel: false
    })
  } catch (e) {
    showError(e)
  }
}

function goDetail(id?: number) {
  if (id) {
    uni.navigateTo({ url: `/pages/settlement/detail?id=${id}` })
  }
}

function isConfirmed(status?: number) {
  return status === 1 || status === 4
}

function statusClass(status?: number) {
  if (status === 1 || status === 4) return 'tag--ok'
  if (status === 2) return 'tag--warn'
  if (status === 3) return 'tag--danger'
  return ''
}

function copyText(text?: string) {
  if (!text) return
  uni.setClipboardData({ data: text, success: () => undefined })
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
  margin-bottom: 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__title {
    font-size: 32rpx;
    font-weight: 600;
  }

  &__hint {
    margin: 12rpx 0 20rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
    line-height: 1.6;
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__no {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__meta {
    margin-top: 12rpx;
    color: $field-text-secondary;
  }

  &__deadline,
  &__dispute {
    margin-top: 8rpx;
    color: #b26a00;
    font-size: 26rpx;
  }
}

.found {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eef0f3;

  &__name {
    margin-bottom: 16rpx;
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
    padding: 24rpx 0;
    text-align: center;
    color: $field-text-secondary;

    &--active {
      color: $field-primary;
      font-weight: 600;
      border-bottom: 4rpx solid $field-primary;
    }
  }
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 20rpx;
}

.mini-btn {
  flex: 1;
  min-width: 180rpx;
  height: 68rpx;
  line-height: 68rpx;
  color: #ffffff;
  background-color: $field-primary;
  border-radius: 12rpx;
  font-size: 26rpx;

  &--plain {
    color: $field-primary;
    background-color: #ffffff;
    border: 1rpx solid $field-primary;
  }
}

.tag {
  padding: 4rpx 16rpx;
  color: $field-primary;
  background-color: #eef4ff;
  border-radius: 999rpx;
  font-size: 24rpx;

  &--ok {
    color: #1a7f43;
    background-color: #e8f7ee;
  }

  &--warn {
    color: #b26a00;
    background-color: #fff7e6;
  }

  &--danger {
    color: #cf1322;
    background-color: #fff1f0;
  }
}

.field {
  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $field-text-secondary;
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
  color: #ffffff;
  background-color: $field-primary;

  &--ghost {
    color: $field-primary;
    background-color: #ffffff;
    border: 1rpx solid $field-primary;
  }
}

.empty {
  padding: 60rpx 0;
  color: $field-text-secondary;
  text-align: center;
}

.scope-note {
  margin-top: 16rpx;
  color: $field-text-secondary;
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
