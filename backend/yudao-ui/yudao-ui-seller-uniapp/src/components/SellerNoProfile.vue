<template>
  <!-- 身份未匹配：明确空态，不给假列表。首页、交易、我的三页共用 -->
  <view class="empty">
    <view class="empty-visual" aria-hidden="true">
      <view class="empty-visual__document">
        <view class="empty-visual__line empty-visual__line--long"></view>
        <view class="empty-visual__line"></view>
      </view>
      <view class="empty-visual__search"></view>
    </view>

    <view class="empty__eyebrow">手机号已验证</view>
    <view class="empty__title">暂未关联到你的交易档案</view>
    <view class="empty__desc">当前手机号下还没有找到卖货记录，这不会影响现场为你建单。</view>

    <view class="guide-card">
      <view class="guide-card__title">你可以这样处理</view>
      <view class="guide-step">
        <view class="guide-step__number">1</view>
        <view class="guide-step__body">
          <view class="guide-step__title">联系现场收货员</view>
          <view class="guide-step__desc">请对方为你建单，并核对建档手机号</view>
        </view>
      </view>
      <view class="guide-step">
        <view class="guide-step__number">2</view>
        <view class="guide-step__body">
          <view class="guide-step__title">确认登录手机号</view>
          <view class="guide-step__desc">如果手机号填错，可退出后重新验证</view>
        </view>
      </view>
    </view>

    <button class="empty__primary" @click="contactService">查看处理方法</button>
    <button class="empty__secondary" @click="signOut">
      <text>换手机号重新验证</text>
      <text class="empty__secondary-arrow" aria-hidden="true">›</text>
    </button>

    <view class="empty__note">
      <view class="empty__note-icon" aria-hidden="true"></view>
      <text>交易记录由回收企业依法留存，不会因退出登录而删除</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useSellerAuthStore } from '@/store/auth'

/**
 * 「手机号验证过了、但没有匹配到自然人主体」的空态。
 *
 * <p>登录成功但一个主体都绑不上时（换号、手机号与建档手机号不一致）落在首页，
 * 交易与我的两页此时也没有任何数据可显示，所以三页共用这一个空态：
 * 说清「不影响现场建单」，并给出联系收货员 / 换手机号两个出口。
 */
defineOptions({ name: 'SellerNoProfile' })

const auth = useSellerAuthStore()

function contactService() {
  uni.showModal({
    title: '如何关联交易记录',
    content: '请联系现场收货员，让对方为你建单，并核对建档手机号是否与当前登录手机号一致。',
    showCancel: false,
    confirmText: '我知道了'
  })
}

function signOut() {
  auth.signOut()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  text-align: center;

  &-visual {
    position: relative;
    width: 136rpx;
    height: 136rpx;
    margin: 4rpx 0 28rpx;
    background: linear-gradient(145deg, #eaf3ff, #dcecff);
    border: 8rpx solid rgba(255, 255, 255, 0.92);
    border-radius: 44rpx;
    box-shadow: 0 18rpx 44rpx rgba(22, 119, 255, 0.14);

    &__document {
      position: absolute;
      top: 31rpx;
      left: 35rpx;
      box-sizing: border-box;
      width: 55rpx;
      height: 70rpx;
      padding: 21rpx 10rpx 0;
      background-color: #ffffff;
      border: 4rpx solid #4b91f7;
      border-radius: 8rpx;
    }

    &__line {
      width: 24rpx;
      height: 4rpx;
      margin-bottom: 10rpx;
      background-color: #9cc4f7;
      border-radius: 4rpx;

      &--long {
        width: 32rpx;
      }
    }

    &__search {
      position: absolute;
      right: 27rpx;
      bottom: 25rpx;
      box-sizing: border-box;
      width: 40rpx;
      height: 40rpx;
      background-color: #ffffff;
      border: 5rpx solid #1677ff;
      border-radius: 50%;

      &::after {
        position: absolute;
        right: -13rpx;
        bottom: -9rpx;
        width: 20rpx;
        height: 5rpx;
        background-color: #1677ff;
        border-radius: 5rpx;
        transform: rotate(48deg);
        content: '';
      }
    }
  }

  &__eyebrow {
    margin-bottom: 12rpx;
    color: #3077df;
    font-size: 24rpx;
    font-weight: 600;
    letter-spacing: 3rpx;
  }

  &__title {
    color: #172033;
    font-size: 42rpx;
    font-weight: 800;
    line-height: 1.3;
  }

  &__desc {
    max-width: 620rpx;
    margin: 18rpx 18rpx 34rpx;
    color: #667085;
    font-size: 27rpx;
    line-height: 1.65;
  }

  &__primary,
  &__secondary {
    box-sizing: border-box;
    width: 100%;
    height: 96rpx;
    margin: 0;
    border-radius: 18rpx;
    font-size: 30rpx;
    font-weight: 700;
    line-height: 96rpx;

    &::after {
      border: 0;
    }
  }

  &__primary {
    margin-top: 28rpx;
    color: #ffffff;
    background: linear-gradient(100deg, #1677ff 0%, #2d8bff 100%);
    box-shadow: 0 14rpx 28rpx rgba(22, 119, 255, 0.2);
  }

  &__secondary {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10rpx;
    margin-top: 12rpx;
    color: #386cae;
    background-color: transparent;
    font-size: 27rpx;

    &-arrow {
      margin-top: -2rpx;
      font-size: 38rpx;
      font-weight: 400;
    }
  }

  &__note {
    display: flex;
    align-items: flex-start;
    justify-content: center;
    gap: 12rpx;
    margin: 24rpx 16rpx 0;
    color: #98a2b3;
    font-size: 22rpx;
    line-height: 1.6;
  }

  &__note-icon {
    position: relative;
    flex-shrink: 0;
    box-sizing: border-box;
    width: 22rpx;
    height: 18rpx;
    margin-top: 9rpx;
    border: 3rpx solid #8cb2e5;
    border-radius: 4rpx;

    &::before {
      position: absolute;
      top: -13rpx;
      left: 3rpx;
      box-sizing: border-box;
      width: 10rpx;
      height: 12rpx;
      border: 3rpx solid #8cb2e5;
      border-bottom: 0;
      border-radius: 7rpx 7rpx 0 0;
      content: '';
    }
  }
}

.guide-card {
  box-sizing: border-box;
  width: 100%;
  padding: 32rpx 30rpx 18rpx;
  background-color: rgba(255, 255, 255, 0.96);
  border: 1rpx solid rgba(22, 119, 255, 0.08);
  border-radius: 26rpx;
  box-shadow: 0 18rpx 54rpx rgba(31, 55, 88, 0.08);
  text-align: left;

  &__title {
    margin-bottom: 22rpx;
    color: #172033;
    font-size: 28rpx;
    font-weight: 700;
  }
}

.guide-step {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 0 0 24rpx;

  &__number {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 44rpx;
    height: 44rpx;
    color: #1677ff;
    background-color: #eaf3ff;
    border-radius: 14rpx;
    font-size: 23rpx;
    font-weight: 700;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    color: #344054;
    font-size: 27rpx;
    font-weight: 700;
    line-height: 1.45;
  }

  &__desc {
    margin-top: 5rpx;
    color: #7b8494;
    font-size: 24rpx;
    line-height: 1.55;
  }
}
</style>
