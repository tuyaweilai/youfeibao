<template>
  <view class="page" :class="{ 'page--editing': !result }">
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
      <view class="intro"><view class="intro__eyebrow">收料作业 / 新建记录</view><view class="intro__title">登记一笔收购</view><view class="intro__desc">先选出售者，再拍照核对重量与金额</view></view>
      <view class="section-nav"><button v-for="(title, index) in sectionTitles" :key="title" @click="jumpTo(index)"><text>{{ String(index + 1).padStart(2, '0') }}</text>{{ title }}</button></view>
      <view id="acq-section-0" class="section-heading"><text>01</text>基础信息<text class="section-heading__note">确定本次交易对象</text></view>
      <!-- 出售者 -->
      <view class="card">
        <view class="card__title">出售者</view>
        <view class="card__note">手机号或身份证号，填写一项即可查询</view>
        <view class="row">
          <view class="field"><label for="lookup-mobile" class="field__label">手机号</label><input id="lookup-mobile" v-model="lookup.mobile" class="input" type="number" maxlength="11" placeholder="请输入手机号" /></view>
          <view class="field"><label for="lookup-id" class="field__label">身份证号</label><input id="lookup-id" v-model="lookup.idCardNo" class="input" maxlength="18" placeholder="或输入身份证号" /></view>
        </view>
        <button class="btn btn--ghost" :loading="looking" :disabled="looking" @click="onLookup">{{ looking ? '正在查找…' : '查找并带出档案' }}</button>

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

      <view id="acq-section-1" class="section-heading"><text>02</text>现场凭证<text class="section-heading__note">拍照后自动识别</text></view>
      <!-- 现场照片（货物流证据） -->
      <view class="card">
        <view class="card__title">现场照片</view>
        <view class="photos">
          <view v-for="photo in photos" :key="photo.key" class="photo">
            <view class="photo__label">{{ photo.label }}</view>
            <button class="photo__box" :aria-label="`拍摄或更换${photo.label}`" :disabled="uploading[photo.key] || !!recognizing" @click="pickPhoto(photo.key)">
              <image
                v-if="photoUrl[photo.key] || photoData[photo.key]"
                :src="photoUrl[photo.key] || photoData[photo.key]"
                :alt="photo.label"
                mode="aspectFill"
                class="photo__img"
              />
              <view v-else class="photo__empty"><view class="photo__camera" aria-hidden="true" /><text class="photo__add">{{ photoHint(photo.key) }}</text></view>
              <text v-if="(photoUrl[photo.key] || photoData[photo.key]) && (uploading[photo.key] || recognizing === photo.key)" class="photo__progress">{{ photoHint(photo.key) }}</text>
            </button>
            <button v-if="photoUrl[photo.key] || photoData[photo.key]" class="link" :disabled="uploading[photo.key] || !!recognizing" @click="removePhoto(photo.key)">删除</button>
          </view>
        </view>
        <view class="hint">
          车牌由车辆照片自动识别（车头优先，读不出就试车尾；识别不出可手工录入，不拦提交）。
          磅单照片也会自动识别磅单号与毛皮净重，识别不到的部分手工补。
        </view>
      </view>

      <view id="acq-section-2" class="section-heading"><text>03</text>计量计价<text class="section-heading__note">以实际过磅为准</text></view>
      <!-- 数量与计价 -->
      <view class="card">
        <view class="card__title">数量与计价</view>
        <view class="field">
          <label class="field__label" for="acq-quantity">数量{{ selectedGoods?.unit ? `（${selectedGoods.unit}）` : '' }}（展示与发票明细，不参与金额）</label>
          <input id="acq-quantity" v-model="form.quantity" class="input" type="digit" placeholder="0" />
          <view v-if="prefilledQuantityText" class="hint">
            来自预约：{{ prefilledQuantityText }}，请以实际过磅为准。
          </view>
        </view>
        <view class="field">
          <label class="field__label" for="acq-grossWeight">毛重</label>
          <input id="acq-grossWeight" v-model="form.grossWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-tareWeight">皮重</label>
          <input id="acq-tareWeight" v-model="form.tareWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-netWeight">净重（自动 = 毛重 − 皮重）</label>
          <input id="acq-netWeight" v-model="form.netWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">扣杂录法</text>
          <picker :range="deductionMethodNames" :value="deductionMethodIndex" @change="onDeductionMethodChange">
            <view class="picker">{{ deductionMethodNames[deductionMethodIndex] }}</view>
          </picker>
        </view>
        <view class="field">
          <label class="field__label" for="acq-deduction">扣杂（{{ form.deductionMethod === 'RATIO' ? '比例，如 0.1' : '重量' }}）</label>
          <input id="acq-deduction" v-model="form.deduction" class="input" type="digit" placeholder="0" />
        </view>
        <view class="settlement">
          <text class="settlement__label">结算重量（自动 = 毛重 − 皮重 − 扣杂）</text>
          <text class="settlement__value">{{ settlementWeightText }}</text>
        </view>
        <view class="field">
          <label class="field__label" for="acq-unitPrice">含税单价（元）</label>
          <input id="acq-unitPrice" v-model="form.unitPrice" class="input" type="digit" placeholder="0.00" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-adjustmentAmount">调整项（元，可正可负）</label>
          <input id="acq-adjustmentAmount" v-model="form.adjustmentAmount" class="input" type="text" placeholder="如 -20 或 20" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-adjustmentReason">调整原因（运费 / 补贴 / 折让）</label>
          <input id="acq-adjustmentReason" v-model="form.adjustmentReason" class="input" placeholder="调整项非 0 时必填" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-amount">金额（元，自动 = 结算重量 × 单价 + 调整项）</label>
          <input id="acq-amount" v-model="form.amount" class="input" type="digit" placeholder="0.00" />
        </view>
      </view>

      <view id="acq-section-3" class="section-heading"><text>04</text>运输结算<text class="section-heading__note">核对车牌与交易信息</text></view>
      <!-- 磅单与结算 -->
      <view class="card">
        <view class="card__title">磅单与结算</view>
        <view class="field">
          <label class="field__label" for="acq-weightTicketNo">磅单号</label>
          <input id="acq-weightTicketNo" v-model="form.weightTicketNo" class="input" placeholder="拍磅单照片自动填，也可手输" />
        </view>
        <view v-if="weightTicketWarnings.length" class="hint hint--warn">
          <view v-for="warning in weightTicketWarnings" :key="warning">{{ warning }}</view>
        </view>
        <view v-if="weightTicketRawLines.length" class="raw">
          <button class="raw__toggle" :aria-expanded="showRawLines" @click="showRawLines = !showRawLines">
            {{ showRawLines ? '收起识别到的文字' : `展开识别到的文字（${weightTicketRawLines.length} 行）` }}
          </button>
          <view v-if="showRawLines" class="raw__list">
            <text v-for="(line, index) in weightTicketRawLines" :key="index" class="raw__line">{{ line }}</text>
          </view>
        </view>
        <view class="field">
          <label class="field__label" for="acq-weightTicketPlateNo">磅单识别车牌</label>
          <input id="acq-weightTicketPlateNo" v-model="form.weightTicketPlateNo" class="input" placeholder="如 京A12345" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-vehiclePlateNo">车头车尾识别车牌</label>
          <input id="acq-vehiclePlateNo" v-model="form.vehiclePlateNo" class="input" placeholder="拍车头照片自动填，也可手输" />
          <view v-if="plateIsManual" class="hint">
            车牌已有值（来自预约 / 交接批次或手输），拍照不会再自动识别，也不会覆盖它；要重新识别请先清空这一栏。
          </view>
        </view>
        <view class="plate" :class="plateClass">{{ plateText }}</view>
        <view class="field">
          <label class="field__label" for="acq-tradeAddress">交易地点</label>
          <input id="acq-tradeAddress" v-model="form.tradeAddress" class="input" placeholder="现场地点" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-settlementMethod">结算方式</label>
          <input id="acq-settlementMethod" v-model="form.settlementMethod" class="input" placeholder="如 银行转账，过磅后 3 日内结清" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-driverName">司机姓名（运输信息，不参与确认与收款）</label>
          <input id="acq-driverName" v-model="form.driverName" class="input" placeholder="选填" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-driverMobile">司机手机号（运输信息）</label>
          <input id="acq-driverMobile" v-model="form.driverMobile" class="input" placeholder="选填" />
        </view>
        <view class="field">
          <label class="field__label" for="acq-remark">备注</label>
          <input id="acq-remark" v-model="form.remark" class="input" placeholder="选填" />
        </view>
      </view>

      <view class="action-bar">
        <view class="action-bar__summary"><text>本次收购金额</text><view><text class="action-bar__currency">¥</text>{{ amountDisplay }}</view></view>
        <view class="action-bar__buttons"><button class="btn btn--ghost" :disabled="submitting || photoBusy" @click="onSaveDraft">暂存本地</button><button class="btn btn--primary" :loading="submitting" :disabled="submitting || photoBusy" @click="onSubmit">{{ submitting ? '正在提交…' : photoBusy ? '照片处理中…' : '提交登记' }}</button></view>
      </view>
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
  recognizeAcquisitionPlate,
  recognizeAcquisitionWeightTicket,
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
import { chooseImage, compressDataUrl, pathToDataUrl, dataUrlToUploadPath, uploadImage } from '@/utils/upload'
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
/** 正在识别车牌的那张照片：只用来把照片框的文案换成「识别中…」 */
const recognizing = ref<PhotoKey | ''>('')
/** 上一次识别回填的车牌：用来区分「框里这个值是识别来的还是一个一个字打进去的」 */
const lastRecognizedPlate = ref('')
/** 磅单识别回的提示（重量不自洽 / 车号可能不完整……），显示在磅单号下面 */
const weightTicketWarnings = ref<string[]>([])
/** 磅单识别到的原始文字行：解析取不到时人还能照着填 */
const weightTicketRawLines = ref<string[]>([])
const showRawLines = ref(false)
/** 上一次识别回填的磅单字段值：与车牌同一口径，用来区分「识别来的」与「人填的」 */
const lastRecognizedWeightTicket = ref<Record<string, string>>({})
/** 本单是否按有效磅次计量（重量由磅次决定，识别的重量不回填） */
const measuredByWeighing = ref(false)
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

const sectionTitles = ['基础信息', '现场凭证', '计量计价', '运输结算']
const photoBusy = computed(() => PHOTO_KEYS.some(key => uploading[key]) || !!recognizing.value)
const amountDisplay = computed(() => {
  const value = Number(form.amount)
  return form.amount === '' || !Number.isFinite(value) ? '—' : value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
})
function jumpTo(index: number) { uni.pageScrollTo({ selector: `#acq-section-${index}`, duration: 0 }) }

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
/**
 * 车牌是「人工／带出来的值」而不是识别结果。
 *
 * 它直接决定了拍车辆照片时调不调识别（人工值优先，ADR 0013），也决定要不要在输入框下面把这件
 * 事说明白——不然「拍了照片却什么都没发生」会被当成功能坏了。
 */
const plateIsManual = computed(
  () => !!form.vehiclePlateNo && form.vehiclePlateNo !== lastRecognizedPlate.value
)
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
    measuredByWeighing.value = !!effective
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
    // 车辆照片：拍完就识别车牌（#112）。弱网下上传失败也照识别——本地已有图，识别不依赖上传
    await recognizeVehiclePlate(key)
    // 磅单照片：拍完就识别磅单字段（#113）
    await recognizeWeightTicket(key)
  } catch (e) {
    if (!(e as Error).message?.includes('未选择')) {
      showError(e)
    }
  } finally {
    uploading[key] = false
  }
}

/** 照片框的文案：识别 / 上传 / 可拍三态 */
function photoHint(key: PhotoKey): string {
  if (recognizing.value === key) return '识别中…'
  return uploading[key] ? '上传中…' : '+ 拍照'
}

/** 腾讯要求 base64 后单张不超过 10M（与建档向导同一道拦截） */
const MAX_BASE64_LENGTH = 10 * 1024 * 1024

/**
 * 车辆照片拍完即识别车牌（#112）。
 *
 * 三条规则：
 * - **人工填过的值优先**：框里已有车牌、且它不等于上一次识别回填的值，就不调识别；
 * - **车头优先、车头读不出来再试车尾**：哪张照片刚拍就试哪张，先拍车头失败、后拍车尾时自然轮到车尾；
 * - **重拍会重新识别**：框里还是上一次识别的结果（人没改过）时照旧调——识别错了直接点照片重拍即可；
 * - **识别失败不拦提交**：提示一句「未识别到车牌，请手工录入」，照片照旧留着（ADR 0013）。
 */
async function recognizeVehiclePlate(key: PhotoKey) {
  if (key === 'weightTicketImageUrl' || plateIsManual.value) {
    return
  }
  recognizing.value = key
  try {
    const compressed = await compressDataUrl(photoData[key], MAX_BASE64_LENGTH)
    const imageBase64 = toBase64(compressed)
    if (imageBase64.length > MAX_BASE64_LENGTH) {
      // 不静默把一张必被厂商拒的图传上去
      uni.showToast({ title: '照片太大（识别要求单张不超过 10M），请重拍', icon: 'none' })
      return
    }
    const resp = await recognizeAcquisitionPlate({ imageBase64 })
    if (resp.plateNo) {
      form.vehiclePlateNo = resp.plateNo
      lastRecognizedPlate.value = resp.plateNo
      if (resp.warnings?.length) {
        // 置信度偏低之类的提示：回填了但请人核对
        uni.showToast({ title: resp.warnings[0], icon: 'none' })
      }
    } else {
      uni.showToast({ title: '未识别到车牌，请手工录入', icon: 'none' })
    }
  } catch (e) {
    // 识别失败（超时 / 未配供应商）只是一条提示：照片已留住，手工录入那条路照走
    uni.showToast({ title: (e as Error).message || '识别失败，可手工录入', icon: 'none' })
  } finally {
    recognizing.value = ''
  }
}

/** 去掉 dataURL 前缀：识别接口要的是纯 base64 */
function toBase64(dataUrl: string): string {
  const index = dataUrl.indexOf(',')
  return index >= 0 ? dataUrl.slice(index + 1) : dataUrl
}

/**
 * 磅单照片拍完即识别磅单号 / 毛重 / 皮重 / 净重 / 车号 / 扣率（#113）。
 *
 * 三条规则（与车牌同一口径）：
 * - **人工值优先**：只在空着的框里填；框里还是上一次识别回填的值（人没改过）时允许被覆盖，
 *   否则重拍一次就什么都改不了了；
 * - **按有效磅次计量的单子不回填三个重量**：重量以磅次为准，填了也不生效；
 * - **识别失败不拦提交**：提示一句并把识别到的原始文字行给出来，人照着念的表填。
 */
async function recognizeWeightTicket(key: PhotoKey) {
  if (key !== 'weightTicketImageUrl') {
    return
  }
  recognizing.value = key
  try {
    const compressed = await compressDataUrl(photoData[key], MAX_BASE64_LENGTH)
    const imageBase64 = toBase64(compressed)
    if (imageBase64.length > MAX_BASE64_LENGTH) {
      uni.showToast({ title: '照片太大（识别要求单张不超过 10M），请重拍', icon: 'none' })
      return
    }
    const resp = await recognizeAcquisitionWeightTicket({ imageBase64 })
    weightTicketWarnings.value = resp.warnings || []
    weightTicketRawLines.value = resp.rawLines || []
    fillIfVacant('weightTicketNo', resp.weightTicketNo)
    fillIfVacant('weightTicketPlateNo', resp.plateNo)
    // 录法要在扣杂值**填之前**判：填完再判就永远不成立，扣率会被当成「按重量」算
    const deductionWasVacant = !String(form.deduction ?? '')
    fillIfVacant('deduction', resp.deduction == null ? undefined : String(resp.deduction))
    if (resp.deductionMethod && deductionWasVacant) {
      form.deductionMethod = resp.deductionMethod
    }
    if (measuredByWeighing.value) {
      // 填了也不生效：与其让人以为识别没工作，不如说清重量以磅次为准
      weightTicketWarnings.value = [...weightTicketWarnings.value, '本单按有效磅次计量，重量以磅次为准']
    } else {
      fillIfVacant('grossWeight', resp.grossWeight == null ? undefined : String(resp.grossWeight))
      fillIfVacant('tareWeight', resp.tareWeight == null ? undefined : String(resp.tareWeight))
      fillIfVacant('netWeight', resp.netWeight == null ? undefined : String(resp.netWeight))
    }
    if (weightTicketWarnings.value.length) {
      uni.showToast({ title: weightTicketWarnings.value[0], icon: 'none' })
    }
    if (weightTicketRawLines.value.length && !resp.grossWeight && !resp.weightTicketNo) {
      // 一个字都没认出来：把原文摊开，让人照着念的表填（比一句「识别失败」有用）
      showRawLines.value = true
      uni.showToast({ title: '未识别出磅单内容，请展开文字手工录入', icon: 'none' })
    }
  } catch (e) {
    // 超时 / 未配供应商：照片已留住，手工录入那条路照走
    uni.showToast({ title: (e as Error).message || '磅单识别失败，可手工录入', icon: 'none' })
  } finally {
    recognizing.value = ''
  }
}

/**
 * 只在空缺处回填（人工值优先，ADR 0013）。
 *
 * 「框里那个值是不是上一次识别填的」靠 {@link lastRecognizedWeightTicket} 判：
 * 人没改过就允许被新的识别结果覆盖（否则重拍没有意义），改过就不碰。
 */
function fillIfVacant(field: keyof typeof form, value?: string) {
  if (!value) {
    return
  }
  const current = String(form[field] ?? '')
  if (current && current !== lastRecognizedWeightTicket.value[field]) {
    return
  }
  form[field] = value as never
  lastRecognizedWeightTicket.value[field] = value
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
  measuredByWeighing.value = false
  // 磅单识别（#113）的提醒也归本次登记：下一张单子要清干净
  weightTicketWarnings.value = []
  weightTicketRawLines.value = []
  showRawLines.value = false
  lastRecognizedWeightTicket.value = {}
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
  if (!form.payeeId) return '请先带收方档案'
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
  if (submitting.value || photoBusy.value) return
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
  if (submitting.value || photoBusy.value) return
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
.page { box-sizing: border-box; width: 100%; max-width: 480px; min-height: 100vh; margin: 0 auto; padding: 22px 16px calc(28px + env(safe-area-inset-bottom)); color: #233e2e; background: #f3f7f5; font-size: 16px; line-height: 1.6; }
.page--editing { padding-bottom: calc(160px + env(safe-area-inset-bottom)); }
button { box-sizing: border-box; margin: 0; font-family: inherit; cursor: pointer; transition: background-color 180ms; &::after { border: 0; } &:focus-visible { outline: 3px solid #76a98c; outline-offset: 2px; } &[disabled] { opacity: .65; } }
.intro { padding: 0 4px; &__eyebrow { font-size: 12px; color: #617468; } &__title { margin-top: 6px; font-size: 27px; font-weight: 700; } &__desc { margin-top: 6px; font-size: 14px; color: #617468; } }
.section-nav { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin: 22px 0 26px; padding: 12px 6px; border-radius: 16px; background: #e5efe8; button { min-width: 0; padding: 0; min-height: 52px; font-size: 12px; line-height: 1.7; color: #28573e; background: transparent; > text { display: block; font-size: 16px; font-weight: 600; } &:active { background: #d1e3d7; } } }
.section-heading { display: flex; align-items: center; gap: 8px; margin: 26px 2px 12px; font-size: 18px; font-weight: 600; scroll-margin-top: 60px; > text:first-child { font-size: 12px; font-weight: 500; color: #176b4c; background: #e0eee5; padding: 3px 6px; border-radius: 6px; } &__note { margin-left: auto; color: #617468; font-size: 11px; font-weight: 400; } }
.card { padding: 20px 18px; margin-bottom: 14px; border: 1px solid #e0e9e3; border-radius: 18px; background: #fff; &__title { margin-bottom: 16px; font-size: 17px; font-weight: 600; overflow-wrap: anywhere; } &__note { margin: -8px 0 16px; color: #617468; font-size: 13px; } }
.row { display: grid; gap: 0; margin-bottom: 16px; }
.field { margin-bottom: 18px; &__label { display: block; margin-bottom: 8px; color: #395747; font-size: 14px; line-height: 1.6; } &:last-child { margin-bottom: 0; } }
.input { box-sizing: border-box; width: 100%; min-width: 0; height: 48px; padding: 0 13px; color: #233e2e; font-size: 16px; background: #f5f8f6; border: 1px solid #dce6df; border-radius: 10px; &:focus-within { border-color: #176b4c; box-shadow: 0 0 0 2px #e0eee5; } }
.picker { position: relative; box-sizing: border-box; min-height: 48px; padding: 12px 32px 12px 13px; line-height: 24px; font-size: 16px; border: 1px solid #dce6df; border-radius: 10px; background: #f5f8f6; overflow-wrap: anywhere; cursor: pointer; &::after { content: ''; position: absolute; right: 16px; top: 18px; width: 7px; height: 7px; border-right: 1.5px solid #617468; border-bottom: 1.5px solid #617468; transform: rotate(45deg); } }
.kv { margin-top: 16px; padding: 12px; border-radius: 10px; background: #f5f8f6; &__row { display: flex; justify-content: space-between; gap: 16px; padding: 5px 0; font-size: 13px; > text:last-child { text-align: right; overflow-wrap: anywhere; min-width: 0; } } &__k { flex-shrink: 0; color: #617468; } }
.photos { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.photo { min-width: 0; text-align: center; &__label { margin-bottom: 8px; font-size: 12px; color: #526c5b; } &__box { position: relative; display: flex; align-items: center; justify-content: center; width: 100%; height: 104px; padding: 0; border: 1px dashed #a8c5b2; background: #f0f7f2; border-radius: 12px; overflow: hidden; } &__img { width: 100%; height: 100%; } &__empty { display: flex; flex-direction: column; align-items: center; gap: 10px; } &__add { font-size: 12px; color: #35664a; line-height: 1.5; } &__camera { position: relative; width: 25px; height: 18px; border: 1.5px solid #488363; border-radius: 5px; &::before { content: ''; position: absolute; width: 8px; height: 8px; border: 1.5px solid #488363; border-radius: 50%; left: 7px; top: 4px; } &::after { content: ''; position: absolute; width: 10px; height: 3px; background: #488363; border-radius: 2px 2px 0 0; top: -4px; left: 6px; } } &__progress { position: absolute; bottom: 0; left: 0; right: 0; color: #fff; background: #174e3b; font-size: 11px; line-height: 26px; } }
.settlement { display: flex; flex-direction: column; gap: 6px; margin: 4px 0 20px; padding: 16px; background: #e5f0e9; border-radius: 12px; &__label { font-size: 12px; color: #456b54; } &__value { font-weight: 600; font-size: 22px; color: #176b4c; overflow-wrap: anywhere; } }
.plate { margin: 4px 0 20px; padding: 12px; border-radius: 10px; background: #f2f5f3; color: #617468; font-size: 14px; &--ok { background: #eaf5ed; color: #21633c; } &--bad { background: #fff1ec; color: #963a1b; } }
.seller { margin-top: 16px; padding: 16px; background: #edf5ef; border-radius: 12px; &__name { font-size: 19px; font-weight: 600; } &__meta { margin-top: 4px; font-size: 14px; color: #617468; overflow-wrap: anywhere; } }
.appointment { padding: 14px; margin-top: 12px; border: 1px solid #dde7e0; border-radius: 12px; cursor: pointer; &--active { border-color: #176b4c; background: #edf5ef; } &__row { display: flex; justify-content: space-between; gap: 12px; } &__cat { font-weight: 600; } &__qty { color: #176b4c; } &__meta { margin-top: 6px; color: #617468; font-size: 13px; } &__actions { display: flex; gap: 24px; } }
.arrangement__item { margin-top: 12px; }
.hint { margin-top: 12px; line-height: 1.75; color: #617468; font-size: 13px; &--warn { padding: 12px; border-radius: 10px; background: #fff3e8; color: #8b461d; } }
.raw { margin-top: 12px; &__toggle { width: 100%; min-height: 44px; padding: 8px 0; text-align: left; color: #176b4c; background: transparent; font-size: 14px; line-height: 1.6; } &__list { margin: 8px 0 16px; padding: 12px; border-radius: 10px; background: #f5f8f6; } &__line { display: block; font-size: 12px; line-height: 1.7; color: #617468; overflow-wrap: anywhere; } }
.link { display: inline-flex; align-items: center; min-height: 44px; padding: 0 4px; color: #176b4c; font-size: 14px; background: transparent; line-height: 1.6; &--danger { color: #963a1b; } }
.btn { width: 100%; min-height: 48px; padding: 10px 12px; font-size: 16px; line-height: 26px; border-radius: 11px; &--primary { color: #fff; background: #176b4c; &:active { background: #12543b; } } &--ghost { color: #176b4c; background: #f3f8f5; border: 1px solid #bdd5c6; &:active { background: #e3efe7; } } }
.action-bar { position: fixed; z-index: 10; left: 50%; bottom: 0; transform: translateX(-50%); box-sizing: border-box; width: 100%; max-width: 480px; padding: 12px 16px calc(14px + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #dce6df; &__summary { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 10px; > text { font-size: 13px; color: #617468; flex-shrink: 0; } > view { font-size: 23px; font-weight: 600; color: #174e3b; overflow-wrap: anywhere; text-align: right; min-width: 0; } } &__currency { font-size: 14px; margin-right: 4px; } &__buttons { display: grid; grid-template-columns: 1fr 1.6fr; gap: 12px; } }
.tip { padding: 4px; color: #617468; font-size: 12px; line-height: 1.8; }
.result { padding: 28px 20px; &__title { color: #176b4c; font-size: 26px; font-weight: 700; } &__no { margin-top: 10px; color: #617468; font-size: 13px; overflow-wrap: anywhere; } &__quota { margin-top: 22px; padding: 16px; background: #edf5ef; border-radius: 12px; line-height: 1.7; &--warn { background: #fff1ec; color: #963a1b; } } &__warn { margin-top: 16px; color: #8b461d; font-size: 14px; } &__actions { display: flex; gap: 12px; margin-top: 24px; } }
@media (prefers-reduced-motion: reduce) { button { transition: none; } }
</style>
