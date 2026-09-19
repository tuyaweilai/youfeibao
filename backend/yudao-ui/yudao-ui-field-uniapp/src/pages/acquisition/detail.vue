<template>
  <view v-if="acquisition" class="page">
    <view class="card status">
      <view class="status__label">当前进度</view>
      <view class="status__name">{{ acquisition.statusName || '未知' }}</view>
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
      <view class="kv"><text class="kv__k">毛重 / 皮重 / 净重</text><text>{{ acquisition.grossWeight ?? '-' }} / {{ acquisition.tareWeight ?? '-' }} / {{ acquisition.netWeight ?? '-' }}</text></view>
      <view class="kv"><text class="kv__k">扣杂</text><text>{{ deductionText }}</text></view>
      <view class="kv"><text class="kv__k">结算重量（计价基准）</text><text>{{ acquisition.settlementWeight ?? '-' }}</text></view>
      <view v-if="acquisition.adjustmentAmount" class="kv">
        <text class="kv__k">调整项</text><text>{{ acquisition.adjustmentAmount }} 元{{ acquisition.adjustmentReason ? `（${acquisition.adjustmentReason}）` : '' }}</text>
      </view>
      <view v-if="acquisition.driverName" class="kv"><text class="kv__k">司机</text><text>{{ acquisition.driverName }} {{ acquisition.driverMobile }}</text></view>
      <view class="kv"><text class="kv__k">磅单号</text><text>{{ acquisition.weightTicketNo || '-' }}</text></view>
      <view class="kv"><text class="kv__k">交易地点</text><text>{{ acquisition.tradeAddress || '-' }}</text></view>
      <view class="kv"><text class="kv__k">结算方式</text><text>{{ acquisition.settlementMethod || '-' }}</text></view>
      <view class="kv"><text class="kv__k">税收分类编码</text><text>{{ acquisition.mergedCode || '-' }}</text></view>
      <view v-if="acquisition.invoicePartnerOrderId" class="kv">
        <text class="kv__k">开票单号</text><text>{{ acquisition.invoicePartnerOrderId }}</text>
      </view>
    </view>

    <view class="card">
      <view class="card__title">现场照片与车牌</view>
      <view class="photos">
        <view v-for="photo in photoViews" :key="photo.label" class="photo">
          <view class="photo__label">{{ photo.label }}</view>
          <image v-if="photo.url" :src="photo.url" mode="aspectFill" class="photo__img" @click="preview(photo.url)" />
          <view v-else class="photo__empty">未拍</view>
        </view>
      </view>
      <view class="kv"><text class="kv__k">磅单识别车牌</text><text>{{ acquisition.weightTicketPlateNo || '-' }}</text></view>
      <view class="kv"><text class="kv__k">车头车尾识别车牌</text><text>{{ acquisition.vehiclePlateNo || '-' }}</text></view>
      <view class="plate" :class="plateClass">{{ plateText }}</view>

      <button class="btn btn--ghost" @click="showCorrect = !showCorrect">
        {{ showCorrect ? '收起修正' : '修正识别结果' }}
      </button>

      <view v-if="showCorrect" class="correct">
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
        <button class="btn btn--primary" :loading="saving" @click="onCorrect">保存并重新比对</button>
      </view>
    </view>

    <view class="actions">
      <button class="btn btn--primary" :loading="exporting" @click="onExport">导出确认书（Excel）</button>
      <button class="btn btn--ghost" @click="onPrint">打印本页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAcquisition, correctAcquisition, AcquisitionVO } from '@/api/acquisition'
import { downloadWithAuth } from '@/utils/download'
import { comparePlate } from '@/utils/plate'

defineOptions({ name: 'FieldAcquisitionDetail' })

const acquisition = ref<AcquisitionVO | null>(null)
const exporting = ref(false)
const saving = ref(false)
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
  if (id) {
    load(id)
  }
})

async function load(id: number) {
  try {
    acquisition.value = await getAcquisition(id)
    showCorrect.value = false
  } catch (e) {
    uni.showModal({ title: '加载失败', content: (e as Error).message, showCancel: false })
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

function toNum(value: string): number | null {
  if (value === '' || value == null) return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
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

.photos {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.photo {
  flex: 1;

  &__label {
    margin-bottom: 8rpx;
    color: $field-text-secondary;
    font-size: 24rpx;
  }

  &__img {
    width: 100%;
    height: 160rpx;
    border-radius: 12rpx;
  }

  &__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 160rpx;
    background-color: #f5f6f8;
    border-radius: 12rpx;
    color: $field-text-secondary;
    font-size: 24rpx;
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

.correct {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #eef0f3;
}

.input {
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

.actions {
  display: flex;
  gap: 16rpx;
}

.btn {
  flex: 1;
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
</style>
