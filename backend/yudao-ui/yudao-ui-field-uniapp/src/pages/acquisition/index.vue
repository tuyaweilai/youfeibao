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

      <!-- 数量与金额 -->
      <view class="card">
        <view class="card__title">数量与金额</view>
        <view class="field">
          <text class="field__label">数量{{ selectedGoods?.unit ? `（${selectedGoods.unit}）` : '' }}</text>
          <input v-model="form.quantity" class="input" type="digit" placeholder="0" />
        </view>
        <view class="field">
          <text class="field__label">含税单价（元）</text>
          <input v-model="form.unitPrice" class="input" type="digit" placeholder="0.00" />
        </view>
        <view class="field">
          <text class="field__label">金额（元，自动）</text>
          <input v-model="form.amount" class="input" type="digit" placeholder="0.00" />
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
import { createAcquisition, AcquisitionCreateReq, AcquisitionCreateResp } from '@/api/acquisition'
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
const seller = ref<PayeeVO | null>(null)
const lookedUp = ref(false)
const looking = ref(false)
const submitting = ref(false)
const result = ref<AcquisitionCreateResp | null>(null)
const clientRequestId = ref(newId())

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
  goodsConfigId: undefined as number | undefined,
  specification: '',
  quantity: '',
  unitPrice: '',
  amount: '',
  grossWeight: '',
  tareWeight: '',
  netWeight: '',
  weightTicketNo: '',
  weightTicketPlateNo: '',
  vehiclePlateNo: '',
  tradeAddress: '',
  settlementMethod: '',
  remark: ''
})

const goodsNames = computed(() => goodsList.value.map((item) => item.name || ''))
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

onLoad(() => {
  loadGoods()
})

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

// 金额与净重自动推算；现场也允许直接改金额
watch(
  () => [form.quantity, form.unitPrice],
  () => {
    const quantity = toNum(form.quantity)
    const unitPrice = toNum(form.unitPrice)
    if (quantity != null && unitPrice != null) {
      form.amount = (quantity * unitPrice).toFixed(2)
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
}

function validate(): string | null {
  if (!form.payeeId) return '请先带出售者档案'
  if (!form.goodsConfigId) return '请选择品类'
  if (!(toNum(form.quantity)! > 0)) return '请填写数量'
  if (!(toNum(form.amount)! > 0)) return '金额需大于 0（数量 × 单价）'
  if (!form.weightTicketNo.trim() && !photoData.weightTicketImageUrl && !photoUrl.weightTicketImageUrl) {
    return '磅单号与磅单照片至少填一个'
  }
  return null
}

function buildPayload(): AcquisitionCreateReq {
  return {
    clientRequestId: clientRequestId.value,
    payeeId: form.payeeId!,
    goodsConfigId: form.goodsConfigId!,
    specification: form.specification || undefined,
    quantity: toNum(form.quantity) ?? undefined,
    unitPrice: toNum(form.unitPrice) ?? undefined,
    amount: toNum(form.amount) ?? undefined,
    grossWeight: toNum(form.grossWeight) ?? undefined,
    tareWeight: toNum(form.tareWeight) ?? undefined,
    netWeight: toNum(form.netWeight) ?? undefined,
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
    specification: '',
    quantity: '',
    unitPrice: '',
    amount: '',
    grossWeight: '',
    tareWeight: '',
    netWeight: '',
    weightTicketNo: '',
    weightTicketPlateNo: '',
    vehiclePlateNo: '',
    tradeAddress: '',
    settlementMethod: '',
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
