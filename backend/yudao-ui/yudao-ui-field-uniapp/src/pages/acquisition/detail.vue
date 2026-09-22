<template>
  <view v-if="acquisition" class="page">
    <view class="summary">
      <view class="summary__top">
        <text class="summary__label">收购金额（元）</text>
        <text class="summary__badge">{{ acquisition.statusName || '状态未知' }}</text>
      </view>
      <view class="summary__amount">{{ money(acquisition.amount) }}</view>
      <view class="summary__hint">{{ statusHint }}</view>
      <!-- 异常不替换档位：钱付了但票没开出来这种事，不能让一个「已付款」盖住（ADR 0021 / 0038） -->
      <view v-if="acquisition.abnormal" class="summary__abnormal">
        <text v-for="reason in acquisition.abnormalReasons || []" :key="reason" class="summary__abnormal-item">
          {{ reason }}
        </text>
      </view>
      <view class="summary__order"><text>收购单号</text><text selectable>{{ acquisition.acquisitionNo || '—' }}</text></view>
    </view>

    <view class="card">
      <view class="card__title">出售者信息</view>
      <view class="seller">
        <view class="seller__avatar" aria-hidden="true">{{ (acquisition.sellerName || '售').slice(0, 1) }}</view>
        <view class="seller__body">
          <view class="seller__name">{{ acquisition.sellerName || '未提供姓名' }}</view>
          <view class="seller__type">{{ acquisition.sellerSubjectTypeName || '自然人出售者' }}</view>
        </view>
      </view>
      <view class="kv"><text class="kv__k">联系电话</text><text selectable>{{ acquisition.sellerMobile || '—' }}</text></view>
    </view>

    <view class="card">
      <view class="card__title">货物与计价</view>
      <view class="goods"><text class="goods__name">{{ acquisition.categoryName || '未提供品类' }}</text><text class="goods__spec">{{ acquisition.specification || '未填写规格' }}</text></view>
      <view class="kv"><text class="kv__k">数量</text><text>{{ acquisition.quantity ?? '—' }} {{ acquisition.unit }}</text></view>
      <view class="kv"><text class="kv__k">单价</text><text>{{ acquisition.unitPrice ?? '—' }} 元</text></view>
      <view class="weights">
        <view><view class="weights__label">毛重</view><view class="weights__value">{{ acquisition.grossWeight ?? '—' }}</view></view>
        <view><view class="weights__label">皮重</view><view class="weights__value">{{ acquisition.tareWeight ?? '—' }}</view></view>
        <view><view class="weights__label">净重</view><view class="weights__value">{{ acquisition.netWeight ?? '—' }}</view></view>
      </view>
      <view class="kv"><text class="kv__k">扣杂</text><text>{{ deductionText }}</text></view>
      <view class="settlement-weight"><view>结算重量<view class="settlement-weight__hint">计价基准</view></view><text>{{ acquisition.settlementWeight ?? '—' }}</text></view>
      <view v-if="acquisition.adjustmentAmount" class="kv">
        <text class="kv__k">调整项</text><text>{{ acquisition.adjustmentAmount }} 元{{ acquisition.adjustmentReason ? `（${acquisition.adjustmentReason}）` : '' }}</text>
      </view>
      <view class="kv"><text class="kv__k">结算方式</text><text>{{ acquisition.settlementMethod || '—' }}</text></view>
    </view>

    <view class="card">
      <view class="card__title">运输与交易</view>
      <view v-if="acquisition.driverName" class="kv"><text class="kv__k">司机</text><text>{{ acquisition.driverName }} {{ acquisition.driverMobile }}</text></view>
      <view class="kv"><text class="kv__k">磅单号</text><text selectable>{{ acquisition.weightTicketNo || '—' }}</text></view>
      <view class="kv"><text class="kv__k">交易地点</text><text>{{ acquisition.tradeAddress || '—' }}</text></view>
    </view>

    <view class="card">
      <view class="card__title">开票信息</view>
      <view class="kv"><text class="kv__k">税收分类编码</text><text selectable>{{ acquisition.mergedCode || '—' }}</text></view>
      <view v-if="acquisition.invoicePartnerOrderId" class="kv">
        <text class="kv__k">开票单号</text><text selectable>{{ acquisition.invoicePartnerOrderId }}</text>
      </view>
      <button
        v-if="canDownloadInvoice"
        class="btn btn--ghost invoice-download"
        :loading="downloadingInvoice"
        :disabled="downloadingInvoice"
        @click="onDownloadInvoice"
      >
        下载反向发票（PDF）
      </button>
    </view>

    <view class="card">
      <view class="card__title">现场照片与车牌</view>
      <view class="photos">
        <view v-for="photo in photoViews" :key="photo.label" class="photo">
          <view class="photo__label">{{ photo.label }}</view>
          <image v-if="photo.url" :src="photo.url" mode="aspectFill" class="photo__img" @click="preview(photo.url)" />
          <view v-else class="photo__empty"><view class="photo__placeholder" aria-hidden="true" />未上传照片</view>
        </view>
      </view>
      <view class="kv"><text class="kv__k">磅单识别车牌</text><text>{{ acquisition.weightTicketPlateNo || '-' }}</text></view>
      <view class="kv"><text class="kv__k">车头车尾识别车牌</text><text>{{ acquisition.vehiclePlateNo || '-' }}</text></view>
      <view class="plate" :class="plateClass">{{ plateText }}</view>

      <button class="btn btn--ghost correction-toggle" :aria-expanded="showCorrect" @click="showCorrect = !showCorrect">
        {{ showCorrect ? '收起修正' : '修正识别结果' }}
      </button>

      <view v-if="showCorrect" class="correct">
        <view class="correct__title">修正识别结果</view>
        <view class="correct__hint">填写需要修正的内容，保存后重新比对车牌。</view>
        <view class="field">
          <text class="field__label">毛重</text>
          <input v-model="correct.grossWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">皮重</text>
          <input v-model="correct.tareWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">净重</text>
          <input v-model="correct.netWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">扣杂录法</text>
          <picker :range="deductionMethodNames" :value="correctDeductionMethodIndex" @change="onDeductionMethodChange">
            <view class="picker">{{ deductionMethodNames[correctDeductionMethodIndex] }}</view>
          </picker>
        </view>
        <view class="field">
          <text class="field__label">扣杂（{{ correct.deductionMethod === 'RATIO' ? '比例，如 0.1' : '重量' }}）</text>
          <input v-model="correct.deduction" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">含税单价</text>
          <input v-model="correct.unitPrice" class="input" type="digit" placeholder="0.00" />
        </view>
        <view class="field">
          <text class="field__label">调整项（元，可正可负）</text>
          <input v-model="correct.adjustmentAmount" class="input" type="digit" placeholder="0.00" />
        </view>
        <view class="field">
          <text class="field__label">调整原因</text>
          <input v-model="correct.adjustmentReason" class="input" placeholder="调整项非 0 时必填" />
        </view>
        <view class="field">
          <text class="field__label">磅单号</text>
          <input v-model="correct.weightTicketNo" class="input" placeholder="磅单号" />
        </view>
        <view class="field">
          <text class="field__label">磅单识别车牌</text>
          <input v-model="correct.weightTicketPlateNo" class="input" placeholder="如 京A12345" />
        </view>
        <view class="field">
          <text class="field__label">车头车尾识别车牌</text>
          <input v-model="correct.vehiclePlateNo" class="input" placeholder="如 京A12345" />
        </view>
        <view class="plate" :class="correctPlateClass">{{ correctPlateText }}</view>
        <view class="field">
          <text class="field__label">修正说明</text>
          <input v-model="correct.remark" class="input" placeholder="如 磅单识别把 3 看成 8" />
        </view>
        <button class="btn btn--primary" :loading="saving" :disabled="saving" @click="onCorrect">保存并重新比对</button>
      </view>
    </view>

    <view class="document-actions">
      <button class="btn btn--ghost" :loading="exporting" :disabled="exporting" @click="onExport">导出确认书（Excel）</button>
      <button class="btn btn--ghost" @click="onPrint">打印本页</button>
    </view>
    <view class="actions">
      <view class="actions__hint">结束收货后生成结算单，交由出售者确认；他一确认，系统就自动发起开票</view>
      <button class="btn btn--primary" :loading="generating" :disabled="generating" @click="onEndBatch">结束本次收货 · 生成结算单</button>
    </view>
  </view>
  <view v-else class="page empty-state">
    <view class="empty-state__title">{{ loadError ? '暂时无法查看收购单' : '正在加载收购单' }}</view>
    <view class="empty-state__desc">{{ loadError || '正在读取收购明细，请稍候…' }}</view>
    <button v-if="loadError && acquisitionId" class="btn btn--primary" @click="load(acquisitionId)">重新加载</button>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { downloadInvoice, getAcquisition, correctAcquisition, AcquisitionVO } from '@/api/acquisition'
import { generateSettlement } from '@/api/settlement'
import { downloadWithAuth } from '@/utils/download'
import { comparePlate } from '@/utils/plate'

defineOptions({ name: 'FieldAcquisitionDetail' })

const acquisition = ref<AcquisitionVO | null>(null)
const acquisitionId = ref(0)
const loadError = ref('')
const exporting = ref(false)

function money(value?: number) {
  if (value == null || !Number.isFinite(Number(value))) return '—'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
const saving = ref(false)
const generating = ref(false)
const showCorrect = ref(false)

const correct = reactive({
  grossWeight: '',
  tareWeight: '',
  netWeight: '',
  deduction: '',
  deductionMethod: 'WEIGHT',
  unitPrice: '',
  adjustmentAmount: '',
  adjustmentReason: '',
  weightTicketNo: '',
  weightTicketPlateNo: '',
  vehiclePlateNo: '',
  remark: ''
})

const deductionMethodNames = ['按重量', '按比例']
const correctDeductionMethodIndex = computed(() => (correct.deductionMethod === 'RATIO' ? 1 : 0))

function onDeductionMethodChange(event: any) {
  correct.deductionMethod = Number(event.detail.value) === 1 ? 'RATIO' : 'WEIGHT'
}

const deductionText = computed(() => {
  const value = acquisition.value?.deduction
  if (value == null) return '-'
  return acquisition.value?.deductionMethod === 'RATIO' ? `${value}（比例）` : `${value}`
})

const photoViews = computed(() => [
  { label: '磅单', url: acquisition.value?.weightTicketImageUrl || '' },
  { label: '车头', url: acquisition.value?.vehicleFrontImageUrl || '' },
  { label: '车尾', url: acquisition.value?.vehicleRearImageUrl || '' }
])

const statusHint = computed(() => {
  // 档位与「下一步是谁的事」都由后端按开票单的四条状态线派生（ADR 0038），前端不再自己拼一套
  return acquisition.value?.statusNextStep || ''
})

/** 票开出来了才有原件可下（「已上传税局」是另一条线，不影响能不能下） */
const canDownloadInvoice = computed(
  () => acquisition.value?.status === 3 && !!acquisition.value?.invoicePartnerOrderId
)
const downloadingInvoice = ref(false)

async function onDownloadInvoice() {
  const partnerOrderId = acquisition.value?.invoicePartnerOrderId
  if (!partnerOrderId) return
  downloadingInvoice.value = true
  try {
    const record = await downloadInvoice(partnerOrderId)
    const files = record.files || []
    // 工行只回 PDF（工行答复 2026-09-18）；真拿了别的格式也照下，不把票扣在手里
    const file = files.find((item) => (item.fileType || '').toUpperCase() === 'PDF') || files[0]
    if (!file?.downloadId) {
      uni.showModal({
        title: '暂时拿不到发票原件',
        content: record.errorMsg || '工行还没回发票文件，稍后再试，或让开票员在后台重试。',
        showCancel: false
      })
      return
    }
    await downloadWithAuth(
      `/icbc/invoice-download/download-file?downloadId=${file.downloadId}&fileType=${file.fileType || 'PDF'}`,
      file.fileName || `${acquisition.value?.acquisitionNo || 'invoice'}.pdf`
    )
  } catch (e) {
    uni.showModal({ title: '下载失败', content: (e as Error).message, showCancel: false })
  } finally {
    downloadingInvoice.value = false
  }
}

const plateText = computed(() => {
  const matched = acquisition.value?.plateMatched
  if (matched === true) return '车牌比对：一致'
  if (matched === false) return '车牌比对：不一致'
  return '车牌比对：无法比对（缺车牌）'
})
const plateClass = computed(() => ({
  'plate--ok': acquisition.value?.plateMatched === true,
  'plate--bad': acquisition.value?.plateMatched === false
}))

const correctPlateResult = computed(() =>
  comparePlate(correct.weightTicketPlateNo, correct.vehiclePlateNo)
)
const correctPlateText = computed(() => {
  if (correctPlateResult.value === null) return '车牌比对：无法比对（缺车牌）'
  return correctPlateResult.value ? '车牌比对：一致' : '车牌比对：不一致'
})
const correctPlateClass = computed(() => ({
  'plate--ok': correctPlateResult.value === true,
  'plate--bad': correctPlateResult.value === false
}))

onLoad((options) => {
  const id = Number(options?.id)
  if (Number.isSafeInteger(id) && id > 0) {
    acquisitionId.value = id
    load(id)
  } else {
    loadError.value = '收购单编号无效，请返回列表重新打开。'
  }
})

async function load(id: number) {
  loadError.value = ''
  try {
    acquisition.value = await getAcquisition(id)
    if (!acquisition.value) loadError.value = '未找到这张收购单，请返回列表核对。'
    showCorrect.value = false
  } catch (e) {
    loadError.value = (e as Error).message || '网络异常，请稍后重试。'
  }
}

function preview(url: string) {
  uni.previewImage({ urls: [url], current: url })
}

async function onCorrect() {
  if (!acquisition.value?.id) return
  saving.value = true
  try {
    await correctAcquisition({
      id: acquisition.value.id,
      grossWeight: toNum(correct.grossWeight) ?? undefined,
      tareWeight: toNum(correct.tareWeight) ?? undefined,
      netWeight: toNum(correct.netWeight) ?? undefined,
      deduction: toNum(correct.deduction) ?? undefined,
      deductionMethod: correct.deductionMethod,
      unitPrice: toNum(correct.unitPrice) ?? undefined,
      adjustmentAmount: toNum(correct.adjustmentAmount) ?? undefined,
      adjustmentReason: correct.adjustmentReason || undefined,
      weightTicketNo: correct.weightTicketNo || undefined,
      weightTicketPlateNo: correct.weightTicketPlateNo || undefined,
      vehiclePlateNo: correct.vehiclePlateNo || undefined,
      remark: correct.remark || undefined
    })
    uni.showToast({ title: '已修正', icon: 'success' })
    await load(acquisition.value.id)
  } catch (e) {
    uni.showModal({ title: '修正失败', content: (e as Error).message, showCancel: false })
  } finally {
    saving.value = false
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

/** 结束本次收货：把这位出售者尚未归组的收购单聚合成一张结算单，然后转达确认链接 */
async function onEndBatch() {
  if (!acquisition.value?.payeeId) {
    uni.showToast({ title: '该收购单缺少收方档案', icon: 'none' })
    return
  }
  generating.value = true
  try {
    await generateSettlement({ payeeId: acquisition.value.payeeId })
    uni.showToast({ title: '结算单已生成', icon: 'success' })
    uni.navigateTo({ url: '/pages/settlement/index' })
  } catch (e) {
    uni.showModal({ title: '未能生成', content: (e as Error).message, showCancel: false })
  } finally {
    generating.value = false
  }
}

function toNum(value: string): number | null {
  if (value === '' || value == null) return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}
</script>

<style lang="scss" scoped>
.page { box-sizing: border-box; width: 100%; max-width: 480px; min-height: 100vh; margin: 0 auto; padding: 20px 16px calc(128px + env(safe-area-inset-bottom)); color: #233e2e; background: #f3f7f5; }
button { cursor: pointer; &::after { border: 0; } &:focus-visible { outline: 3px solid #80b69a; outline-offset: 3px; } }
.summary { padding: 22px 20px 18px; margin-bottom: 18px; color: #fff; background: #174e3b; border-radius: 20px; box-shadow: 0 8px 20px rgba(23,78,59,.09);
  &__top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
  &__label { font-size: 13px; color: #d3e5da; }
  &__badge { padding: 5px 10px; color: #e2f6e9; background: #346650; border: 1px solid #4f7e65; border-radius: 7px; font-size: 12px; }
  &__amount { margin: 16px 0 10px; font-size: 32px; font-weight: 600; line-height: 1.3; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
  &__hint { font-size: 12px; line-height: 1.7; color: #d3e5da; }
  &__abnormal { display: flex; flex-direction: column; gap: 4px; margin-top: 10px; padding: 10px 12px; background: rgba(255, 255, 255, 0.14); border-left: 3px solid #ffd9a0; border-radius: 8px;
    &-item { font-size: 12px; line-height: 1.6; color: #ffe9c7; }
  }
  &__order { display: flex; flex-wrap: wrap; gap: 6px 12px; justify-content: space-between; padding-top: 15px; margin-top: 18px; border-top: 1px solid #41705b; font-size: 11px; color: #d3e5da; overflow-wrap: anywhere; }
}
.card { padding: 20px 16px; margin-bottom: 14px; background: #fff; border: 1px solid #e4ece7; border-radius: 18px;
  &__title { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; font-size: 17px; font-weight: 600; &::before { content: ''; width: 3px; height: 15px; border-radius: 2px; background: #358764; } }
}
.kv { display: flex; justify-content: space-between; align-items: baseline; padding: 12px 0; gap: 16px; font-size: 14px; line-height: 1.65; border-bottom: 1px solid #f0f3f1;
  &:last-child { border-bottom: 0; padding-bottom: 0; }
  &__k { color: #6b7e71; flex: 0 0 auto; max-width: 48%; }
  > :last-child { min-width: 0; text-align: right; overflow-wrap: anywhere; }
}
.invoice-download { margin-top: 14px; }
.seller { display: flex; align-items: center; gap: 12px; padding-bottom: 12px;
  &__avatar { display: flex; justify-content: center; align-items: center; flex-shrink: 0; width: 42px; height: 42px; color: #176b4c; background: #eaf3ed; border-radius: 13px; font-size: 19px; font-weight: 600; }
  &__body { min-width: 0; }
  &__name { font-size: 18px; font-weight: 600; overflow-wrap: anywhere; }
  &__type { margin-top: 5px; color: #6b7e71; font-size: 12px; }
}
.goods { display: flex; align-items: baseline; flex-wrap: wrap; gap: 8px 12px; margin: 4px 0 10px; &__name { font-size: 20px; font-weight: 600; } &__spec { color: #6b7e71; font-size: 12px; overflow-wrap: anywhere; } }
.weights { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); margin: 16px 0 4px; padding: 16px 0; background: #f4f8f5; border-radius: 12px;
  > view { text-align: center; padding: 0 6px; &:not(:last-child) { border-right: 1px solid #dfe9e2; } }
  &__label { color: #6b7e71; font-size: 12px; }
  &__value { margin-top: 8px; font-size: 17px; font-weight: 600; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
}
.settlement-weight { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin: 12px 0 4px; padding: 14px; background: #eaf4ed; color: #176b4c; border-radius: 12px; font-size: 14px;
  > text { font-size: 23px; font-weight: 600; min-width: 0; overflow-wrap: anywhere; text-align: right; }
  &__hint { margin-top: 5px; font-size: 11px; color: #567a63; }
}
.photos { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 10px; margin-bottom: 16px; }
.photo { min-width: 0;
  &__label { margin-bottom: 8px; color: #63796a; font-size: 12px; }
  &__img { width: 100%; height: 86px; border-radius: 10px; cursor: pointer; }
  &__empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; height: 86px; background: #f4f7f5; border: 1px dashed #d4dfd8; border-radius: 10px; color: #6b7e71; font-size: 11px; }
  &__placeholder { width: 21px; height: 17px; border: 1.5px solid #96ae9f; border-radius: 3px; position: relative; &::after { content: ''; position: absolute; width: 8px; height: 8px; left: 5px; top: 6px; border-top: 1.5px solid #96ae9f; border-left: 1.5px solid #96ae9f; transform: rotate(45deg); } }
}
.plate { margin: 14px 0; padding: 12px; border-radius: 10px; background: #f3f6f4; color: #63796a; font-size: 13px; line-height: 1.6;
  &--ok { color: #216742; background: #eaf5ed; }
  &--bad { color: #a33535; background: #fff0ee; }
}
.correct { margin-top: 22px; padding-top: 20px; border-top: 1px solid #e5ece7; &__title { font-size: 17px; font-weight: 600; } &__hint { margin: 8px 0 18px; color: #6b7e71; font-size: 12px; line-height: 1.7; } }
.field { margin-bottom: 18px; &__label { display: block; margin-bottom: 8px; color: #405c49; font-size: 14px; line-height: 1.5; } }
.input, .picker { box-sizing: border-box; width: 100%; height: 50px; padding: 0 14px; background: #f7f9f7; border: 1px solid #dce6df; border-radius: 11px; font-size: 16px; &:focus-within { border-color: #278158; box-shadow: 0 0 0 3px #e8f2eb; } }
.picker { display: flex; align-items: center; justify-content: space-between; &::after { content: '⌄'; color: #6b7e71; } }
.btn { margin: 0; padding: 0 12px; min-height: 48px; border-radius: 11px; font-size: 14px; font-weight: 600; line-height: 48px;
  &--primary { background: #176b4c; color: #fff; &:active { background: #125c40; } }
  &--ghost { color: #176b4c; background: #fff; border: 1px solid #ccded2; &:active { background: #eaf3ed; } }
  &[disabled] { color: #657b6c; background: #e3ece6; }
}
.document-actions { display: grid; grid-template-columns: minmax(0,1.5fr) minmax(0,1fr); gap: 12px; margin-top: 18px; }
.actions { position: fixed; z-index: 10; bottom: 0; left: 50%; transform: translateX(-50%); box-sizing: border-box; width: 100%; max-width: 480px; padding: 10px 20px calc(14px + env(safe-area-inset-bottom)); border-top: 1px solid #e5ece7; background: #fff; box-shadow: 0 -4px 20px rgba(23,78,59,.04);
  &__hint { margin-bottom: 8px; color: #63796a; font-size: 11px; text-align: center; line-height: 1.5; }
}
.empty-state { padding-top: 72px; text-align: center; &__title { font-size: 20px; font-weight: 600; } &__desc { padding: 16px 0 24px; color: #63796a; font-size: 14px; line-height: 1.7; overflow-wrap: anywhere; } }
@media print {
  .page { max-width: none; padding: 0; background: #fff; }
  .actions, .document-actions, .correction-toggle, .correct { display: none; }
  .card, .summary { break-inside: avoid; box-shadow: none; border: 1px solid #ccc; }
  .summary { color: #203b2e; background: #fff; }
  .summary__label, .summary__hint, .summary__order, .summary__badge { color: #203b2e; background: transparent; }
}
</style>
