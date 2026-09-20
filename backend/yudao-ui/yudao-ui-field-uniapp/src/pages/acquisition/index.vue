<template>
  <view class="page">
    <!-- 登记成功：先给额度结论与下一步 -->
    <view v-if="result" class="card result">
      <view class="result__title">登记成功</view>
      <view class="result__no">{{ result.acquisitionNo }}</view>
      <view class="result__quota" :class="{ 'result__quota--warn': result.quotaPassed === false }">
        {{ result.quotaMessage || '额度未返回' }}
      </view>
      <view v-if="result.monthlyOverExempt" class="result__warn">
        该出售者本月销售额已超 10 万元免征线，须按时代办申报缴款。
      </view>
      <view class="result__actions">
        <button class="btn btn--ghost" @click="goDetail">查看确认书</button>
        <button class="btn btn--primary" @click="resetAll">再登一笔</button>
      </view>
    </view>

    <template v-else>
      <!-- 出售者 -->
      <view class="card">
        <view class="card__title">出售者</view>
        <view class="row">
          <input v-model="lookup.idCardNo" class="input" placeholder="身份证号" />
          <input v-model="lookup.mobile" class="input" placeholder="手机号" />
        </view>
        <button class="btn btn--ghost" :loading="looking" @click="onLookup">带出档案</button>

        <view v-if="seller" class="seller">
          <view class="seller__name">{{ seller.name }}</view>
          <view class="seller__meta">{{ seller.mobile || seller.idCardNo }}</view>
          <button class="link" @click="clearSeller">重新选择</button>
        </view>
        <view v-else-if="lookedUp" class="hint hint--warn">
          <view>没查到档案。新出售者需先完成实名与收方入驻，再回来登记。</view>
          <button class="link" @click="goPayee">去新建档案</button>
        </view>
      </view>

      <!-- 到站预约（#35）：只用来带出，不是订单 -->
      <view v-if="appointments.length" class="card">
        <view class="card__title">本场预约（带出，不是订单）</view>
        <view class="hint">
          预约只是他事先说了一句「大概什么时候来、卖什么」；数量以实际过磅为准，不占额度、不产生开票。
        </view>
        <view
          v-for="item in appointments"
          :key="item.id"
          class="appointment"
          :class="{ 'appointment--active': selectedAppointmentId === item.id }"
          @click="applyAppointment(item)"
        >
          <view class="appointment__row">
            <text class="appointment__cat">{{ item.categoryName }}</text>
            <text class="appointment__qty">{{ item.expectedQuantityText || '未填数量' }}</text>
          </view>
          <view class="appointment__meta">
            {{ formatArrival(item.expectedArrivalTime) }} 到站
            <template v-if="item.plateNo"> · {{ item.plateNo }}</template>
          </view>
          <view class="appointment__actions">
            <text class="link" @click.stop="applyAppointment(item)">带出</text>
            <text class="link link--danger" @click.stop="onNoShow(item)">未到场</text>
          </view>
        </view>
      </view>

      <!-- 交接批次（#50）：计量只认被选定的那一次磅次 -->
      <view v-if="handoverBatch" class="card">
        <view class="card__title">交接批次 {{ handoverBatch.batchNo }}</view>
        <view class="kv">
          <view class="kv__row"><text class="kv__k">参与计量</text><text>{{ effectiveWeighingText }}</text></view>
          <view class="kv__row"><text class="kv__k">车牌</text><text>{{ handoverBatch.plateNo || '-' }}</text></view>
        </view>
        <view class="hint">
          重量与磅单以该批次的有效磅次为准，下面手填的值不会被采用；其余磅次留档不参与。
        </view>
      </view>

      <!-- 场站：一次到场批次按「出售者 + 场站」聚合（ADR 0018） -->
      <view class="card">
        <view class="card__title">场站</view>
        <picker :range="stationNames" :value="stationIndex < 0 ? 0 : stationIndex" @change="onStationChange">
          <view class="picker">{{ selectedStation?.name || '请选择场站' }}</view>
        </picker>
        <view class="hint">同一位出售者在同一场站的收购单会并进同一张结算单。</view>
      </view>

      <!-- 品类 -->
      <view class="card">
        <view class="card__title">品类</view>
        <picker :range="goodsNames" :value="goodsIndex < 0 ? 0 : goodsIndex" @change="onGoodsChange">
          <view class="picker">{{ selectedGoods ? selectedGoods.name : '请选择品类' }}</view>
        </picker>
        <view v-if="selectedGoods" class="kv">
          <view class="kv__row"><text class="kv__k">计量单位</text><text>{{ selectedGoods.unit || '-' }}</text></view>
          <view class="kv__row"><text class="kv__k">税率</text><text>{{ taxRateText }}</text></view>
          <view class="kv__row"><text class="kv__k">计税方法</text><text>{{ taxMethodText }}</text></view>
          <view class="kv__row"><text class="kv__k">税收分类编码</text><text>{{ selectedGoods.mergedCode || '-' }}</text></view>
        </view>
      </view>

      <!-- 有效采购安排（#51）：可选；不选就是「直接收购」，报表照常统计 -->
      <view class="card">
        <view class="card__title">有效采购安排（可选）</view>
        <picker
          :range="arrangementNames"
          :value="arrangementIndex"
          @change="onArrangementChange"
        >
          <view class="picker">{{ arrangementPickerText }}</view>
        </picker>
        <picker
          v-if="selectedArrangement"
          :range="arrangementItemNames"
          :value="arrangementItemIndex < 0 ? 0 : arrangementItemIndex"
          @change="onArrangementItemChange"
        >
          <view class="picker arrangement__item">{{ arrangementItemPickerText }}</view>
        </picker>
        <view class="hint">
          有效 = 执行中且未过期的采购订单。不选就是「直接收购」，报表照常统计，不是缺失；
          选了就要选到品类明细，且明细品类要与上面的品类一致。
        </view>
      </view>

      <!-- 数量与计价 -->
      <view class="card">
        <view class="card__title">数量与计价</view>
        <view class="field">
          <text class="field__label">数量{{ selectedGoods?.unit ? `（${selectedGoods.unit}）` : '' }}（展示与发票明细，不参与金额）</text>
          <input v-model="form.quantity" class="input" type="digit" placeholder="0" />
          <view v-if="prefilledQuantityText" class="hint">
            来自预约：{{ prefilledQuantityText }}，请以实际过磅为准。
          </view>
        </view>
        <view class="field">
          <text class="field__label">毛重</text>
          <input v-model="form.grossWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">皮重</text>
          <input v-model="form.tareWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">净重（自动 = 毛重 − 皮重）</text>
          <input v-model="form.netWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">扣杂录法</text>
          <picker :range="deductionMethodNames" :value="deductionMethodIndex" @change="onDeductionMethodChange">
            <view class="picker">{{ deductionMethodNames[deductionMethodIndex] }}</view>
          </picker>
        </view>
        <view class="field">
          <text class="field__label">扣杂（{{ form.deductionMethod === 'RATIO' ? '比例，如 0.1' : '重量' }}）</text>
          <input v-model="form.deduction" class="input" type="digit" placeholder="0" />
        </view>
        <view class="settlement">
          <text class="settlement__label">结算重量（自动 = 毛重 − 皮重 − 扣杂）</text>
          <text class="settlement__value">{{ settlementWeightText }}</text>
        </view>
        <view class="field">
          <text class="field__label">含税单价（元）</text>
          <input v-model="form.unitPrice" class="input" type="digit" placeholder="0.00" />
        </view>
        <view class="field">
          <text class="field__label">调整项（元，可正可负）</text>
          <input v-model="form.adjustmentAmount" class="input" type="digit" placeholder="0.00" />
        </view>
        <view class="field">
          <text class="field__label">调整原因（运费 / 补贴 / 折让）</text>
          <input v-model="form.adjustmentReason" class="input" placeholder="调整项非 0 时必填" />
        </view>
        <view class="field">
          <text class="field__label">金额（元，自动 = 结算重量 × 单价 + 调整项）</text>
          <input v-model="form.amount" class="input" type="digit" placeholder="0.00" />
        </view>
      </view>

      <!-- 现场照片（货物流证据） -->
      <view class="card">
        <view class="card__title">现场照片</view>
        <view class="photos">
          <view v-for="photo in photos" :key="photo.key" class="photo">
            <view class="photo__label">{{ photo.label }}</view>
            <view class="photo__box" @click="pickPhoto(photo.key)">
              <image
                v-if="photoUrl[photo.key] || photoData[photo.key]"
                :src="photoUrl[photo.key] || photoData[photo.key]"
                mode="aspectFill"
                class="photo__img"
              />
              <text v-else class="photo__add">{{ uploading[photo.key] ? '上传中…' : '+ 拍照' }}</text>
            </view>
            <button v-if="photoUrl[photo.key] || photoData[photo.key]" class="link" @click="removePhoto(photo.key)">删除</button>
          </view>
        </view>
        <view class="hint">
          一期无真实 OCR：磅单重量与车牌默认手工录入，照片只作为货物流证据留存（ADR 0013）。
        </view>
      </view>

      <!-- 磅单与结算 -->
      <view class="card">
        <view class="card__title">磅单与结算</view>
        <view class="field">
          <text class="field__label">磅单号</text>
          <input v-model="form.weightTicketNo" class="input" placeholder="如 WD20261201001" />
        </view>
        <view class="field">
          <text class="field__label">磅单识别车牌</text>
          <input v-model="form.weightTicketPlateNo" class="input" placeholder="如 京A12345" />
        </view>
        <view class="field">
          <text class="field__label">车头车尾识别车牌</text>
          <input v-model="form.vehiclePlateNo" class="input" placeholder="如 京A12345" />
        </view>
        <view class="plate" :class="plateClass">{{ plateText }}</view>
        <view class="field">
          <text class="field__label">交易地点</text>
          <input v-model="form.tradeAddress" class="input" placeholder="现场地点" />
        </view>
        <view class="field">
          <text class="field__label">结算方式</text>
          <input v-model="form.settlementMethod" class="input" placeholder="如 银行转账，过磅后 3 日内结清" />
        </view>
        <view class="field">
          <text class="field__label">司机姓名（运输信息，不参与确认与收款）</text>
          <input v-model="form.driverName" class="input" placeholder="选填" />
        </view>
        <view class="field">
          <text class="field__label">司机手机号（运输信息）</text>
          <input v-model="form.driverMobile" class="input" placeholder="选填" />
        </view>
        <view class="field">
          <text class="field__label">备注</text>
          <input v-model="form.remark" class="input" placeholder="选填" />
        </view>
      </view>

      <button class="btn btn--primary submit" :loading="submitting" @click="onSubmit">提交登记</button>
      <button class="btn btn--ghost" @click="onSaveDraft">暂存到本地（弱网用）</button>
      <view class="tip">漏填品类，或磅单号与磅单照片都没有，会拦住提交；断网时自动暂存，恢复后到「待补传」补传。</view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getEnabledGoodsList, GoodsConfigVO } from '@/api/goodsConfig'
import { findReturningCustomer, PayeeVO } from '@/api/payee'
import { getStationPage, StationVO } from '@/api/station'
import {
  createAcquisition,
  getUsablePurchaseArrangements,
  AcquisitionCreateReq,
  AcquisitionCreateResp,
  PurchaseArrangementVO
} from '@/api/acquisition'
import { getHandoverBatch, getWeighings, HandoverBatchVO } from '@/api/handover'
import {
  AppointmentVO,
  getPendingAppointments,
  markAppointmentArrived,
  markAppointmentNoShow
} from '@/api/appointment'
import { chooseImage, pathToDataUrl, dataUrlToUploadPath, uploadImage } from '@/utils/upload'
import { comparePlate } from '@/utils/plate'
import { saveDraft } from '@/utils/draft'

defineOptions({ name: 'FieldAcquisition' })

type PhotoKey = 'weightTicketImageUrl' | 'vehicleFrontImageUrl' | 'vehicleRearImageUrl'

const PHOTO_KEYS: PhotoKey[] = ['weightTicketImageUrl', 'vehicleFrontImageUrl', 'vehicleRearImageUrl']
const photos: { key: PhotoKey; label: string }[] = [
  { key: 'weightTicketImageUrl', label: '磅单照片' },
  { key: 'vehicleFrontImageUrl', label: '车头照片' },
  { key: 'vehicleRearImageUrl', label: '车尾照片' }
]

const goodsList = ref<GoodsConfigVO[]>([])
const goodsIndex = ref(-1)
const stations = ref<StationVO[]>([])
const stationIndex = ref(-1)
const seller = ref<PayeeVO | null>(null)
const lookedUp = ref(false)
const looking = ref(false)
const submitting = ref(false)
const result = ref<AcquisitionCreateResp | null>(null)
const clientRequestId = ref(newId())
const appointments = ref<AppointmentVO[]>([])
const selectedAppointmentId = ref<number | null>(null)
/** 从预约带出的数量文案（一律带「约」）：只作提示，不当准数用 */
const prefilledQuantityText = ref('')
/** 交接批次（#50）：填了就按该批次的有效磅次计量 */
const handoverBatch = ref<HandoverBatchVO | null>(null)
const effectiveWeighingText = ref('')
/** 有效采购安排（#51）：执行中且未过期的采购订单，指数 0 = 不关联（直接收购） */
const arrangements = ref<PurchaseArrangementVO[]>([])
const arrangementIndex = ref(0)
const arrangementItemIndex = ref(-1)

const uploading = reactive<Record<PhotoKey, boolean>>({
  weightTicketImageUrl: false,
  vehicleFrontImageUrl: false,
  vehicleRearImageUrl: false
})
// 已上传成功的照片 URL
const photoUrl = reactive<Record<PhotoKey, string>>({
  weightTicketImageUrl: '',
  vehicleFrontImageUrl: '',
  vehicleRearImageUrl: ''
})
// 本地照片（base64）：弱网时先存本地，提交或补传时再上传
const photoData = reactive<Record<PhotoKey, string>>({
  weightTicketImageUrl: '',
  vehicleFrontImageUrl: '',
  vehicleRearImageUrl: ''
})

const lookup = reactive({ idCardNo: '', mobile: '' })
const form = reactive({
  payeeId: undefined as number | undefined,
  stationId: undefined as number | undefined,
  goodsConfigId: undefined as number | undefined,
  purchaseOrderId: undefined as number | undefined,
  purchaseOrderItemId: undefined as number | undefined,
  specification: '',
  quantity: '',
  unitPrice: '',
  amount: '',
  grossWeight: '',
  tareWeight: '',
  netWeight: '',
  deduction: '',
  deductionMethod: 'WEIGHT',
  adjustmentAmount: '',
  adjustmentReason: '',
  weightTicketNo: '',
  weightTicketPlateNo: '',
  vehiclePlateNo: '',
  tradeAddress: '',
  settlementMethod: '',
  driverName: '',
  driverMobile: '',
  remark: ''
})

const deductionMethodNames = ['按重量', '按比例']
const deductionMethodIndex = computed(() => (form.deductionMethod === 'RATIO' ? 1 : 0))

function onDeductionMethodChange(event: any) {
  form.deductionMethod = Number(event.detail.value) === 1 ? 'RATIO' : 'WEIGHT'
}

const goodsNames = computed(() => goodsList.value.map((item) => item.name || ''))
// 采购安排选择器：第 0 项是「不关联（直接收购）」，其余按下标对应 arrangements
const arrangementNames = computed(() => [
  '不关联（直接收购）',
  ...arrangements.value.map((item) => item.orderNo || `订单 ${item.orderId}`)
])
const selectedArrangement = computed(() =>
  arrangementIndex.value >= 1 ? arrangements.value[arrangementIndex.value - 1] : undefined
)
const arrangementItems = computed(() => selectedArrangement.value?.items || [])
const arrangementItemNames = computed(() =>
  arrangementItems.value.map((item) => `${item.categoryName || '品类'}${item.unit ? `（${item.unit}）` : ''}`)
)
const selectedArrangementItem = computed(() =>
  arrangementItemIndex.value >= 0 ? arrangementItems.value[arrangementItemIndex.value] : undefined
)
const arrangementPickerText = computed(() =>
  selectedArrangement.value ? (selectedArrangement.value.orderNo || '已选采购订单') : '不关联（直接收购）'
)
const arrangementItemPickerText = computed(
  () => selectedArrangementItem.value?.categoryName || '请选择品类明细'
)
const stationNames = computed(() => stations.value.map((item) => item.name || item.stationCode || ''))
const selectedStation = computed(() => (stationIndex.value >= 0 ? stations.value[stationIndex.value] : undefined))

function onStationChange(event: any) {
  stationIndex.value = Number(event.detail.value)
  form.stationId = selectedStation.value?.id
}
const selectedGoods = computed(() => (goodsIndex.value >= 0 ? goodsList.value[goodsIndex.value] : undefined))
const taxRateText = computed(() =>
  selectedGoods.value?.taxRate != null ? `${Number(selectedGoods.value.taxRate) * 100}%` : '-'
)
const taxMethodText = computed(() => {
  const method = selectedGoods.value?.taxMethod
  if (method === 'SIMPLE') return '简易计税'
  if (method === 'GENERAL') return '一般计税'
  return '-'
})

const plateResult = computed(() => comparePlate(form.weightTicketPlateNo, form.vehiclePlateNo))
const plateText = computed(() => {
  if (plateResult.value === null) return '车牌比对：无法比对（缺车牌）'
  return plateResult.value ? '车牌比对：一致' : '车牌比对：不一致'
})
const plateClass = computed(() => ({
  'plate--ok': plateResult.value === true,
  'plate--bad': plateResult.value === false
}))

onLoad((options) => {
  loadGoods()
  loadStations()
  // 从「交接批次」页过来：按批次带上出售者、车牌、司机与有效磅次的重量
  if (options?.handoverBatchId) {
    applyHandoverBatch(Number(options.handoverBatchId))
  }
})

/**
 * 按交接批次登记：只带出，不算账——真正的计量在服务端按有效磅次完成。
 */
async function applyHandoverBatch(batchId: number) {
  try {
    const detail = await getHandoverBatch(batchId)
    handoverBatch.value = detail
    if (detail.payeeId) {
      seller.value = { id: detail.payeeId, name: detail.sellerName || '', mobile: detail.sellerMobile || '' }
      lookedUp.value = true
      form.payeeId = detail.payeeId
      await loadArrangements(detail.payeeId)
      // 批次上若已挂采购订单（#50 预留的字段），在这里替现场选好订单，明细仍由现场挑
      if (detail.purchaseOrderId) {
        selectArrangementByOrderId(detail.purchaseOrderId)
      }
    }
    if (detail.stationId) {
      form.stationId = detail.stationId
      const index = stations.value.findIndex((station) => station.id === detail.stationId)
      if (index >= 0) stationIndex.value = index
    }
    if (detail.plateNo) form.vehiclePlateNo = detail.plateNo
    if (detail.driverName) form.driverName = detail.driverName
    if (detail.driverMobile) form.driverMobile = detail.driverMobile
    if (detail.visitAddress) form.tradeAddress = detail.visitAddress
    const list = await getWeighings(batchId)
    const effective = list.find((item) => item.effective)
    if (effective) {
      form.grossWeight = effective.grossWeight == null ? '' : String(effective.grossWeight)
      form.tareWeight = effective.tareWeight == null ? '' : String(effective.tareWeight)
      form.netWeight = effective.netWeight == null ? '' : String(effective.netWeight)
      if (effective.weightTicketNo) form.weightTicketNo = effective.weightTicketNo
      effectiveWeighingText.value = `第 ${effective.seqNo} 次磅次（毛重 ${effective.grossWeight} / 皮重 ${effective.tareWeight}）`
    } else {
      effectiveWeighingText.value = '还没有指定有效磅次，请先到交接批次页指定'
    }
  } catch (e) {
    showError(e)
  }
}

async function loadStations() {
  try {
    const page = await getStationPage()
    stations.value = page.list || []
  } catch (e) {
    // 场站拉不到不阻断登记；提交时只是缺场站维度
    stations.value = []
  }
  // 批次带出的场站可能在 stationIndex 还没算好时先落进 form
  if (form.stationId != null) {
    const index = stations.value.findIndex((station) => station.id === form.stationId)
    if (index >= 0) stationIndex.value = index
  }
}

async function loadGoods() {
  try {
    goodsList.value = await getEnabledGoodsList()
  } catch (e) {
    showError(e)
  }
}

function onGoodsChange(event: any) {
  goodsIndex.value = Number(event.detail.value)
  form.goodsConfigId = selectedGoods.value?.id
}

// 结算重量 = 毛重 − 皮重 − 扣杂，是本平台唯一的计价基准（ADR 0019）
const settlementWeight = computed(() => {
  const net = toNum(form.netWeight)
  if (net == null) return null
  const deduction = toNum(form.deduction) ?? 0
  const deductionWeight = form.deductionMethod === 'RATIO' ? net * deduction : deduction
  const result = net - deductionWeight
  return result >= 0 ? Number(result.toFixed(4)) : null
})
const settlementWeightText = computed(() =>
  settlementWeight.value == null ? '待录入毛重 / 皮重' : `${settlementWeight.value}`
)

// 金额 = 结算重量 × 单价 + 调整项；没有重量时退回「数量 × 单价」的历史口径
watch(
  () => [
    form.quantity,
    form.unitPrice,
    form.netWeight,
    form.deduction,
    form.deductionMethod,
    form.adjustmentAmount
  ],
  () => {
    const unitPrice = toNum(form.unitPrice)
    const adjustment = toNum(form.adjustmentAmount) ?? 0
    const settlement = settlementWeight.value
    if (settlement != null && unitPrice != null) {
      form.amount = (settlement * unitPrice + adjustment).toFixed(2)
      return
    }
    const quantity = toNum(form.quantity)
    if (quantity != null && unitPrice != null) {
      form.amount = (quantity * unitPrice + adjustment).toFixed(2)
    }
  }
)
watch(
  () => [form.grossWeight, form.tareWeight],
  () => {
    const gross = toNum(form.grossWeight)
    const tare = toNum(form.tareWeight)
    if (gross != null && tare != null && gross >= tare) {
      form.netWeight = (gross - tare).toFixed(2)
    }
  }
)

async function pickPhoto(key: PhotoKey) {
  if (uploading[key]) return
  uploading[key] = true
  try {
    const paths = await chooseImage(1)
    if (!paths.length) return
    // 先把照片读成 base64 存本地，保证断网也不丢
    photoData[key] = await pathToDataUrl(paths[0])
    try {
      photoUrl[key] = await uploadImage(paths[0])
    } catch {
      // 网络不好：照片留在本地，提交或补传时再传
      photoUrl[key] = ''
    }
  } catch (e) {
    if (!(e as Error).message?.includes('未选择')) {
      showError(e)
    }
  } finally {
    uploading[key] = false
  }
}

function removePhoto(key: PhotoKey) {
  photoUrl[key] = ''
  photoData[key] = ''
}

async function ensurePhotosUploaded() {
  for (const key of PHOTO_KEYS) {
    if (photoData[key] && !photoUrl[key]) {
      photoUrl[key] = await uploadImage(dataUrlToUploadPath(photoData[key]))
    }
  }
}

async function onLookup() {
  if (!lookup.idCardNo && !lookup.mobile) {
    uni.showToast({ title: '请填身份证号或手机号', icon: 'none' })
    return
  }
  looking.value = true
  try {
    const found = await findReturningCustomer({
      idCardNo: lookup.idCardNo || undefined,
      mobile: lookup.mobile || undefined
    })
    seller.value = found
    form.payeeId = found?.id
    lookedUp.value = true
    if (found) {
      uni.showToast({ title: `已带出：${found.name}`, icon: 'none' })
      if (found.id) {
        loadAppointments(found.id)
      }
    } else {
      appointments.value = []
    }
    if (found?.id) {
      loadArrangements(found.id)
    }
  } catch (e) {
    showError(e)
  } finally {
    looking.value = false
  }
}

function clearSeller() {
  seller.value = null
  lookedUp.value = false
  form.payeeId = undefined
  appointments.value = []
  selectedAppointmentId.value = null
  prefilledQuantityText.value = ''
  handoverBatch.value = null
  effectiveWeighingText.value = ''
  loadArrangements()
}

async function loadAppointments(payeeId: number) {
  try {
    appointments.value = await getPendingAppointments(payeeId)
  } catch {
    // 预约是锦上添花：拉不到不影响正常登记
    appointments.value = []
  }
}

/**
 * 拉取该出售者的有效采购安排（执行中且未过期的采购订单 + 品类明细）。
 * 拉不到不阻断登记：不选采购安排就是「直接收购」，照常落单。
 */
async function loadArrangements(payeeId?: number) {
  arrangements.value = []
  arrangementIndex.value = 0
  arrangementItemIndex.value = -1
  form.purchaseOrderId = undefined
  form.purchaseOrderItemId = undefined
  if (!payeeId) return
  try {
    arrangements.value = await getUsablePurchaseArrangements(payeeId)
  } catch {
    arrangements.value = []
  }
}

/** 选择采购订单：第 0 项是不关联；选了订单后清空明细，只有一条明细时自动选上。 */
function onArrangementChange(event: any) {
  const index = Number(event.detail.value)
  arrangementIndex.value = index
  arrangementItemIndex.value = -1
  form.purchaseOrderItemId = undefined
  const arrangement = index >= 1 ? arrangements.value[index - 1] : undefined
  form.purchaseOrderId = arrangement?.orderId
  if (arrangement?.items?.length === 1) {
    arrangementItemIndex.value = 0
    form.purchaseOrderItemId = arrangement.items[0].itemId
  }
}

function onArrangementItemChange(event: any) {
  arrangementItemIndex.value = Number(event.detail.value)
  form.purchaseOrderItemId = selectedArrangementItem.value?.itemId
}

/** 按订单编号预选（批次上挂了采购订单时用）。 */
function selectArrangementByOrderId(orderId: number) {
  const index = arrangements.value.findIndex((item) => item.orderId === orderId)
  if (index < 0) return
  arrangementIndex.value = index + 1
  arrangementItemIndex.value = -1
  form.purchaseOrderId = orderId
  form.purchaseOrderItemId = undefined
  const items = arrangements.value[index].items || []
  if (items.length === 1) {
    arrangementItemIndex.value = 0
    form.purchaseOrderItemId = items[0].itemId
  }
}

/**
 * 带出预约内容：品类、约多少、车牌。数量一律只是「约」，以实际过磅为准。
 */
function applyAppointment(item: AppointmentVO) {
  selectedAppointmentId.value = item.id
  prefilledQuantityText.value = item.expectedQuantityText || ''
  // 到场预约带出场站：这笔收购单因此能归入该场站的结算单（ADR 0018）
  if (item.stationId != null) {
    const stationIdx = stations.value.findIndex((station) => station.id === item.stationId)
    if (stationIdx >= 0) {
      stationIndex.value = stationIdx
      form.stationId = item.stationId
    } else {
      form.stationId = item.stationId
    }
  }
  if (item.goodsConfigId) {
    const index = goodsList.value.findIndex((goods) => goods.id === item.goodsConfigId)
    if (index >= 0) {
      goodsIndex.value = index
      form.goodsConfigId = item.goodsConfigId
    }
  }
  if (item.expectedQuantity != null) {
    form.quantity = String(item.expectedQuantity)
    if (!prefilledQuantityText.value) {
      prefilledQuantityText.value = `约 ${item.expectedQuantity}`
    }
  }
  if (item.plateNo) {
    form.vehiclePlateNo = item.plateNo
  }
  uni.showToast({ title: `已带出预约（${item.expectedQuantityText || '数量未填'}）`, icon: 'none' })
}

function onNoShow(item: AppointmentVO) {
  uni.showModal({
    title: '标记未到场',
    content: '标记后这条预约就结束了，不影响你登记收购。',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await markAppointmentNoShow(item.id, '现场标记未到场')
        uni.showToast({ title: '已标记未到场', icon: 'none' })
        if (form.payeeId) {
          loadAppointments(form.payeeId)
        }
      } catch (e) {
        showError(e)
      }
    }
  })
}

function formatArrival(value?: number) {
  if (value === undefined || value === null) return '未填时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '未填时间'
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function validate(): string | null {
  if (!form.payeeId) return '请先带出售者档案'
  if (!form.goodsConfigId) return '请选择品类'
  if (!(toNum(form.quantity)! > 0)) return '请填写数量'
  if (!(toNum(form.amount)! > 0)) return '金额需大于 0（结算重量 × 单价 + 调整项）'
  const adjustment = toNum(form.adjustmentAmount)
  if (adjustment != null && adjustment !== 0 && !form.adjustmentReason.trim()) {
    return '调整项非 0 时必须填写调整原因（运费 / 补贴 / 折让）'
  }
  if (!form.weightTicketNo.trim() && !photoData.weightTicketImageUrl && !photoUrl.weightTicketImageUrl) {
    return '磅单号与磅单照片至少填一个'
  }
  return null
}

function buildPayload(): AcquisitionCreateReq {
  return {
    clientRequestId: clientRequestId.value,
    payeeId: form.payeeId!,
    stationId: form.stationId,
    handoverBatchId: handoverBatch.value?.id,
    purchaseOrderId: form.purchaseOrderId,
    purchaseOrderItemId: form.purchaseOrderItemId,
    goodsConfigId: form.goodsConfigId!,
    specification: form.specification || undefined,
    quantity: toNum(form.quantity) ?? undefined,
    unitPrice: toNum(form.unitPrice) ?? undefined,
    amount: toNum(form.amount) ?? undefined,
    grossWeight: toNum(form.grossWeight) ?? undefined,
    tareWeight: toNum(form.tareWeight) ?? undefined,
    netWeight: toNum(form.netWeight) ?? undefined,
    deduction: toNum(form.deduction) ?? undefined,
    deductionMethod: form.deductionMethod,
    adjustmentAmount: toNum(form.adjustmentAmount) ?? undefined,
    adjustmentReason: form.adjustmentReason.trim() || undefined,
    driverName: form.driverName.trim() || undefined,
    driverMobile: form.driverMobile.trim() || undefined,
    weightTicketNo: form.weightTicketNo.trim() || undefined,
    weightTicketPlateNo: form.weightTicketPlateNo || undefined,
    vehiclePlateNo: form.vehiclePlateNo || undefined,
    tradeAddress: form.tradeAddress || undefined,
    settlementMethod: form.settlementMethod || undefined,
    source: 'ONLINE',
    remark: form.remark || undefined
  }
}

async function onSubmit() {
  const invalid = validate()
  if (invalid) {
    uni.showModal({ title: '还差一点', content: invalid, showCancel: false })
    return
  }
  submitting.value = true
  try {
    // 先把本地照片传上去；传不动就暂存
    await ensurePhotosUploaded()
    const payload = buildPayload()
    PHOTO_KEYS.forEach((key) => {
      if (photoUrl[key]) {
        payload[key] = photoUrl[key]
      }
    })
    result.value = await createAcquisition(payload)
    // 他来过就标到场；预约只是排队与带出，失败了也不影响这笔已经落下的收购单
    if (selectedAppointmentId.value) {
      try {
        await markAppointmentArrived(selectedAppointmentId.value, result.value?.id)
      } catch {
        // 忽略：预约状态不是业务事实
      }
    }
  } catch (e) {
    if (isNetworkError(e)) {
      saveCurrentDraft()
    } else {
      // 后端 ACQUISITION_REQUIRED_ELEMENT_MISSING 会在这里逐项列出缺什么
      showError(e)
    }
  } finally {
    submitting.value = false
  }
}

function onSaveDraft() {
  const invalid = validate()
  if (invalid) {
    uni.showModal({ title: '还差一点', content: invalid, showCancel: false })
    return
  }
  saveCurrentDraft()
}

function saveCurrentDraft() {
  try {
    const photoUrls: Record<string, string> = {}
    const pending = PHOTO_KEYS.filter((key) => photoData[key] && !photoUrl[key]).map((key) => {
      return { key, dataUrl: photoData[key] }
    })
    PHOTO_KEYS.forEach((key) => {
      if (photoUrl[key]) {
        photoUrls[key] = photoUrl[key]
      }
    })
    saveDraft({
      clientRequestId: clientRequestId.value,
      createdAt: Date.now(),
      summary: `${seller.value?.name || '出售者'} · ${form.amount || 0} 元`,
      payload: buildPayload(),
      photos: pending,
      photoUrls
    })
    uni.showModal({
      title: '已暂存',
      content: '这一笔已存在本地；网络恢复后到首页「待补传」里补传。',
      showCancel: false
    })
  } catch (e) {
    showError(e)
  }
}

function isNetworkError(e: unknown): boolean {
  const message = (e as Error).message || ''
  return /网络|HTTP|超时|上传|下载|timeout|fail/i.test(message)
}

function goDetail() {
  if (result.value?.id) {
    uni.navigateTo({ url: `/pages/acquisition/detail?id=${result.value.id}` })
  }
}

function goPayee() {
  uni.navigateTo({ url: '/pages/payee/index' })
}

function resetAll() {
  result.value = null
  clearSeller()
  goodsIndex.value = -1
  PHOTO_KEYS.forEach((key) => removePhoto(key))
  clientRequestId.value = newId()
  Object.assign(form, {
    payeeId: undefined,
    goodsConfigId: undefined,
    purchaseOrderId: undefined,
    purchaseOrderItemId: undefined,
    specification: '',
    quantity: '',
    unitPrice: '',
    amount: '',
    grossWeight: '',
    tareWeight: '',
    netWeight: '',
    deduction: '',
    deductionMethod: 'WEIGHT',
    adjustmentAmount: '',
    adjustmentReason: '',
    weightTicketNo: '',
    weightTicketPlateNo: '',
    vehiclePlateNo: '',
    tradeAddress: '',
    settlementMethod: '',
    driverName: '',
    driverMobile: '',
    remark: ''
  })
  lookup.idCardNo = ''
  lookup.mobile = ''
}

function toNum(value: string): number | null {
  if (value === '' || value == null) return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

function newId(): string {
  const globalCrypto = (globalThis as any).crypto
  if (globalCrypto?.randomUUID) {
    return globalCrypto.randomUUID()
  }
  return `field-${Date.now()}-${Math.floor(Math.random() * 1e6)}`
}

function showError(e: unknown) {
  uni.showModal({ title: '提交失败', content: (e as Error).message || '请重试', showCancel: false })
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

.row {
  display: flex;
  gap: 16rpx;
}

.input {
  flex: 1;
  height: 80rpx;
  padding: 0 20rpx;
  margin-bottom: 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
}

.field {
  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
  }
}

.picker {
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
}

.kv {
  margin-top: 20rpx;

  &__row {
    display: flex;
    justify-content: space-between;
    padding: 8rpx 0;
  }

  &__k {
    color: $field-text-secondary;
  }
}

.photos {
  display: flex;
  gap: 20rpx;
}

.photo {
  flex: 1;

  &__label {
    margin-bottom: 8rpx;
    color: $field-text-secondary;
    font-size: 24rpx;
  }

  &__box {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 180rpx;
    background-color: #f5f6f8;
    border-radius: 12rpx;
    overflow: hidden;
  }

  &__img {
    width: 100%;
    height: 100%;
  }

  &__add {
    color: $field-text-secondary;
    font-size: 26rpx;
  }
}

.settlement {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4rpx 0 20rpx;
  padding: 16rpx 20rpx;
  background-color: #eef4ff;
  border-radius: 12rpx;

  &__label {
    color: $field-text-secondary;
    font-size: 26rpx;
  }

  &__value {
    font-weight: 600;
    color: $field-primary;
  }
}

.plate {
  margin: 4rpx 0 20rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  background-color: #f5f6f8;
  color: $field-text-secondary;

  &--ok {
    background-color: #e8f7ee;
    color: #1a7f43;
  }

  &--bad {
    background-color: #fff1f0;
    color: #cf1322;
  }
}

.seller {
  margin-top: 24rpx;
  padding: 24rpx;
  background-color: #eef4ff;
  border-radius: 12rpx;

  &__name {
    font-size: 32rpx;
    font-weight: 600;
  }

  &__meta {
    margin-top: 8rpx;
    color: $field-text-secondary;
  }
}

.appointment {
  padding: 16rpx;
  margin-top: 12rpx;
  border: 1rpx solid #eef0f3;
  border-radius: 12rpx;

  &--active {
    border-color: $field-primary;
    background-color: #eef4ff;
  }

  &__row {
    display: flex;
    justify-content: space-between;
    gap: 16rpx;
  }

  &__cat {
    font-weight: 600;
  }

  &__qty {
    color: $field-primary;
  }

  &__meta {
    margin-top: 6rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
  }

  &__actions {
    display: flex;
    gap: 32rpx;
    margin-top: 10rpx;
  }
}

.arrangement__item {
  margin-top: 12rpx;
}

.hint {
  margin-top: 20rpx;
  line-height: 1.6;
  color: $field-text-secondary;

  &--warn {
    color: #b26a00;
  }
}

.link {
  display: inline-block;
  padding: 0;
  margin-top: 12rpx;
  color: $field-primary;
  font-size: 26rpx;
  background-color: transparent;
  text-align: left;
}

.btn {
  width: 100%;
  margin-top: 8rpx;

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

.submit {
  margin-top: 16rpx;
}

.tip {
  margin-top: 20rpx;
  color: $field-text-secondary;
  font-size: 24rpx;
  line-height: 1.6;
}

.result {
  &__title {
    font-size: 36rpx;
    font-weight: 700;
  }

  &__no {
    margin-top: 12rpx;
    color: $field-text-secondary;
  }

  &__quota {
    margin-top: 24rpx;
    padding: 20rpx;
    background-color: #eef4ff;
    border-radius: 12rpx;
    line-height: 1.6;

    &--warn {
      background-color: #fff1f0;
      color: #cf1322;
    }
  }

  &__warn {
    margin-top: 16rpx;
    color: #b26a00;
    line-height: 1.6;
  }

  &__actions {
    display: flex;
    gap: 16rpx;
    margin-top: 32rpx;
  }
}
</style>
