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
</style>
