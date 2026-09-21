<template>
  <view class="home">
    <view class="welcome">
      <view>
        <view class="welcome__date">{{ dateText }}</view>
        <view class="welcome__name">{{ auth.nickname || '收货员' }}，你好</view>
        <view class="welcome__role">{{ roleText }}</view>
      </view>
      <view class="welcome__avatar" aria-hidden="true">{{ (auth.nickname || '收')[0] }}</view>
    </view>

    <view class="overview">
      <view class="section-heading">
        <view>
          <view class="overview__title">业务概览</view>
          <view class="overview__scope">当前账号可见数据 · 累计</view>
        </view>
        <button class="refresh" :disabled="loading" @click="loadStats">{{ loading ? '更新中…' : '刷新' }}</button>
      </view>
      <view class="stats" aria-live="polite">
        <view v-for="item in stats" :key="item.label" class="stat">
          <view class="stat__label"><text class="stat__dot" :class="'stat__dot--' + item.tone" />{{ item.label }}</view>
          <view class="stat__value">{{ item.value === null ? '—' : item.value }}<text v-if="item.value !== null" class="stat__unit">{{ item.unit }}</text></view>
          <view class="stat__note">{{ item.value === null ? (loading ? '正在读取' : '暂不可用') : item.note }}</view>
        </view>
      </view>
      <view v-if="statsError" class="overview__error">部分数据未能加载，请稍后刷新。</view>
    </view>

    <button v-if="draftCount > 0" class="draft" @click="go('/pages/offline/index')">
      <text>{{ draftCount }} 条收购登记待补传</text><text>去处理 ›</text>
    </button>

    <view class="section-heading section-heading--work">
      <view class="section-title">收料作业</view><text class="section-caption">从这里开始一笔收购</text>
    </view>
    <button class="primary-action" @click="go('/pages/acquisition/index')">
      <view class="primary-action__icon" aria-hidden="true">＋</view>
      <view class="primary-action__body">
        <view class="primary-action__title">收购登记</view>
        <view class="primary-action__desc">登记货物、重量与收购金额</view>
      </view>
      <text class="primary-action__arrow" aria-hidden="true">→</text>
    </button>

    <view class="menu">
      <button v-for="item in menus" :key="item.url" class="menu__item" @click="go(item.url)">
        <view class="menu__top">
          <image class="menu__icon" :src="item.icon" mode="aspectFit" aria-hidden="true" />
          <text class="menu__arrow" aria-hidden="true">↗</text>
        </view>
        <view class="menu__title">{{ item.title }}</view>
        <view class="menu__desc">{{ item.desc }}</view>
      </button>
    </view>

    <view class="section-heading section-heading--work">
      <view class="section-title">出货作业</view><text class="section-caption">客户、交付与开票</text>
    </view>
    <view class="outbound">
      <button
        v-for="item in outboundMenus"
        :key="item.title"
        class="outbound__item"
        @click="showOutboundInfo(item)"
      >
        <image class="outbound__icon" :src="item.icon" mode="aspectFit" aria-hidden="true" />
        <view class="outbound__body">
          <view class="outbound__title">{{ item.title }}</view>
          <view class="outbound__desc">{{ item.desc }}</view>
        </view>
        <text class="outbound__status">待开放</text>
        <text class="outbound__arrow" aria-hidden="true">›</text>
      </button>
    </view>

    <view class="home__footer">
      <text>反向发票合规平台</text>
      <button class="home__logout" @click="onLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { countDrafts } from '@/utils/draft'
import { getAcquisitionPage } from '@/api/acquisition'
import { getSettlementPage } from '@/api/settlement'

defineOptions({ name: 'FieldHome' })
const auth = useAuthStore()
const draftCount = ref(0)
const loading = ref(false)
const statsError = ref(false)
const acquisitionCount = ref<number | null>(null)
const pendingCount = ref<number | null>(null)
const disputedCount = ref<number | null>(null)
const dateText = ref('')
const ROLE_NAMES: Record<string, string> = {
  recycling_receiver: '收货员', recycling_invoicer: '开票员', recycling_finance: '财务',
  tenant_admin: '管理员', common: '普通用户', super_admin: '超级管理员'
}
const roleText = computed(() => auth.roles.length
  ? auth.roles.map(role => ROLE_NAMES[role] || role).join(' / ') : '企业工作人员')
const stats = computed(() => [
  { label: '收购单', value: acquisitionCount.value, unit: '笔', note: '累计登记', tone: 'green' },
  { label: '待确认结算', value: pendingCount.value, unit: '单', note: '等待出售者确认', tone: 'amber' },
  { label: '有异议结算', value: disputedCount.value, unit: '单', note: '需核对处理', tone: 'red' },
  { label: '待补传草稿', value: draftCount.value, unit: '条', note: '仅统计本机', tone: 'blue' }
])
function icon(paths: string) {
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#176b4c" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">' + paths + '</svg>'
  )
}
const menus = [
  { title: '交接批次', desc: '车辆交接 · 磅次管理', url: '/pages/handover/index', icon: icon('<path d="M3 7h11v11H3zM14 11h4l3 4v3h-7M6 7V4h5"/><circle cx="7" cy="19" r="2"/><circle cx="18" cy="19" r="2"/>') },
  { title: '自然人建档', desc: '实名入驻 · 协议授权', url: '/pages/payee/index', icon: icon('<circle cx="9" cy="8" r="4"/><path d="M2 21v-2a7 7 0 0 1 14 0v2M20 7v6M17 10h6"/>') },
  { title: '结算与确认', desc: '结束收货 · 转达确认', url: '/pages/settlement/index', icon: icon('<path d="M14 3H5v18h14V8zM14 3v5h5M8 14l3 3 5-6"/>') },
  { title: '我的收购单', desc: '查看记录 · 跟进进度', url: '/pages/my-acquisitions/index', icon: icon('<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/>') }
]

const outboundMenus = [
  { title: '用废企业', desc: '供货客户档案', icon: icon('<path d="M3 21V7l8-4v18M11 9h10v12M7 8v2M7 13v2M15 13h2M15 17h2M1 21h22"/>') },
  { title: '送货单', desc: '发货、运输、签收', icon: icon('<path d="M3 7h11v11H3zM14 11h4l3 4v3h-7"/><circle cx="7" cy="19" r="2"/><circle cx="18" cy="19" r="2"/><path d="M5 3h8M10 1l3 2-3 2"/>') },
  { title: '正向开票', desc: '给用废企业开票', icon: icon('<path d="M5 3h14v18l-3-2-4 2-4-2-3 2zM8 7l4 4 4-4M8 12h8M8 15h8M12 11v6"/>') }
]

function showOutboundInfo(item: { title: string; desc: string }) {
  uni.showModal({
    title: item.title,
    content: item.desc + '。该功能暂未开放。',
    showCancel: false,
    confirmText: '知道了'
  })
}

async function loadStats() {
  if (loading.value) return
  loading.value = true
  statsError.value = false
  draftCount.value = countDrafts()
  try {
    const results = await Promise.allSettled([
      getAcquisitionPage({ pageNo: 1, pageSize: 1 }),
      getSettlementPage({ pageNo: 1, pageSize: 1, confirmStatus: 0 }),
      getSettlementPage({ pageNo: 1, pageSize: 1, confirmStatus: 2 })
    ])
    const targets = [acquisitionCount, pendingCount, disputedCount]
    results.forEach((result, index) => {
      const total = result.status === 'fulfilled' ? result.value?.total : null
      targets[index].value = typeof total === 'number' && Number.isFinite(total) && total >= 0 ? total : null
      if (targets[index].value === null) statsError.value = true
    })
  } finally {
    loading.value = false
  }
}
onShow(() => {
  if (!auth.token) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  const now = new Date()
  dateText.value = now.getFullYear() + '年' + (now.getMonth() + 1) + '月' + now.getDate() + '日 · 周' + '日一二三四五六'[now.getDay()]
  void loadStats()
})
function go(url: string) { uni.navigateTo({ url }) }
function onLogout() {
  auth.logout()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.home { box-sizing: border-box; max-width: 480px; min-height: 100vh; margin: 0 auto; padding: 24px 20px calc(20px + env(safe-area-inset-bottom)); background: #f3f7f5; color: #172b22; }
button { margin: 0; padding: 0; text-align: left; line-height: 1.5; cursor: pointer; &::after { border: 0; } &:focus-visible { outline: 3px solid #65ad8b; outline-offset: 3px; } }
.welcome { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px;
  &__date { font-size: 12px; color: #62766b; }
  &__name { margin-top: 8px; font-size: 23px; font-weight: 700; overflow-wrap: anywhere; }
  &__role { margin-top: 6px; font-size: 12px; color: #61746a; }
  &__avatar { display: flex; flex-shrink: 0; align-items: center; justify-content: center; width: 46px; height: 46px; border: 3px solid #fff; border-radius: 16px; background: #dcece2; color: #176b4c; font-size: 20px; font-weight: 600; }
}
.section-heading { display: flex; justify-content: space-between; align-items: center; gap: 8px;
  &--work { margin: 26px 0 14px; }
}
.section-title { font-size: 18px; font-weight: 700; }
.section-caption { font-size: 11px; color: #667a6f; }
.overview { padding: 20px; border-radius: 22px; background: #174e3b; color: #fff; box-shadow: 0 8px 20px rgba(23,78,59,.1);
  &__title { font-size: 17px; font-weight: 600; }
  &__scope { margin-top: 5px; font-size: 11px; color: #c2d9ce; }
  &__error { margin-top: 14px; padding-top: 12px; border-top: 1px solid #497461; color: #ffe2ad; font-size: 12px; }
}
.refresh { padding: 10px 4px 10px 12px; min-height: 44px; background: transparent; color: #e0f3e8; font-size: 12px; &[disabled] { color: #c2d9ce; background: transparent; } }
.stats { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 20px; gap: 20px 16px; }
.stat { min-width: 0;
  &__label { display: flex; align-items: center; gap: 7px; color: #e5f1eb; font-size: 13px; }
  &__dot { width: 6px; height: 6px; border-radius: 50%; background: #8be4af; &--amber { background: #ffd184; } &--red { background: #ffaca4; } &--blue { background: #a6d0ff; } }
  &__value { margin-top: 6px; font-size: 30px; font-weight: 600; line-height: 1.3; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
  &__unit { margin-left: 6px; font-size: 12px; font-weight: 400; color: #c2d9ce; }
  &__note { margin-top: 4px; font-size: 11px; color: #c2d9ce; }
}
.draft { display: flex; justify-content: space-between; gap: 8px; width: 100%; min-height: 48px; padding: 14px; margin-top: 14px; background: #fff1d9; color: #855418; border-radius: 12px; font-size: 13px; }
.primary-action { display: flex; align-items: center; gap: 14px; width: 100%; padding: 20px 16px; background: #e0eee6; border: 1px solid #cde2d6; border-radius: 18px; color: #174e3b;
  &__icon { display: flex; align-items: center; justify-content: center; width: 42px; height: 42px; flex-shrink: 0; background: #176b4c; color: #fff; border-radius: 13px; font-size: 30px; }
  &__body { flex: 1; min-width: 0; }
  &__title { font-size: 18px; font-weight: 700; }
  &__desc { margin-top: 4px; font-size: 12px; color: #486b58; }
  &__arrow { font-size: 22px; }
  &:active { background: #cfe3d7; }
}
.menu { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 12px;
  &__item { padding: 16px; background: #fff; border: 1px solid #e4ece7; border-radius: 18px; &:active { background: #eaf3ed; } }
  &__top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
  &__icon { width: 25px; height: 25px; padding: 8px; background: #eef5f0; border-radius: 12px; }
  &__arrow { color: #76897d; font-size: 18px; }
  &__title { font-size: 16px; font-weight: 600; color: #243c2e; }
  &__desc { margin-top: 6px; color: #63766a; font-size: 11px; line-height: 1.6; }
}
.outbound { overflow: hidden; background: #fff; border: 1px solid #e4ece7; border-radius: 18px;
  &__item { display: flex; align-items: center; gap: 12px; width: 100%; padding: 18px 16px; box-sizing: border-box; border-radius: 0; background: transparent; &:not(:last-child) { border-bottom: 1px solid #edf1ee; } &:active { background: #eaf3ed; } }
  &__icon { flex-shrink: 0; width: 25px; height: 25px; padding: 10px; background: #eef5f0; border-radius: 13px; }
  &__body { flex: 1; min-width: 0; }
  &__title { font-size: 16px; font-weight: 600; color: #243c2e; }
  &__desc { margin-top: 5px; color: #63766a; font-size: 12px; }
  &__status { flex-shrink: 0; padding: 3px 6px; color: #687b6f; background: #f3f6f4; border-radius: 5px; font-size: 10px; }
  &__arrow { color: #76897d; font-size: 22px; }
}
.home__footer { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: 22px; font-size: 11px; color: #687b6f; }
.home__logout { min-height: 44px; padding: 12px 0 12px 12px; color: #687b6f; font-size: 12px; background: transparent; }
</style>
