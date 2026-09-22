<template>
  <view class="page" :class="{ 'page--empty': !naturalPersonId }">
    <SellerNoProfile v-if="!naturalPersonId" />

    <template v-else>
      <view class="card">
        <view class="card__title">我的资料</view>
        <view class="kv"><text class="kv__k">姓名</text><text>{{ profile.name || '—' }}</text></view>
        <view class="kv"><text class="kv__k">手机号</text><text>{{ profile.mobileMasked || '—' }}</text></view>
        <view class="kv"><text class="kv__k">身份证号</text><text>{{ profile.idCardMasked || '—' }}</text></view>
        <view class="kv"><text class="kv__k">实名认证</text><text>{{ profile.realNameStatusName || '—' }}</text></view>
        <view class="kv"><text class="kv__k">客服电话</text><text>{{ profile.serviceMobile || '—' }}</text></view>
      </view>

      <!-- 发票与税费是自己一页（不占底部导航），从这里和首页「常用服务」进 -->
      <view class="entry-grid">
        <button class="entry" @click="goInvoice">
          <view class="entry__title">发票税费</view>
          <view class="entry__desc">按年度看开票与税额</view>
        </button>
        <button class="entry" @click="goAppointments">
          <view class="entry__title">我的预约</view>
          <view class="entry__desc">到站预约与状态</view>
        </button>
      </view>

      <view class="card">
        <view class="card__title">收款账户</view>
        <view v-for="(card, i) in profile.bankCards" :key="i" class="bank-card">
          <view class="kv">
            <text class="kv__k">{{ card.enterpriseName }}</text>
            <text>{{ card.bankName || '—' }} 尾号 {{ card.cardTail || '—' }}</text>
          </view>
          <view v-if="card.changeStatusName" class="change-status">
            {{ card.changeStatusName }}（换卡审核期间，该企业新交易的付款会挂起；原卡在你确认前仍然有效）
          </view>
          <view class="record__actions">
            <text class="link" @click="onChangeCard(card)">变更银行卡</text>
          </view>
        </view>
        <view v-if="!profile.bankCards?.length" class="muted">还没有登记收款账户。</view>
        <view class="scope-note">
          工行收方入驻只绑本人一张卡；换卡要重新走一次工行审核，不允许多张卡。
        </view>
      </view>

      <view class="card">
        <view class="card__title">企业授权</view>
        <view class="scope-note">撤销只拦未来的开票与代办税费，已开出的票不追溯。</view>
        <view v-for="item in authorizations" :key="item.tenantId" class="auth">
          <view class="record__row">
            <text class="record__cat">{{ item.enterpriseName }}</text>
            <text :class="item.revoked ? 'closed' : 'open'">
              {{ item.revoked ? '已撤销' : (item.reverseInvoiceAuthorized && item.taxAgencyAuthorized ? '已授权' : '未授权') }}
            </text>
          </view>
          <view class="record__meta">反向开票 {{ item.reverseInvoiceAuthorized ? '已授权' : '未授权' }} · 代办税费
            {{ item.taxAgencyAuthorized ? '已授权' : '未授权' }}</view>
          <view class="record__actions">
            <text v-if="!item.revoked" class="link link--danger" @click="onRevoke(item)">撤销授权</text>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="logout-note">{{ profile.logoutNote }}</view>
        <button class="btn" @click="signOut">退出登录</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import SellerNoProfile from '@/components/SellerNoProfile.vue'
import {
  getAuthorizations,
  getProfile,
  revokeAuthorization,
  SellerAuthorization,
  SellerBankCard,
  SellerProfile
} from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'

defineOptions({ name: 'SellerMy' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)
const profile = reactive<SellerProfile>({})
const authorizations = ref<SellerAuthorization[]>([])

onLoad(() => {
  if (!auth.token) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
})

onShow(() => {
  load()
})

async function load() {
  if (!naturalPersonId.value) {
    return
  }
  try {
    Object.assign(profile, await getProfile(naturalPersonId.value))
    authorizations.value = await getAuthorizations(naturalPersonId.value)
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  }
}

function goInvoice() {
  uni.navigateTo({ url: '/pages/invoice/index' })
}

function goAppointments() {
  uni.navigateTo({ url: '/pages/appointment/index' })
}

function onChangeCard(card: SellerBankCard) {
  if (!card.tenantId) {
    uni.showToast({ title: '请先让回收企业登记收款账户', icon: 'none' })
    return
  }
  if (card.changeStatusName) {
    uni.showModal({
      title: '收款账户变更中',
      content: `${card.changeStatusName}。审核通过前，该企业新交易的付款会挂起；原卡在你确认前仍然有效。`,
      showCancel: false
    })
    return
  }
  uni.navigateTo({
    url: `/pages/bankCard/index?tenantId=${card.tenantId}` +
      `&enterpriseName=${encodeURIComponent(card.enterpriseName || '')}` +
      `&cardTail=${encodeURIComponent(card.cardTail || '')}`
  })
}

function onRevoke(item: SellerAuthorization) {
  uni.showModal({
    title: '撤销授权',
    content: `确定撤销对「${item.enterpriseName}」的开票与代办税费授权？已开出的票不受影响。`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await revokeAuthorization(naturalPersonId.value, item.tenantId, '自然人自助撤销')
        uni.showToast({ title: '已撤销', icon: 'none' })
        load()
      } catch (e) {
        uni.showToast({ title: (e as Error).message, icon: 'none' })
      }
    }
  })
}

function signOut() {
  auth.signOut()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
@import '../../styles/seller-page.scss';

/* 我的页的两块入口：发票税费（独立页）与我的预约 */
.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
  margin-bottom: 24rpx;
}

.entry {
  box-sizing: border-box;
  margin: 0;
  padding: 26rpx 24rpx;
  text-align: left;
  background-color: #ffffff;
  border: 1rpx solid rgba(22, 119, 255, 0.08);
  border-radius: 22rpx;
  box-shadow: 0 10rpx 28rpx rgba(31, 55, 88, 0.055);

  &__title {
    color: #26354a;
    font-size: 28rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 6rpx;
    color: #8a94a4;
    font-size: 22rpx;
    line-height: 1.4;
  }
}
</style>
