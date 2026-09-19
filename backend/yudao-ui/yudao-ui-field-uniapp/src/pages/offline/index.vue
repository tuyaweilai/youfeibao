<template>
  <view class="page">
    <view class="toolbar">
      <button class="btn btn--primary" :loading="syncing" @click="onSyncAll">全部补传</button>
    </view>

    <view v-if="drafts.length === 0" class="empty">没有待补传的草稿</view>

    <view v-for="draft in drafts" :key="draft.clientRequestId" class="card">
      <view class="card__top">
        <text class="card__summary">{{ draft.summary }}</text>
        <text class="tag">待补传</text>
      </view>
      <view class="card__time">暂存于 {{ formatTime(draft.createdAt) }}</view>
      <view v-if="draft.photos.length" class="card__meta">含 {{ draft.photos.length }} 张照片</view>
      <view v-if="draft.errorMsg" class="card__error">上次失败：{{ draft.errorMsg }}</view>
      <view class="card__actions">
        <button class="btn btn--ghost" :loading="syncing" @click="onSyncOne(draft)">重试</button>
        <button class="btn btn--danger" @click="onRemove(draft)">删除</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { AcquisitionDraft, listDrafts, removeDraft, syncDrafts } from '@/utils/draft'

defineOptions({ name: 'FieldOffline' })

const drafts = ref<AcquisitionDraft[]>([])
const syncing = ref(false)

onShow(() => {
  refresh()
})

function refresh() {
  drafts.value = listDrafts()
}

async function onSyncAll() {
  if (!drafts.value.length) {
    uni.showToast({ title: '没有待补传的草稿', icon: 'none' })
    return
  }
  syncing.value = true
  try {
    const result = await syncDrafts()
    refresh()
    uni.showModal({
      title: '补传完成',
      content: `成功 ${result.success} 条，失败 ${result.failed} 条`,
      showCancel: false
    })
  } finally {
    syncing.value = false
  }
}

async function onSyncOne(draft: AcquisitionDraft) {
  syncing.value = true
  try {
    const result = await syncDrafts([draft.clientRequestId])
    refresh()
    if (result.failed > 0) {
      uni.showToast({ title: '仍有失败，可稍后重试', icon: 'none' })
    } else {
      uni.showToast({ title: '已补传', icon: 'success' })
    }
  } finally {
    syncing.value = false
  }
}

function onRemove(draft: AcquisitionDraft) {
  uni.showModal({
    title: '删除草稿',
    content: '删除后这条登记不会补传，确认删除？',
    success: (res) => {
      if (res.confirm) {
        removeDraft(draft.clientRequestId)
        refresh()
      }
    }
  })
}

function formatTime(ts: number) {
  return new Date(ts).toLocaleString()
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
}

.toolbar {
  margin-bottom: 20rpx;
}

.empty {
  margin-top: 120rpx;
  color: $field-text-secondary;
  text-align: center;
}

.card {
  padding: 32rpx;
  margin-bottom: 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__summary {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__time {
    margin-top: 12rpx;
    color: $field-text-secondary;
    font-size: 24rpx;
  }

  &__meta {
    margin-top: 8rpx;
    color: $field-text-secondary;
    font-size: 24rpx;
  }

  &__error {
    margin-top: 12rpx;
    color: #cf1322;
    font-size: 24rpx;
    line-height: 1.6;
  }

  &__actions {
    display: flex;
    gap: 16rpx;
    margin-top: 24rpx;
  }
}

.tag {
  padding: 4rpx 16rpx;
  color: #b26a00;
  background-color: #fff7e6;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.btn {
  flex: 1;

  &--primary {
    color: #ffffff;
    background-color: $field-primary;
  }

  &--ghost {
    color: $field-primary;
    background-color: #ffffff;
    border: 1rpx solid $field-primary;
  }

  &--danger {
    color: #cf1322;
    background-color: #ffffff;
    border: 1rpx solid #cf1322;
  }
}
</style>
