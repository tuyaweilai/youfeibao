<template>
  <view class="page" :class="{ 'page--action': settlement && !isConfirmed }">
    <view v-if="loading" class="loading" aria-busy="true" aria-label="正在加载结算详情">
      <view class="skeleton skeleton--hero" /><view class="skeleton" /><view class="skeleton" />
    </view>
    <view v-else-if="error" class="error" role="alert">
      <view class="section-title">暂时无法加载结算单</view>
      <view class="muted">{{ error }}</view>
      <button class="primary" @click="load">重新加载</button>
    </view>
    <template v-else-if="settlement">
      <view class="hero">
        <view class="hero__top"><text>结算金额（元）</text><text class="badge" :class="{ 'badge--warn': needsAttention }">{{ settlement.confirmStatusName || '状态未知' }}</text></view>
        <view class="hero__amount">{{ money(settlement.totalAmount) }}</view>
        <view class="hero__meta"><text>共 {{ settlement.acquisitionCount ?? '—' }} 笔收购单</text><text>当前版本 v{{ settlement.currentVersionNo ?? '—' }}</text></view>
        <view class="hero__number">{{ settlement.settlementNo || '—' }}</view>
      </view>

      <view class="status" :class="{ 'status--warn': needsAttention }">
        <view class="status__title">{{ isConfirmed ? '计量与计价事实已确认' : needsAttention ? '需要跟进' : '等待出售者确认' }}</view>
        <view class="status__hint">{{ statusHint }}</view>
        <view v-if="settlement.disputeReasonName" class="status__detail">异议：{{ settlement.disputeReasonName }}<text v-if="settlement.disputeCount"> · {{ settlement.disputeCount }} 次</text></view>
        <view v-if="settlement.enterpriseNotReplied" class="status__detail">企业尚未回复，出售者侧会看到此提示。</view>
      </view>

      <view class="card">
        <view class="section-title">出售者信息</view>
        <view class="seller"><view class="avatar" aria-hidden="true">{{ (settlement.sellerName || '售').slice(0, 1) }}</view><view><view class="seller__name">{{ settlement.sellerName || '未提供姓名' }}</view><view class="muted">{{ settlement.sellerMobile || '未提供手机号' }}</view></view></view>
        <view v-if="settlement.stationName" class="kv"><text>所属场站</text><text>{{ settlement.stationName }}</text></view>
        <view v-if="settlement.generateTime" class="kv"><text>生成时间</text><text>{{ formatTime(settlement.generateTime) }}</text></view>
        <view v-if="settlement.deadlineTime" class="kv"><text>确认截止</text><text>{{ formatTime(settlement.deadlineTime) }}</text></view>
        <view v-if="settlement.confirmTime" class="kv"><text>确认时间</text><text>{{ formatTime(settlement.confirmTime) }}</text></view>
      </view>

      <view class="section-heading"><view class="section-title">收购明细</view><text>按当前版本展示</text></view>
      <view v-for="(line, index) in settlement.lines" :key="line.acquisitionId || index" class="card line">
        <view class="line__top"><view class="line__category"><text class="line__index">{{ String(index + 1).padStart(2, '0') }}</text>{{ line.categoryName || '未提供品类' }}</view><text v-if="line.status === 9" class="cancel-tag">已作废</text></view>
        <view class="line__no">{{ line.acquisitionNo || '—' }}</view>
        <view class="metrics">
          <view><view class="label">结算重量<text v-if="line.unit">（{{ line.unit }}）</text></view><view class="metric">{{ line.settlementWeight ?? '—' }}</view></view>
          <view><view class="label">单价（元<text v-if="line.unit">/{{ line.unit }}</text>）</view><view class="metric">{{ money(line.unitPrice) }}</view></view>
        </view>
        <view v-if="line.adjustmentAmount" class="kv"><text>调整金额</text><text>{{ money(line.adjustmentAmount) }} 元</text></view>
        <view class="line__total"><text>收购金额</text><view><text class="currency">¥</text>{{ money(line.amount) }}</view></view>
        <view v-if="line.status === 9 && line.cancelReason" class="cancel-reason">作废原因：{{ line.cancelReason }}</view>
        <button v-if="line.acquisitionId" class="detail-link" @click="goAcquisition(line.acquisitionId)"><text>查看收购单</text><text aria-hidden="true">→</text></button>
      </view>
      <view v-if="!settlement.lines?.length" class="card empty">这张结算单下暂无收购明细。</view>

      <view class="card">
        <view class="section-title">版本记录</view>
        <view class="section-note">保留每次变更，便于核对结算依据</view>
        <view v-for="version in settlement.versions" :key="version.id || version.versionNo" class="version">
          <view class="version__top"><view class="version__label">v{{ version.versionNo }}<text v-if="version.versionNo === settlement.currentVersionNo" class="current">当前版本</text></view><text class="version__time">{{ formatTime(version.createTime) }}</text></view>
          <view class="version__reason">{{ version.changeReason || '生成结算单' }}</view>
          <view v-if="version.snapshotHash" class="version__hash">快照哈希 · {{ version.snapshotHash.slice(0, 24) }}{{ version.snapshotHash.length > 24 ? '…' : '' }}</view>
        </view>
        <view v-if="!settlement.versions?.length" class="empty">暂无版本记录</view>
      </view>

      <view class="scope-note"><view class="scope-note__title">确认后会发生什么？</view><view>出售者确认的是计量与计价事实，不代表已付款或已开票。确认后，系统自动逐张发起开票流程；出售者还需在工行页面确认开票信息，企业才可付款，付款成功后开票。</view></view>
      <view v-if="!isConfirmed" class="action-bar"><view class="action-bar__hint">复制后转发给出售者，由本人完成确认</view><button class="primary" :loading="forwarding" :disabled="forwarding" @click="onForward">{{ forwarding ? '正在获取链接…' : '复制确认链接' }}</button></view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { forwardSettlementLink, getSettlement, SettlementVO } from '@/api/settlement'

defineOptions({ name: 'FieldSettlementDetail' })
const settlement = ref<SettlementVO | null>(null)
const settlementId = ref(0)
const loading = ref(true)
const error = ref('')
const forwarding = ref(false)
const isConfirmed = computed(() => [1, 4].includes(settlement.value?.confirmStatus ?? -1))
const needsAttention = computed(() => [2, 3].includes(settlement.value?.confirmStatus ?? -1))
const statusHint = computed(() => {
  switch (settlement.value?.confirmStatus) {
    case 1: return '出售者已确认，系统已自动逐张发起开票；等待出售者在工行页面确认开票信息。'
    case 2: return settlement.value?.enterpriseNotReplied ? '出售者提出异议，请企业核对并回复。' : '出售者提出异议，请在结算单页跟进处理。'
    case 3: return '已超期未确认，需要线下签字确认。'
    case 4: return '已完成线下签字，等价于出售者确认。'
    case 0: return '请将确认链接转发给出售者。本人确认后，系统会自动发起开票流程。'
    default: return '暂未获取到确认状态，请核对结算记录。'
  }
})
onLoad(query => { settlementId.value = Number(query?.id); void load() })
async function load() {
  loading.value = true
  error.value = ''
  try {
    if (!Number.isSafeInteger(settlementId.value) || settlementId.value <= 0) throw new Error('结算单编号无效，请返回列表重新打开。')
    const result = await getSettlement(settlementId.value)
    if (!result) throw new Error('未找到这张结算单。')
    settlement.value = result
  } catch (e) {
    error.value = (e as Error).message || '网络异常，请稍后重试。'
  } finally { loading.value = false }
}
async function onForward() {
  if (!settlement.value?.id || forwarding.value) return
  forwarding.value = true
  try {
    const resp = await forwardSettlementLink({ settlementId: settlement.value.id, sendSms: false })
    if (!resp.link) throw new Error(resp.message || '暂未生成确认链接，请稍后重试。')
    await new Promise<void>((resolve, reject) => {
      uni.setClipboardData({ data: resp.link!, success: () => resolve(), fail: () => reject(new Error('复制失败，请重试。')) })
    })
    uni.showModal({ title: '链接已复制', content: `${resp.message || ''}\n\n${resp.notificationText || ''}\n${resp.link}`, showCancel: false })
  } catch (e) {
    uni.showModal({ title: '操作失败', content: (e as Error).message, showCancel: false })
  } finally { forwarding.value = false }
}
function goAcquisition(id: number) { uni.navigateTo({ url: `/pages/acquisition/detail?id=${id}` }) }
function money(value?: number) {
  return value == null || !Number.isFinite(Number(value)) ? '—' : Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatTime(ts?: number) {
  if (!ts) return '—'
  const date = new Date(ts)
  if (Number.isNaN(date.getTime())) return '—'
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<style lang="scss" scoped>
.page { box-sizing: border-box; width: 100%; max-width: 480px; min-height: 100vh; margin: 0 auto; padding: 20px 16px calc(28px + env(safe-area-inset-bottom)); background: #f3f7f5; color: #233e2e; font-size: 16px; line-height: 1.6; }
.page--action { padding-bottom: calc(146px + env(safe-area-inset-bottom)); }
button { box-sizing: border-box; margin: 0; cursor: pointer; font-family: inherit; transition: background-color 180ms; &::after { border: 0; } &:focus-visible { outline: 3px solid #76a98c; outline-offset: 3px; } }
.hero { padding: 22px 20px 18px; border-radius: 21px; color: #fff; background: #174e3b;
  &__top { display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 13px; color: #d5e8de; }
  &__amount { margin: 14px 0; font-size: 34px; line-height: 1.25; font-weight: 650; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
  &__meta { display: flex; flex-wrap: wrap; gap: 8px 20px; font-size: 13px; color: #d5e8de; }
  &__number { margin-top: 18px; padding-top: 13px; border-top: 1px solid #42705e; font-size: 12px; color: #d5e8de; overflow-wrap: anywhere; }
}
.badge { padding: 4px 10px; border-radius: 7px; color: #174e3b; background: #ddf2e5; font-size: 12px; flex-shrink: 0; &--warn { color: #853817; background: #ffe8d2; } }
.status { padding: 18px 4px 22px; &__title { font-size: 16px; font-weight: 600; color: #176b4c; } &__hint { margin-top: 6px; color: #536c5d; font-size: 14px; line-height: 1.75; } &__detail { margin-top: 10px; font-size: 14px; } &--warn { margin: 14px 0; padding: 16px; border: 1px solid #f0d8c4; border-radius: 14px; background: #fff4e9; color: #853817; .status__title, .status__hint { color: #853817; } } }
.card { padding: 20px 18px; margin-bottom: 16px; background: #fff; border: 1px solid #e0e9e3; border-radius: 18px; }
.section-title { font-size: 17px; font-weight: 600; }
.section-note { margin-top: 4px; font-size: 12px; color: #617468; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin: 24px 4px 12px; > text { font-size: 12px; color: #617468; } }
.seller { display: flex; align-items: center; gap: 12px; padding: 18px 0; margin-bottom: 6px; border-bottom: 1px solid #edf2ee; &__name { font-size: 18px; font-weight: 600; overflow-wrap: anywhere; } }
.avatar { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 46px; height: 46px; color: #176b4c; background: #eaf3ed; border-radius: 13px; font-size: 20px; font-weight: 600; }
.muted { color: #617468; font-size: 14px; margin-top: 4px; }
.kv { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; padding: 8px 0; font-size: 14px; > text:first-child { color: #617468; flex-shrink: 0; } > text:last-child { text-align: right; overflow-wrap: anywhere; min-width: 0; } }
.line { &__top { display: flex; align-items: center; justify-content: space-between; gap: 12px; } &__category { font-size: 18px; font-weight: 600; overflow-wrap: anywhere; } &__index { display: inline-block; color: #617468; font-size: 12px; margin-right: 10px; font-weight: 400; } &__no { font-size: 12px; color: #617468; margin-top: 8px; overflow-wrap: anywhere; } &__total { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; padding-top: 16px; border-top: 1px solid #edf2ee; > text { font-size: 14px; flex-shrink: 0; } > view { font-size: 23px; font-weight: 600; text-align: right; overflow-wrap: anywhere; min-width: 0; font-variant-numeric: tabular-nums; } } }
.metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin: 18px 0; padding: 14px; border-radius: 12px; background: #f5f8f6; > view { min-width: 0; } }
.label { font-size: 12px; color: #617468; }
.metric { margin-top: 4px; font-size: 18px; font-weight: 500; overflow-wrap: anywhere; font-variant-numeric: tabular-nums; }
.currency { font-size: 14px; margin-right: 4px; }
.detail-link { display: flex; align-items: center; justify-content: space-between; width: 100%; min-height: 44px; padding: 10px 0 0; margin-top: 6px; color: #176b4c; background: transparent; font-size: 14px; line-height: 1.6; &:active { background: #edf5ef; } > text:last-child { font-size: 22px; } }
.cancel-tag, .current { display: inline-block; padding: 2px 8px; font-size: 11px; border-radius: 5px; font-weight: 400; }
.cancel-tag, .cancel-reason { color: #963a1b; background: #fff2e9; }
.cancel-reason { margin-top: 12px; padding: 10px; font-size: 13px; border-radius: 8px; }
.current { margin-left: 8px; color: #176b4c; background: #eaf3ed; }
.version { margin-top: 18px; padding-left: 14px; border-left: 2px solid #c9dfd1; &__top { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 6px; } &__label { font-size: 16px; font-weight: 600; } &__time { font-size: 12px; color: #617468; } &__reason { margin-top: 8px; font-size: 14px; overflow-wrap: anywhere; } &__hash { margin-top: 6px; font-size: 11px; color: #617468; overflow-wrap: anywhere; } }
.scope-note { padding: 4px 4px 12px; color: #617468; font-size: 13px; line-height: 1.8; &__title { margin-bottom: 6px; font-size: 14px; font-weight: 600; color: #395c48; } }
.action-bar { position: fixed; z-index: 10; bottom: 0; left: 50%; transform: translateX(-50%); box-sizing: border-box; width: 100%; max-width: 480px; padding: 12px 16px calc(16px + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #e0e9e3; &__hint { text-align: center; margin-bottom: 8px; font-size: 12px; color: #617468; } }
.primary { min-height: 48px; width: 100%; padding: 0 14px; line-height: 48px; color: #fff; background: #176b4c; border-radius: 12px; font-size: 16px; font-weight: 600; &:active { background: #12543b; } &[disabled] { background: #dce8e0; color: #526e5d; } }
.empty { padding: 24px 0; font-size: 14px; color: #617468; text-align: center; }
.error { padding: 22px; border-radius: 18px; background: #fff; .primary { margin-top: 22px; } }
.skeleton { height: 190px; margin-bottom: 16px; border-radius: 18px; background: #e5eee8; &--hero { height: 210px; background: #d5e5da; } }
@media (prefers-reduced-motion: reduce) { button { transition: none; } }
</style>
