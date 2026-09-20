<template>
  <view class="page">
    <view class="header">
      <view class="header__name">{{ profile?.name || auth.nickname || '司机' }}</view>
      <view class="header__meta">
        {{ profile?.sourceName || '' }}{{ profile?.mobile ? ' · ' + profile.mobile : '' }}
      </view>
      <view class="header__logout" @click="onLogout">退出</view>
    </view>

    <view v-if="draftCount > 0" class="draft-bar" @click="goOffline">
      有 {{ draftCount }} 条节点待补传（弱网时暂存在本机）→ 去补传
    </view>

    <view class="filters">
      <view
        v-for="tab in tabs"
        :key="tab.value ?? 'all'"
        :class="['filters__item', { 'filters__item--active': status === tab.value }]"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="!list.length" class="empty">
      <view class="empty__title">暂时没有派给你的任务</view>
      <view class="empty__desc">调度派车后这里会出现；下拉可以刷新。</view>
    </view>

    <view v-else class="list">
      <view v-for="task in list" :key="task.id" class="card" @click="goDetail(task.id!)">
        <view class="card__top">
          <text class="card__no">{{ task.taskNo }}</text>
          <text :class="['tag', statusClass(task.status)]">{{ task.statusName }}</text>
        </view>
        <view class="card__line">
          <text class="card__label">提货点</text>
          <text class="card__value">{{ task.pickupAddress }}</text>
        </view>
        <view class="card__line">
          <text class="card__label">联系人</text>
          <text class="card__value">
            {{ task.pickupContactName || '—' }}{{ task.pickupContactPhone ? ' · ' + task.pickupContactPhone : '' }}
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
          <text class="card__label">时间窗</text>
          <text class="card__value">{{ timeRange(task.expectedStartTime, task.expectedEndTime) }}</text>
        </view>
      </view>
    </view>

    <view class="footer-tip">下拉刷新任务列表；上报节点在任务详情里。</view>
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
.page {
  padding: 24rpx 24rpx 80rpx;
}

.header {
  position: relative;
  padding: 8rpx 8rpx 24rpx;

  &__name {
    font-size: 40rpx;
    font-weight: 700;
  }

  &__meta {
    margin-top: 8rpx;
    color: #6b7a72;
    font-size: 24rpx;
  }

  &__logout {
    position: absolute;
    right: 8rpx;
    top: 8rpx;
    color: #6b7a72;
    font-size: 26rpx;
  }
}

.draft-bar {
  margin-bottom: 20rpx;
  padding: 20rpx 24rpx;
  background-color: #fff7ed;
  border: 1rpx solid #fed7aa;
  border-radius: 12rpx;
  color: #b45309;
  font-size: 26rpx;
}

.filters {
  display: flex;
  gap: 12rpx;
  margin-bottom: 20rpx;

  &__item {
    padding: 10rpx 24rpx;
    background-color: #fff;
    border: 1rpx solid #e5e7eb;
    border-radius: 999rpx;
    color: #4b5563;
    font-size: 24rpx;

    &--active {
      background-color: #16a34a;
      border-color: #16a34a;
      color: #fff;
    }
  }
}

.card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 16rpx;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16rpx;
  }

  &__no {
    font-size: 26rpx;
    font-weight: 600;
  }

  &__line {
    display: flex;
    margin-top: 8rpx;
    font-size: 26rpx;
  }

  &__label {
    width: 120rpx;
    color: #8a919f;
  }

  &__value {
    flex: 1;
    color: #17221d;
  }
}

.tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;

  &--todo {
    background-color: #fef3c7;
    color: #b45309;
  }

  &--doing {
    background-color: #dcfce7;
    color: #15803d;
  }

  &--done {
    background-color: #e5e7eb;
    color: #4b5563;
  }

  &--cancel {
    background-color: #fee2e2;
    color: #b91c1c;
  }
}

.empty {
  padding: 120rpx 24rpx;
  text-align: center;
  color: #8a919f;

  &__title {
    font-size: 32rpx;
    color: #17221d;
  }

  &__desc {
    margin-top: 16rpx;
    font-size: 26rpx;
    line-height: 1.6;
  }
}

.footer-tip {
  margin-top: 24rpx;
  text-align: center;
  color: #9ca3af;
  font-size: 22rpx;
}
</style>
