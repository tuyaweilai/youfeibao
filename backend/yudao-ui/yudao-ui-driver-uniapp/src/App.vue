<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { countDrafts } from '@/utils/draft'

onLaunch(() => {
  // 司机端启动钩子：有待补传的节点就提醒一声（补传入口在任务列表页顶部）
  const pending = countDrafts()
  if (pending > 0) {
    uni.showToast({ title: `有 ${pending} 条节点待补传`, icon: 'none' })
  }
})
</script>

<template>
  <slot />
</template>

<style lang="scss">
page {
  background-color: #f3f7f5;
  color: #17221d;
  font-size: 28rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.placeholder {
  padding: 48rpx 32rpx;

  &__title {
    font-size: 40rpx;
    font-weight: 600;
  }

  &__desc {
    margin-top: 20rpx;
    color: #8a919f;
    line-height: 1.6;
  }
}
</style>
