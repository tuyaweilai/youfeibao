<template>
  <view class="page" :class="{ 'page--empty': !naturalPersonId }">
    <!-- 手机号验证过了但一个主体都没绑上：三页共用同一个空态 -->
    <SellerNoProfile v-if="!naturalPersonId" />

    <template v-else>
      <view class="home-hero">
        <view class="home-hero__top">
          <view>
            <view class="home-hero__eyebrow">自然人出售者服务</view>
            <view class="home-hero__title">你好，{{ auth.subject?.name || '出售者' }}</view>
          </view>
          <button class="home-hero__profile" aria-label="进入我的" @click="goMine">
            <view class="home-hero__profile-head"></view>
            <view class="home-hero__profile-body"></view>
          </button>
        </view>
        <button class="home-hero__pending" @click="goPending">
          <view>
            <view class="home-hero__pending-label">待你处理</view>
            <view class="home-hero__pending-count">
              {{ homeLoading ? '—' : (home.pendingCount || 0) }}<text> 项</text>
            </view>
          </view>
          <view class="home-hero__pending-action">立即查看 <text>›</text></view>
        </button>
        <view v-if="home.stationName" class="home-hero__station">
          当前场站：{{ home.stationName }}
        </view>
      </view>

      <view class="section-head">
        <view>
          <view class="section-head__title">常用服务</view>
          <view class="section-head__desc">查看你的交易、款项和票据</view>
        </view>
      </view>

      <view class="quick-grid">
        <button class="quick-entry" @click="goPending">
          <view class="quick-entry__icon quick-entry__icon--pending"></view>
          <view class="quick-entry__title">待我确认</view>
          <view class="quick-entry__desc">结算与签署</view>
          <view v-if="home.pendingCount" class="quick-entry__badge">{{ home.pendingCount }}</view>
        </button>
        <button class="quick-entry" @click="goTransaction('records')">
          <view class="quick-entry__icon quick-entry__icon--records"></view>
          <view class="quick-entry__title">交易记录</view>
          <view class="quick-entry__desc">卖货明细</view>
        </button>
        <button class="quick-entry" @click="goTransaction('payments')">
          <view class="quick-entry__icon quick-entry__icon--payments"></view>
          <view class="quick-entry__title">收款记录</view>
          <view class="quick-entry__desc">付款与回单</view>
        </button>
        <button class="quick-entry" @click="goInvoice">
          <view class="quick-entry__icon quick-entry__icon--invoice"></view>
          <view class="quick-entry__title">发票税费</view>
          <view class="quick-entry__desc">发票与税额</view>
        </button>
        <button class="quick-entry" @click="goAppointments">
          <view class="quick-entry__icon quick-entry__icon--calendar"></view>
          <view class="quick-entry__title">我的预约</view>
          <view class="quick-entry__desc">查看到站预约</view>
        </button>
        <button class="quick-entry" @click="goMine">
          <view class="quick-entry__icon quick-entry__icon--profile"></view>
          <view class="quick-entry__title">我的资料</view>
          <view class="quick-entry__desc">账户与授权</view>
        </button>
      </view>

      <view class="section-head section-head--pending">
        <view>
          <view class="section-head__title">最近待办</view>
          <view class="section-head__desc">优先处理会影响结算的事项</view>
        </view>
        <button class="section-head__link" @click="goPending">全部 <text>›</text></button>
      </view>

      <view class="card dashboard-pending">
        <view v-if="homeLoading" class="dashboard-pending__empty">正在加载待办…</view>
        <view v-else-if="!home.pendingItems?.length" class="dashboard-pending__empty">
          <view class="dashboard-pending__ok" aria-hidden="true"></view>
          <view class="dashboard-pending__empty-title">暂时没有待处理事项</view>
          <view class="dashboard-pending__empty-desc">新的结算或签署任务会显示在这里</view>
        </view>
        <button
          v-for="item in (home.pendingItems || []).slice(0, 2)"
          :key="`overview-${item.type}-${item.settlementId || item.title}`"
          class="dashboard-pending__item"
          @click="openPending(item)"
        >
          <view class="dashboard-pending__item-main">
            <view class="dashboard-pending__item-type">{{ item.typeName }}</view>
            <view class="dashboard-pending__item-title">{{ item.title }}</view>
            <view class="dashboard-pending__item-meta">
              {{ item.enterpriseName }}<text v-if="item.deadlineTime"> · 截止 {{ formatTime(item.deadlineTime) }}</text>
            </view>
          </view>
          <view class="dashboard-pending__arrow">›</view>
        </button>
      </view>

      <view class="entry-note">
        <view class="entry-note__icon" aria-hidden="true"></view>
        <view>
          <view class="entry-note__title">找不到场站、实名或建档入口？</view>
          <view class="entry-note__desc">场站请扫描现场二维码；实名和自填建档请使用收货员提供的专属链接。</view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import SellerNoProfile from '@/components/SellerNoProfile.vue'
import { getHome, PendingItem, SellerHome } from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'
import { useAgreementSign } from '@/composables/useAgreementSign'
import { currentStationId, forgetStationId, rememberStationId, switchSellerTab } from '@/utils/nav'

defineOptions({ name: 'SellerHome' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)
const { signAgreement } = useAgreementSign()

const home = reactive<SellerHome>({})
const homeLoading = ref(false)

onLoad((query) => {
  // 场站编号来自扫码进屋的链接：底部导航换页带不了 query，落到本地存储再取
  rememberStationId((query?.stationId as string) || '')
  if (!auth.token) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
})

onShow(() => {
  loadHome()
})

async function loadHome() {
  if (!naturalPersonId.value) {
    return
  }
  homeLoading.value = true
  try {
    const hint = currentStationId()
    Object.assign(home, await getHome(naturalPersonId.value, hint))
    // 后端认不出这个场站（已删 / 换库）：忘掉它，别再拿它去筛待办
    if (hint && !home.stationId) {
      forgetStationId()
    }
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  } finally {
    homeLoading.value = false
  }
}

function goPending() {
  switchSellerTab('/pages/transaction/index', 'pending')
}

function goTransaction(hint: 'records' | 'payments') {
  switchSellerTab('/pages/transaction/index', hint)
}

function goMine() {
  switchSellerTab('/pages/my/index')
}

function goInvoice() {
  uni.navigateTo({ url: '/pages/invoice/index' })
}

function goAppointments() {
  uni.navigateTo({ url: '/pages/appointment/index' })
}

function openPending(item: PendingItem) {
  if (item.type === 'SETTLEMENT' && item.settlementId) {
    uni.navigateTo({ url: `/pages/settlement/detail?id=${item.settlementId}` })
    return
  }
  // 待签电子协议：本人点一下才现取签署链接并跳转（#95）
  if (item.type === 'AGREEMENT' && item.payeeId) {
    signAgreement(naturalPersonId.value, item.payeeId)
    return
  }
  uni.showToast({ title: `${item.typeName}：请到现场与收货员办理`, icon: 'none' })
}

function formatTime(time?: string) {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}
</script>

<style lang="scss" scoped>
@import '../../styles/seller-page.scss';

.home-hero {
  padding: 34rpx;
  color: #ffffff;
  background: linear-gradient(125deg, #176ee6 0%, #3b91ff 100%);
  border-radius: 30rpx;
  box-shadow: 0 18rpx 42rpx rgba(22, 119, 255, 0.23);

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
  }

  &__eyebrow {
    color: rgba(255, 255, 255, 0.72);
    font-size: 22rpx;
    letter-spacing: 3rpx;
  }

  &__title {
    margin-top: 8rpx;
    font-size: 38rpx;
    font-weight: 800;
  }

  &__profile {
    position: relative;
    flex-shrink: 0;
    box-sizing: border-box;
    width: 78rpx;
    height: 78rpx;
    margin: 0;
    padding: 0;
    background-color: rgba(255, 255, 255, 0.16);
    border: 2rpx solid rgba(255, 255, 255, 0.28);
    border-radius: 24rpx;

    &-head {
      position: absolute;
      top: 16rpx;
      left: 27rpx;
      width: 20rpx;
      height: 20rpx;
      border: 4rpx solid #ffffff;
      border-radius: 50%;
    }

    &-body {
      position: absolute;
      bottom: 14rpx;
      left: 18rpx;
      width: 38rpx;
      height: 20rpx;
      border: 4rpx solid #ffffff;
      border-bottom: 0;
      border-radius: 24rpx 24rpx 0 0;
    }
  }

  &__pending {
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-sizing: border-box;
    width: 100%;
    min-height: 116rpx;
    margin: 30rpx 0 0;
    padding: 20rpx 24rpx;
    color: #ffffff;
    text-align: left;
    background-color: rgba(255, 255, 255, 0.14);
    border: 1rpx solid rgba(255, 255, 255, 0.22);
    border-radius: 20rpx;

    &-label { color: rgba(255, 255, 255, 0.76); font-size: 22rpx; }
    &-count {
      margin-top: 2rpx;
      font-size: 38rpx;
      font-weight: 800;
      text { font-size: 22rpx; font-weight: 500; }
    }
    &-action {
      font-size: 24rpx;
      font-weight: 600;
      text { margin-left: 5rpx; font-size: 32rpx; }
    }
  }

  &__station {
    margin-top: 18rpx;
    color: rgba(255, 255, 255, 0.78);
    font-size: 22rpx;
  }
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20rpx;
  margin: 38rpx 4rpx 18rpx;

  &--pending { margin-top: 34rpx; }
  &__title { color: #172033; font-size: 31rpx; font-weight: 800; }
  &__desc { margin-top: 4rpx; color: #7c8798; font-size: 22rpx; }
  &__link {
    flex-shrink: 0;
    margin: 0;
    padding: 10rpx 0 6rpx 20rpx;
    color: #3479d2;
    background-color: transparent;
    font-size: 24rpx;
    line-height: 1.2;
    text { margin-left: 4rpx; font-size: 30rpx; }
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.quick-entry {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  box-sizing: border-box;
  min-height: 190rpx;
  margin: 0;
  padding: 22rpx 20rpx;
  text-align: left;
  background-color: #ffffff;
  border: 1rpx solid rgba(22, 119, 255, 0.08);
  border-radius: 22rpx;
  box-shadow: 0 10rpx 28rpx rgba(31, 55, 88, 0.055);

  &__icon {
    position: relative;
    box-sizing: border-box;
    width: 48rpx;
    height: 48rpx;
    margin-bottom: 16rpx;
    background-color: #eaf3ff;
    border-radius: 15rpx;

    &::before,
    &::after { position: absolute; box-sizing: border-box; content: ''; }

    &--pending::before { top: 11rpx; left: 14rpx; width: 20rpx; height: 25rpx; border: 3rpx solid #347fdc; border-radius: 4rpx; }
    &--pending::after { top: 17rpx; left: 19rpx; width: 11rpx; height: 6rpx; border-bottom: 3rpx solid #347fdc; border-left: 3rpx solid #347fdc; transform: rotate(-45deg); }
    &--records::before { top: 10rpx; left: 12rpx; width: 24rpx; height: 29rpx; border: 3rpx solid #347fdc; border-radius: 4rpx; }
    &--records::after { top: 18rpx; left: 18rpx; width: 12rpx; height: 3rpx; background-color: #347fdc; box-shadow: 0 8rpx 0 #347fdc; }
    &--payments::before { top: 13rpx; left: 8rpx; width: 32rpx; height: 23rpx; border: 3rpx solid #347fdc; border-radius: 5rpx; }
    &--payments::after { top: 19rpx; left: 8rpx; width: 32rpx; height: 4rpx; background-color: #347fdc; }
    &--invoice::before { top: 9rpx; left: 13rpx; width: 22rpx; height: 31rpx; border: 3rpx solid #347fdc; border-radius: 3rpx; }
    &--invoice::after { top: 17rpx; left: 19rpx; width: 10rpx; height: 3rpx; background-color: #347fdc; box-shadow: 0 7rpx 0 #347fdc, 0 14rpx 0 #347fdc; }
    &--calendar::before { top: 12rpx; left: 9rpx; width: 30rpx; height: 27rpx; border: 3rpx solid #347fdc; border-radius: 5rpx; }
    &--calendar::after { top: 18rpx; left: 9rpx; width: 30rpx; height: 3rpx; background-color: #347fdc; box-shadow: 7rpx 8rpx 0 -1rpx #347fdc, 17rpx 8rpx 0 -1rpx #347fdc; }
    &--profile::before { top: 9rpx; left: 17rpx; width: 15rpx; height: 15rpx; border: 3rpx solid #347fdc; border-radius: 50%; }
    &--profile::after { bottom: 8rpx; left: 11rpx; width: 27rpx; height: 15rpx; border: 3rpx solid #347fdc; border-bottom: 0; border-radius: 16rpx 16rpx 0 0; }
  }

  &__title { color: #26354a; font-size: 25rpx; font-weight: 700; line-height: 1.35; }
  &__desc { margin-top: 5rpx; color: #8a94a4; font-size: 20rpx; line-height: 1.35; }
  &__badge {
    position: absolute;
    top: 15rpx;
    right: 15rpx;
    min-width: 30rpx;
    height: 30rpx;
    padding: 0 6rpx;
    color: #ffffff;
    text-align: center;
    background-color: #e5484d;
    border: 4rpx solid #ffffff;
    border-radius: 999rpx;
    font-size: 18rpx;
    font-weight: 700;
    line-height: 30rpx;
  }
}

.dashboard-pending {
  padding-top: 10rpx;
  padding-bottom: 10rpx;

  &__empty { padding: 30rpx 8rpx; color: #7c8798; text-align: center; }
  &__ok {
    position: relative;
    width: 62rpx;
    height: 62rpx;
    margin: 0 auto 15rpx;
    background-color: #eaf7f0;
    border-radius: 20rpx;
    &::after { position: absolute; top: 20rpx; left: 17rpx; width: 26rpx; height: 13rpx; border-bottom: 5rpx solid #1f9a5c; border-left: 5rpx solid #1f9a5c; transform: rotate(-45deg); content: ''; }
  }
  &__empty-title { color: #344054; font-size: 27rpx; font-weight: 700; }
  &__empty-desc { margin-top: 7rpx; color: #98a2b3; font-size: 22rpx; }
  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-sizing: border-box;
    width: 100%;
    margin: 0;
    padding: 24rpx 4rpx;
    text-align: left;
    background-color: transparent;
    border-bottom: 1rpx solid #edf0f4;
    border-radius: 0;
    &:last-child { border-bottom: 0; }
  }
  &__item-main { flex: 1; min-width: 0; }
  &__item-type { color: #3479d2; font-size: 21rpx; font-weight: 700; }
  &__item-title { margin-top: 5rpx; color: #26354a; font-size: 27rpx; font-weight: 700; }
  &__item-meta { margin-top: 7rpx; color: #8a94a4; font-size: 21rpx; line-height: 1.5; }
  &__arrow { flex-shrink: 0; margin-left: 18rpx; color: #7aa7df; font-size: 42rpx; font-weight: 300; }
}

.entry-note {
  display: flex;
  align-items: flex-start;
  gap: 18rpx;
  padding: 24rpx;
  color: #5e7088;
  background-color: #edf5ff;
  border: 1rpx solid #d9e9ff;
  border-radius: 20rpx;

  &__icon {
    position: relative;
    flex-shrink: 0;
    box-sizing: border-box;
    width: 38rpx;
    height: 38rpx;
    border: 3rpx solid #5f95d8;
    border-radius: 50%;
    &::before { position: absolute; top: 5rpx; left: 15rpx; width: 3rpx; height: 14rpx; background-color: #5f95d8; content: ''; }
    &::after { position: absolute; bottom: 6rpx; left: 15rpx; width: 3rpx; height: 3rpx; background-color: #5f95d8; content: ''; }
  }
  &__title { color: #345f94; font-size: 25rpx; font-weight: 700; }
  &__desc { margin-top: 6rpx; font-size: 22rpx; line-height: 1.65; }
}
</style>
