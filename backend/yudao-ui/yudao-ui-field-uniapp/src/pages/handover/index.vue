<template>
  <view class="page">
    <!-- 还没有选中批次：登记一个交接批次 -->
    <template v-if="!batch">
      <view class="card">
        <view class="card__title">谁、在哪儿、怎么来的</view>
        <view class="card__hint">
          一个交易对方的一次物理交接记为一个交接批次。同一车同一天两次送货是两个批次，
          磅单与收购单各归各；没有预约、没有采购订单也能建批次。
        </view>
        <input v-model="lookup" class="input" placeholder="出售者手机号或身份证号" />
        <button class="btn btn--ghost" :loading="finding" @click="onFind">带出档案</button>
        <view v-if="seller" class="found">
          <text class="found__name">{{ seller.name }}</text>
          <text class="found__meta">{{ seller.mobile || seller.idCardNo }}</text>
        </view>
        <view v-else-if="lookedUp" class="hint hint--warn">
          没查到档案。新出售者要先建档（实名 / 收方入驻），再回来建批次。
        </view>
      </view>

      <view v-if="seller" class="card">
        <view class="card__title">交接信息</view>
        <view class="field">
          <text class="field__label">场站（到场收货填）</text>
          <picker :range="stationNames" :value="stationIndex < 0 ? 0 : stationIndex" @change="onStationChange">
            <view class="picker">{{ selectedStation?.name || '请选择场站' }}</view>
          </picker>
        </view>
        <view class="field">
          <text class="field__label">上门地址（上门回收填；与场站至少填一个）</text>
          <input v-model="form.visitAddress" class="input" placeholder="如：某某路 1 号" />
        </view>
        <view class="field">
          <text class="field__label">来源方式</text>
          <picker :range="sourceTypeNames" :value="sourceTypeIndex" @change="onSourceTypeChange">
            <view class="picker">{{ sourceTypeNames[sourceTypeIndex] }}</view>
          </picker>
        </view>
        <view class="field">
          <text class="field__label">车牌号</text>
          <input v-model="form.plateNo" class="input" placeholder="如 京A12345" />
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
        <button class="btn btn--primary" :loading="submitting" @click="onCreateBatch">登记交接批次</button>
      </view>
    </template>

    <!-- 已选中批次：磅次与有效磅次 -->
    <template v-else>
      <view class="card">
        <view class="card__title">批次 {{ batch.batchNo }}</view>
        <view class="kv">
          <view class="kv__row"><text class="kv__k">出售者</text><text>{{ batch.sellerName || '-' }}</text></view>
          <view class="kv__row"><text class="kv__k">车牌</text><text>{{ batch.plateNo || '-' }}</text></view>
          <view class="kv__row"><text class="kv__k">地点</text><text>{{ batch.stationName || batch.visitAddress || '-' }}</text></view>
          <view class="kv__row"><text class="kv__k">来源方式</text><text>{{ batch.sourceTypeName || '-' }}</text></view>
          <view class="kv__row">
            <text class="kv__k">参与计量</text>
            <text>{{ batch.effectiveWeighingSeqNo ? `第 ${batch.effectiveWeighingSeqNo} 次磅次` : '还没有有效磅次' }}</text>
          </view>
        </view>
        <view v-if="batch.weighingChangeLocked" class="hint hint--warn">
          该批次已产生收购单，计量结果已引用当时那一版磅次，不能再改有效磅次。
        </view>
      </view>

      <view class="card">
        <view class="card__title">磅次（每次过磅都留一条）</view>
        <view v-if="!weighings.length" class="empty">还没有磅次。先称毛重，再称皮重。</view>
        <view v-for="item in weighings" :key="item.id" class="weighing">
          <view class="weighing__top">
            <text class="weighing__seq">第 {{ item.seqNo }} 次</text>
            <text class="tag" :class="item.effective ? 'tag--ok' : ''">{{ item.effectiveText }}</text>
          </view>
          <view class="weighing__meta">
            毛重 {{ item.grossWeight }} · 皮重 {{ item.tareWeight }} · 净重 {{ item.netWeight }}
          </view>
          <view v-if="item.weightTicketNo" class="weighing__meta">磅单号 {{ item.weightTicketNo }}</view>
          <button
            v-if="!item.effective"
            class="mini-btn mini-btn--plain"
            :disabled="batch.weighingChangeLocked"
            @click="onSelectEffective(item)"
          >
            指定这一次参与计量
          </button>
        </view>

        <view class="field">
          <text class="field__label">毛重</text>
          <input v-model="weighingForm.grossWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">皮重</text>
          <input v-model="weighingForm.tareWeight" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">磅单号</text>
          <input v-model="weighingForm.weightTicketNo" class="input" placeholder="如 WD20261201001" />
        </view>
        <view class="field">
          <text class="field__label">备注（如复磅原因）</text>
          <input v-model="weighingForm.remark" class="input" placeholder="选填" />
        </view>
        <button class="btn btn--primary" :loading="submitting" @click="onAddWeighing">新增一次磅次</button>
        <view class="hint">
          第一次磅次自动成为有效磅次；复磅后再把参与计量的那一次指过去，其余留档不参与。
        </view>
      </view>

      <button class="btn btn--primary" @click="goAcquisition">按此批次登记收购</button>
      <button class="btn btn--ghost" @click="reset">登记下一批</button>
    </template>

    <!-- 最近批次：现场常用「先建批次，过磅后再补」 -->
    <view v-if="!batch" class="card">
      <view class="card__title">最近批次</view>
      <view v-if="!recent.length" class="empty">暂无批次。</view>
      <view v-for="item in recent" :key="item.id" class="weighing" @click="openBatch(item.id!)">
        <view class="weighing__top">
          <text class="weighing__seq">{{ item.batchNo }}</text>
          <text class="tag" :class="item.effectiveWeighingSeqNo ? 'tag--ok' : 'tag--warn'">
            {{ item.effectiveWeighingSeqNo ? `有效：第 ${item.effectiveWeighingSeqNo} 次` : '未指定有效磅次' }}
          </text>
        </view>
        <view class="weighing__meta">
          {{ item.sellerName }} · {{ item.plateNo }} · {{ item.acquisitionCount }} 张收购单
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { findReturningCustomer, PayeeVO } from '@/api/payee'
import { getStationPage, StationVO } from '@/api/station'
import {
  addWeighing,
  createHandoverBatch,
  getHandoverBatch,
  getHandoverBatchPage,
  getWeighings,
  HandoverBatchVO,
  SOURCE_TYPES,
  selectEffectiveWeighing,
  WeighingVO
} from '@/api/handover'

defineOptions({ name: 'FieldHandover' })

const lookup = ref('')
const finding = ref(false)
const lookedUp = ref(false)
const submitting = ref(false)
const seller = ref<PayeeVO | null>(null)
const stations = ref<StationVO[]>([])
const stationIndex = ref(-1)
const sourceTypeIndex = ref(1)
const batch = ref<HandoverBatchVO | null>(null)
const weighings = ref<WeighingVO[]>([])
const recent = ref<HandoverBatchVO[]>([])

const form = reactive({
  visitAddress: '',
  plateNo: '',
  driverName: '',
  driverMobile: '',
  remark: ''
})
const weighingForm = reactive({
  grossWeight: '',
  tareWeight: '',
  weightTicketNo: '',
  remark: ''
})

const sourceTypeNames = SOURCE_TYPES.map((item) => item.label)
const stationNames = computed(() => stations.value.map((item) => item.name || item.stationCode || ''))
const selectedStation = computed(() => (stationIndex.value >= 0 ? stations.value[stationIndex.value] : undefined))

onShow(() => {
  loadStations()
  loadRecent()
})

async function loadStations() {
  try {
    const page = await getStationPage()
    stations.value = page.list || []
  } catch {
    stations.value = []
  }
}

async function loadRecent() {
  try {
    const page = await getHandoverBatchPage({ pageSize: 10 })
    recent.value = page.list || []
  } catch {
    recent.value = []
  }
}

function onStationChange(event: any) {
  stationIndex.value = Number(event.detail.value)
}

function onSourceTypeChange(event: any) {
  sourceTypeIndex.value = Number(event.detail.value)
}

async function onFind() {
  if (!lookup.value) {
    uni.showToast({ title: '请填手机号或身份证号', icon: 'none' })
    return
  }
  finding.value = true
  try {
    const found = await findReturningCustomer({ idCardNo: lookup.value, mobile: lookup.value })
    seller.value = found
    lookedUp.value = true
    if (!found) {
      uni.showModal({ title: '没查到档案', content: '新出售者要先完成实名与收方入驻，再回来建批次。', showCancel: false })
    }
  } catch (e) {
    showError(e)
  } finally {
    finding.value = false
  }
}

function toNum(value: string): number | null {
  if (value === '' || value == null) return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

async function onCreateBatch() {
  if (!seller.value?.id) {
    uni.showToast({ title: '请先带出售者档案', icon: 'none' })
    return
  }
  if (!form.plateNo.trim()) {
    uni.showModal({ title: '还差一点', content: '请填车牌号', showCancel: false })
    return
  }
  if (!selectedStation.value && !form.visitAddress.trim()) {
    uni.showModal({ title: '还差一点', content: '场站与上门地址至少填一个', showCancel: false })
    return
  }
  submitting.value = true
  try {
    const id = await createHandoverBatch({
      payeeId: seller.value.id,
      stationId: selectedStation.value?.id,
      visitAddress: form.visitAddress.trim() || undefined,
      occurTime: Date.now(),
      sourceType: SOURCE_TYPES[sourceTypeIndex.value].value,
      driverName: form.driverName.trim() || undefined,
      driverMobile: form.driverMobile.trim() || undefined,
      plateNo: form.plateNo.trim(),
      remark: form.remark.trim() || undefined
    })
    await openBatch(id)
    uni.showToast({ title: '已登记批次', icon: 'none' })
  } catch (e) {
    showError(e)
  } finally {
    submitting.value = false
  }
}

async function openBatch(id: number) {
  batch.value = await getHandoverBatch(id)
  weighings.value = await getWeighings(id)
}

async function onAddWeighing() {
  const gross = toNum(weighingForm.grossWeight)
  const tare = toNum(weighingForm.tareWeight)
  if (gross == null || tare == null) {
    uni.showModal({ title: '还差一点', content: '毛重与皮重都要填', showCancel: false })
    return
  }
  if (gross < 0 || tare < 0 || tare > gross) {
    uni.showModal({ title: '重量不对', content: '毛重与皮重不能为负，皮重不能大于毛重', showCancel: false })
    return
  }
  submitting.value = true
  try {
    await addWeighing({
      batchId: batch.value!.id!,
      grossWeight: gross,
      tareWeight: tare,
      weightTicketNo: weighingForm.weightTicketNo.trim() || undefined,
      plateNo: batch.value!.plateNo,
      remark: weighingForm.remark.trim() || undefined
    })
    weighingForm.grossWeight = ''
    weighingForm.tareWeight = ''
    weighingForm.weightTicketNo = ''
    weighingForm.remark = ''
    await openBatch(batch.value!.id!)
    uni.showToast({ title: '已新增磅次', icon: 'none' })
  } catch (e) {
    showError(e)
  } finally {
    submitting.value = false
  }
}

function onSelectEffective(item: WeighingVO) {
  uni.showModal({
    title: `指定第 ${item.seqNo} 次参与计量`,
    content: '其余磅次留档但不参与计量；该批次已产生收购单后不能再改。',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await selectEffectiveWeighing(batch.value!.id!, item.id!)
        await openBatch(batch.value!.id!)
        uni.showToast({ title: '已指定有效磅次', icon: 'none' })
      } catch (e) {
        showError(e)
      }
    }
  })
}

function goAcquisition() {
  uni.navigateTo({ url: `/pages/acquisition/index?handoverBatchId=${batch.value!.id}` })
}

function reset() {
  batch.value = null
  weighings.value = []
  seller.value = null
  lookedUp.value = false
  lookup.value = ''
  stationIndex.value = -1
  sourceTypeIndex.value = 1
  Object.assign(form, { visitAddress: '', plateNo: '', driverName: '', driverMobile: '', remark: '' })
  loadRecent()
}

function showError(e: unknown) {
  uni.showModal({ title: '操作失败', content: (e as Error).message || '请重试', showCancel: false })
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
}

.card {
  padding: 32rpx;
  margin-bottom: 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__title {
    font-size: 32rpx;
    font-weight: 600;
  }

  &__hint {
    margin: 12rpx 0 20rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
    line-height: 1.6;
  }
}

.found {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eef0f3;

  &__name {
    font-weight: 600;
  }

  &__meta {
    color: $field-text-secondary;
    font-size: 26rpx;
  }
}

.field {
  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
  }
}

.input {
  height: 80rpx;
  padding: 0 20rpx;
  margin-bottom: 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
}

.picker {
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 20rpx;
  margin-bottom: 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
}

.kv {
  margin-top: 16rpx;

  &__row {
    display: flex;
    justify-content: space-between;
    padding: 10rpx 0;
    font-size: 26rpx;
  }

  &__k {
    color: $field-text-secondary;
  }
}

.weighing {
  padding: 20rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__seq {
    font-weight: 600;
  }

  &__meta {
    margin-top: 8rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
  }
}

.tag {
  padding: 4rpx 16rpx;
  color: $field-text-secondary;
  background-color: #f5f6f8;
  border-radius: 999rpx;
  font-size: 24rpx;

  &--ok {
    color: #1a7f43;
    background-color: #e8f7ee;
  }

  &--warn {
    color: #b26a00;
    background-color: #fff7e6;
  }
}

.mini-btn {
  width: 100%;
  height: 68rpx;
  line-height: 68rpx;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #ffffff;
  background-color: $field-primary;
  border-radius: 12rpx;

  &--plain {
    color: $field-primary;
    background-color: #ffffff;
    border: 1rpx solid $field-primary;
  }
}

.btn {
  width: 100%;
  margin-bottom: 20rpx;
  color: #ffffff;
  background-color: $field-primary;

  &--ghost {
    color: $field-primary;
    background-color: #ffffff;
    border: 1rpx solid $field-primary;
  }
}

.hint {
  margin-top: 12rpx;
  color: $field-text-secondary;
  font-size: 24rpx;
  line-height: 1.6;

  &--warn {
    color: #b26a00;
  }
}

.empty {
  padding: 40rpx 0;
  color: $field-text-secondary;
  text-align: center;
}
</style>
