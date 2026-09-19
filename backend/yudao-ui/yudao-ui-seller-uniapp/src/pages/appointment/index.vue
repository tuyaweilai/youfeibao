<template>
  <view class="page">
    <view v-if="!naturalPersonId" class="card empty">
      <view class="empty__title">请先用手机号验证</view>
      <view class="empty__desc">预约到站要用你在这家回收企业的身份。验证后即可预约。</view>
      <button class="btn btn--primary" @click="goLogin">去验证</button>
    </view>

    <template v-else>
      <view class="card notice">
        <view class="notice__title">预约到站不是订单</view>
        <view class="notice__line">· 不占额度、不产生开票、不进五流</view>
        <view class="notice__line">· 没有「企业接受 / 拒绝」，只有到场与未到场</view>
        <view class="notice__line">· 到场后仍由收货员按实际过磅建收购单</view>
      </view>

      <view class="card">
        <view class="card__title">发起预约</view>
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
          <text class="field__label">预计数量（可空，只是一句「约」）</text>
          <input v-model="form.expectedQuantity" class="input" type="digit" placeholder="0" />
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
          <text class="field__label">备注</text>
          <input v-model="form.remark" class="input" placeholder="选填" />
        </view>

        <button class="btn btn--primary" :loading="submitting" @click="onSubmit">提交预约</button>
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
  padding: 24rpx;
}

.card {
  padding: 32rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;

  &__title {
    margin-bottom: 20rpx;
    font-size: 32rpx;
    font-weight: 600;
  }
}

.notice {
  background-color: #f0f5ff;

  &__title {
    margin-bottom: 8rpx;
    font-weight: 600;
  }

  &__line {
    color: $seller-text-secondary;
    font-size: 26rpx;
    line-height: 1.8;
  }
}

.station {
  margin-bottom: 20rpx;
  color: $seller-text-secondary;
  font-size: 26rpx;
}

.field {
  margin-bottom: 20rpx;

  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
  }
}

.input,
.picker {
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;
}

.appointment {
  padding: 16rpx 0;
  border-top: 1rpx solid #eef0f3;

  &__row {
    display: flex;
    justify-content: space-between;
    gap: 16rpx;
  }

  &__cat {
    font-weight: 600;
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

.btn {
  width: 100%;
  color: #ffffff;
  background-color: $seller-primary;

  &--primary {
    margin-top: 8rpx;
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
  margin-top: 12rpx;
  color: $seller-text-secondary;
  font-size: 24rpx;
  line-height: 1.7;
}

.empty {
  margin-top: 80rpx;
  text-align: center;

  &__title {
    font-size: 34rpx;
    font-weight: 700;
  }

  &__desc {
    margin: 16rpx 0 24rpx;
    color: $seller-text-secondary;
    line-height: 1.7;
  }
}
</style>
