<template>
  <view v-if="webViewUrl" class="webview">
    <web-view :src="webViewUrl" />
  </view>

  <view v-else class="page">
    <view class="card">
      <view class="card__title">变更收款账户</view>
      <view class="kv">
        <text class="kv__k">{{ enterpriseName || '回收企业' }}</text>
        <text>当前卡尾号 {{ cardTail || '—' }}</text>
      </view>
      <view class="scope-note">
        工行收方入驻只绑本人一张卡，换卡要重新走一次工行审核。审核期间该企业新交易的付款会挂起，
        <text class="scope-note--strong">原卡在你确认变更前仍然有效</text>。
      </view>
    </view>

    <view class="card">
      <view class="field">
        <text class="field__label">新银行卡号</text>
        <input v-model="form.bankCardNo" class="input" type="number" placeholder="请输入本人银行卡号" />
      </view>
      <view class="field">
        <text class="field__label">开户银行</text>
        <input v-model="form.bankName" class="input" placeholder="例如：中国工商银行（选填）" />
      </view>
      <view class="field">
        <text class="field__label">开户支行</text>
        <input v-model="form.bankBranch" class="input" placeholder="例如：北京分行营业部（选填）" />
      </view>
      <button class="btn btn--primary" :loading="submitting" @click="onSubmit">
        提交并去工行页面绑卡
      </button>
      <view v-if="error" class="error">{{ error }}</view>
      <view v-if="result" class="result">
        <view class="result__status">{{ result.statusName }}</view>
        <view class="result__note">{{ result.scopeNote }}</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { requestBankCardChange, SellerBankCardChange } from '@/api/seller'
import { onboardingFormUrl } from '@/api/public'
import { useSellerAuthStore } from '@/store/auth'

defineOptions({ name: 'SellerBankCard' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)

const tenantId = ref(0)
const enterpriseName = ref('')
const cardTail = ref('')
const webViewUrl = ref('')
const submitting = ref(false)
const error = ref('')
const result = ref<SellerBankCardChange | null>(null)
const form = reactive({ bankCardNo: '', bankName: '', bankBranch: '' })

onLoad((query) => {
  tenantId.value = Number(query?.tenantId || 0)
  enterpriseName.value = decodeURIComponent(String(query?.enterpriseName || ''))
  cardTail.value = String(query?.cardTail || '')
})

function trxChannel(): string {
  // #ifdef MP-WEIXIN
  return '05'
  // #endif
  // #ifndef MP-WEIXIN
  return '03'
  // #endif
}

async function onSubmit() {
  error.value = ''
  if (!naturalPersonId.value) {
    error.value = '登录已过期，请重新登录'
    return
  }
  if (!tenantId.value) {
    error.value = '缺少回收企业，请从「我的资料 → 收款账户」进入'
    return
  }
  const cardNo = form.bankCardNo.trim()
  if (!cardNo) {
    error.value = '请填写新银行卡号'
    return
  }
  submitting.value = true
  try {
    const change = await requestBankCardChange({
      naturalPersonId: naturalPersonId.value,
      tenantId: tenantId.value,
      bankCardNo: cardNo,
      bankName: form.bankName || undefined,
      bankBranch: form.bankBranch || undefined
    })
    result.value = change
    uni.showToast({ title: change.statusName || '已提交银行审核', icon: 'none' })
    if (change.token) {
      openIcbcForm(change.token)
    }
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    submitting.value = false
  }
}

/** 用 ONBOARDING 一次性令牌打开后端输出的工行收方入驻表单（与首次建档同一套机制） */
function openIcbcForm(token: string) {
  const url = onboardingFormUrl(token, trxChannel())
  // #ifdef H5
  window.open(url, '_blank')
  // #endif
  // #ifndef H5
  webViewUrl.value = url
  // #endif
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

.kv {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 10rpx 0;

  &__k {
    color: $seller-text-secondary;
  }
}

.field {
  margin-bottom: 20rpx;

  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $seller-text-secondary;
  }
}

.input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  background-color: #f7f8fa;
  border-radius: 12rpx;
}

.btn {
  width: 100%;
  margin-top: 12rpx;

  &--primary {
    color: #ffffff;
    background-color: $seller-primary;
  }
}

.scope-note {
  margin-top: 12rpx;
  color: $seller-text-secondary;
  font-size: 24rpx;
  line-height: 1.7;

  &--strong {
    color: #cf1322;
  }
}

.result {
  margin-top: 20rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #eef0f3;

  &__status {
    margin-bottom: 8rpx;
    font-weight: 600;
    color: $seller-primary;
  }

  &__note {
    color: $seller-text-secondary;
    font-size: 24rpx;
    line-height: 1.7;
  }
}

.error {
  margin-top: 16rpx;
  color: #cf1322;
}

.webview {
  width: 100%;
  height: 100vh;
}
</style>
