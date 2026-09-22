<template>
  <view v-if="settlement" class="page">
    <view class="card status">
      <view class="status__label">确认状态</view>
      <view class="status__name">{{ settlement.confirmStatusName }}</view>
      <view class="status__hint">{{ statusHint }}</view>
    </view>

    <view class="card">
      <view class="card__title">结算单</view>
      <view class="kv"><text class="kv__k">结算单号</text><text>{{ settlement.settlementNo }}</text></view>
      <view class="kv"><text class="kv__k">出售者</text><text>{{ settlement.sellerName }} {{ settlement.sellerMobile || '' }}</text></view>
      <view class="kv"><text class="kv__k">收购单</text><text>{{ settlement.acquisitionCount }} 笔</text></view>
      <view class="kv"><text class="kv__k">合计金额</text><text>{{ settlement.totalAmount ?? 0 }} 元</text></view>
      <view class="kv"><text class="kv__k">当前版本</text><text>v{{ settlement.currentVersionNo }}</text></view>
      <view v-if="settlement.deadlineTime" class="kv">
        <text class="kv__k">确认截止</text><text>{{ formatTime(settlement.deadlineTime) }}</text>
      </view>
      <view v-if="settlement.disputeReasonName" class="kv">
        <text class="kv__k">异议</text>
        <text>{{ settlement.disputeReasonName }}（{{ settlement.disputeCount }} 次）</text>
      </view>
      <view v-if="settlement.enterpriseNotReplied" class="warn">企业尚未回复，出售者侧会看到这个提示。</view>
      <button v-if="!isConfirmed" class="btn btn--primary" @click="onForward">复制确认链接（确认后自动发起开票）</button>
    </view>

    <view class="card">
      <view class="card__title">逐条收购单（按当前版本）</view>
      <view v-for="line in settlement.lines" :key="line.acquisitionId" class="line">
        <view class="line__top">
          <text class="line__no">{{ line.acquisitionNo }}</text>
          <text class="line__amount">{{ line.amount ?? 0 }} 元</text>
        </view>
        <view class="line__meta">
          {{ line.categoryName }} · 结算重量 {{ line.settlementWeight ?? '-' }} · 单价 {{ line.unitPrice ?? '-' }}
        </view>
        <view v-if="line.adjustmentAmount" class="line__meta">调整项 {{ line.adjustmentAmount }} 元</view>
        <view v-if="line.status === 9" class="line__cancel">已作废{{ line.cancelReason ? `：${line.cancelReason}` : '' }}</view>
      </view>
      <view v-if="!settlement.lines?.length" class="empty">这张结算单下没有收购单。</view>
    </view>

    <view class="card">
      <view class="card__title">版本留痕</view>
      <view v-for="version in settlement.versions" :key="version.id" class="version">
        <view class="version__top">
          <text>v{{ version.versionNo }}</text>
          <text class="version__time">{{ formatTime(version.createTime) }}</text>
        </view>
        <view class="version__reason">{{ version.changeReason || '生成结算单' }}</view>
        <view class="version__hash">快照哈希 {{ (version.snapshotHash || '').slice(0, 24) }}…</view>
      </view>
    </view>

    <view class="scope-note">
      确认表达的是出售者对计量与计价事实的认可。他确认后，系统会自动把这一批逐张推去开票，
      并请他在工行页面上确认开票信息；他确认完企业才能付款，付款成功即开票（ADR 0038 / 0039）。
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { forwardSettlementLink, getSettlement, SettlementVO } from '@/api/settlement'

defineOptions({ name: 'FieldSettlementDetail' })

const settlement = ref<SettlementVO | null>(null)

const isConfirmed = computed(() => {
  const status = settlement.value?.confirmStatus
  return status === 1 || status === 4
})

const statusHint = computed(() => {
  switch (settlement.value?.confirmStatus) {
    case 1:
      return '出售者已确认，系统已自动逐张发起开票；等他在工行页面上确认开票信息。'
    case 2:
      return settlement.value?.enterpriseNotReplied
        ? '出售者有异议，企业尚未回复。'
        : '出售者有异议，请在结算单页处理。'
    case 3:
      return '超期未确认，需要线下签字确认。'
    case 4:
      return '已线下签字确认，等价于出售者确认。'
    default:
      return '等出售者确认（把上面的链接转达给他）；确认后系统会自动发起开票。'
  }
})

onLoad(async (query) => {
  const id = Number(query?.id)
  if (!id) return
  try {
    settlement.value = await getSettlement(id)
  } catch (e) {
    uni.showModal({ title: '加载失败', content: (e as Error).message, showCancel: false })
  }
})

async function onForward() {
  if (!settlement.value?.id) return
  try {
    const resp = await forwardSettlementLink({ settlementId: settlement.value.id, sendSms: false })
    if (resp.link) {
      uni.setClipboardData({ data: resp.link, success: () => undefined })
    }
    uni.showModal({
      title: '链接已复制',
      content: `${resp.message || ''}\n\n${resp.notificationText || ''}\n${resp.link || ''}`,
      showCancel: false
    })
  } catch (e) {
    uni.showModal({ title: '操作失败', content: (e as Error).message, showCancel: false })
  }
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
  margin-bottom: 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__title {
    margin-bottom: 16rpx;
    font-size: 32rpx;
    font-weight: 600;
  }
}

.status {
  &__label {
    color: $field-text-secondary;
    font-size: 26rpx;
  }

  &__name {
    margin-top: 8rpx;
    font-size: 40rpx;
    font-weight: 700;
  }

  &__hint {
    margin-top: 12rpx;
    color: $field-text-secondary;
    line-height: 1.6;
  }
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
  gap: 24rpx;

  &__k {
    color: $field-text-secondary;
  }
}

.line {
  padding: 20rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__top {
    display: flex;
    justify-content: space-between;
  }

  &__no {
    font-weight: 600;
  }

  &__meta {
    margin-top: 6rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
  }

  &__cancel {
    margin-top: 6rpx;
    color: #cf1322;
    font-size: 26rpx;
  }
}

.version {
  padding: 16rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__top {
    display: flex;
    justify-content: space-between;
  }

  &__time,
  &__hash {
    color: $field-text-secondary;
    font-size: 24rpx;
  }

  &__reason {
    margin: 6rpx 0;
  }
}

.warn {
  margin-top: 12rpx;
  padding: 16rpx 20rpx;
  color: #b26a00;
  background-color: #fff7e6;
  border-radius: 12rpx;
  font-size: 26rpx;
}

.btn {
  margin-top: 20rpx;
  color: #ffffff;
  background-color: $field-primary;
}

.empty {
  padding: 40rpx 0;
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
