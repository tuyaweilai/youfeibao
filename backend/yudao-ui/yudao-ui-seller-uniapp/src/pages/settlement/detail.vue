<template>
  <view class="page">
    <view v-if="loading" class="card muted">加载中…</view>
    <view v-else-if="error" class="card error">{{ error }}</view>

    <template v-else-if="settlement">
      <view class="card">
        <view class="head">
          <text class="head__no">{{ settlement.settlementNo }}</text>
          <text :class="confirmed ? 'open' : 'closed'">{{ settlement.confirmStatusName }}</text>
        </view>
        <view class="kv"><text class="kv__k">出售者</text><text>{{ settlement.sellerName }}</text></view>
        <view class="kv"><text class="kv__k">生成时间</text><text>{{ formatTime(settlement.generateTime) }}</text></view>
        <view class="kv"><text class="kv__k">版本号</text><text>{{ settlement.currentVersionNo }}</text></view>
        <view class="kv">
          <text class="kv__k">合计结算重量</text><text>{{ settlement.totalSettlementWeight ?? 0 }}</text>
        </view>
        <view class="kv"><text class="kv__k">合计金额</text><text>{{ settlement.totalAmount ?? 0 }} 元</text></view>
        <view v-if="settlement.deadlineTime" class="deadline">确认截止 {{ formatTime(settlement.deadlineTime) }}</view>
      </view>

      <view v-if="settlement.enterpriseReplyNote || settlement.enterpriseNotReplied" class="card reply">
        <view v-if="settlement.enterpriseReplyNote" class="reply__line">
          企业回复：{{ settlement.enterpriseReplyNote }}
        </view>
        <view v-if="settlement.enterpriseNotReplied" class="reply__line reply__line--warn">
          企业尚未回复你的异议，可联系客服或让收货员转达。
        </view>
      </view>

      <view class="card">
        <view class="card__title">逐条收购单</view>
        <view v-for="line in settlement.lines" :key="line.acquisitionId" class="line">
          <view class="line__row">
            <text class="line__cat">{{ line.categoryName }}</text>
            <text class="line__amount">{{ line.amount ?? 0 }} 元</text>
          </view>
          <view class="line__meta">
            {{ line.acquisitionNo }} · 结算重量 {{ line.settlementWeight ?? 0 }} {{ line.unit || '' }}
            · 单价 {{ line.unitPrice ?? 0 }}
          </view>
          <view v-if="line.deduction" class="line__meta">
            扣杂 {{ line.deduction }}（{{ line.deductionMethod === 'RATIO' ? '按比例' : '按重量' }}）
          </view>
          <view v-if="line.adjustmentAmount" class="line__meta">
            调整项 {{ line.adjustmentAmount }} 元 · {{ line.adjustmentReason }}
          </view>
          <view v-if="line.cancelReason" class="line__warn">作废原因：{{ line.cancelReason }}</view>
        </view>
        <view class="scope-note">确认的是计量与计价事实，不是付款节奏的承诺。</view>
        <view class="record__actions">
          <text class="link" @click="printSettlement">打印结算确认书</text>
        </view>
      </view>

      <!-- 待确认 / 有异议：确认与异议两个动作 -->
      <view v-if="actionable" class="card action">
        <label class="check" @click="agreed = !agreed">
          <checkbox :checked="agreed" />
          <text class="check__text">
            我已核对这一版的品类、结算重量、单价与金额，确认无误。
          </text>
        </label>
        <button class="btn btn--primary" :disabled="!agreed || submitting" @click="onConfirm">确认结算</button>
        <button class="btn btn--ghost" @click="openDispute">有异议</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  confirmSettlement,
  disputeSettlement,
  getSettlement,
  Settlement
} from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'
import { openHtmlWithAuth } from '@/utils/download'

defineOptions({ name: 'SellerSettlementDetail' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)
const settlement = ref<Settlement | null>(null)
const loading = ref(false)
const error = ref('')
const agreed = ref(false)
const submitting = ref(false)
const settlementId = ref(0)

const confirmed = computed(() => [1, 4].includes(settlement.value?.confirmStatus ?? -1))
const actionable = computed(() => [0, 2, 3].includes(settlement.value?.confirmStatus ?? -1))

const REASONS = [
  { code: '01', label: '重量不符' },
  { code: '02', label: '扣杂不符' },
  { code: '03', label: '单价不符' },
  { code: '04', label: '品类或等级不符' },
  { code: '05', label: '货物不符' },
  { code: '99', label: '其他（须附说明）' }
]

onLoad((query) => {
  settlementId.value = Number(query?.id || 0)
  load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    settlement.value = await getSettlement(naturalPersonId.value, settlementId.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function onConfirm() {
  if (!settlement.value) return
  submitting.value = true
  try {
    await confirmSettlement(
      naturalPersonId.value,
      settlement.value.id,
      settlement.value.currentVersionId as number
    )
    uni.showToast({ title: '已确认', icon: 'success' })
    agreed.value = false
    load()
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function openDispute() {
  uni.showActionSheet({
    itemList: REASONS.map((reason) => reason.label),
    success: (res) => {
      const reason = REASONS[res.tapIndex]
      if (!reason) return
      askNote(reason.code, reason.label)
    }
  })
}

function askNote(code: string, label: string) {
  // 说明用可编辑弹窗（H5 用 prompt 兜底）：
  // #ifdef H5
  const note = window.prompt(`${label}：请补充说明（选「其他」时必填）`, '') || ''
  submitDispute(code, note)
  // #endif
  // #ifndef H5
  uni.showModal({
    title: label,
    content: `已选择「${label}」。请在现场向收货员说明，或联系客服转达。`,
    showCancel: false,
    success: () => submitDispute(code, label)
  })
  // #endif
}

async function submitDispute(code: string, note: string) {
  if (code === '99' && !note) {
    uni.showToast({ title: '选「其他」必须附说明', icon: 'none' })
    return
  }
  try {
    await disputeSettlement(naturalPersonId.value, settlementId.value, code, note || undefined)
    uni.showToast({ title: '已提交异议', icon: 'none' })
    load()
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  }
}

function printSettlement() {
  openHtmlWithAuth(
    `/icbc/seller/portal/settlement/confirmation?naturalPersonId=${naturalPersonId.value}&settlementId=${settlementId.value}`
  ).catch((e) => uni.showToast({ title: (e as Error).message, icon: 'none' }))
}

function formatTime(time?: string) {
  if (!time) return '—'
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

.head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;

  &__no {
    font-size: 34rpx;
    font-weight: 700;
  }
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 8rpx 0;

  &__k {
    color: $seller-text-secondary;
  }
}

.deadline {
  margin-top: 12rpx;
  color: #b26a00;
}

.reply {
  background-color: #fff7e6;

  &__line {
    line-height: 1.7;

    &--warn {
      margin-top: 8rpx;
      color: #b26a00;
    }
  }
}

.line {
  padding: 16rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__row {
    display: flex;
    justify-content: space-between;
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
  }

  &__warn {
    margin-top: 6rpx;
    color: #b26a00;
    font-size: 26rpx;
  }
}

.scope-note {
  margin-top: 12rpx;
  color: $seller-text-secondary;
  font-size: 24rpx;
  line-height: 1.7;
}

.check {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-bottom: 20rpx;

  &__text {
    line-height: 1.6;
  }
}

.btn {
  width: 100%;
  margin-top: 8rpx;

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

.record__actions {
  margin-top: 12rpx;
}

.link {
  color: $seller-primary;
}

.open {
  color: #1a7f43;
}

.closed {
  color: #cf1322;
}

.muted {
  color: $seller-text-secondary;
}

.error {
  color: #cf1322;
}
</style>
