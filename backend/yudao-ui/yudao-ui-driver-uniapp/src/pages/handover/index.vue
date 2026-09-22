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
          该出售者的交接已登记。以上为现场参考信息，结算重量以回场复磅为准，收购单由磅房生成。
        </view>
      </template>

      <template v-else>
        <view class="tip tip--note">
          <view class="note-title">记录现场交接，结算以回场复磅为准</view>
          <view>填写品类、参考量和凭证照片，参考单价选填。本页不计算结算金额。</view>
          <view class="note-detail">磅房据此生成收购单；参考量或参考单价如有修正，由收货员说明原因。</view>
        </view>

        <!-- 1. 出售者 -->
        <view class="card">
          <view class="card__title"><text class="section-number">01</text>出售者信息</view>
          <view v-if="payeeName" class="picked">
            出售者：<text class="picked__name">{{ payeeName }}</text>
            <text v-if="!payeeId" class="picked__reset" @click="payeeId = null; payeeName = ''">换人</text>
          </view>
          <template v-if="!payeeId">
            <view class="card__tip">身份证号、手机号任选一项，查询已有出售者档案。</view>
            <view class="field"><label for="handover-field-1" class="field__label">身份证号</label><input id="handover-field-1" v-model="lookup.idCardNo" class="field__input" placeholder="请输入身份证号" /></view>
            <view class="field"><label for="handover-field-2" class="field__label">手机号</label><input id="handover-field-2" v-model="lookup.mobile" class="field__input" type="number" maxlength="11" placeholder="请输入手机号" /></view>
            <button class="btn btn--ghost" :loading="busy" :disabled="busy" @click="onLookup">带出档案</button>
            <button class="btn btn--link" :disabled="busy" @click="goOnboarding">新出售者？去现场建档</button>
          </template>
          <view class="field"><label for="handover-field-3" class="field__label">出售者姓名</label><input id="handover-field-3" v-model="payeeName" class="field__input" placeholder="出售者姓名" /></view>
        </view>

        <!-- 2. 品类与参考值 -->
        <view class="card">
          <view class="card__title"><text class="section-number">02</text>货物与参考值</view>
          <view class="field">
            <text class="field__label">货物品类</text>
            <picker :range="categoryNames" :value="categoryIndex" @change="onCategoryChange">
              <view class="picker" role="button" aria-label="选择货物品类">{{ selectedCategory?.name || '请选择品类' }}</view>
            </picker>
          </view>
          <view class="field">
            <label for="handover-field-4" class="field__label">参考量{{ selectedCategory?.unit ? `（${selectedCategory.unit}）` : '' }}</label>
            <input id="handover-field-4" v-model="form.referenceQuantity" class="field__input" type="digit" placeholder="现场约定的量" />
          </view>
          <view class="field">
            <label for="handover-field-5" class="field__label">参考单价（元 / {{ selectedCategory?.unit || '单位' }}）</label>
            <input id="handover-field-5" v-model="form.referenceUnitPrice" class="field__input" type="digit" placeholder="现场约定的价，选填" />
          </view>
        </view>

        <!-- 3. 凭证照片 -->
        <view class="card">
          <view class="card__title"><text class="section-number">03</text>凭证照片</view>
          <view class="card__tip">至少上传 1 张货物或磅单照片，作为交接凭证保存。</view>
          <view class="thumbs">
            <view v-for="(url, index) in form.photos" :key="url" class="photo-item">
              <image :src="url" class="thumb" mode="aspectFill" :aria-label="`交接凭证照片 ${index + 1}`" />
              <button class="photo-remove" :disabled="busy" :aria-label="`移除第 ${index + 1} 张照片`" @click="removePhoto(index)">移除</button>
            </view>
            <button class="thumb thumb--add" :loading="busy" :disabled="busy" @click="onAddPhoto"><text class="photo-plus">＋</text><text>添加照片</text></button>
          </view>
        </view>

        <!-- 4. 要件 -->
        <view class="card">
          <view class="card__title"><text class="section-number">04</text>证件状态</view>
          <view class="card__tip">
            缺少证件也可先登记收货，记录将标为“待补档”。补齐后由企业放行付款与开票。
          </view>
          <view class="document-toggle">
            <view><view class="document-toggle__title">证件齐全</view><view class="card__tip">身份证与银行卡</view></view>
            <switch aria-label="身份证与银行卡是否齐全" color="#0f766e" :checked="documentComplete" :disabled="busy" @change="onDocumentToggle" />
          </view>
          <view v-if="!documentComplete" class="field">
            <text class="field__label">缺少的证件</text>
            <picker :range="gapOptions" :value="gapIndex" @change="onGapChange">
              <view class="picker" role="button" aria-label="选择缺少的证件">{{ gapOptions[gapIndex] }}</view>
            </picker>
          </view>
        </view>

        <view class="actions">
          <button class="btn btn--primary" :loading="busy" :disabled="busy" @click="onSubmit">提交交接登记</button>
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
  if (busy.value) return
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
  if (busy.value) return
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
  if (busy.value) return
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

<style scoped lang="scss">
@use "@/styles/theme.scss" as *;
.page { padding: 16px 16px calc(32px + env(safe-area-inset-bottom)); font-size: 16px; line-height: 1.6; }
.tip { color: $driver-muted; font-size: 13px; line-height: 22px; }
.tip--note { margin-bottom: 18px; padding: 16px; border: 1px solid #cfe3dd; border-radius: 14px; background: #eaf4f0; color: #3e625d; }
.note-title { margin-bottom: 7px; color: #24534e; font-size: 15px; font-weight: 600; line-height: 24px; }
.note-detail { margin-top: 8px; font-size: 12px; line-height: 20px; }
.card { margin-bottom: 16px; padding: 20px 18px; border-radius: 16px; background: #fff; }
.card__title { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; color: $driver-text; font-size: 18px; font-weight: 600; line-height: 28px; }
.section-number { display: inline-flex; justify-content: center; align-items: center; width: 28px; height: 28px; border-radius: 8px; background: $driver-soft; color: $driver-primary; font-size: 12px; font-weight: 600; }
.card__tip { color: $driver-muted; font-size: 13px; line-height: 22px; }
.field { margin-top: 18px; }
.field__label { display: block; margin-bottom: 8px; color: #365256; font-size: 14px; line-height: 22px; font-weight: 500; }
.field__input, .picker { box-sizing: border-box; width: 100%; min-height: 50px; padding: 0 14px; border: 1px solid $driver-border; border-radius: 10px; background: #f7f9f9; color: $driver-text; font-size: 16px; }
.field__input { height: 50px; }
.picker { position: relative; padding: 12px 34px 12px 14px; line-height: 24px; cursor: pointer; overflow-wrap: anywhere; }
.picker::after { content: ''; position: absolute; right: 16px; top: 19px; width: 7px; height: 7px; border-right: 1.5px solid #607579; border-bottom: 1.5px solid #607579; transform: rotate(45deg); }
.btn { display: flex; align-items: center; justify-content: center; box-sizing: border-box; width: 100%; min-height: 48px; margin-top: 16px; padding: 12px; border-radius: 10px; font-size: 15px; font-weight: 500; line-height: 24px; }
.btn::after, .thumb--add::after, .photo-remove::after { border: none; }
.btn--ghost { border: 1px solid #a9ccc3; background: #edf6f2; color: $driver-primary; }
.btn--link { margin-top: 4px; background: transparent; color: $driver-primary; font-size: 14px; }
.btn--primary { margin-top: 0; background: $driver-primary; color: #fff; font-size: 16px; font-weight: 600; min-height: 52px; }
.btn[disabled] { opacity: .6; }
.picked { padding: 12px 14px; background: $driver-soft; border-radius: 10px; color: #345952; font-size: 14px; }
.picked__name { font-weight: 600; font-size: 16px; }
.picked__reset { margin-left: 12px; color: $driver-primary; }
.thumbs { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 16px; }
.thumb { display: block; width: 88px; height: 88px; border-radius: 10px; background: #eef3f1; }
.thumb--add { display: flex; flex-direction: column; align-items: center; justify-content: center; margin: 0; padding: 0; border: 1px dashed #93b7ad; color: $driver-primary; font-size: 12px; line-height: 22px; }
.photo-plus { font-size: 27px; line-height: 32px; font-weight: 300; }
.photo-remove { margin: 2px 0 0; padding: 0; min-height: 44px; background: transparent; color: #b42318; font-size: 12px; line-height: 44px; }
.document-toggle { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin: 18px 0 0; padding: 14px 0 0; border-top: 1px solid #edf1f1; }
.document-toggle__title { font-size: 16px; font-weight: 500; }
.actions { margin-top: 24px; }
.card__top { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #edf1f1; }
.card__no { min-width: 0; overflow-wrap: anywhere; color: $driver-muted; font-size: 13px; }
.tag { flex-shrink: 0; padding: 4px 9px; border-radius: 6px; background: $driver-soft; color: $driver-primary; font-size: 12px; white-space: nowrap; }
.line { display: flex; gap: 12px; margin-top: 12px; font-size: 16px; line-height: 25px; }
.line__label { flex: 0 0 60px; font-size: 14px; color: $driver-muted; }
.line__value { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.line__value--warn { color: #9a4a0b; }
.empty { padding: 64px 16px; text-align: center; color: $driver-muted; font-size: 16px; }
</style>
