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
        <view class="line"><text class="line__label">提货点</text><text class="line__value">{{ task.pickupAddress || '—' }}</text></view>
        <view class="line"><text class="line__label">货物</text><text class="line__value">{{ cargoText }}</text></view>
        <view class="line"><text class="line__label">时间窗</text><text class="line__value">{{ timeRange }}</text></view>
        <view v-if="hasStops" class="line">
          <text class="line__label">待提</text>
          <text class="line__value line__value--warn">还剩 {{ pendingStopCount }} 家没提</text>
        </view>
        <view v-if="task.cancelReason" class="line"><text class="line__label">取消原因</text><text class="line__value">{{ task.cancelReason }}</text></view>
      </view>

      <view v-if="task.scopeNote" class="tip tip--note">{{ task.scopeNote }}</view>

      <view v-if="task.status === 1" class="actions">
        <button class="btn btn--primary" :loading="busy" @click="onAccept">接单</button>
      </view>

      <!-- 停靠点：一车提多家，各自处理、各自推进 -->
      <view v-if="hasStops" class="card">
        <view class="card__title">停靠点（还剩 {{ pendingStopCount }} 家没提）</view>
        <view v-for="stop in task.stops" :key="stop.id" class="stop" :class="{ 'stop--done': stop.status === 2, 'stop--cancel': stop.status === 3 }">
          <view class="stop__head">
            <text class="stop__name">第 {{ stop.stopNo }} 站 · {{ stop.payeeName || '未署出售者' }}</text>
            <text class="stop__status">{{ stop.statusName }}</text>
          </view>
          <view class="node__line">{{ stop.address }}</view>
          <view v-if="stop.cargoName" class="node__line">
            货物：{{ stop.cargoName }}{{ stop.estimatedQuantity ? ` · 约 ${stop.estimatedQuantity}${stop.quantityUnit || ''}` : '' }}
          </view>
          <view class="chips chips--tight">
            <text
              v-for="type in STOP_SCOPED_TYPES"
              :key="type"
              :class="['chip', 'chip--mini', { 'chip--active': stopNodeReported(stop, type) }]"
            >{{ nodeTypeName(type) }}</text>
          </view>
          <view v-if="stop.cancelReason" class="node__line node__line--warn">已取消：{{ stop.cancelReason }}</view>
        </view>
      </view>

      <!-- 按停靠点上报（提货相关三类） -->
      <view v-if="canReport && hasStops" class="card">
        <view class="card__title">按停靠点上报</view>
        <view class="tip">选一家停靠点，再报走到哪一步。**每家各自的进度互不相串**。</view>
        <view class="chips">
          <text
            v-for="stop in reportableStops"
            :key="stop.id"
            :class="['chip', { 'chip--active': stopId === stop.id }]"
            @click="stopId = stop.id"
          >{{ stop.payeeName || `第 ${stop.stopNo} 站` }}</text>
        </view>
        <view class="chips">
          <text
            v-for="type in STOP_SCOPED_TYPES"
            :key="type"
            :class="['chip', { 'chip--active': nodeType === type }]"
            @click="nodeType = type"
          >{{ nodeTypeName(type) }}</text>
        </view>
        <view v-if="nodePhotoRequired" class="tip tip--warn">这一步必须有照片（货物流凭证）。</view>
        <view class="photos">
          <view v-for="(path, idx) in photoPaths" :key="idx" class="photos__item">
            <image :src="path" mode="aspectFill" class="photos__img" />
            <view class="photos__del" @click="removePhoto(idx)">×</view>
          </view>
          <view class="photos__add" @click="addPhotos">+ 拍照</view>
        </view>
        <button class="btn btn--primary" :loading="busy" @click="onReport">上报节点</button>
      </view>

      <!-- 整趟收尾：到达场站 / 卸货完成（不属于任何单个停靠点） -->
      <view v-if="canReport && hasStops" class="card">
        <view class="card__title">整趟收尾</view>
        <view class="tip">回到场站 / 卸完货报这里。它是整趟活的收尾，**不归到任何一家停靠点**。</view>
        <view class="chips">
          <text
            v-for="type in TASK_SCOPED_TYPES"
            :key="type"
            :class="['chip', { 'chip--active': taskNodeType === type }]"
            @click="taskNodeType = type"
          >{{ nodeTypeName(type) }}</text>
        </view>
        <view v-if="taskNodePhotoRequired" class="tip tip--warn">这一步必须有照片（货物流凭证）。</view>
        <view class="photos">
          <view v-for="(path, idx) in taskPhotoPaths" :key="idx" class="photos__item">
            <image :src="path" mode="aspectFill" class="photos__img" />
            <view class="photos__del" @click="removeTaskPhoto(idx)">×</view>
          </view>
          <view class="photos__add" @click="addTaskPhotos">+ 拍照</view>
        </view>
        <button class="btn btn--primary" :loading="busy" @click="onReportTaskNode">上报收尾节点</button>
      </view>

      <!-- 单点 / 历史口径：没有停靠点时五类都可报 -->
      <view v-if="canReport && !hasStops" class="card">
        <view class="card__title">上报运输节点</view>
        <view class="tip">这趟任务没有停靠点（单点 / 历史口径），五类节点都可上报。</view>
        <view class="chips">
          <text
            v-for="type in ALL_NODE_TYPES"
            :key="type"
            :class="['chip', { 'chip--active': nodeType === type }]"
            @click="nodeType = type"
          >{{ nodeTypeName(type) }}</text>
        </view>
        <view v-if="nodePhotoRequired" class="tip tip--warn">这一步必须有照片（货物流凭证）。</view>
        <view class="photos">
          <view v-for="(path, idx) in photoPaths" :key="idx" class="photos__item">
            <image :src="path" mode="aspectFill" class="photos__img" />
            <view class="photos__del" @click="removePhoto(idx)">×</view>
          </view>
          <view class="photos__add" @click="addPhotos">+ 拍照</view>
        </view>
        <button class="btn btn--primary" :loading="busy" @click="onReport">上报节点</button>
      </view>

      <!-- 上报异常（独立标记，不改任务状态） -->
      <view v-if="canReport" class="card card--abnormal">
        <view class="card__title">上报异常</view>
        <view class="tip">异常是**独立标记**，不改变任务状态；调度会根据它决定是否改派。</view>
        <view v-if="hasStops" class="chips">
          <text
            :class="['chip', { 'chip--active': abnormalStopId === undefined }]"
            @click="abnormalStopId = undefined"
          >路上 / 不带停靠点</text>
          <text
            v-for="stop in reportableStops"
            :key="stop.id"
            :class="['chip', { 'chip--active': abnormalStopId === stop.id }]"
            @click="abnormalStopId = stop.id"
          >{{ stop.payeeName || `第 ${stop.stopNo} 站` }}</text>
        </view>
        <view class="chips">
          <text
            v-for="item in abnormalTypeOptions"
            :key="item.value"
            :class="['chip', { 'chip--danger': abnormalType === item.value }]"
            @click="abnormalType = item.value"
          >{{ item.label }}</text>
        </view>
        <textarea v-model="abnormalReason" class="textarea" placeholder="说明发生了什么（必填）" maxlength="200" />
        <view class="photos">
          <view v-for="(path, idx) in abnormalPhotoPaths" :key="idx" class="photos__item">
            <image :src="path" mode="aspectFill" class="photos__img" />
            <view class="photos__del" @click="removeAbnormalPhoto(idx)">×</view>
          </view>
          <view class="photos__add" @click="addAbnormalPhotos">+ 拍照</view>
        </view>
        <button class="btn btn--danger" :loading="busy" @click="onReportAbnormal">上报异常</button>
      </view>

      <view class="card">
        <view class="card__title">运输时间线</view>
        <view v-if="!task.nodes || !task.nodes.length" class="empty empty--inline">还没有上报任何节点</view>
        <view v-else>
          <view v-for="node in task.nodes" :key="node.id" class="node">
            <view class="node__head">
              <text class="node__name">{{ node.nodeTypeName || '异常' }}</text>
              <text class="node__time">发生 {{ fmt(node.nodeTime) }}</text>
            </view>
            <view v-if="node.abnormalType" class="node__line node__line--warn">
              异常 · {{ node.abnormalTypeName }}｜{{ node.abnormalReason || '—' }}
            </view>
            <view v-if="node.abnormalType" class="node__line node__line--warn">
              {{ node.abnormalResolved ? `已解决：${node.abnormalResolvedName || '—'} ${fmt(node.abnormalResolvedAt)}` : '待调度处理' }}
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
        <view v-if="brokenPointText" class="tip tip--warn">{{ brokenPointText }}</view>
      </view>

      <view class="card card--handoff">
        <view class="card__title">现场为出售者建档</view>
        <view class="tip">
          现场没有收货员时（司机上门），准入四步在这一端完成：实名 → 收方入驻 → 框架协议 → 首次授权。
          **确认一律由出售者本人做**，这里只把链接交给他。
        </view>
        <button class="btn btn--ghost" @click="goOnboarding">为出售者建档</button>
      </view>

      <view v-if="task.status === 4" class="tip tip--bottom">
        这趟活已完成；如发现凭证缺失，请联系调度在后台补录。
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { DriverStopVO, DriverTaskVO, getTask, acceptTask, reportNode, reportAbnormal } from '@/api/task'
import { chooseImage, pathToDataUrl, uploadImage } from '@/utils/upload'
import { saveDraft } from '@/utils/draft'

const task = ref<DriverTaskVO>()
const taskId = ref<number>(0)
const busy = ref(false)

// 五类节点（与后端 LogisticsTransportNodeTypeEnum 对齐）
const ALL_NODE_TYPES = [1, 2, 3, 4, 5]
// 按停靠点上报的三类；整趟收尾的两类（V5 #72）
const STOP_SCOPED_TYPES = [1, 2, 3]
const TASK_SCOPED_TYPES = [4, 5]
// 照片必填的两类：交接完成与卸货完成
const NODE_PHOTO_REQUIRED_TYPES = [2, 5]
const NODE_TYPE_NAME: Record<number, string> = {
  1: '到达提货点',
  2: '交接完成',
  3: '起运',
  4: '到达场站',
  5: '卸货完成'
}
// 八类异常（与后端 LogisticsTransportAbnormalTypeEnum 对齐）
const abnormalTypeOptions = [
  { value: 1, label: '车辆故障' },
  { value: 2, label: '交通事故' },
  { value: 3, label: '天气延误' },
  { value: 4, label: '道路封闭' },
  { value: 5, label: '货物损坏' },
  { value: 6, label: '对方不在' },
  { value: 7, label: '地址错误' },
  { value: 8, label: '其他' }
]

// 按停靠点上报：选中的停靠点与节点类型
const stopId = ref<number>()
const nodeType = ref<number>(1)
// 整趟收尾
const taskNodeType = ref<number>(4)
const taskPhotoPaths = ref<string[]>([])
// 单点口径
const photoPaths = ref<string[]>([])
// 异常
const abnormalStopId = ref<number>()
const abnormalType = ref<number>(1)
const abnormalReason = ref('')
const abnormalPhotoPaths = ref<string[]>([])

const hasStops = computed(() => (task.value?.stops?.length || 0) > 0)
const reportableStops = computed<DriverStopVO[]>(() =>
  (task.value?.stops || []).filter((stop) => stop.status !== 2 && stop.status !== 3)
)
const pendingStopCount = computed(() => task.value?.pendingStopCount ?? reportableStops.value.length)
const nodePhotoRequired = computed(() => NODE_PHOTO_REQUIRED_TYPES.includes(nodeType.value))
const taskNodePhotoRequired = computed(() => NODE_PHOTO_REQUIRED_TYPES.includes(taskNodeType.value))
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
const brokenPointText = computed(() => {
  const parts: string[] = []
  if (task.value?.missingNodeNames?.length) {
    parts.push(`尚未上报：${task.value.missingNodeNames.join('、')}`)
  }
  if (task.value?.missingEvidenceNames?.length) {
    parts.push(`缺凭证：${task.value.missingEvidenceNames.join('、')}`)
  }
  return parts.length ? `断点 · ${parts.join('；')}` : ''
})

function nodeTypeName(type: number) {
  return NODE_TYPE_NAME[type] || '节点'
}

function stopNodeReported(stop: DriverStopVO, type: number) {
  return (stop.nodes || []).some((node) => node.nodeType === type)
}

function fmt(ms?: number) {
  if (!ms) return '—'
  const d = new Date(ms)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load() {
  task.value = await getTask(taskId.value)
  // 默认选中第一家还没提的停靠点
  if (!stopId.value || !reportableStops.value.some((s) => s.id === stopId.value)) {
    stopId.value = reportableStops.value[0]?.id
  }
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
async function addTaskPhotos() {
  try {
    const paths = await chooseImage(3)
    taskPhotoPaths.value = [...taskPhotoPaths.value, ...paths].slice(0, 6)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '选择照片失败', icon: 'none' })
  }
}
function removeTaskPhoto(idx: number) {
  taskPhotoPaths.value.splice(idx, 1)
}
async function addAbnormalPhotos() {
  try {
    const paths = await chooseImage(3)
    abnormalPhotoPaths.value = [...abnormalPhotoPaths.value, ...paths].slice(0, 6)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '选择照片失败', icon: 'none' })
  }
}
function removeAbnormalPhoto(idx: number) {
  abnormalPhotoPaths.value.splice(idx, 1)
}

function preview(url: string) {
  uni.previewImage({ urls: task.value?.nodes?.flatMap((n) => n.photos || []) || [url] })
}

function goOnboarding() {
  uni.navigateTo({ url: '/pages/onboarding/index' })
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

async function uploadPhotos(paths: string[]): Promise<string[]> {
  const photos: string[] = []
  for (const path of paths) {
    photos.push(await uploadImage(path))
  }
  return photos
}

async function saveNodeDraft(clientRequestId: string, nodeTime: number, nodeTypeValue: number,
                            stopIdValue: number | undefined, paths: string[], location: object) {
  const drafts = []
  for (let i = 0; i < paths.length; i++) {
    drafts.push({ key: `photo-${i}`, dataUrl: await pathToDataUrl(paths[i]) })
  }
  const stop = (task.value?.stops || []).find((s) => s.id === stopIdValue)
  saveDraft({
    clientRequestId,
    createdAt: nodeTime,
    kind: 'NODE',
    summary: `${task.value?.taskNo || ''} ${stop?.payeeName ? stop.payeeName + '·' : ''}${nodeTypeName(nodeTypeValue)}`,
    payload: {
      taskId: taskId.value,
      stopId: stopIdValue,
      nodeType: nodeTypeValue,
      nodeTime,
      clientRequestId,
      ...location
    },
    photos: drafts,
    photoUrls: {}
  })
}

async function doReportNode(nodeTypeValue: number, stopIdValue: number | undefined, paths: string[]) {
  if (NODE_PHOTO_REQUIRED_TYPES.includes(nodeTypeValue) && paths.length === 0) {
    uni.showToast({ title: '这一步必须有照片', icon: 'none' })
    return
  }
  busy.value = true
  const nodeTime = Date.now()
  const clientRequestId = `driver-${nodeTime}-${Math.floor(Math.random() * 1e6)}`
  const location = await getLocationSnapshot()
  try {
    await reportNode({
      taskId: taskId.value,
      stopId: stopIdValue,
      nodeType: nodeTypeValue,
      nodeTime,
      photos: await uploadPhotos(paths),
      clientRequestId,
      ...location
    })
    uni.showToast({ title: '已上报节点', icon: 'none' })
    await load()
  } catch (e) {
    await saveNodeDraft(clientRequestId, nodeTime, nodeTypeValue, stopIdValue, paths, location)
    uni.showToast({ title: '网络不通，已暂存本机，稍后补传', icon: 'none' })
  } finally {
    busy.value = false
  }
}

async function onReport() {
  await doReportNode(nodeType.value, hasStops.value ? stopId.value : undefined, photoPaths.value)
  photoPaths.value = []
}

async function onReportTaskNode() {
  await doReportNode(taskNodeType.value, undefined, taskPhotoPaths.value)
  taskPhotoPaths.value = []
}

async function onReportAbnormal() {
  if (!abnormalReason.value.trim()) {
    uni.showToast({ title: '请填异常说明', icon: 'none' })
    return
  }
  busy.value = true
  const nodeTime = Date.now()
  const clientRequestId = `driver-abn-${nodeTime}-${Math.floor(Math.random() * 1e6)}`
  const location = await getLocationSnapshot()
  const abnormalTypeValue = abnormalType.value
  const reason = abnormalReason.value.trim()
  const stopIdValue = abnormalStopId.value
  try {
    await reportAbnormal({
      taskId: taskId.value,
      stopId: stopIdValue,
      abnormalType: abnormalTypeValue,
      abnormalReason: reason,
      nodeTime,
      photos: await uploadPhotos(abnormalPhotoPaths.value),
      clientRequestId,
      ...location
    })
    abnormalPhotoPaths.value = []
    abnormalReason.value = ''
    uni.showToast({ title: '已上报异常', icon: 'none' })
    await load()
  } catch (e) {
    const drafts = []
    for (let i = 0; i < abnormalPhotoPaths.value.length; i++) {
      drafts.push({ key: `photo-${i}`, dataUrl: await pathToDataUrl(abnormalPhotoPaths.value[i]) })
    }
    saveDraft({
      clientRequestId,
      createdAt: nodeTime,
      kind: 'ABNORMAL',
      summary: `${task.value?.taskNo || ''} 异常·${abnormalTypeOptions.find((t) => t.value === abnormalTypeValue)?.label || ''}`,
      payload: {
        taskId: taskId.value,
        stopId: stopIdValue,
        abnormalType: abnormalTypeValue,
        abnormalReason: reason,
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

    &--warn {
      color: #b45309;
    }
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

.stop {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f1f5f9;

  &--done {
    opacity: 0.7;
  }

  &--cancel {
    opacity: 0.5;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__name {
    font-size: 28rpx;
    font-weight: 600;
  }

  &__status {
    color: #6b7a72;
    font-size: 24rpx;
  }
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

    &--warn {
      color: #b45309;
    }
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

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin: 16rpx 0;

  &--tight {
    gap: 8rpx;
    margin: 8rpx 0 0;
  }
}

.chip {
  padding: 10rpx 24rpx;
  background-color: #f3f4f6;
  border: 1rpx solid #e5e7eb;
  border-radius: 999rpx;
  color: #4b5563;
  font-size: 24rpx;

  &--mini {
    padding: 4rpx 16rpx;
    font-size: 22rpx;
  }

  &--active {
    background-color: #16a34a;
    border-color: #16a34a;
    color: #fff;
  }

  &--danger {
    background-color: #dc2626;
    border-color: #dc2626;
    color: #fff;
  }
}

.textarea {
  width: 100%;
  min-height: 120rpx;
  margin: 8rpx 0 4rpx;
  box-sizing: border-box;
  padding: 16rpx;
  background-color: #f9fafb;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  font-size: 26rpx;
}

.card--abnormal {
  border: 1rpx solid #fecaca;
}

.btn {
  &--primary {
    background-color: #16a34a;
    color: #fff;
    border-radius: 12rpx;
  }

  &--danger {
    background-color: #dc2626;
    color: #fff;
    border-radius: 12rpx;
  }

  &--ghost {
    background-color: transparent;
    border: 1rpx solid #e5e7eb;
    color: #6b7a72;
    font-size: 26rpx;
  }
}

.tip {
  color: #8a919f;
  font-size: 24rpx;
  line-height: 1.6;

  &--warn {
    color: #b45309;
  }

  &--note {
    margin-bottom: 20rpx;
    padding: 16rpx 20rpx;
    background-color: #eff6ff;
    border-radius: 12rpx;
    color: #1d4ed8;
  }

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
