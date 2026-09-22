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
  background-color: #f4f7fb;
  color: #172033;
  font-size: 28rpx;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
}

button {
  cursor: pointer;
}

button::after {
  border: 0;
}

button[disabled] {
  cursor: not-allowed;
}
</style>
