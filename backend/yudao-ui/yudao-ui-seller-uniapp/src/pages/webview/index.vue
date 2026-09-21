<template>
  <web-view v-if="url" :src="url" />
  <view v-else class="empty">
    <view class="empty__title">链接无效</view>
    <view class="empty__desc">请返回并重新点「去签署」。</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

/**
 * 外部页面承接（#95）：小程序 / App 不能直接打开第三方 URL，用 `<web-view>` 打开。
 * H5 不走这里（在调用处直接 `window.location.href` 跳转）。
 */
defineOptions({ name: 'SellerWebview' })

const url = ref('')

onLoad((query) => {
  const raw = query?.url ? String(query.url) : ''
  // uni 有时已经把 query 解码过一次；再解一次失败就保留原样，不为一个 URL 把页面打挂
  try {
    url.value = decodeURIComponent(raw)
  } catch {
    url.value = raw
  }
})
</script>

<style lang="scss" scoped>
.empty {
  margin-top: 120rpx;
  text-align: center;

  &__title {
    font-size: 34rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 16rpx;
    color: $seller-text-secondary;
  }
}
</style>
