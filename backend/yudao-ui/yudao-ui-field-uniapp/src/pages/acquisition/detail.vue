<template>
  <view v-if="acquisition" class="page">
    <view class="card status">
      <view class="status__label">当前进度</view>
      <view class="status__name">{{ acquisition.statusName || statusFallback }}</view>
      <view class="status__hint">{{ statusHint }}</view>
    </view>

    <view class="card">
      <view class="card__title">收购确认书</view>
      <view class="kv"><text class="kv__k">收购单号</text><text>{{ acquisition.acquisitionNo }}</text></view>
      <view class="kv"><text class="kv__k">出售者</text><text>{{ acquisition.sellerName }} {{ acquisition.sellerMobile }}</text></view>
      <view class="kv"><text class="kv__k">品类</text><text>{{ acquisition.categoryName }}</text></view>
      <view class="kv"><text class="kv__k">规格</text><text>{{ acquisition.specification || '-' }}</text></view>
      <view class="kv"><text class="kv__k">数量</text><text>{{ acquisition.quantity }} {{ acquisition.unit }}</text></view>
      <view class="kv"><text class="kv__k">单价</text><text>{{ acquisition.unitPrice }} 元</text></view>
      <view class="kv"><text class="kv__k">金额</text><text>{{ acquisition.amount }} 元</text></view>
      <view class="kv"><text class="kv__k">净重</text><text>{{ acquisition.netWeight ?? '-' }}</text></view>
      <view class="kv"><text class="kv__k">磅单号</text><text>{{ acquisition.weightTicketNo || '-' }}</text></view>
      <view class="kv"><text class="kv__k">交易地点</text><text>{{ acquisition.tradeAddress || '-' }}</text></view>
      <view class="kv"><text class="kv__k">结算方式</text><text>{{ acquisition.settlementMethod || '-' }}</text></view>
      <view class="kv"><text class="kv__k">税收分类编码</text><text>{{ acquisition.mergedCode || '-' }}</text></view>
      <view v-if="acquisition.invoicePartnerOrderId" class="kv">
        <text class="kv__k">开票单号</text><text>{{ acquisition.invoicePartnerOrderId }}</text>
      </view>
    </view>

    <view class="actions">
      <button class="btn btn--primary" :loading="exporting" @click="onExport">导出确认书（Excel）</button>
      <button class="btn btn--ghost" @click="onPrint">打印本页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAcquisition, AcquisitionVO } from '@/api/acquisition'
import { downloadWithAuth } from '@/utils/download'

defineOptions({ name: 'FieldAcquisitionDetail' })

const acquisition = ref<AcquisitionVO | null>(null)
const exporting = ref(false)

const statusFallback = computed(() => '未知')
const statusHint = computed(() => {
  switch (acquisition.value?.status) {
    case 0:
      return '已登记，等待发起开票'
    case 1:
      return '待付款：预开票成功后由付方支付'
    case 2:
      return '已付款，等待开出并上传发票'
    case 3:
      return '已开票：可在「我的收购单」看到发票号'
    default:
      return ''
  }
})

onLoad((options) => {
  const id = Number(options?.id)
  if (id) {
    load(id)
  }
})

async function load(id: number) {
  try {
    acquisition.value = await getAcquisition(id)
  } catch (e) {
    uni.showModal({ title: '加载失败', content: (e as Error).message, showCancel: false })
  }
}

async function onExport() {
  if (!acquisition.value?.id) return
  exporting.value = true
  try {
    await downloadWithAuth(
      `/icbc/acquisition/confirmation/export?id=${acquisition.value.id}`,
      `收购确认书_${acquisition.value.acquisitionNo}.xlsx`
    )
  } catch (e) {
    uni.showModal({ title: '导出失败', content: (e as Error).message, showCancel: false })
  } finally {
    exporting.value = false
  }
}

function onPrint() {
  // #ifdef H5
  window.print()
  // #endif
  // #ifndef H5
  uni.showToast({ title: '请用系统分享导出后打印', icon: 'none' })
  // #endif
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
}

.card {
  padding: 32rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__title {
    margin-bottom: 24rpx;
    font-size: 32rpx;
    font-weight: 600;
  }
}

.status {
  &__label {
    color: $field-text-secondary;
    font-size: 26rpx;
  }

  &__name {
    margin-top: 8rpx;
    font-size: 40rpx;
    font-weight: 700;
  }

  &__hint {
    margin-top: 12rpx;
    color: $field-text-secondary;
    line-height: 1.6;
  }
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
  gap: 24rpx;

  &__k {
    color: $field-text-secondary;
    flex-shrink: 0;
  }
}

.actions {
  display: flex;
  gap: 16rpx;
}

.btn {
  flex: 1;

  &--primary {
    color: #ffffff;
    background-color: $field-primary;
  }

  &--ghost {
    color: $field-primary;
    background-color: #ffffff;
    border: 1rpx solid $field-primary;
  }
}
</style>
