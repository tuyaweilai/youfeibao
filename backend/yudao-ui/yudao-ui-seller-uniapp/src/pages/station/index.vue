<template>
  <view class="page">
    <view v-if="loading" class="card loading">正在解析场站…</view>

    <view v-else-if="error" class="card">
      <view class="error">{{ error }}</view>
      <button class="btn btn--ghost" @click="load">重试</button>
    </view>

    <template v-else-if="station">
      <view class="card">
        <view class="station__enterprise">{{ station.enterpriseName }}</view>
        <view class="station__name">{{ station.stationName }}</view>
        <view class="kv"><text class="kv__k">地址</text><text>{{ station.address || '—' }}</text></view>
        <view class="kv">
          <text class="kv__k">收货状态</text>
          <text :class="station.open ? 'open' : 'closed'">{{ station.openStatusName }}</text>
        </view>
        <view v-if="station.contactMobile" class="kv">
          <text class="kv__k">场站电话</text><text>{{ station.contactMobile }}</text>
        </view>
      </view>

      <view class="card tip-card">
        <view class="tip">本页只有公开信息，不含任何个人信息。</view>
        <view class="tip">要看「我的待确认」，请用手机号验证后查看。</view>
        <view v-if="station.guide?.length" class="guide">
          <view v-for="(line, i) in station.guide" :key="i" class="guide__line">· {{ line }}</view>
        </view>
      </view>

      <button class="btn btn--primary" @click="goMine">查看我的待确认</button>
      <button class="btn btn--ghost" @click="goAppointment">预约到站（不是下单）</button>
      <view class="foot">预约只是告诉他你大概什么时候来；不占额度、不产生开票、不进五流。</view>
      <view class="foot">令牌一次性链接（收购确认书上的二维码）也仍然可用</view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { resolveStation, PublicStationVO } from '@/api/public'
import { setTenantId } from '@/config/env'
import { getSubject, getToken } from '@/utils/auth'

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
    uni.navigateTo({ url: `/pages/home/index?stationId=${stationId}` })
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
  padding: 24rpx;
}

.card {
  padding: 32rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
}

.station {
  &__enterprise {
    color: $seller-text-secondary;
    font-size: 26rpx;
  }

  &__name {
    margin: 8rpx 0 20rpx;
    font-size: 40rpx;
    font-weight: 700;
  }
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 10rpx 0;

  &__k {
    color: $seller-text-secondary;
  }
}

.open {
  color: #1a7f43;
}

.closed {
  color: #b26a00;
}

.tip-card {
  background-color: #f0f5ff;
}

.tip {
  color: $seller-text-secondary;
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
  width: 100%;
  color: #ffffff;
  background-color: $seller-primary;

  &--ghost {
    color: $seller-primary;
    background-color: #ffffff;
    border: 1rpx solid $seller-primary;
  }
}

.foot {
  margin-top: 20rpx;
  color: $seller-text-secondary;
  font-size: 24rpx;
  text-align: center;
}

.loading,
.error {
  color: $seller-text-secondary;
  text-align: center;
}

.error {
  color: #cf1322;
}
</style>
