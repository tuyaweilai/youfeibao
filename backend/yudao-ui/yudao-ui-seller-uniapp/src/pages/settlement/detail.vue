<template>
  <view class="page">
    <view v-if="loading" class="card muted state-card">正在加载结算信息…</view>
    <view v-else-if="error" class="card error state-card">{{ error }}</view>

    <template v-else-if="settlement">
      <view class="card summary-card">
        <view class="head">
          <text class="head__no">{{ settlement.settlementNo }}</text>
          <text class="head__status" :class="confirmed ? 'head__status--open' : 'head__status--pending'">
            {{ settlement.confirmStatusName }}
          </text>
        </view>
        <view class="summary-total">
          <view class="summary-total__label">本次结算金额</view>
          <view class="summary-total__amount"><text>¥</text>{{ settlement.totalAmount ?? 0 }}</view>
          <view class="summary-total__weight">结算重量 {{ settlement.totalSettlementWeight ?? 0 }}</view>
        </view>
        <view class="kv"><text class="kv__k">出售者</text><text>{{ settlement.sellerName }}</text></view>
        <view class="kv"><text class="kv__k">生成时间</text><text>{{ formatTime(settlement.generateTime) }}</text></view>
        <view class="kv"><text class="kv__k">版本号</text><text>{{ settlement.currentVersionNo }}</text></view>
        <view v-if="settlement.deadlineTime" class="deadline">确认截止 {{ formatTime(settlement.deadlineTime) }}</view>
      </view>

      <view v-if="!realNamePassed" class="card realname">
        <view class="realname__title">你还未完成实名，这笔货款打不到你的银行卡</view>
        <view class="realname__desc">
          实名由你本人在微信里完成（人脸核验）；完成后收方入驻由平台自动办理。
          这不影响你先确认下面的重量与金额。
        </view>
        <button class="btn btn--primary" :loading="linking" @click="goRealName">去实名（用微信打开）</button>
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

      <!-- 开票信息确认（#106）：确认结算之后，这一批里每张票都要本人在工行页面上确认一次 -->
      <view v-if="showInvoiceSection" class="card action">
        <view class="card__title">开票信息确认</view>
        <view class="invoice-hint">{{ invoiceHeadline }}</view>
        <view v-for="item in invoiceItems" :key="item.acquisitionId" class="invoice">
          <view class="invoice__row">
            <text class="invoice__no">{{ item.acquisitionNo }}</text>
            <text :class="['invoice__stage', item.stage === 'CONFIRMED' ? 'invoice__stage--ok' : '']">
              {{ item.stageName }}
            </text>
          </view>
          <view class="invoice__meta">{{ item.categoryName }} · {{ item.amount ?? 0 }} 元</view>
          <view v-if="item.message" class="invoice__msg">{{ item.message }}</view>
          <view v-for="fail in item.failures || []" :key="fail.code" class="invoice__fail">
            {{ fail.name }}：{{ fail.message }}<text v-if="fail.remedy">（{{ fail.remedy }}）</text>
          </view>
          <button
            v-if="item.confirmPageAvailable"
            class="btn btn--primary"
            @click="openConfirmPage(item)"
          >
            去工行确认这一张
          </button>
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
  InvoiceConfirmItem,
  REAL_NAME_STATUS,
  confirmSettlement,
  disputeSettlement,
  getProfile,
  getSettlement,
  getSettlementInvoiceStatus,
  mintRealNameLink,
  Settlement
} from '@/api/seller'
import { openExternalUrl } from '@/utils/external'
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
/** 实名是否已通过（默认 true：进度没拿到之前不吓人）。实名与确认是两件事，不阻断确认 */
const realNamePassed = ref(true)
const linking = ref(false)

const confirmed = computed(() => [1, 4].includes(settlement.value?.confirmStatus ?? -1))

/** 已确认过才显示开票确认这一段：没确认就没有「逐张去工行确认」这回事 */
const invoiceItems = ref<InvoiceConfirmItem[]>([])
const showInvoiceSection = computed(() => invoiceItems.value.length > 0)
const invoiceHeadline = computed(() => {
  const pending = invoiceItems.value.filter((item) => item.stage === 'WAITING_CONFIRM').length
  if (pending > 0) {
    return `还有 ${pending} 张要在工行页面上确认开票信息；确认后企业才能付款、付款成功即开票。`
  }
  const blocked = invoiceItems.value.filter((item) => item.stage === 'BLOCKED').length
  if (blocked > 0) {
    return '这一批还有票没能发起开票，原因写在下面；企业处理完就能继续。'
  }
  return '这一批的开票信息都已确认，等企业付款。'
})
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
    // 实名是平台级的、记在自然人主体上：拿它决定要不要显示提醒（不改确认动作）
    const profile = await getProfile(naturalPersonId.value)
    realNamePassed.value = profile.realNameStatus === REAL_NAME_STATUS.PASSED
    if (confirmed.value) {
      loadInvoiceStatus()
    }
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

/**
 * 去实名：取一枚 ONBOARDING 一次性令牌，回到自然人端的实名入口。
 * 人脸只在微信环境能唤起，那边会按环境给出「用微信打开」的二维码指引（不在这里静默失败）。
 */
async function goRealName() {
  if (!settlement.value?.payeeId) {
    uni.showToast({ title: '缺少收方档案，请联系客服', icon: 'none' })
    return
  }
  linking.value = true
  try {
    const link = await mintRealNameLink(naturalPersonId.value, settlement.value.payeeId)
    if (!link.token) {
      throw new Error('未取到实名入口，请稍后重试')
    }
    uni.navigateTo({
      url: `/pages/index/index?token=${encodeURIComponent(link.token)}&purpose=ONBOARDING`
    })
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  } finally {
    linking.value = false
  }
}

async function loadInvoiceStatus() {
  try {
    invoiceItems.value = (await getSettlementInvoiceStatus(naturalPersonId.value, settlementId.value)) || []
  } catch (e) {
    // 拿不到步骤不影响他已经确认的事实：不弹错，页面上只是不显示这一段
    invoiceItems.value = []
  }
}

/** 打开工行的自然人确认页面；H5 用新标签，小程序用 web-view（ADR 0016） */
function openConfirmPage(item: InvoiceConfirmItem) {
  if (!item.confirmPageUrl) {
    uni.showToast({ title: '确认页暂时打不开，请让企业重新发起', icon: 'none' })
    return
  }
  openExternalUrl(item.confirmPageUrl, true)
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
    // 确认后立刻把「这一批的票要逐张去工行确认」摆出来：这就是他下一步要做的事（ADR 0039）
    await load()
    loadInvoiceStatus()
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
  box-sizing: border-box;
  min-height: calc(100vh - 44px);
  padding: 32rpx 32rpx 64rpx;
  background:
    radial-gradient(circle at 88% 0%, rgba(58, 149, 255, 0.11), transparent 28%),
    linear-gradient(180deg, #f7faff 0%, #f4f7fb 100%);
}

.card {
  padding: 34rpx 32rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border: 1rpx solid rgba(22, 119, 255, 0.08);
  border-radius: 26rpx;
  box-shadow: 0 16rpx 44rpx rgba(31, 55, 88, 0.07);

  &__title {
    margin-bottom: 22rpx;
    color: $seller-text;
    font-size: 32rpx;
    font-weight: 800;
  }
}

.summary-card {
  overflow: hidden;
  border: 0;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 24rpx;

  &__no {
    min-width: 0;
    color: #566174;
    font-size: 25rpx;
    font-weight: 600;
    overflow-wrap: anywhere;
  }

  &__status {
    flex-shrink: 0;
    padding: 8rpx 16rpx;
    border-radius: 999rpx;
    font-size: 23rpx;
    font-weight: 700;

    &--open {
      color: #16734a;
      background-color: #eaf7f0;
    }

    &--pending {
      color: #a05f00;
      background-color: #fff4df;
    }
  }
}

.summary-total {
  margin: 0 -32rpx 26rpx;
  padding: 30rpx 32rpx;
  color: #ffffff;
  background: linear-gradient(115deg, #1677ff 0%, #3b91ff 100%);

  &__label {
    color: rgba(255, 255, 255, 0.78);
    font-size: 23rpx;
  }

  &__amount {
    margin-top: 6rpx;
    font-size: 54rpx;
    font-weight: 800;
    letter-spacing: -1rpx;

    text {
      margin-right: 8rpx;
      font-size: 28rpx;
      font-weight: 600;
    }
  }

  &__weight {
    display: inline-block;
    margin-top: 10rpx;
    padding: 7rpx 13rpx;
    color: rgba(255, 255, 255, 0.9);
    background-color: rgba(255, 255, 255, 0.14);
    border-radius: 10rpx;
    font-size: 23rpx;
  }
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 11rpx 0;
  color: #344054;

  &__k {
    color: $seller-text-secondary;
  }
}

.deadline {
  margin-top: 16rpx;
  padding: 16rpx 18rpx;
  color: #9a5d00;
  background-color: #fff6e6;
  border-radius: 12rpx;
  font-size: 25rpx;
}

.reply {
  background-color: #fff8ea;
  border-color: #f4dfb5;
  box-shadow: none;

  &__line {
    line-height: 1.7;

    &--warn {
      margin-top: 8rpx;
      color: #b26a00;
    }
  }
}

.realname {
  background: linear-gradient(135deg, #fff9ec, #fff5df);
  border: 1rpx solid #f0d9a8;
  box-shadow: none;

  &__title {
    color: #8e5600;
    font-size: 29rpx;
    font-weight: 800;
  }

  &__desc {
    margin: 12rpx 0 20rpx;
    color: #8a6a1f;
    font-size: 26rpx;
    line-height: 1.7;
  }
}

/* 开票信息确认（#106）：一张票一段，能点就直接去工行确认 */
.invoice-hint {
  margin-bottom: 16rpx;
  color: #5b6b7c;
  font-size: 26rpx;
  line-height: 1.6;
}

.invoice {
  padding: 24rpx 0;
  border-bottom: 1px solid #f0f3f1;

  &:last-child {
    border-bottom: 0;
    padding-bottom: 0;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
  }

  &__no {
    font-size: 28rpx;
    font-weight: 600;
    overflow-wrap: anywhere;
  }

  &__stage {
    flex: 0 0 auto;
    padding: 4rpx 14rpx;
    color: #8a5a12;
    background-color: #fff6e6;
    border-radius: 999rpx;
    font-size: 22rpx;

    &--ok {
      color: #176b4c;
      background-color: #eaf3ed;
    }
  }

  &__meta {
    margin-top: 8rpx;
    color: #5b6b7c;
    font-size: 24rpx;
  }

  &__msg {
    margin-top: 8rpx;
    color: #203b2e;
    font-size: 24rpx;
    line-height: 1.6;
  }

  &__fail {
    margin-top: 8rpx;
    color: #ad3b12;
    font-size: 22rpx;
    line-height: 1.6;
  }

  .btn {
    margin-top: 16rpx;
  }
}

.line {
  padding: 22rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__row {
    display: flex;
    justify-content: space-between;
  }

  &__cat {
    color: #344054;
    font-size: 29rpx;
    font-weight: 700;
  }

  &__amount {
    color: $seller-primary;
    font-weight: 600;
  }

  &__meta {
    margin-top: 6rpx;
    color: $seller-text-secondary;
    font-size: 24rpx;
    line-height: 1.65;
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
  margin-bottom: 22rpx;
  padding: 20rpx;
  background-color: #f7f9fc;
  border-radius: 16rpx;

  &__text {
    line-height: 1.6;
  }
}

.btn {
  box-sizing: border-box;
  width: 100%;
  height: 94rpx;
  margin-top: 14rpx;
  border-radius: 18rpx;
  font-size: 29rpx;
  font-weight: 700;
  line-height: 94rpx;

  &--primary {
    color: #ffffff;
    background: linear-gradient(100deg, $seller-primary 0%, #2d8bff 100%);
    box-shadow: 0 12rpx 24rpx rgba(22, 119, 255, 0.18);
  }

  &--ghost {
    color: $seller-primary;
    background-color: #edf5ff;
    border: 2rpx solid #d4e7ff;
  }

  &[disabled] {
    color: #ffffff;
    background: #aabed8;
    box-shadow: none;
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
  color: #c4322b;
}

.state-card {
  margin-top: 70rpx;
  padding: 44rpx 32rpx;
  line-height: 1.65;
  text-align: center;
}
</style>
