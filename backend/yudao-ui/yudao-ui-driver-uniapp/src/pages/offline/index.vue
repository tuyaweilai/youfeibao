<template>
  <view class="page">
    <view class="tip">
      这些是弱网时暂存在本机的节点上报。补传按**客户端请求号**幂等：即使重复提交，服务端也只会落一条节点，
      所以可以放心反复重试。
    </view>

    <view v-if="!drafts.length" class="empty">
      <view class="empty__title">没有待补传的节点</view>
      <view class="empty__desc">现场上报成功或已补传完成时会自动清空。</view>
    </view>

    <view v-else>
      <view v-for="draft in drafts" :key="draft.clientRequestId" class="card">
        <view class="card__title">{{ draft.summary }}</view>
        <view class="line">发生时间：{{ fmt(draft.payload.nodeTime) }}</view>
        <view class="line">暂存时间：{{ fmt(draft.createdAt) }}</view>
        <view class="line">照片：{{ draft.photos.length }} 张</view>
        <view v-if="draft.errorMsg" class="line line--error">上次失败：{{ draft.errorMsg }}</view>
        <button class="btn btn--ghost" @click="remove(draft.clientRequestId)">删除这条草稿</button>
      </view>

      <button class="btn btn--primary" :loading="busy" @click="sync">全部补传</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listDrafts, NodeDraft, removeDraft, syncOfflineNodes } from '@/utils/draft'

const drafts = ref<NodeDraft[]>([])
const busy = ref(false)

function refresh() {
  drafts.value = listDrafts()
}

function fmt(ms?: number) {
  if (!ms) return '—'
  const d = new Date(ms)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function remove(clientRequestId: string) {
  uni.showModal({
    title: '删除草稿',
    content: '这条节点还从未上报到服务端，删除后现场就只剩口头记忆了。确定删？',
    success: (res) => {
      if (res.confirm) {
        removeDraft(clientRequestId)
        refresh()
      }
    }
  })
}

async function sync() {
  busy.value = true
  try {
    const result = await syncOfflineNodes()
    refresh()
    uni.showToast({
      title: `补传成功 ${result.success} 条${result.failed ? `，失败 ${result.failed} 条` : ''}`,
      icon: 'none'
    })
  } finally {
    busy.value = false
  }
}

onShow(refresh)
</script>

<style scoped lang="scss">
.page {
  padding: 24rpx 24rpx 80rpx;
}

.tip {
  margin-bottom: 24rpx;
  padding: 20rpx 24rpx;
  background-color: #eff6ff;
  border: 1rpx solid #bfdbfe;
  border-radius: 12rpx;
  color: #1d4ed8;
  font-size: 24rpx;
  line-height: 1.6;
}

.card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 16rpx;

  &__title {
    margin-bottom: 12rpx;
    font-size: 28rpx;
    font-weight: 600;
  }
}

.line {
  margin-top: 6rpx;
  color: #6b7a72;
  font-size: 24rpx;

  &--error {
    color: #b91c1c;
  }
}

.btn {
  margin-top: 16rpx;
  border-radius: 12rpx;

  &--primary {
    background-color: #16a34a;
    color: #fff;
  }

  &--ghost {
    background-color: transparent;
    border: 1rpx solid #e5e7eb;
    color: #6b7a72;
    font-size: 26rpx;
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
</style>
