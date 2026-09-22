<template>
  <div class="track">
    <el-button type="primary" plain size="small" @click="visible = true">查看轨迹</el-button>

    <el-dialog
      v-model="visible"
      title="运输轨迹"
      width="760px"
      append-to-body
      destroy-on-close
      @opened="initMap"
      @closed="teardown"
    >
      <el-alert type="info" :closable="false" class="mb-10px" :title="NOTE" />

      <div v-if="loadError" class="track__error">{{ loadError }}</div>
      <div v-show="!loadError" ref="mapEl" class="track__map"></div>

      <div class="track__meta">
        <span>起点：{{ ORIGIN_NAME }}</span>
        <span>终点：{{ DESTINATION_NAME }}</span>
        <span>途经：{{ ROAD_NAMES.join(' → ') }}</span>
        <span>全程约 {{ (TOTAL_LENGTH / 1000).toFixed(1) }} 公里</span>
      </div>
      <div class="track__meta">
        <span>行程进度：{{ Math.round(progress * 100) }}%</span>
        <span>已行驶：{{ (traveled / 1000).toFixed(2) }} 公里</span>
        <span v-if="arrived" class="track__arrived">已到达终点</span>
      </div>

      <template #footer>
        <el-button :disabled="!!loadError" @click="replay">重播</el-button>
        <el-button type="primary" @click="visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { AMapMapInstance, AMapNS, AMapOverlay, loadAMap } from '@/utils/amap'

/**
 * 运输轨迹弹窗：高德地图上演示一段「九龙坡区 → 大渡口区」的车辆行驶轨迹。
 *
 * 线上是**真实路网上的驾车路径**（杨家坪 → 九宫庙，沿西郊路/杨渡路/钢铁路/钢花路走），
 * 不再是两点之间的直尺线；但它仍然是演示数据：不代表该趟任务的实际行驶轨迹，也不参与
 * 一票一档、异常统计或任何报表；真实凭证只有节点照片与上报记录。所以：
 *  - 只读：组件里没有任何写接口调用；
 *  - 一眼可辨：页面上写明是演示轨迹，线是深蓝色实线而不是真实定位数据；
 *  - 用完即毁：弹窗关闭就 destroy 地图并停掉动画，不留常驻的第三方实例。
 */
const NOTE =
  '演示轨迹：从九龙坡区杨家坪到大渡口区九宫庙，按真实路网生成的驾车路线，仅用于展示效果，不代表该趟任务的实际行驶轨迹。'

/**
 * 驾车路线：九龙坡区（杨家坪 西子路）→ 大渡口区（九宫庙 钢花路），[经度, 纬度]。
 *
 * 这是按真实路网算出来的驾车路径（5.06 公里，途经西子路→西郊路→杨渡路→钢铁路→钢花支路→钢花路），
 * 不是直尺线；已抽稀到 29 个折点，够贴住路口又不必把整条线塞进源码。
 * 要换起终点，用 OSRM 重新生成一次即可：
 *   curl 'https://router.project-osrm.org/route/v1/driving/{lng},{lat};{lng},{lat}?overview=full&geometries=geojson'
 */
const ROUTE: [number, number][] = [
  [106.51661, 29.50183],
  [106.51597, 29.50171],
  [106.51547, 29.50186],
  [106.51176, 29.50389],
  [106.50781, 29.50385],
  [106.50719, 29.50404],
  [106.5061, 29.50316],
  [106.50267, 29.50144],
  [106.50231, 29.50148],
  [106.50195, 29.50108],
  [106.49964, 29.49992],
  [106.49901, 29.4994],
  [106.49864, 29.49874],
  [106.49862, 29.49796],
  [106.499, 29.49504],
  [106.49771, 29.49087],
  [106.49646, 29.48986],
  [106.49119, 29.48729],
  [106.48945, 29.48755],
  [106.48834, 29.48728],
  [106.48611, 29.48593],
  [106.48594, 29.4857],
  [106.48578, 29.48473],
  [106.48527, 29.48441],
  [106.48489, 29.48385],
  [106.48465, 29.48373],
  [106.48369, 29.48402],
  [106.4827, 29.48159],
  [106.48194, 29.48054]
]

/** 途经路口（用来在地图上打小点标注路名） */
const WAYPOINTS: { name: string; position: [number, number] }[] = [
  { name: '西郊路', position: [106.50732, 29.50398] },
  { name: '杨渡路', position: [106.50267, 29.50144] },
  { name: '钢铁路', position: [106.49844, 29.49307] },
  { name: '钢花支路', position: [106.49119, 29.48729] }
]
const ROAD_NAMES = ['西郊路', '杨渡路', '钢铁路', '钢花支路', '钢花路']
const ORIGIN_NAME = '九龙坡区 · 杨家坪（西子路）'
const DESTINATION_NAME = '大渡口区 · 九宫庙（钢花路）'

/** 一辆车跑完全程的时长（毫秒）：演示要看得清，比真车快，不代表真实车速 */
const DURATION = 20000

const visible = ref(false)
const loadError = ref('')
const progress = ref(0)
const traveled = ref(0)
const arrived = ref(false)
const mapEl = ref<HTMLDivElement>()

let amap: AMapNS | undefined
let map: AMapMapInstance | undefined
let car: AMapOverlay | undefined
let rafId = 0
let startAt = 0

/** 两点的近似距离（米）：只用来让车匀速跑，不要求测绘精度 */
function distanceOf(from: [number, number], to: [number, number]) {
  const [fromLng, fromLat] = from
  const [toLng, toLat] = to
  const midLat = ((fromLat + toLat) / 2) * (Math.PI / 180)
  const dx = (toLng - fromLng) * 111320 * Math.cos(midLat)
  const dy = (toLat - fromLat) * 110540
  return Math.hypot(dx, dy)
}

/** 每一段累积里程，用来按里程（而不是按折点个数）取位置，车才会匀速 */
const cumulative = (() => {
  const rows = [0]
  for (let i = 1; i < ROUTE.length; i++) {
    rows.push(rows[i - 1] + distanceOf(ROUTE[i - 1], ROUTE[i]))
  }
  return rows
})()
const TOTAL_LENGTH = cumulative[cumulative.length - 1]

/** 按已行驶里程取路上一点 */
function positionAt(meters: number): [number, number] {
  const target = Math.min(Math.max(meters, 0), TOTAL_LENGTH)
  let index = 0
  while (index < cumulative.length - 2 && cumulative[index + 1] < target) {
    index += 1
  }
  const segmentLength = cumulative[index + 1] - cumulative[index] || 1
  const local = (target - cumulative[index]) / segmentLength
  const [fromLng, fromLat] = ROUTE[index]
  const [toLng, toLat] = ROUTE[index + 1]
  return [fromLng + (toLng - fromLng) * local, fromLat + (toLat - fromLat) * local]
}

/** 车头朝向：取车轮前后各 20 米的连线方向，免得在拐弯处抖 */
function bearingAt(meters: number): number {
  let [fromLng, fromLat] = positionAt(Math.max(meters - 20, 0))
  let [toLng, toLat] = positionAt(Math.min(meters + 20, TOTAL_LENGTH))
  if (fromLng === toLng && fromLat === toLat) {
    // 路线太短时退化成首/末段的方向
    const segment = meters <= 0 ? ROUTE.slice(0, 2) : ROUTE.slice(-2)
    ;[fromLng, fromLat] = segment[0]
    ;[toLng, toLat] = segment[1]
  }
  const dLng = (toLng - fromLng) * Math.cos((Math.PI / 180) * ((fromLat + toLat) / 2))
  const dLat = toLat - fromLat
  return ((Math.atan2(dLng, dLat) * 180) / Math.PI + 360) % 360
}

function drawTrack() {
  if (!amap || !map) return
  const ns = amap
  const [origin, destination] = [ROUTE[0], ROUTE[ROUTE.length - 1]]
  // 底线（白）压在轨迹下面，避免深色线糊在深色底图上
  const casing = new ns.Polyline({
    path: ROUTE,
    strokeColor: '#ffffff',
    strokeWeight: 10,
    strokeOpacity: 0.9,
    lineJoin: 'round',
    lineCap: 'round'
  })
  const line = new ns.Polyline({
    path: ROUTE,
    strokeColor: '#1d4ed8',
    strokeWeight: 6,
    strokeOpacity: 0.9,
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true
  })
  const originMarker = new ns.Marker({
    position: origin,
    label: { content: `起点：${ORIGIN_NAME}`, direction: 'right' }
  })
  const destinationMarker = new ns.Marker({
    position: destination,
    label: { content: `终点：${DESTINATION_NAME}`, direction: 'right' }
  })
  // 途经路口打小点并标路名，一眼能看出这是沿着街道走的
  const waypointMarkers = WAYPOINTS.map(
    (waypoint) =>
      new ns.Marker({
        position: waypoint.position,
        content:
          '<div style="width:10px;height:10px;background:#1d4ed8;border:2px solid #fff;' +
          'border-radius:50%;box-shadow:0 1px 3px rgba(0,0,0,.3)"></div>',
        offset: new ns.Pixel(-7, -7),
        label: { content: waypoint.name, direction: 'right' }
      })
  )
  car = new ns.Marker({
    position: origin,
    offset: new ns.Pixel(-15, -15),
    content:
      '<div style="width:30px;height:30px;line-height:26px;text-align:center;font-size:15px;color:#1d4ed8;' +
      'background:#fff;border:2px solid #1d4ed8;border-radius:50%;box-shadow:0 1px 4px rgba(0,0,0,.3)">▲</div>'
  })
  map.add([casing, line, originMarker, destinationMarker, ...waypointMarkers, car])
  // 折点多了以后 setFitView 容易把边贴到屏幕边上，留点内边距
  map.setFitView([line], false, [70, 70, 70, 70])
}

function stopAnimation() {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
}

function tick(now: number) {
  const ratio = Math.min((now - startAt) / DURATION, 1)
  progress.value = ratio
  traveled.value = TOTAL_LENGTH * ratio
  car?.setPosition(positionAt(traveled.value))
  car?.setAngle?.(bearingAt(traveled.value))
  if (ratio < 1) {
    rafId = requestAnimationFrame(tick)
  } else {
    rafId = 0
    arrived.value = true
  }
}

function replay() {
  if (!amap || !map || !car) return
  stopAnimation()
  arrived.value = false
  progress.value = 0
  traveled.value = 0
  car.setPosition(ROUTE[0])
  car.setAngle?.(bearingAt(0))
  startAt = performance.now()
  rafId = requestAnimationFrame(tick)
}

async function initMap() {
  loadError.value = ''
  try {
    amap = await loadAMap()
    // 加载是异步的，弹窗可能已经被关掉了
    if (!visible.value || !mapEl.value) return
    map = new amap.Map(mapEl.value, { zoom: 13, center: ROUTE[0], viewMode: '2D' })
    drawTrack()
    replay()
  } catch (e) {
    loadError.value = (e as Error).message || '地图初始化失败'
  }
}

function teardown() {
  stopAnimation()
  map?.destroy()
  map = undefined
  car = undefined
  amap = undefined
  progress.value = 0
  traveled.value = 0
  arrived.value = false
}

onBeforeUnmount(teardown)
</script>

<style scoped lang="scss">
.track {
  &__map {
    width: 100%;
    height: 360px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
  }

  &__error {
    padding: 24px;
    color: var(--el-color-danger);
    font-size: 13px;
    text-align: center;
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    margin-top: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  &__arrived {
    color: var(--el-color-success);
  }
}
</style>
