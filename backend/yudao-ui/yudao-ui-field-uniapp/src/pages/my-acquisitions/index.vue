<template>
  <view class="page">
    <view class="heading">
      <view>
        <view class="heading__eyebrow">收料作业 / 收购记录</view>
        <view class="heading__title">我的收购单</view>
      </view>
      <button class="refresh" :disabled="loading" @click="load(true)">{{ loading ? '加载中…' : '刷新' }}</button>
    </view>

    <view class="summary">
      <view><text class="summary__number">{{ total === null ? '—' : total }}</text><text class="summary__unit">笔收购单</text></view>
      <text class="summary__note">当前账号可见记录</text>
    </view>
    <view class="list-heading"><text>收购明细</text><text class="list-heading__hint">按登记顺序 · 最新在前</text></view>

    <view v-if="loading && !list.length" class="skeletons" aria-label="正在加载收购记录" aria-busy="true">
      <view v-for="index in 3" :key="index" class="skeleton">
        <view class="skeleton__line skeleton__line--short" />
        <view class="skeleton__line" />
        <view class="skeleton__line skeleton__line--amount" />
      </view>
    </view>

    <view v-if="error" class="error" role="alert">
      <view class="error__title">{{ list.length ? '记录未能更新' : '暂时无法加载记录' }}</view>
      <view class="error__message">{{ error }}</view>
      <button class="retry" :disabled="loading" @click="load(failedReset)">重新加载</button>
    </view>

    <view v-if="!loading && !error && !list.length" class="empty">
      <view class="empty__illustration" aria-hidden="true"><view /><view /><view /></view>
      <view class="empty__title">还没有收购记录</view>
      <view class="empty__desc">完成收购登记后，可在这里查看明细与处理进度。</view>
      <button class="primary" @click="goCreate">登记第一笔收购</button>
    </view>

    <button v-for="item in list" :key="item.id || item.acquisitionNo" class="card" :disabled="!item.id" @click="goDetail(item.id)">
      <view class="card__top">
        <view class="seller">
          <view class="seller__avatar" aria-hidden="true">{{ (item.sellerName || '售').slice(0, 1) }}</view>
          <view class="seller__body">
            <view class="seller__name">{{ item.sellerName || '未提供出售者' }}</view>
            <view class="seller__goods">{{ item.categoryName || '未提供品类' }}<text v-if="item.specification"> · {{ item.specification }}</text></view>
          </view>
        </view>
        <text class="tag" :class="{ 'tag--abnormal': item.abnormal }">{{ item.statusName || '状态未知' }}</text>
      </view>
      <view class="card__money">
        <view>
          <view class="card__label">收购金额</view>
          <view class="card__amount"><text class="card__currency">¥</text>{{ money(item.amount) }}</view>
        </view>
        <view v-if="item.quantity != null" class="card__quantity">
          <view class="card__label">数量<text v-if="item.unit">（{{ item.unit }}）</text></view>
          <view>{{ item.quantity }}</view>
        </view>
      </view>
      <view class="card__number"><text>单号</text><text>{{ item.acquisitionNo || '—' }}</text></view>
      <!-- 异常不替换业务状态：已付款但未开票等异常仍须可见。 -->
      <view v-if="item.abnormal" class="card__abnormal">
        <text class="card__alert-title">需关注</text>
        <text>{{ (item.abnormalReasons || []).join('；') || '存在异常，请查看详情核对' }}</text>
      </view>
      <view v-else-if="item.statusNextStep" class="card__next">{{ item.statusNextStep }}</view>
      <view class="card__footer">
        <text>{{ item.abnormal ? '查看异常与处理进度' : '查看收购明细' }}</text><text aria-hidden="true">→</text>
      </view>
    </button>

    <view v-if="list.length" class="list-footer">
      <button v-if="hasMore && !error" class="load-more" :disabled="loading" @click="load(false)">{{ loading ? '正在加载…' : '加载更多记录' }}</button>
      <view v-if="!error" class="list-footer__count">{{ loading ? '正在更新记录' : hasMore ? '已显示 ' + list.length + ' / ' + total + ' 笔' : '已显示全部 ' + list.length + ' 笔记录' }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getAcquisitionPage, AcquisitionVO } from '@/api/acquisition'

defineOptions({ name: 'FieldMyAcquisitions' })

const list = ref<AcquisitionVO[]>([])
const total = ref<number | null>(null)
const loading = ref(false)
const error = ref('')
const failedReset = ref(true)
const pageNo = ref(0)
const pageSize = 20
const hasMore = computed(() => total.value !== null && list.value.length < total.value)

onShow(() => { void load(true) })

async function load(reset = true) {
  if (loading.value) return
  loading.value = true
  error.value = ''
  failedReset.value = reset
  const nextPage = reset ? 1 : pageNo.value + 1
  try {
    const page = await getAcquisitionPage({ pageNo: nextPage, pageSize })
    const rows = page.list || []
    if (!reset && !rows.length && list.value.length < page.total) {
      error.value = '记录已发生变化，请刷新列表后重试。'
      failedReset.value = true
      return
    }
    if (reset) {
      list.value = rows
    } else {
      const existingIds = new Set(list.value.map(item => item.id))
      list.value = [...list.value, ...rows.filter(item => item.id == null || !existingIds.has(item.id))]
    }
    total.value = page.total
    pageNo.value = nextPage
  } catch (e) {
    error.value = (e as Error).message || '网络异常，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function money(value?: number) {
  if (value == null || !Number.isFinite(Number(value))) return '—'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function goDetail(id?: number) {
  if (id) uni.navigateTo({ url: `/pages/acquisition/detail?id=${id}` })
}
function goCreate() { uni.navigateTo({ url: '/pages/acquisition/index' }) }
</script>

<style lang="scss" scoped>
.page { box-sizing: border-box; width: 100%; max-width: 480px; min-height: 100vh; margin: 0 auto; padding: 24px 16px calc(28px + env(safe-area-inset-bottom)); background: #f3f7f5; color: #233e2e; }
button { box-sizing: border-box; margin: 0; font-family: inherit; cursor: pointer; &::after { border: 0; } &:focus-visible { outline: 3px solid #76a98c; outline-offset: 3px; } }
.heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 0 4px;
  &__eyebrow { font-size: 12px; color: #5d7165; }
  &__title { margin-top: 8px; font-size: 27px; font-weight: 700; line-height: 1.4; }
}
.refresh { padding: 10px 12px; min-height: 44px; background: #e5efe8; color: #176b4c; font-size: 13px; line-height: 24px; border-radius: 12px; &[disabled] { color: #617969; background: #e9eeeb; } }
.summary { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 22px 0 24px; padding: 18px 20px; border: 1px solid #cfe0d6; border-radius: 17px; background: #e5f0e9;
  &__number { color: #176b4c; font-size: 30px; font-weight: 600; font-variant-numeric: tabular-nums; }
  &__unit { margin-left: 8px; font-size: 13px; color: #41694f; }
  &__note { flex-shrink: 0; font-size: 11px; color: #526f5d; }
}
.list-heading { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 0 4px; margin-bottom: 14px; font-size: 16px; font-weight: 600; &__hint { color: #63766a; font-size: 11px; font-weight: 400; } }
.card { display: block; width: 100%; padding: 18px 16px 0; margin-bottom: 14px; background: #fff; border: 1px solid #e0e9e3; border-radius: 18px; text-align: left; line-height: 1.5; color: #233e2e; box-shadow: 0 3px 12px rgba(23,78,59,.025); transition: background-color 180ms, border-color 180ms;
  &:active { background: #eff7f1; border-color: #9ec4ad; }
  &__top { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
  &__money { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-top: 22px; }
  &__label { font-size: 11px; font-weight: 400; color: #62766a; }
  &__amount { margin-top: 5px; font-size: 25px; font-weight: 650; line-height: 1.35; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
  &__currency { margin-right: 4px; font-size: 15px; font-weight: 500; }
  &__quantity { max-width: 32%; text-align: right; font-size: 16px; overflow-wrap: anywhere; > view:last-child { margin-top: 7px; } }
  &__money > view:first-child { flex: 1; min-width: 0; }
  &__number { display: flex; gap: 10px; margin-top: 16px; color: #67796d; font-size: 11px; > text:first-child { flex-shrink: 0; } > text:last-child { min-width: 0; overflow-wrap: anywhere; } }
  &__next { margin-top: 12px; padding: 10px 12px; color: #536b5c; background: #f4f8f5; border-radius: 9px; font-size: 12px; line-height: 1.7; }
  &__abnormal { display: flex; flex-direction: column; gap: 3px; margin-top: 12px; padding: 12px; color: #963a1b; background: #fff2e9; border-radius: 9px; font-size: 12px; line-height: 1.7; }
  &__alert-title { font-weight: 600; }
  &__footer { display: flex; align-items: center; justify-content: space-between; min-height: 46px; margin-top: 14px; border-top: 1px solid #edf2ee; color: #176b4c; font-size: 12px; > text:last-child { font-size: 20px; } }
}
.seller { display: flex; align-items: center; gap: 10px; min-width: 0;
  &__avatar { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 38px; height: 38px; color: #286848; background: #ecf4ef; border-radius: 11px; font-size: 17px; font-weight: 600; }
  &__body { min-width: 0; }
  &__name { font-size: 17px; font-weight: 600; overflow-wrap: anywhere; }
  &__goods { margin-top: 4px; color: #62766a; font-size: 12px; overflow-wrap: anywhere; }
}
.tag { flex-shrink: 0; max-width: 35%; padding: 4px 8px; border-radius: 7px; background: #eaf4ee; color: #276342; font-size: 11px; line-height: 1.6; &--abnormal { color: #963a1b; background: #fff2e9; } }
.error { padding: 18px; margin-bottom: 16px; background: #fff3ed; border: 1px solid #efdbcf; border-radius: 15px; &__title { font-size: 16px; font-weight: 600; color: #883c24; } &__message { margin: 8px 0; color: #883c24; font-size: 13px; line-height: 1.7; overflow-wrap: anywhere; } }
.retry { display: inline-block; min-height: 44px; padding: 10px 14px; color: #883c24; background: #fff; font-size: 13px; line-height: 24px; border-radius: 9px; }
.empty { padding: 40px 22px; text-align: center; background: #fff; border: 1px solid #e0e9e3; border-radius: 18px;
  &__illustration { box-sizing: border-box; width: 52px; height: 65px; padding: 18px 10px; margin: 0 auto 20px; border: 2px solid #9cbaa8; border-radius: 8px; background: #edf5ef; > view { height: 2px; margin-bottom: 9px; background: #9cbaa8; &:last-child { width: 60%; } } }
  &__title { font-size: 18px; font-weight: 600; }
  &__desc { margin: 12px 0 24px; color: #63766a; font-size: 14px; line-height: 1.8; }
}
.primary { min-height: 48px; padding: 0 16px; color: #fff; background: #176b4c; border-radius: 11px; font-size: 16px; line-height: 48px; }
.list-footer { margin-top: 22px; text-align: center; &__count { padding: 14px 0; color: #63766a; font-size: 12px; } }
.load-more { width: 100%; min-height: 48px; padding: 0 16px; border: 1px solid #cddfd3; background: #fff; color: #176b4c; font-size: 14px; line-height: 48px; border-radius: 11px; }
.skeleton { padding: 22px 18px; margin-bottom: 14px; background: #fff; border: 1px solid #e0e9e3; border-radius: 18px; &__line { height: 13px; margin-bottom: 20px; border-radius: 5px; background: #eaf0ec; &--short { width: 42%; height: 20px; } &--amount { width: 65%; height: 28px; margin-top: 30px; } } }
@media (prefers-reduced-motion: reduce) { .card { transition: none; } }
</style>
