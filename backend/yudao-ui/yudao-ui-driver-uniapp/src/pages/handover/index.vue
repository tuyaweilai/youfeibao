<template>
  <view class="page">
    <view v-if="!taskId" class="empty">缺少任务编号，请从任务详情进入</view>
    <template v-else>
      <!-- 已经登记过：只读展示，不重复登记 -->
      <template v-if="existing">
        <view class="card">
          <view class="card__top">
            <text class="card__no">{{ existing.handoverNo }}</text>
            <text class="tag">{{ existing.documentStatusName }}</text>
          </view>
          <view class="line"><text class="line__label">出售者</text><text class="line__value">{{ existing.payeeName || '—' }}</text></view>
          <view class="line"><text class="line__label">品类</text><text class="line__value">{{ existing.categoryName || '—' }}</text></view>
          <view class="line">
            <text class="line__label">现场参考</text>
            <text class="line__value">{{ existing.referenceQuantity }} {{ existing.unit || '' }} · {{ existing.referenceUnitPrice ?? '—' }} 元/{{ existing.unit || '单位' }}</text>
          </view>
          <view v-if="existing.documentGap" class="line">
            <text class="line__label">缺要件</text>
            <text class="line__value line__value--warn">{{ existing.documentGap }}</text>
          </view>
          <view v-if="existing.photos?.length" class="thumbs">
            <image v-for="(url, index) in existing.photos" :key="index" :src="url" class="thumb" mode="aspectFill" />
          </view>
        </view>
        <view class="tip tip--note">
          这一家已经登记过交接。登记的是**现场谈好的事**，不是收购单：结算重量回场复磅才定，
          收购单那时才生成（本页没有任何金额入口）。
        </view>
      </template>

      <template v-else>
        <view class="tip tip--note">
          现场只登记事实：品类、参考量、参考单价与凭证照片。**这里没有金额**——回场复磅后才是结算重量，
          收购单由磅房按这份登记生成；参考量 / 参考单价被修正时要由收货员说明原因。
        </view>

        <!-- 1. 出售者 -->
        <view class="card">
          <view class="card__title">1. 卖给谁</view>
          <view v-if="payeeName" class="picked">
            出售者：<text class="picked__name">{{ payeeName }}</text>
            <text v-if="!payeeId" class="picked__reset" @click="payeeId = null; payeeName = ''">换人</text>
          </view>
          <template v-if="!payeeId">
            <view class="field"><text class="field__label">身份证号</text><input v-model="lookup.idCardNo" class="field__input" placeholder="选填" /></view>
            <view class="field"><text class="field__label">手机号</text><input v-model="lookup.mobile" class="field__input" placeholder="选填" /></view>
            <button class="btn btn--ghost" :loading="busy" @click="onLookup">带出档案</button>
            <button class="btn" @click="goOnboarding">新出售者：现场建档（准入四步）</button>
          </template>
          <view class="field"><text class="field__label">姓名（快照）</text><input v-model="payeeName" class="field__input" placeholder="出售者姓名" /></view>
        </view>

        <!-- 2. 品类与参考值 -->
        <view class="card">
          <view class="card__title">2. 谈好的品类与参考值</view>
          <view class="field">
            <text class="field__label">品类（权威品类，来自企业编码配置）</text>
            <picker :range="categoryNames" :value="categoryIndex" @change="onCategoryChange">
              <view class="picker">{{ selectedCategory?.name || '请选择品类' }}</view>
            </picker>
          </view>
          <view class="field">
            <text class="field__label">参考量{{ selectedCategory?.unit ? `（${selectedCategory.unit}）` : '' }}</text>
            <input v-model="form.referenceQuantity" class="field__input" type="digit" placeholder="现场约定的量" />
          </view>
          <view class="field">
            <text class="field__label">参考单价（元 / {{ selectedCategory?.unit || '单位' }}）</text>
            <input v-model="form.referenceUnitPrice" class="field__input" type="digit" placeholder="现场约定的价，选填" />
          </view>
        </view>

        <!-- 3. 凭证照片 -->
        <view class="card">
          <view class="card__title">3. 凭证照片</view>
          <view class="card__tip">现场凭证至少一张（货或磅单）。照片进一票一档的货物流证据。</view>
          <view class="thumbs">
            <image v-for="(url, index) in form.photos" :key="url" :src="url" class="thumb" mode="aspectFill" @click="removePhoto(index)" />
            <view class="thumb thumb--add" @click="onAddPhoto">＋</view>
          </view>
        </view>

        <!-- 4. 要件 -->
        <view class="card">
          <view class="card__title">4. 证件齐了吗</view>
          <view class="card__tip">
            缺身份证或银行卡可以**先收货**：登记照记，但这一笔会被记成「待补档」，
            付款与开票被门禁拦住，补齐后由企业放行。
          </view>
          <view class="line">
            <text class="line__label">证件齐全（身份证 + 银行卡）</text>
            <switch :checked="documentComplete" @change="onDocumentToggle" />
          </view>
          <view v-if="!documentComplete" class="field">
            <text class="field__label">缺什么</text>
            <picker :range="gapOptions" :value="gapIndex" @change="onGapChange">
              <view class="picker">{{ gapOptions[gapIndex] }}</view>
            </picker>
          </view>
        </view>

        <view class="actions">
          <button class="btn btn--primary" :loading="busy" @click="onSubmit">提交交接登记</button>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { findReturningCustomer } from '@youfeibao/field-shared'
import { createHandover, getEnabledGoodsConfigs, getMyHandoverList, type DriverHandoverVO, type GoodsConfigVO } from '@/api/handover'
import { chooseImage, uploadImage } from '@/utils/upload'

const taskId = ref<number>()
const stopId = ref<number>()
const address = ref('')

const payeeId = ref<number | null>(null)
const payeeName = ref('')
const payeeMobile = ref('')
const lookup = reactive({ idCardNo: '', mobile: '' })

const categories = ref<GoodsConfigVO[]>([])
const categoryIndex = ref(0)
const categoryNames = computed(() => categories.value.map((item) => `${item.name}${item.unit ? `（${item.unit}）` : ''}`))
const selectedCategory = computed(() => categories.value[categoryIndex.value])

const form = reactive({
  referenceQuantity: '',
  referenceUnitPrice: '',
  photos: [] as string[],
  remark: ''
})
const documentComplete = ref(true)
const gapOptions = ['缺身份证', '缺银行卡', '身份证和银行卡都缺']
const gapIndex = ref(0)

const existing = ref<DriverHandoverVO | null>(null)
const busy = ref(false)

/**
 * 幂等键在页面存续期内保持稳定：弱网点两次「提交」或重试不会落两条登记。
 * 键里带 taskId + stopId，同一家在别的任务上还能各自登记。
 */
const clientRequestId = `driver-handover-${Date.now()}-${Math.floor(Math.random() * 1e6)}`

onLoad(async (query) => {
  taskId.value = query?.taskId ? Number(query.taskId) : undefined
  stopId.value = query?.stopId ? Number(query.stopId) : undefined
  address.value = query?.address ? decodeURIComponent(query.address) : ''
  payeeId.value = query?.payeeId ? Number(query.payeeId) : null
  payeeName.value = query?.payeeName ? decodeURIComponent(query.payeeName) : ''
  payeeMobile.value = query?.payeeMobile ? decodeURIComponent(query.payeeMobile) : ''
  if (!taskId.value) {
    return
  }
  await Promise.all([loadCategories(), loadExisting()])
})

async function loadCategories() {
  try {
    categories.value = (await getEnabledGoodsConfigs()) || []
  } catch (error) {
    uni.showToast({ title: (error as Error).message || '品类加载失败', icon: 'none' })
  }
}

async function loadExisting() {
  try {
    const list = (await getMyHandoverList(taskId.value as number)) || []
    existing.value = list.find((item) => (stopId.value ? item.stopId === stopId.value : !item.stopId)) || null
  } catch (error) {
    // 读不到既有登记不该挡住现场干活：留空让他登记，重复提交由服务端按停靠点拦住
    console.warn('读取交接登记失败', error)
  }
}

async function onLookup() {
  if (!lookup.idCardNo && !lookup.mobile) {
    uni.showToast({ title: '至少填身份证号或手机号', icon: 'none' })
    return
  }
  busy.value = true
  try {
    const payee = await findReturningCustomer({ idCardNo: lookup.idCardNo || undefined, mobile: lookup.mobile || undefined })
    if (!payee) {
      uni.showToast({ title: '没查到档案，请先现场建档', icon: 'none' })
      return
    }
    payeeId.value = payee.id ?? null
    payeeName.value = payee.name || ''
    payeeMobile.value = payee.mobile || ''
  } catch (error) {
    uni.showToast({ title: (error as Error).message || '带档失败', icon: 'none' })
  } finally {
    busy.value = false
  }
}

function goOnboarding() {
  uni.navigateTo({ url: '/pages/onboarding/index' })
}

function onCategoryChange(event: { detail: { value: number } }) {
  categoryIndex.value = Number(event.detail.value)
}

async function onAddPhoto() {
  try {
    const paths = await chooseImage(1)
    if (!paths.length) {
      return
    }
    busy.value = true
    const url = await uploadImage(paths[0])
    form.photos.push(url)
  } catch (error) {
    uni.showToast({ title: (error as Error).message || '上传失败', icon: 'none' })
  } finally {
    busy.value = false
  }
}

function removePhoto(index: number) {
  form.photos.splice(index, 1)
}

// 事件参数用 any：uni-app 的 switch 事件类型不在标准 DOM 事件里，写窄了与模板对不上
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function onDocumentToggle(event: any) {
  documentComplete.value = !!event?.detail?.value
}

function onGapChange(event: { detail: { value: number } }) {
  gapIndex.value = Number(event.detail.value)
}

async function onSubmit() {
  if (!payeeId.value) {
    uni.showToast({ title: '先带出或新建收方档案', icon: 'none' })
    return
  }
  if (!selectedCategory.value) {
    uni.showToast({ title: '请选择品类', icon: 'none' })
    return
  }
  const quantity = Number(form.referenceQuantity)
  if (!quantity || quantity <= 0) {
    uni.showToast({ title: '参考量必须大于 0', icon: 'none' })
    return
  }
  if (!form.photos.length) {
    uni.showToast({ title: '至少拍一张凭证照片', icon: 'none' })
    return
  }
  busy.value = true
  try {
    await createHandover({
      taskId: taskId.value as number,
      stopId: stopId.value,
      payeeId: payeeId.value,
      payeeName: payeeName.value || undefined,
      payeeMobile: payeeMobile.value || undefined,
      goodsConfigId: selectedCategory.value.id,
      categoryName: selectedCategory.value.name,
      unit: selectedCategory.value.unit,
      referenceQuantity: quantity,
      referenceUnitPrice: form.referenceUnitPrice ? Number(form.referenceUnitPrice) : undefined,
      photos: form.photos,
      documentStatus: documentComplete.value ? 'COMPLETE' : 'PENDING',
      documentGap: documentComplete.value ? undefined : gapOptions[gapIndex.value],
      clientRequestId,
      remark: form.remark || undefined
    })
    uni.showToast({ title: '交接已登记', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (error) {
    uni.showToast({ title: (error as Error).message || '提交失败', icon: 'none' })
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx;
  padding-bottom: 60rpx;
}
.tip--note {
  margin-bottom: 20rpx;
}
.thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 16rpx;
}
.thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  background: #eef2f0;
}
.thumb--add {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56rpx;
  color: #8a9490;
  border: 2rpx dashed #c8d2cd;
}
</style>
