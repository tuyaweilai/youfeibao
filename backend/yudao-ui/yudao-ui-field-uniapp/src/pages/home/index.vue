<template>
  <view class="home">
    <view class="home__title">收货现场</view>
    <view class="home__desc">现场端工程骨架（#22）。业务页面见 #23 / #24 / #25 / #26 / #27。</view>
  </view>
</template>

<script setup lang="ts">
defineOptions({ name: 'FieldHome' })
</script>

<style lang="scss" scoped>
.home {
  padding: 40rpx;

  &__title {
    font-size: 44rpx;
    font-weight: 600;
  }

  &__desc {
    margin-top: 20rpx;
    color: $field-text-secondary;
    line-height: 1.6;
  }
}
</style>
