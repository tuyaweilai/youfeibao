<template>
  <view class="page">
    <view v-if="!task" class="empty">加载中…</view>
    <template v-else>
      <view class="card">
        <view class="card__top">
          <text class="card__no">{{ task.taskNo }}</text>
          <text class="tag">{{ task.statusName }}</text>
        </view>
        <view class="line"><text class="line__label">车牌号</text><text class="line__value">{{ task.plateNo || '—' }}</text></view>
        <view class="line"><text class="line__label">出发地</text><text class="line__value">{{ task.departureAddress || '—' }}</text></view>
        <view class="line"><text class="line__label">提货点</text><text class="line__value">{{ task.pickupAddress }}</text></view>
        <view class="line"><text class="line__label">联系人</text><text class="line__value">{{ task.pickupContactName || '—' }}</text></view>
        <view class="line">
          <text class="line__label">电话</text>
          <text class="line__value" @click="callContact">{{ task.pickupContactPhone || '—' }}</text>
        </view>
        <view class="line"><text class="line__label">货物</text><text class="line__value">{{ cargoText }}</text></view>
        <view class="line"><text class="line__label">时间窗</text><text class="line__value">{{ timeRange }}</text></view>
        <view v-if="task.cancelReason" class="line"><text class="line__label">取消原因</text><text class="line__value">{{ task.cancelReason }}</text></view>
      </view>

      <view v-if="task.status === 1" class="actions">
        <button class="btn btn--primary" :loading="busy" @click="onAccept">接单</button>
      </view>

      <view class="card">
        <view class="card__title">运输时间线</view>
        <view v-if="!task.nodes || !task.nodes.length" class="empty empty--inline">还没有上报任何节点</view>
        <view v-else>
          <view v-for="node in task.nodes" :key="node.id" class="node">
            <view class="node__head">
              <text class="node__name">{{ node.nodeTypeName }}</text>
              <text class="node__time">发生 {{ fmt(node.nodeTime) }}</text>
            </view>
            <view class="node__line">上报 {{ fmt(node.reportTime) }} · {{ node.operatorName || '—' }}</view>
            <view class="node__line">{{ node.location || '未记录位置' }}</view>
            <view v-if="node.photos && node.photos.length" class="node__photos">
              <image
                v-for="(url, idx) in node.photos"
                :key="idx"
                :src="url"
                class="node__photo"
                mode="aspectFill"
                @click="preview(url)"
              />
            </view>
          </view>
        </view>
      </view>

      <view v-if="canReport" class="card">
        <view class="card__title">上报起运</view>
        <view class="tip">
          上报时记录**发生时间**与当时的位置快照；照片是这趟运输的凭证。钱不在这里算——现场不产生金额。
        </view>
        <view class="photos">
          <view v-for="(path, idx) in photoPaths" :key="idx" class="photos__item">
            <image :src="path" mode="aspectFill" class="photos__img" />
            <view class="photos__del" @click="removePhoto(idx)">×</view>
          </view>
          <view class="photos__add" @click="addPhotos">+ 拍照</view>
        </view>
        <button class="btn btn--primary" :loading="busy" @click="onReport">上报起运</button>
      </view>

      <view v-if="task.status === 3 || task.status === 4" class="tip tip--bottom">
        送达与卸货完成由后续版本支持；本期只开放「起运」。
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { DriverTaskVO, getTask, acceptTask, reportNode } from '@/api/task'
import { chooseImage, pathToDataUrl, uploadImage } from '@/utils/upload'
import { saveDraft } from '@/utils/draft'

const task = ref<DriverTaskVO>()
const taskId = ref<number>(0)
const busy = ref(false)
const photoPaths = ref<string[]>([])

const canReport = computed(() => !!task.value && (task.value.status === 2 || task.value.status === 3))
const cargoText = computed(() => {
  if (!task.value?.cargoName) return '以现场交接为准'
  const qty = task.value.estimatedQuantity ? ` · 约 ${task.value.estimatedQuantity}${task.value.quantityUnit || ''}` : ''
  return task.value.cargoName + qty
})
const timeRange = computed(() => {
  if (!task.value?.expectedStartTime && !task.value?.expectedEndTime) return '未指定'
  return `${fmt(task.value?.expectedStartTime)} ~ ${fmt(task.value?.expectedEndTime)}`
})

function fmt(ms?: number) {
  if (!ms) return '—'
  const d = new Date(ms)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load() {
  task.value = await getTask(taskId.value)
}

async function onAccept() {
  busy.value = true
  try {
    await acceptTask(taskId.value)
    uni.showToast({ title: '已接单', icon: 'none' })
    await load()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '接单失败', icon: 'none' })
  } finally {
    busy.value = false
  }
}

async function addPhotos() {
  try {
    const paths = await chooseImage(3)
    photoPaths.value = [...photoPaths.value, ...paths].slice(0, 6)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '选择照片失败', icon: 'none' })
  }
}

function removePhoto(idx: number) {
  photoPaths.value.splice(idx, 1)
}

function preview(url: string) {
  uni.previewImage({ urls: task.value?.nodes?.flatMap((n) => n.photos || []) || [url] })
}

function callContact() {
  if (task.value?.pickupContactPhone) {
    uni.makePhoneCall({ phoneNumber: task.value.pickupContactPhone })
  }
}

/**
 * 位置快照：上报时取一次，拿不到就不带——**不伪造 GPS，也不因为拿不到定位就拦住现场作业**
 *（ADR 0031 与研究结论：缺轨迹不是拒收的理由）。
 */
function getLocationSnapshot(): Promise<{ latitude?: number; longitude?: number }> {
  return new Promise((resolve) => {
    uni.getLocation({
      type: 'gcj02',
      success: (res) => resolve({ latitude: res.latitude, longitude: res.longitude }),
      fail: () => resolve({})
    })
  })
}

async function onReport() {
  busy.value = true
  // 发生时间取点击这一刻；上报时间由服务端落，两者分开留痕
  const nodeTime = Date.now()
  const clientRequestId = `driver-${nodeTime}-${Math.floor(Math.random() * 1e6)}`
  const location = await getLocationSnapshot()
  try {
    const photos: string[] = []
    for (const path of photoPaths.value) {
      photos.push(await uploadImage(path))
    }
    await reportNode({
      taskId: taskId.value,
      nodeType: 3, // 起运（本期只开放这一类）
      nodeTime,
      photos,
      clientRequestId,
      ...location
    })
    photoPaths.value = []
    uni.showToast({ title: '已上报起运', icon: 'none' })
    await load()
  } catch (e) {
    // 弱网：把照片一起暂存到本机（base64），恢复后在「待补传」里重传
    const drafts = []
    for (let i = 0; i < photoPaths.value.length; i++) {
      drafts.push({ key: `photo-${i}`, dataUrl: await pathToDataUrl(photoPaths.value[i]) })
    }
    saveDraft({
      clientRequestId,
      createdAt: nodeTime,
      summary: `${task.value?.taskNo || ''} 起运`,
      payload: {
        taskId: taskId.value,
        nodeType: 3,
        nodeTime,
        clientRequestId,
        ...location
      },
      photos: drafts,
      photoUrls: {}
    })
    uni.showToast({ title: '网络不通，已暂存本机，稍后补传', icon: 'none' })
  } finally {
    busy.value = false
  }
}

onLoad((options) => {
  taskId.value = Number(options?.id || 0)
  load()
})
</script>

<style scoped lang="scss">
.page {
  padding: 24rpx 24rpx 80rpx;
}

.card {
  margin-bottom: 24rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 16rpx;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16rpx;
  }

  &__no {
    font-size: 28rpx;
    font-weight: 600;
  }

  &__title {
    margin-bottom: 16rpx;
    font-size: 30rpx;
    font-weight: 600;
  }
}

.line {
  display: flex;
  margin-top: 10rpx;
  font-size: 26rpx;

  &__label {
    width: 130rpx;
    color: #8a919f;
  }

  &__value {
    flex: 1;
  }
}

.tag {
  padding: 4rpx 16rpx;
  background-color: #dcfce7;
  border-radius: 999rpx;
  color: #15803d;
  font-size: 22rpx;
}

.actions {
  margin-bottom: 24rpx;
}

.node {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f1f5f9;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__name {
    font-size: 28rpx;
    font-weight: 600;
  }

  &__time {
    color: #6b7a72;
    font-size: 24rpx;
  }

  &__line {
    margin-top: 6rpx;
    color: #6b7a72;
    font-size: 24rpx;
  }

  &__photos {
    display: flex;
    gap: 12rpx;
    margin-top: 12rpx;
  }

  &__photo {
    width: 140rpx;
    height: 140rpx;
    border-radius: 12rpx;
  }
}

.photos {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin: 20rpx 0;

  &__item {
    position: relative;
  }

  &__img {
    width: 160rpx;
    height: 160rpx;
    border-radius: 12rpx;
  }

  &__del {
    position: absolute;
    right: -10rpx;
    top: -10rpx;
    width: 40rpx;
    height: 40rpx;
    background-color: rgba(0, 0, 0, 0.6);
    border-radius: 50%;
    color: #fff;
    font-size: 28rpx;
    line-height: 40rpx;
    text-align: center;
  }

  &__add {
    width: 160rpx;
    height: 160rpx;
    border: 1rpx dashed #cbd5e1;
    border-radius: 12rpx;
    color: #8a919f;
    font-size: 26rpx;
    line-height: 160rpx;
    text-align: center;
  }
}

.btn {
  &--primary {
    background-color: #16a34a;
    color: #fff;
    border-radius: 12rpx;
  }
}

.tip {
  color: #8a919f;
  font-size: 24rpx;
  line-height: 1.6;

  &--bottom {
    margin-top: 16rpx;
    text-align: center;
  }
}

.empty {
  padding: 80rpx 0;
  text-align: center;
  color: #8a919f;
  font-size: 26rpx;

  &--inline {
    padding: 32rpx 0;
  }
}
</style>
