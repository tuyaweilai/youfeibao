<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 现场端启动钩子：后续在这里恢复登录态、检查待补传草稿
})
</script>

<template>
  <slot />
</template>

<style lang="scss">
page {
  background-color: #f5f6f8;
  color: #1f2329;
  font-size: 28rpx;
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
