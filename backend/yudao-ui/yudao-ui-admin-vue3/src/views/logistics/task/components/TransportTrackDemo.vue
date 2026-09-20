<template>
  <div class="track">
    <div class="track__head">
      <el-tag type="warning" effect="dark">演示数据</el-tag>
      <span class="track__title">运输轨迹（模拟）</span>
      <el-button
        v-if="track?.enabled && points.length"
        link
        type="primary"
        @click="togglePlay"
      >
        {{ playing ? '暂停' : '播放' }}
      </el-button>
    </div>

    <el-alert v-if="loadError" type="error" :closable="false" :title="loadError" />

    <template v-else-if="track && !track.enabled">
      <el-alert
        type="info"
        :closable="false"
        title="轨迹演示未开启"
        description="这是演示能力，默认关闭（logistics.demo.transport-track.enabled=false）。开启后这里会画一条模拟折线，真实凭证仍然只有节点照片与上报记录。"
      />
    </template>

    <template v-else-if="track">
      <el-alert type="warning" :closable="false" :title="track.note" class="mb-10px" />

      <svg v-if="polyline" class="track__canvas" viewBox="0 0 100 100" preserveAspectRatio="none">
        <!-- 模拟轨迹：虚线一眼区别于任何「真实路径」 -->
        <polyline :points="polyline" fill="none" stroke="#f59e0b" stroke-width="1" stroke-dasharray="3 2" />
        <!-- 锚点：真实上报过的节点位置 -->
        <circle
          v-for="(anchor, idx) in anchorDots"
          :key="'a' + idx"
          :cx="anchor.x"
          :cy="anchor.y"
          r="2"
          fill="#16a34a"
        />
        <!-- 当前回放位置 -->
        <circle v-if="currentDot" :cx="currentDot.x" :cy="currentDot.y" r="2.6" fill="#ef4444" />
      </svg>

      <div v-if="points.length" class="track__scrub">
        <el-slider v-model="cursor" :min="0" :max="points.length - 1" :show-tooltip="false" />
        <div class="track__meta">
          <span>{{ points.length }} 个模拟点</span>
          <span>当前：{{ fmt(points[cursor]?.time) }}</span>
          <span v-if="anchors.length">锚点 {{ anchors.length }} 个（来自真实上报）</span>
          <span v-else class="track__warn">无锚点：连起点都是编的</span>
        </div>
      </div>
      <el-empty v-else description="没有可画的点" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { LogisticsTrackDemoApi, TransportTrackDemoVO } from '@/api/logistics/demo'
import { formatDate } from '@/utils/formatTime'

/**
 * 运输轨迹演示件（#76 V9）。
 *
 * 三条边界写在代码里，也写在页面上：
 *  1. 只读：这个组件没有任何写入动作，后端那条接口也不落库；
 *  2. 一眼可辨：虚线 + 橙色 + 「演示数据」角标 + 说明文案，绝不与真实凭证混在一起；
 *  3. 可关：后端开关关掉时它退化成一句说明，不画任何东西。
 *
 * 不引地图服务（要 key 与报备）：用 SVG 把经纬度线性映射到 0–100 的画布上自绘。
 * 它证明不了任何事，所以也**不该**被当成地图用。
 */
const props = defineProps<{ taskId: number }>()

const track = ref<TransportTrackDemoVO>()
const loadError = ref('')
const cursor = ref(0)
const playing = ref(false)
let timer: ReturnType<typeof setInterval> | undefined

const points = computed(() => track.value?.points || [])
const anchors = computed(() => track.value?.anchors || [])

/** 把经纬度映射到 0–100 的画布（x=经度，y=纬度取反，因为屏幕上方是北） */
function project(rows: { latitude?: number; longitude?: number }[]) {
  const lats = rows.map((r) => r.latitude ?? 0)
  const lngs = rows.map((r) => r.longitude ?? 0)
  const minLat = Math.min(...lats)
  const maxLat = Math.max(...lats)
  const minLng = Math.min(...lngs)
  const maxLng = Math.max(...lngs)
  const spanLat = maxLat - minLat || 0.001
  const spanLng = maxLng - minLng || 0.001
  const pad = 6
  return (row: { latitude?: number; longitude?: number }) => ({
    x: pad + ((row.longitude ?? 0 - minLng) / spanLng) * (100 - pad * 2),
    y: pad + ((maxLat - (row.latitude ?? 0)) / spanLat) * (100 - pad * 2)
  })
}

const polyline = computed(() => {
  if (!points.value.length) return ''
  const p = project(points.value)
  return points.value.map((row) => {
    const { x, y } = p(row)
    return `${x.toFixed(2)},${y.toFixed(2)}`
  }).join(' ')
})

const anchorDots = computed(() => {
  if (!anchors.value.length) return []
  const p = project(anchors.value)
  return anchors.value.map((row) => p(row))
})

const currentDot = computed(() => {
  const row = points.value[cursor.value]
  if (!row) return undefined
  return project(points.value)(row)
})

function fmt(ms?: number) {
  return ms ? formatDate(new Date(ms)) : '—'
}

function stopPlay() {
  playing.value = false
  if (timer) {
    clearInterval(timer)
    timer = undefined
  }
}

function togglePlay() {
  if (playing.value) {
    stopPlay()
    return
  }
  playing.value = true
  cursor.value = 0
  timer = setInterval(() => {
    if (cursor.value >= points.value.length - 1) {
      stopPlay()
      return
    }
    cursor.value += 1
  }, 300)
}

async function load() {
  loadError.value = ''
  stopPlay()
  cursor.value = 0
  try {
    track.value = await LogisticsTrackDemoApi.getTrack(props.taskId)
  } catch (e) {
    // 权限没给（例如岗位不发演示权限）时不该把详情页拖红
    loadError.value = (e as Error).message || '演示轨迹加载失败'
  }
}

watch(() => props.taskId, load, { immediate: true })
onBeforeUnmount(stopPlay)
</script>

<style scoped lang="scss">
.track {
  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
  }

  &__canvas {
    width: 100%;
    height: 220px;
    background-color: #f8fafc;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
  }

  &__scrub {
    margin-top: 6px;
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  &__warn {
    color: var(--el-color-warning);
  }
}
</style>
