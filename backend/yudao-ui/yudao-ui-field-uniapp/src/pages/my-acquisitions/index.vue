<template>
  <view class="page">
    <view v-if="!loading && list.length === 0" class="empty">还没有登记过收购单</view>

    <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item.id)">
      <view class="card__top">
        <text class="card__no">{{ item.acquisitionNo }}</text>
        <text class="tag" :class="{ 'tag--abnormal': item.abnormal }">{{ item.statusName }}</text>
      </view>
      <view class="card__meta">{{ item.sellerName }} · {{ item.categoryName }}</view>
      <view class="card__amount">{{ item.amount }} 元</view>
      <!-- 异常不替换档位：钱付了但票没开出来，列表上就要看得见（ADR 0021 / 0038） -->
      <view v-if="item.abnormal" class="card__abnormal">
        {{ (item.abnormalReasons || []).join('；') }}
      </view>
      <view v-else-if="item.statusNextStep" class="card__next">{{ item.statusNextStep }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getAcquisitionPage, AcquisitionVO } from '@/api/acquisition'

defineOptions({ name: 'FieldMyAcquisitions' })

const list = ref<AcquisitionVO[]>([])
const loading = ref(false)

onShow(() => {
  load()
})

async function load() {
  loading.value = true
  try {
    const page = await getAcquisitionPage({ pageNo: 1, pageSize: 50 })
    list.value = page.list || []
  } catch (e) {
    uni.showModal({ title: '加载失败', content: (e as Error).message, showCancel: false })
  } finally {
    loading.value = false
  }
}

function goDetail(id?: number) {
  if (id) {
    uni.navigateTo({ url: `/pages/acquisition/detail?id=${id}` })
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
}

.empty {
  margin-top: 120rpx;
  color: $field-text-secondary;
  text-align: center;
}

.card {
  padding: 32rpx;
  margin-bottom: 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__no {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__meta {
    margin-top: 12rpx;
    color: $field-text-secondary;
  }

  &__amount {
    margin-top: 12rpx;
    font-size: 34rpx;
    font-weight: 700;
  }

  &__next {
    margin-top: 12rpx;
    color: $field-text-secondary;
    font-size: 24rpx;
    line-height: 1.6;
  }

  &__abnormal {
    margin-top: 12rpx;
    padding: 12rpx 16rpx;
    color: #ad3b12;
    background-color: #fff3ec;
    border-radius: 10rpx;
    font-size: 24rpx;
    line-height: 1.6;
  }
}

.tag {
  padding: 4rpx 16rpx;
  color: $field-primary;
  background-color: #eef4ff;
  border-radius: 999rpx;
  font-size: 24rpx;

  &--abnormal {
    color: #ad3b12;
    background-color: #fff3ec;
  }
}
</style>
