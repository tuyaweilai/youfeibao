<template>
  <view class="page">
    <view class="header">
      <view class="header__eyebrow">优废宝 · 司机工作台</view>
      <view class="header__name">{{ profile?.name || auth.nickname || '司机' }}</view>
      <view class="header__meta">
        {{ profile?.sourceName || '' }}{{ profile?.mobile ? ' · ' + profile.mobile : '' }}
      </view>
      <button class="header__logout" @click="onLogout">退出</button>
    </view>

    <view v-if="draftCount > 0" class="draft-bar" @click="goOffline">
      有 {{ draftCount }} 条节点待补传（弱网时暂存在本机）→ 去补传
    </view>

    <view class="filters">
      <button
        v-for="tab in tabs"
        :key="tab.value ?? 'all'"
        :class="['filters__item', { 'filters__item--active': status === tab.value }]"
        :aria-pressed="status === tab.value"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </view>

    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="!list.length" class="empty">
      <view class="empty__title">暂时没有派给你的任务</view>
      <view class="empty__desc">调度派车后这里会出现；下拉可以刷新。</view>
    </view>

    <view v-else class="list">
      <view v-for="task in list" :key="task.id" class="card" @click="goDetail(task.id!)">
        <view class="card__top">
          <view class="card__identifier"><text class="card__caption">任务编号</text><text class="card__no">{{ task.taskNo }}</text></view>
          <text :class="['tag', statusClass(task.status)]">{{ task.statusName }}</text>
        </view>
        <view class="card__destination">
          <text class="card__caption">提货点</text>
          <view class="card__address">{{ task.pickupAddress || '未填写提货地址' }}</view>
        </view>
        <view class="card__line">
          <text class="card__label">联系人</text>
          <text class="card__value">
            <text>{{ task.pickupContactName || '—' }}</text><text v-if="task.pickupContactPhone" class="card__phone">{{ task.pickupContactPhone }}</text>
          </text>
        </view>
        <view class="card__line">
          <text class="card__label">货物</text>
          <text class="card__value">
            {{ task.cargoName || '现场交接为准' }}
            <text v-if="task.estimatedQuantity">· 约 {{ task.estimatedQuantity }}{{ task.quantityUnit || '' }}</text>
          </text>
        </view>
        <view class="card__line">
          <text class="card__label">待提货</text>
          <text class="card__value">
            {{ (task.pendingStopCount ?? 0) > 0 ? `还剩 ${task.pendingStopCount} 家没提` : '—' }}
          </text>
        </view>
        <view class="card__line">
          <text class="card__label">计划时间</text>
          <text class="card__value">{{ timeRange(task.expectedStartTime, task.expectedEndTime) }}</text>
        </view>
      </view>
    </view>

    <view class="footer-tip">下拉刷新列表 · 点击任务查看详情与上报节点</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { DriverProfileVO, DriverTaskVO, getProfile, getTaskPage } from '@/api/task'
import { useAuthStore } from '@/store/auth'
import { countDrafts } from '@/utils/draft'

const auth = useAuthStore()
const loading = ref(true)
const list = ref<DriverTaskVO[]>([])
const profile = ref<DriverProfileVO>()
const status = ref<number | undefined>(undefined)
const draftCount = computed(() => countDrafts())

const tabs: { label: string; value: number | undefined }[] = [
  { label: '全部', value: undefined },
  { label: '待接单', value: 1 },
  { label: '执行中', value: 2 },
  { label: '运输中', value: 3 },
  { label: '已完成', value: 4 }
]

function statusClass(value?: number) {
  if (value === 4) return 'tag--done'
  if (value === 5) return 'tag--cancel'
  if (value === 3 || value === 2) return 'tag--doing'
  return 'tag--todo'
}

function fmt(ms?: number) {
  if (!ms) return '—'
  const d = new Date(ms)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function timeRange(start?: number, end?: number) {
  if (!start && !end) return '未指定'
  return `${fmt(start)} ~ ${fmt(end)}`
}

async function load() {
  loading.value = true
  try {
    if (!auth.isLoggedIn()) {
      uni.reLaunch({ url: '/pages/login/index' })
      return
    }
    profile.value = await getProfile()
    const page = await getTaskPage({ pageNo: 1, pageSize: 50, status: status.value })
    list.value = page.list
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function switchTab(value: number | undefined) {
  status.value = value
  load()
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/task/detail?id=${id}` })
}

function goOffline() {
  uni.navigateTo({ url: '/pages/offline/index' })
}

function onLogout() {
  auth.logout()
  uni.reLaunch({ url: '/pages/login/index' })
}

onShow(() => {
  load()
})
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style scoped lang="scss">
@use "@/styles/theme.scss" as *;
.page {
  padding: 16px 16px calc(32px + env(safe-area-inset-bottom));
  font-size: 16px;
  line-height: 1.6;
}
.header {
  position: relative;
  padding: 22px 20px;
  margin-bottom: 20px;
  border-radius: 18px;
  background: #103f43;
  color: #fff;
  &__eyebrow { margin-bottom: 14px; padding-right: 48px; font-size: 12px; line-height: 20px; color: #b5d8d4; }
  &__name { font-size: 24px; font-weight: 600; line-height: 34px; overflow-wrap: anywhere; }
  &__meta { margin-top: 6px; font-size: 14px; line-height: 22px; color: #c6dedb; font-variant-numeric: tabular-nums; }
  &__logout { position: absolute; right: 10px; top: 10px; margin: 0; padding: 0 10px; min-width: 44px; height: 44px; line-height: 44px; background: transparent; color: #d0e2e0; font-size: 13px; border-radius: 8px; }
  &__logout::after { border: none; }
}
.draft-bar { margin-bottom: 16px; padding: 12px 14px; background: #fff7ed; border: 1px solid #fed7aa; border-radius: 12px; color: #9a4a0b; font-size: 14px; line-height: 22px; cursor: pointer; }
.filters {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  padding: 4px;
  border: 1px solid $driver-border;
  border-radius: 13px;
  background: #eaf0ef;
  &__item { flex: 1; min-width: 0; white-space: nowrap; margin: 0; height: 44px; line-height: 44px; padding: 0; background: transparent; border: none; border-radius: 9px; color: #4b6164; font-size: 13px; font-weight: 500; }
  &__item::after { border: none; }
  &__item--active { background: $driver-primary; color: #fff; font-weight: 600; }
}
.card {
  margin-bottom: 16px;
  padding: 18px;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  &__top { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #edf1f1; }
  &__identifier { min-width: 0; }
  &__caption { display: block; font-size: 12px; line-height: 18px; color: $driver-muted; }
  &__no { display: block; margin-top: 3px; font-size: 13px; line-height: 20px; font-weight: 500; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; color: #496065; }
  &__destination { margin: 16px 0; }
  &__address { margin-top: 4px; font-size: 18px; font-weight: 600; line-height: 28px; color: $driver-text; overflow-wrap: anywhere; }
  &__line { display: flex; align-items: baseline; gap: 12px; margin-top: 10px; font-size: 16px; line-height: 25px; }
  &__label { flex: 0 0 56px; color: $driver-muted; font-size: 14px; }
  &__value { flex: 1; min-width: 0; color: $driver-text; overflow-wrap: anywhere; font-variant-numeric: tabular-nums; }
  &__phone { display: inline-block; margin-left: 8px; white-space: nowrap; }
}
.tag { flex-shrink: 0; white-space: nowrap; padding: 4px 10px; border-radius: 7px; font-size: 12px; font-weight: 500; line-height: 20px; }
.tag--todo { background: #fef3c7; color: #92400e; }
.tag--doing { background: $driver-soft; color: $driver-primary; }
.tag--done { background: #edf1f1; color: #4b6164; }
.tag--cancel { background: #fee2e2; color: #b91c1c; }
.empty { padding: 64px 16px; text-align: center; color: $driver-muted; font-size: 16px; line-height: 26px; }
.empty__title { font-size: 18px; font-weight: 600; color: $driver-text; }
.empty__desc { margin-top: 10px; font-size: 14px; line-height: 24px; }
.footer-tip { margin-top: 20px; padding: 0 8px; text-align: center; color: $driver-muted; font-size: 12px; line-height: 20px; }
</style>
