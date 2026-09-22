<template>
  <web-view v-if="url" :src="url" />
  <view v-else class="page">
    <view class="empty">
      <view class="empty__icon" aria-hidden="true">
        <view class="empty__icon-link"></view>
      </view>
      <view class="empty__eyebrow">安全签署</view>
      <view class="empty__title">签署链接已失效</view>
      <view class="empty__desc">一次性签署链接可能已使用或过期，请返回上一页重新获取。</view>
      <button class="empty__button" @click="goBack">返回上一页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { goSellerHome } from '@/utils/nav'

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

function goBack() {
  uni.navigateBack({
    fail: () => goSellerHome()
  })
}
</script>

<style lang="scss" scoped>
.page {
  box-sizing: border-box;
  min-height: calc(100vh - 44px);
  padding: 120rpx 32rpx 56rpx;
  background:
    radial-gradient(circle at 88% 0%, rgba(58, 149, 255, 0.12), transparent 30%),
    linear-gradient(180deg, #f7faff 0%, #f4f7fb 100%);
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;

  &__icon {
    position: relative;
    width: 112rpx;
    height: 112rpx;
    margin-bottom: 28rpx;
    background-color: #e7f1ff;
    border: 7rpx solid #ffffff;
    border-radius: 36rpx;
    box-shadow: 0 16rpx 38rpx rgba(22, 119, 255, 0.14);
  }

  &__icon-link {
    position: absolute;
    top: 38rpx;
    left: 28rpx;
    width: 42rpx;
    height: 20rpx;
    border: 5rpx solid #4b91f7;
    border-radius: 18rpx;
    transform: rotate(-35deg);

    &::after {
      position: absolute;
      top: 13rpx;
      left: 26rpx;
      width: 42rpx;
      height: 20rpx;
      border: 5rpx solid #4b91f7;
      border-radius: 18rpx;
      content: '';
    }
  }

  &__eyebrow {
    margin-bottom: 12rpx;
    color: #3077df;
    font-size: 24rpx;
    font-weight: 700;
    letter-spacing: 3rpx;
  }

  &__title {
    color: $seller-text;
    font-size: 42rpx;
    font-weight: 800;
  }

  &__desc {
    max-width: 590rpx;
    margin-top: 18rpx;
    color: $seller-text-secondary;
    font-size: 27rpx;
    line-height: 1.7;
  }

  &__button {
    box-sizing: border-box;
    width: 100%;
    height: 96rpx;
    margin-top: 44rpx;
    color: #ffffff;
    background: linear-gradient(100deg, $seller-primary 0%, #2d8bff 100%);
    border-radius: 18rpx;
    box-shadow: 0 14rpx 28rpx rgba(22, 119, 255, 0.2);
    font-size: 30rpx;
    font-weight: 700;
    line-height: 96rpx;
  }
}
</style>
