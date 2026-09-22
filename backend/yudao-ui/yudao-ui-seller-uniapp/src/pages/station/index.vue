<template>
  <view class="page">
    <view v-if="loading" class="card loading">
      <view class="loading__spinner"></view>
      <view>正在获取场站信息…</view>
    </view>

    <view v-else-if="error" class="card state-card">
      <view class="state-card__icon state-card__icon--error">!</view>
      <view class="state-card__title">暂时无法打开场站</view>
      <view class="error">{{ error }}</view>
      <button class="btn btn--ghost" @click="load">重试</button>
    </view>

    <template v-else-if="station">
      <view class="station-hero">
        <view class="station-hero__icon" aria-hidden="true">
          <view class="station-hero__roof"></view>
          <view class="station-hero__door"></view>
        </view>
        <view class="station__enterprise">{{ station.enterpriseName }}</view>
        <view class="station__name">{{ station.stationName }}</view>
        <view class="status-chip" :class="station.open ? 'status-chip--open' : 'status-chip--closed'">
          <view class="status-chip__dot"></view>
          <text>{{ station.openStatusName }}</text>
        </view>
      </view>

      <view class="card info-card">
        <view class="kv">
          <view class="kv__icon kv__icon--location"></view>
          <view class="kv__body">
            <text class="kv__k">场站地址</text>
            <text class="kv__value">{{ station.address || '暂无地址信息' }}</text>
          </view>
        </view>
        <view class="kv">
          <view class="kv__icon kv__icon--phone"></view>
          <view class="kv__body">
            <text class="kv__k">联系电话</text>
            <text class="kv__value">{{ station.contactMobile || '请到现场咨询' }}</text>
          </view>
        </view>
      </view>

      <view class="card tip-card">
        <view class="tip-card__title">到站前请注意</view>
        <view class="tip">本页仅展示场站公开信息，不包含个人交易数据。</view>
        <view class="tip">查看待确认结算，需要先验证本人手机号。</view>
        <view v-if="station.guide?.length" class="guide">
          <view v-for="(line, i) in station.guide" :key="i" class="guide__line">· {{ line }}</view>
        </view>
      </view>

      <view class="actions">
        <button class="btn btn--primary" @click="goMine">查看我的待确认</button>
        <button class="btn btn--ghost" @click="goAppointment">预约到站</button>
      </view>
      <view class="foot">预约仅用于告知预计到站时间，不代表下单，也不占用额度。</view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { resolveStation, PublicStationVO } from '@/api/public'
import { setTenantId } from '@/config/env'
import { getSubject, getToken } from '@/utils/auth'
import { rememberStationId, switchSellerTab } from '@/utils/nav'

defineOptions({ name: 'SellerStation' })

const stationCode = ref('')
const station = ref<PublicStationVO | null>(null)
const loading = ref(false)
const error = ref('')

onLoad((query) => {
  stationCode.value = (query?.station as string) || ''
  if (!stationCode.value) {
    error.value = '入口无效：缺少场站码。请重新扫描场站二维码。'
    return
  }
  load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await resolveStation(stationCode.value)
    station.value = data
    // 后续请求都带这个租户：他扫码的那家回收企业
    if (data.tenantId !== undefined && data.tenantId !== null) {
      setTenantId(data.tenantId)
    }
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function goMine() {
  const stationId = station.value?.stationId ?? ''
  if (getToken() && getSubject()) {
    // 底部导航换页带不了 query：场站编号先存本地，首页 onLoad/onShow 再取
    rememberStationId(stationId)
    switchSellerTab('/pages/home/index')
    return
  }
  uni.navigateTo({
    url: `/pages/login/index?station=${encodeURIComponent(stationCode.value)}&stationId=${stationId}`
  })
}

function goAppointment() {
  const extra =
    `station=${encodeURIComponent(stationCode.value)}` +
    `&stationName=${encodeURIComponent(station.value?.stationName || '')}` +
    `&enterpriseName=${encodeURIComponent(station.value?.enterpriseName || '')}`
  if (getToken() && getSubject()) {
    uni.navigateTo({ url: `/pages/appointment/index?${extra}` })
    return
  }
  uni.navigateTo({ url: `/pages/login/index?station=${encodeURIComponent(stationCode.value)}` })
}
</script>

<style lang="scss" scoped>
.page {
  box-sizing: border-box;
  min-height: calc(100vh - 44px);
  padding: 40rpx 32rpx 56rpx;
  background:
    radial-gradient(circle at 88% 0%, rgba(58, 149, 255, 0.13), transparent 30%),
    linear-gradient(180deg, #f7faff 0%, #f4f7fb 100%);
}

.card {
  padding: 34rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border: 1rpx solid rgba(22, 119, 255, 0.08);
  border-radius: 26rpx;
  box-shadow: 0 16rpx 44rpx rgba(31, 55, 88, 0.07);
}

.station-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8rpx 20rpx 38rpx;
  text-align: center;

  &__icon {
    position: relative;
    width: 92rpx;
    height: 92rpx;
    margin-bottom: 22rpx;
    background: linear-gradient(145deg, #2e8cff, #1264e7);
    border: 7rpx solid rgba(255, 255, 255, 0.92);
    border-radius: 28rpx;
    box-shadow: 0 16rpx 38rpx rgba(22, 119, 255, 0.2);
  }

  &__roof {
    position: absolute;
    top: 26rpx;
    left: 24rpx;
    width: 43rpx;
    height: 34rpx;
    border-top: 5rpx solid #ffffff;
    border-right: 5rpx solid #ffffff;
    transform: rotate(-45deg);
  }

  &__door {
    position: absolute;
    right: 25rpx;
    bottom: 18rpx;
    width: 19rpx;
    height: 32rpx;
    border: 5rpx solid #ffffff;
    border-bottom: 0;
    border-radius: 3rpx 3rpx 0 0;
  }
}

.station {
  &__enterprise {
    color: #3077df;
    font-size: 24rpx;
    font-weight: 600;
    letter-spacing: 2rpx;
  }

  &__name {
    margin: 10rpx 0 18rpx;
    color: $seller-text;
    font-size: 44rpx;
    font-weight: 800;
    line-height: 1.3;
  }
}

.status-chip {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 9rpx 18rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 600;

  &__dot {
    width: 10rpx;
    height: 10rpx;
    border-radius: 50%;
  }

  &--open {
    color: #16734a;
    background-color: #eaf7f0;

    .status-chip__dot {
      background-color: #20a464;
    }
  }

  &--closed {
    color: #9b6000;
    background-color: #fff5e2;

    .status-chip__dot {
      background-color: #d7890a;
    }
  }
}

.kv {
  display: flex;
  align-items: flex-start;
  gap: 22rpx;
  padding: 18rpx 0;

  & + & {
    border-top: 1rpx solid #edf0f4;
  }

  &__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
  }

  &__k {
    margin-bottom: 5rpx;
    color: #8992a2;
    font-size: 23rpx;
  }

  &__value {
    color: #344054;
    font-size: 28rpx;
    line-height: 1.55;
  }

  &__icon {
    position: relative;
    flex-shrink: 0;
    box-sizing: border-box;
    width: 42rpx;
    height: 42rpx;
    margin-top: 5rpx;
    border: 3rpx solid #6d9fdf;

    &--location {
      border-radius: 50% 50% 50% 0;
      transform: rotate(-45deg) scale(0.72);

      &::after {
        position: absolute;
        top: 11rpx;
        left: 11rpx;
        width: 10rpx;
        height: 10rpx;
        border: 3rpx solid #6d9fdf;
        border-radius: 50%;
        content: '';
      }
    }

    &--phone {
      width: 28rpx;
      height: 42rpx;
      margin-right: 7rpx;
      margin-left: 7rpx;
      border-radius: 6rpx;

      &::after {
        position: absolute;
        bottom: 4rpx;
        left: 8rpx;
        width: 6rpx;
        height: 2rpx;
        background-color: #6d9fdf;
        content: '';
      }
    }
  }
}

.open {
  color: #1a7f43;
}

.closed {
  color: #b26a00;
}

.tip-card {
  background: linear-gradient(135deg, #f3f8ff, #edf5ff);
  box-shadow: none;

  &__title {
    margin-bottom: 14rpx;
    color: #2a5f9f;
    font-size: 28rpx;
    font-weight: 700;
  }
}

.tip {
  color: #5d6f86;
  font-size: 25rpx;
  line-height: 1.7;
}

.guide {
  margin-top: 12rpx;

  &__line {
    color: $seller-text-secondary;
    font-size: 26rpx;
    line-height: 1.7;
  }
}

.btn {
  box-sizing: border-box;
  width: 100%;
  height: 96rpx;
  margin: 0;
  border-radius: 18rpx;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 96rpx;

  &--primary {
    background: linear-gradient(100deg, $seller-primary 0%, #2d8bff 100%);
    box-shadow: 0 14rpx 28rpx rgba(22, 119, 255, 0.2);
  }

  &--ghost {
    color: $seller-primary;
    background-color: #edf5ff;
    border: 2rpx solid #d4e7ff;
  }
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.foot {
  margin: 24rpx 20rpx 0;
  color: #98a2b3;
  font-size: 22rpx;
  line-height: 1.6;
  text-align: center;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  margin-top: 100rpx;
  color: $seller-text-secondary;
  text-align: center;

  &__spinner {
    width: 46rpx;
    height: 46rpx;
    border: 5rpx solid #dce9fa;
    border-top-color: $seller-primary;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
}

.error {
  margin: 14rpx 0 26rpx;
  color: #c4322b;
  line-height: 1.65;
}

.state-card {
  margin-top: 70rpx;
  text-align: center;

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 80rpx;
    height: 80rpx;
    margin: 0 auto 22rpx;
    border-radius: 26rpx;
    font-size: 38rpx;
    font-weight: 800;

    &--error {
      color: #c4322b;
      background-color: #fff0ee;
    }
  }

  &__title {
    font-size: 34rpx;
    font-weight: 800;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .loading__spinner { animation: none; }
}
</style>
