<template>
  <view class="page">
    <template v-if="!payeeId">
      <view class="card">
        <view class="card__title">建档</view>
        <view class="card__tip">
          先认出人：填身份证号或手机号带出已有档案；没有档案就新建。建档后依次完成准入四步。
        </view>
        <view class="field"><text class="field__label">身份证号</text><input v-model="form.idCardNo" class="field__input" placeholder="选填" /></view>
        <view class="field"><text class="field__label">手机号</text><input v-model="form.mobile" class="field__input" placeholder="选填" /></view>
        <view class="field"><text class="field__label">姓名</text><input v-model="form.name" class="field__input" placeholder="新建时必填" /></view>
        <button class="btn btn--ghost" :loading="busy" @click="onLookup">带档</button>
        <button class="btn" :loading="busy" @click="onCreate">新建档案</button>
      </view>
    </template>

    <template v-else>
      <view class="picked">
        正在为 <text class="picked__name">{{ payeeName || '出售者' }}</text> 办理准入（档案 #{{ payeeId }}）
        <text class="picked__reset" @click="reset">换人</text>
      </view>

      <view class="steps__tip">
        准入四步：实名 → 收方入驻 → 框架收购协议 → 首次授权。四步都做完，这家企业的反向开票才走得通；钱不在这里算。
      </view>

      <!-- 1. 实人认证 -->
      <view class="card">
        <view class="card__head">
          <text class="card__title">1. 实人认证</text>
          <text :class="['tag', isRealNamePassed(overview?.realNameStatus) ? 'tag--ok' : 'tag--todo']">
            {{ overview?.realNameStatusName || '未认证' }}
          </text>
        </view>
        <view v-if="overview?.realNameMsg" class="card__msg">{{ overview.realNameMsg }}</view>
        <view class="card__tip">人脸必须由出售者本人做（工行活体，我们无法替代）。失败可多试几次，或留联系方式走人工。</view>
        <!-- 未通过（未认证 / 认证中 / 未通过）都给发起入口：认证中也要能重新发起（#90） -->
        <button v-if="!isRealNamePassed(overview?.realNameStatus)" class="btn" :loading="busy" @click="startRealNameStep">
          {{ overview?.realNameStatus === REAL_NAME_STATUS.NOT_STARTED ? '发起实名认证' : '重新发起实名认证' }}
        </button>
        <button class="btn btn--ghost" :loading="busy" @click="syncRealNameStep">我已认证完，查一下结果</button>
      </view>

      <!-- 2. 收方入驻 -->
      <view class="card">
        <view class="card__head">
          <text class="card__title">2. 收方入驻（绑定本人银行卡）</text>
          <text :class="['tag', isOnboardingReady(overview?.onboardingState) ? 'tag--ok' : 'tag--todo']">
            {{ overview?.onboardingStateName || overview?.onboardingState || '未入驻' }}
          </text>
        </view>
        <view v-if="overview?.rejectReason" class="card__msg card__msg--warn">被拒原因：{{ overview.rejectReason }}</view>
        <view class="card__tip">工行只绑本人一张卡；换卡要重走这一步，审核期间新交易的付款会挂起。</view>
        <button class="btn" :loading="busy" @click="submitOnboardingStep">发起收方入驻</button>
        <button class="btn btn--ghost" :loading="busy" @click="syncOnboardingStep">我已提交，查一下结果</button>
        <view class="fallback">
          <text class="fallback__title">入驻没通过？留个联系方式，让企业来找你</text>
          <input v-model="fallbackMobile" class="fallback__input" placeholder="手机号" />
          <button class="btn btn--ghost" :loading="busy" @click="leaveContact">留联系方式</button>
        </view>
      </view>

      <!-- 3. 框架收购协议 -->
      <view class="card">
        <view class="card__head">
          <text class="card__title">3. 框架收购协议</text>
          <text :class="['tag', overview?.frameworkAgreement ? 'tag--ok' : 'tag--todo']">
            {{ overview?.frameworkAgreement ? '已签' : '未签' }}
          </text>
        </view>
        <view class="card__tip">税总 5 号公告第十七条要求保存收购合同或协议，所以名称、数量、规格、回收期次、结算方式都要填。</view>
        <view class="field"><text class="field__label">货物名称</text><input v-model="agreement.productName" class="field__input" /></view>
        <view class="field"><text class="field__label">数量</text><input v-model="agreement.quantity" class="field__input" /></view>
        <view class="field"><text class="field__label">规格</text><input v-model="agreement.specification" class="field__input" /></view>
        <view class="field"><text class="field__label">回收期次</text><input v-model="agreement.recyclePeriod" class="field__input" /></view>
        <view class="field"><text class="field__label">结算方式</text><input v-model="agreement.settlementMethod" class="field__input" /></view>
        <button class="btn" :loading="busy" @click="saveAgreementStep">
          {{ overview?.frameworkAgreement ? '重签协议' : '签署协议' }}
        </button>
      </view>

      <!-- 4. 首次授权 -->
      <view class="card">
        <view class="card__head">
          <text class="card__title">4. 首次授权</text>
          <text :class="['tag', overview?.authorization ? 'tag--ok' : 'tag--todo']">
            {{ overview?.authorization ? '已授权' : '未授权' }}
          </text>
        </view>
        <view class="card__tip">
          首次开票要征得出售者同意并留证（同上公告第十二条）。授权与「结算确认」是两件事：确认一律由出售者本人做。
        </view>
        <view class="check" @click="toggle(authorization, 'reverseInvoiceAuthorized')">
          <checkbox :checked="authorization.reverseInvoiceAuthorized" /><text>授权反向开票</text>
        </view>
        <view class="check" @click="toggle(authorization, 'taxAgencyAuthorized')">
          <checkbox :checked="authorization.taxAgencyAuthorized" /><text>授权代办税费</text>
        </view>
        <button class="btn" :loading="busy" @click="authorizeStep">保存授权</button>
      </view>

      <view class="card card--handoff">
        <view class="card__title">把建档链接交给本人</view>
        <view class="card__tip">这一端**没有「替他确认」的按钮**：确认要出售者本人在自己手机上点（ADR 0030）。</view>
        <button class="btn btn--primary" :loading="busy" @click="onHandoff">生成并转达链接</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  REAL_NAME_STATUS,
  PayeeVO,
  createPayee,
  createPublicToken,
  findReturningCustomer,
  isOnboardingReady,
  isRealNamePassed,
  useSellerOnboarding
} from '@youfeibao/field-shared'
import { SELLER_APP_URL } from '@/config/env'

/**
 * 司机端现场建档（V7 #74）。
 *
 * 司机上门时现场没有收货员（ADR 0030），所以准入四步要在司机的手机上完成。
 * **四步的业务逻辑用的是与收货员现场端同一个 composable**（`@youfeibao/field-shared`），
 * 这里只多一层「先认出是谁」和自己的模板。
 *
 * 模板为什么不共享：见 `packages/field-shared/README.md`（跨工程共用一个 .vue 会与
 * uni-app + vue-tsc 的类型生成打架，试过三条路）。
 */
const busy = ref(false)
const payeeId = ref<number>()
const payeeName = ref('')
const form = reactive<PayeeVO>({ idCardNo: '', mobile: '', name: '' })

// 四步逻辑走共享 composable：接口编排、状态、校验、工行表单承载都在那边
// 把要用的都解构出来：模板只认顶层绑定，`steps.xxx` 在模板里是看得见、但方法名直接写在模板里会找不到
const steps = useSellerOnboarding(() => payeeId.value)
const {
  overview,
  fallbackMobile,
  agreement,
  authorization,
  startRealNameStep,
  syncRealNameStep,
  submitOnboardingStep,
  syncOnboardingStep,
  leaveContact,
  saveAgreementStep,
  authorizeStep
} = steps

function tips(message: string) {
  uni.showToast({ title: message, icon: 'none' })
}

async function onLookup() {
  if (!form.idCardNo && !form.mobile) {
    tips('身份证号或手机号至少要填一个')
    return
  }
  busy.value = true
  try {
    const found = await findReturningCustomer({ idCardNo: form.idCardNo, mobile: form.mobile })
    if (!found?.id) {
      tips('没有查到档案，请点「新建档案」')
      return
    }
    payeeId.value = found.id
    payeeName.value = found.name || ''
  } catch (e) {
    tips((e as Error).message || '带档失败')
  } finally {
    busy.value = false
  }
}

async function onCreate() {
  if (!form.name) {
    tips('新建档案至少要填姓名')
    return
  }
  busy.value = true
  try {
    const id = await createPayee({ ...form, businessType: 'RECYCLE' })
    payeeId.value = id
    payeeName.value = form.name || ''
  } catch (e) {
    tips((e as Error).message || '新建档案失败')
  } finally {
    busy.value = false
  }
}

function reset() {
  payeeId.value = undefined
  payeeName.value = ''
}

function toggle(target: { reverseInvoiceAuthorized?: boolean; taxAgencyAuthorized?: boolean }, key: 'reverseInvoiceAuthorized' | 'taxAgencyAuthorized') {
  target[key] = !target[key]
}

/**
 * 把**建档链接**交给出售者本人（与收货员现场端同一做法：签发 ONBOARDING 一次性令牌）。
 *
 * **司机不代点任何东西**（ADR 0030）：实名与入驻要本人在场完成，剩下的也能换到他自己的手机上做。
 */
async function onHandoff() {
  busy.value = true
  try {
    const resp = await createPublicToken({ purpose: 'ONBOARDING', payeeId: payeeId.value })
    const token = resp.token || ''
    const link = SELLER_APP_URL
      ? `${SELLER_APP_URL.replace(/\/$/, '')}/#/?token=${encodeURIComponent(token)}&purpose=ONBOARDING`
      : token
    uni.setClipboardData({
      data: link,
      success: () => tips('建档链接已复制，请交给出售者本人打开')
    })
  } catch (e) {
    tips((e as Error).message || '生成链接失败')
  } finally {
    busy.value = false
  }
}

onLoad((options) => {
  // 从任务详情进来时可能已经知道出售者（后续票接上），先留个口子
  if (options?.payeeId) {
    payeeId.value = Number(options.payeeId)
  }
})
</script>

<style scoped lang="scss">
.page {
  padding: 24rpx;
}

.steps__tip {
  margin-bottom: 20rpx;
  color: #6b7a72;
  font-size: 24rpx;
  line-height: 1.6;
}

.card {
  margin-bottom: 24rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 16rpx;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12rpx;
  }

  &__title {
    margin-bottom: 12rpx;
    font-size: 30rpx;
    font-weight: 600;
  }

  &__tip {
    margin: 10rpx 0 16rpx;
    color: #8a919f;
    font-size: 24rpx;
    line-height: 1.6;
  }

  &__msg {
    margin-bottom: 10rpx;
    color: #1d4ed8;
    font-size: 24rpx;

    &--warn {
      color: #b91c1c;
    }
  }

  &--handoff {
    border: 1rpx solid #bbf7d0;
    background-color: #f0fdf4;
  }
}

.picked {
  margin-bottom: 16rpx;
  padding: 20rpx 24rpx;
  background-color: #eff6ff;
  border: 1rpx solid #bfdbfe;
  border-radius: 12rpx;
  color: #1d4ed8;
  font-size: 26rpx;

  &__name {
    font-weight: 600;
  }

  &__reset {
    float: right;
    color: #6b7a72;
  }
}

.tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;

  &--ok {
    background-color: #dcfce7;
    color: #15803d;
  }

  &--todo {
    background-color: #fef3c7;
    color: #b45309;
  }
}

.field {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;

  &__label {
    width: 160rpx;
    color: #4b5563;
    font-size: 26rpx;
  }

  &__input {
    flex: 1;
    height: 72rpx;
    padding: 0 20rpx;
    background-color: #f8fafc;
    border: 1rpx solid #e5e7eb;
    border-radius: 10rpx;
  }
}

.check {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.btn {
  margin-top: 16rpx;
  background-color: #16a34a;
  border-radius: 12rpx;
  color: #fff;

  &--ghost {
    background-color: transparent;
    border: 1rpx solid #e5e7eb;
    color: #4b5563;
  }

  &--primary {
    background-color: #16a34a;
  }
}

.fallback {
  margin-top: 20rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed #e5e7eb;

  &__title {
    display: block;
    margin-bottom: 10rpx;
    color: #8a919f;
    font-size: 24rpx;
  }

  &__input {
    height: 72rpx;
    padding: 0 20rpx;
    background-color: #f8fafc;
    border: 1rpx solid #e5e7eb;
    border-radius: 10rpx;
  }
}
</style>
