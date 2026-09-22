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
@use "@/styles/theme.scss" as *;
page {
  background-color: $driver-background;
  color: $driver-text;
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
    color: $driver-muted;
    line-height: 1.6;
  }
}

.page { max-width: 760px; margin: 0 auto; box-sizing: border-box; }
button { cursor: pointer; transition: background-color 180ms ease, box-shadow 180ms ease; }
button:focus-visible { outline: 3px solid #69a69e; outline-offset: 3px; }
button::after { border: none; }
.btn { min-height: 48px; border-radius: 12px; }
.card { border: 1px solid $driver-border; box-shadow: 0 3px 12px #18383c04; }
.field__input:focus-within, .textarea:focus-within { outline: 2px solid #69a69e; outline-offset: 1px; }
@media (prefers-reduced-motion: reduce) { button { transition: none; } }
</style>
