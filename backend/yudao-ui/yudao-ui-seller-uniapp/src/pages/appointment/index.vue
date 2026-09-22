<template>
  <view class="page">
    <view v-if="!naturalPersonId" class="card empty">
      <view class="empty__title">请先用手机号验证</view>
      <view class="empty__desc">预约到站要用你在这家回收企业的身份。验证后即可预约。</view>
      <button class="btn btn--primary" @click="goLogin">去验证</button>
    </view>

    <template v-else>
      <view class="card notice">
        <view class="notice__badge">预约说明</view>
        <view class="notice__title">提前告知场站你的到达时间</view>
        <view class="notice__line">· 不占额度、不产生开票、不进五流</view>
        <view class="notice__line">· 没有「企业接受 / 拒绝」，只有到场与未到场</view>
        <view class="notice__line">· 到场后仍由收货员按实际过磅建收购单</view>
      </view>

      <view v-if="stationCode" class="card">
        <view class="card__title">发起预约</view>
        <view class="card__subtitle">以下信息仅用于场站提前安排接待</view>
        <view v-if="stationName" class="station">
          {{ enterpriseName || '回收企业' }} · {{ stationName }}
        </view>

        <view class="field">
          <text class="field__label">品类</text>
          <picker :range="goodsNames" :value="goodsIndex < 0 ? 0 : goodsIndex" @change="onGoodsChange">
            <view class="picker">{{ selectedGoods ? selectedGoods.name : '请选择品类' }}</view>
          </picker>
        </view>

        <view class="field">
          <text class="field__label">预计数量 <text class="field__optional">选填，仅供参考</text></text>
          <input v-model="form.expectedQuantity" class="input" type="digit" placeholder="请输入大概数量" />
        </view>

        <view class="field">
          <text class="field__label">车牌号</text>
          <input v-model="form.plateNo" class="input" placeholder="如 京A12345" />
        </view>

        <view class="field">
          <text class="field__label">预计到站日期</text>
          <picker mode="date" :value="date" @change="onDateChange">
            <view class="picker">{{ date }}</view>
          </picker>
        </view>

        <view class="field">
          <text class="field__label">预计到站时间</text>
          <picker mode="time" :value="time" @change="onTimeChange">
            <view class="picker">{{ time }}</view>
          </picker>
        </view>

        <view class="field">
          <text class="field__label">备注 <text class="field__optional">选填</text></text>
          <input v-model="form.remark" class="input" placeholder="需要场站提前了解的情况" />
        </view>

        <button class="btn btn--primary" :loading="submitting" @click="onSubmit">提交预约</button>
      </view>

      <view v-else class="card appointment-entry">
        <view class="appointment-entry__icon" aria-hidden="true"></view>
        <view class="appointment-entry__body">
          <view class="appointment-entry__title">新预约请扫描场站二维码</view>
          <view class="appointment-entry__desc">
            预约必须确定具体场站。从首页进入时可查看已有预约；需要新增时，请扫描场站现场二维码。
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card__title">我的预约</view>
        <view v-if="!appointments.length" class="muted">还没有预约。</view>
        <view v-for="item in appointments" :key="item.id" class="appointment">
          <view class="appointment__row">
            <text class="appointment__cat">{{ item.categoryName }}</text>
            <text :class="statusClass(item.status)">{{ item.statusName }}</text>
          </view>
          <view class="appointment__meta">
            {{ item.enterpriseName }} · {{ item.stationName }}
          </view>
          <view class="appointment__meta">
            预计 {{ formatTime(item.expectedArrivalTime) }} 到站
            <template v-if="item.expectedQuantityText"> · {{ item.expectedQuantityText }}</template>
            <template v-if="item.plateNo"> · {{ item.plateNo }}</template>
          </view>
          <view v-if="item.cancelReason" class="appointment__meta">取消原因：{{ item.cancelReason }}</view>
          <view v-if="item.status === 0" class="appointment__actions">
            <text class="link link--danger" @click="onCancel(item)">取消预约</text>
          </view>
        </view>
      </view>

      <view class="scope-note">
        预约只是告诉回收企业「我大概什么时候来」。预计数量一律只是「约」，任何统计与额度口径都不引用它。
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  Appointment,
  AppointmentGoods,
  cancelAppointment,
  createAppointment,
  listAppointmentGoods,
  listAppointments
} from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'

defineOptions({ name: 'SellerAppointment' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)

const stationCode = ref('')
const stationName = ref('')
const enterpriseName = ref('')
const goodsList = ref<AppointmentGoods[]>([])
const goodsIndex = ref(-1)
const submitting = ref(false)
const appointments = ref<Appointment[]>([])

const today = new Date()
const pad = (value: number) => String(value).padStart(2, '0')
const date = ref(`${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`)
const time = ref(`${pad(Math.min(today.getHours() + 1, 23))}:00`)

const form = reactive({ expectedQuantity: '', plateNo: '', remark: '' })

const goodsNames = computed(() => goodsList.value.map((item) => item.name || ''))
const selectedGoods = computed(() => (goodsIndex.value >= 0 ? goodsList.value[goodsIndex.value] : undefined))

onLoad((query) => {
  stationCode.value = (query?.station as string) || ''
  stationName.value = (query?.stationName as string) || ''
  enterpriseName.value = (query?.enterpriseName as string) || ''
  if (auth.subject) {
    loadGoods()
    loadAppointments()
  }
})

async function loadGoods() {
  try {
    goodsList.value = await listAppointmentGoods()
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  }
}

async function loadAppointments() {
  try {
    appointments.value = await listAppointments(naturalPersonId.value)
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  }
}

function onGoodsChange(event: any) {
  goodsIndex.value = Number(event.detail.value)
}
function onDateChange(event: any) {
  date.value = event.detail.value
}
function onTimeChange(event: any) {
  time.value = event.detail.value
}

/** 后端 LocalDateTime 走时间戳（毫秒），这里把日期 + 时间拼成毫秒 */
function arrivalMillis(): number {
  const [year, month, day] = date.value.split('-').map(Number)
  const [hour, minute] = time.value.split(':').map(Number)
  return new Date(year, month - 1, day, hour, minute, 0).getTime()
}

async function onSubmit() {
  if (!stationCode.value) {
    uni.showModal({ title: '缺少场站', content: '请重新扫描场站二维码进入。', showCancel: false })
    return
  }
  if (!selectedGoods.value) {
    uni.showToast({ title: '请选择品类', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const quantity = form.expectedQuantity ? Number(form.expectedQuantity) : undefined
    await createAppointment({
      naturalPersonId: naturalPersonId.value,
      stationCode: stationCode.value,
      goodsConfigId: selectedGoods.value.id,
      expectedQuantity: Number.isNaN(quantity) ? undefined : quantity,
      plateNo: form.plateNo || undefined,
      expectedArrivalTime: arrivalMillis(),
      remark: form.remark || undefined
    })
    uni.showToast({ title: '已预约，到场时报手机号', icon: 'none' })
    form.expectedQuantity = ''
    form.plateNo = ''
    form.remark = ''
    goodsIndex.value = -1
    loadAppointments()
  } catch (e) {
    uni.showModal({ title: '预约失败', content: (e as Error).message, showCancel: false })
  } finally {
    submitting.value = false
  }
}

function onCancel(item: Appointment) {
  uni.showModal({
    title: '取消预约',
    content: '取消后不影响你在回收企业的任何记录。',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelAppointment(naturalPersonId.value, item.id, '自然人自助取消')
        uni.showToast({ title: '已取消', icon: 'none' })
        loadAppointments()
      } catch (e) {
        uni.showToast({ title: (e as Error).message, icon: 'none' })
      }
    }
  })
}

function goLogin() {
  uni.redirectTo({ url: `/pages/login/index?station=${encodeURIComponent(stationCode.value)}` })
}

function statusClass(status?: number) {
  if (status === 1) return 'open'
  if (status === 2 || status === 9) return 'closed'
  return ''
}

function formatTime(value?: string | number) {
  if (value === undefined || value === null || value === '') return '—'
  const parsed = typeof value === 'number' ? new Date(value) : new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(parsed.getTime())) return String(value)
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())} ${pad(
    parsed.getHours()
  )}:${pad(parsed.getMinutes())}`
}
</script>

<style lang="scss" scoped>
.page {
  box-sizing: border-box;
  min-height: calc(100vh - 44px);
  padding: 32rpx 32rpx 60rpx;
  background:
    radial-gradient(circle at 88% 0%, rgba(58, 149, 255, 0.11), transparent 28%),
    linear-gradient(180deg, #f7faff 0%, #f4f7fb 100%);
}

.card {
  padding: 34rpx 32rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border: 1rpx solid rgba(22, 119, 255, 0.08);
  border-radius: 26rpx;
  box-shadow: 0 16rpx 44rpx rgba(31, 55, 88, 0.07);

  &__title {
    font-size: 34rpx;
    font-weight: 800;
  }

  &__subtitle {
    margin: 8rpx 0 28rpx;
    color: $seller-text-secondary;
    font-size: 24rpx;
  }
}

.notice {
  background: linear-gradient(135deg, #f0f6ff, #eaf3ff);
  box-shadow: none;

  &__badge {
    display: inline-block;
    padding: 7rpx 14rpx;
    margin-bottom: 14rpx;
    color: #2869bf;
    background-color: rgba(255, 255, 255, 0.72);
    border-radius: 999rpx;
    font-size: 22rpx;
    font-weight: 700;
  }

  &__title {
    margin-bottom: 12rpx;
    color: #244d80;
    font-size: 30rpx;
    font-weight: 800;
  }

  &__line {
    color: #5a6e87;
    font-size: 24rpx;
    line-height: 1.75;
  }
}

.station {
  margin-bottom: 26rpx;
  padding: 18rpx 20rpx;
  color: #2f619e;
  background-color: #edf5ff;
  border-radius: 14rpx;
  font-size: 25rpx;
  font-weight: 600;
}

.field {
  margin-bottom: 26rpx;

  &__label {
    display: block;
    margin-bottom: 12rpx;
    color: #344054;
    font-size: 26rpx;
    font-weight: 600;
  }

  &__optional {
    margin-left: 8rpx;
    color: #98a2b3;
    font-size: 22rpx;
    font-weight: 400;
  }
}

.input,
.picker {
  box-sizing: border-box;
  height: 96rpx;
  padding: 0 24rpx;
  color: $seller-text;
  background-color: #f7f9fc;
  border: 2rpx solid #e7ebf2;
  border-radius: 18rpx;
  font-size: 28rpx;
  line-height: 92rpx;
}

.picker {
  position: relative;

  &::after {
    position: absolute;
    top: 35rpx;
    right: 26rpx;
    width: 13rpx;
    height: 13rpx;
    border-right: 3rpx solid #8c96a6;
    border-bottom: 3rpx solid #8c96a6;
    transform: rotate(45deg);
    content: '';
  }
}

.appointment {
  padding: 24rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__row {
    display: flex;
    justify-content: space-between;
    gap: 16rpx;
  }

  &__cat {
    color: #344054;
    font-size: 29rpx;
    font-weight: 700;
  }

  &__meta {
    margin-top: 6rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
    line-height: 1.6;
  }

  &__actions {
    margin-top: 12rpx;
  }
}

.appointment-entry {
  display: flex;
  align-items: flex-start;
  gap: 22rpx;
  background: linear-gradient(135deg, #f0f6ff, #eaf3ff);
  box-shadow: none;

  &__icon {
    position: relative;
    flex-shrink: 0;
    box-sizing: border-box;
    width: 54rpx;
    height: 54rpx;
    border: 4rpx solid #4d8fdf;
    border-radius: 50% 50% 50% 0;
    transform: rotate(-45deg) scale(0.75);

    &::after {
      position: absolute;
      top: 14rpx;
      left: 14rpx;
      width: 16rpx;
      height: 16rpx;
      border: 4rpx solid #4d8fdf;
      border-radius: 50%;
      content: '';
    }
  }

  &__body { flex: 1; min-width: 0; }
  &__title { color: #295a95; font-size: 28rpx; font-weight: 800; }
  &__desc { margin-top: 8rpx; color: #5d6f86; font-size: 24rpx; line-height: 1.65; }
}

.btn {
  box-sizing: border-box;
  width: 100%;
  height: 96rpx;
  margin: 0;
  border-radius: 18rpx;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 96rpx;

  &--primary {
    margin-top: 6rpx;
    background: linear-gradient(100deg, $seller-primary 0%, #2d8bff 100%);
    box-shadow: 0 14rpx 28rpx rgba(22, 119, 255, 0.2);
  }
}

.link {
  color: $seller-primary;

  &--danger {
    color: #cf1322;
  }
}

.open {
  color: #1a7f43;
}

.closed {
  color: #cf1322;
}

.muted {
  color: $seller-text-secondary;
  line-height: 1.7;
}

.scope-note {
  margin: 14rpx 14rpx 0;
  color: #98a2b3;
  font-size: 22rpx;
  line-height: 1.7;
  text-align: center;
}

.empty {
  margin-top: 70rpx;
  text-align: center;

  &__title {
    font-size: 34rpx;
    font-weight: 800;
  }

  &__desc {
    margin: 16rpx 0 24rpx;
    color: $seller-text-secondary;
    line-height: 1.7;
  }
}
</style>
