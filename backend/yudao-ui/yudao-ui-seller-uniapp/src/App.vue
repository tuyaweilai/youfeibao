<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 自然人端启动钩子
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
</style>
