<template>
  <view class="home">
    <view class="home__header">
      <view class="home__name">{{ auth.nickname || '收货员' }}</view>
      <view class="home__role">{{ roleText }}</view>
    </view>

    <view v-if="draftCount > 0" class="draft" @click="go('/pages/offline/index')">
      <text>有 {{ draftCount }} 条收购登记待补传</text>
      <text class="draft__action">去补传 ›</text>
    </view>

    <view class="menu">
      <view v-for="item in menus" :key="item.url" class="menu__item" @click="go(item.url)">
        <view class="menu__title">{{ item.title }}</view>
        <view class="menu__desc">{{ item.desc }}</view>
      </view>
    </view>

    <button class="home__logout" @click="onLogout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { countDrafts } from '@/utils/draft'

defineOptions({ name: 'FieldHome' })

const auth = useAuthStore()
const draftCount = ref(0)

const ROLE_NAMES: Record<string, string> = {
  recycling_receiver: '收货员',
  recycling_invoicer: '开票员',
  recycling_finance: '财务',
  tenant_admin: '管理员'
}

const roleText = computed(() => {
  if (!auth.roles.length) {
    return '未识别角色'
  }
  return auth.roles.map((role) => ROLE_NAMES[role] || role).join(' / ')
})

const menus = [
  { title: '收购登记', desc: '现场登记一笔收购', url: '/pages/acquisition/index' },
  { title: '出售者建档', desc: '实名、收方入驻、协议与授权', url: '/pages/payee/index' },
  { title: '结算与确认', desc: '结束本次收货、把确认链接转达给出售者', url: '/pages/settlement/index' },
  { title: '我的收购单', desc: '查看每笔卡在哪一步', url: '/pages/my-acquisitions/index' }
]

onShow(() => {
  if (!auth.token) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  draftCount.value = countDrafts()
})

function go(url: string) {
  uni.navigateTo({ url })
}

function onLogout() {
  auth.logout()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.home {
  padding: 32rpx;

  &__header {
    padding: 40rpx 32rpx;
    background-color: #ffffff;
    border-radius: 16rpx;
  }

  &__name {
    font-size: 40rpx;
    font-weight: 600;
  }

  &__role {
    margin-top: 12rpx;
    color: $field-text-secondary;
  }

  &__logout {
    margin-top: 48rpx;
    color: #d03050;
    background-color: #ffffff;
  }
}

.draft {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  margin-top: 24rpx;
  color: #b26a00;
  background-color: #fff7e6;
  border-radius: 16rpx;

  &__action {
    color: $field-primary;
  }
}

.menu {
  margin-top: 24rpx;

  &__item {
    padding: 32rpx;
    margin-bottom: 20rpx;
    background-color: #ffffff;
    border-radius: 16rpx;
  }

  &__title {
    font-size: 32rpx;
    font-weight: 600;
  }

  &__desc {
    margin-top: 8rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
  }
}
</style>
