<template>
  <view class="page">
    <view class="card">
      <view class="card__title">变更收款账户</view>
      <view class="card__subtitle">提交后将由银行重新审核，请填写本人账户</view>
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
        <text class="field__label">这张卡是你本人的工行卡吗？</text>
        <view class="radios">
          <view
            class="radio"
            :class="{ 'radio--on': form.accountCode === '1' }"
            role="radio"
            :aria-checked="form.accountCode === '1'"
            @click="form.accountCode = '1'"
          >
            <text class="radio__dot" />是，本人工行卡
          </view>
          <view
            class="radio"
            :class="{ 'radio--on': form.accountCode === '0' }"
            role="radio"
            :aria-checked="form.accountCode === '0'"
            @click="form.accountCode = '0'"
          >
            <text class="radio__dot" />不是，其他银行
          </view>
        </view>
        <view class="field__hint">是否我行卡由工行审核使用；填错会被驳回，所以请你本人确认。</view>
      </view>
      <button class="btn btn--primary" :loading="submitting" @click="onSubmit">提交变更</button>
      <view v-if="error" class="error" role="alert"><view class="error__dot"></view>{{ error }}</view>
      <view v-if="result" class="result">
        <view class="result__status">{{ result.statusName }}</view>
        <view class="result__note">{{ result.scopeNote || result.message }}</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { requestBankCardChange, SellerBankCardChange } from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'

defineOptions({ name: 'SellerBankCard' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)

const tenantId = ref(0)
const enterpriseName = ref('')
const cardTail = ref('')
const submitting = ref(false)
const error = ref('')
const result = ref<SellerBankCardChange | null>(null)
/** 是否本人我行卡：1-我行用户（默认），0-非我行用户 */
const form = reactive({ bankCardNo: '', accountCode: '1' })

/** 工行收方账号：16-19 位数字（与现场端、后台同一条规则） */
const BANK_CARD_RE = /^\d{16,19}$/

onLoad((query) => {
  tenantId.value = Number(query?.tenantId || 0)
  enterpriseName.value = decodeURIComponent(String(query?.enterpriseName || ''))
  cardTail.value = String(query?.cardTail || '')
})

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
  if (!BANK_CARD_RE.test(cardNo)) {
    error.value = '银行卡号应为 16-19 位数字，请核对后重填'
    return
  }
  submitting.value = true
  try {
    // 后端直接走工行的收方修改数据接口提交（#89）：这里不再拿一次性令牌去开页面
    result.value = await requestBankCardChange({
      naturalPersonId: naturalPersonId.value,
      tenantId: tenantId.value,
      bankCardNo: cardNo,
      accountCode: form.accountCode
    })
    uni.showToast({ title: result.value.statusName || '已提交银行审核', icon: 'none' })
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    submitting.value = false
  }
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
  padding: 36rpx 32rpx;
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
    margin: 8rpx 0 26rpx;
    color: $seller-text-secondary;
    font-size: 24rpx;
  }
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 18rpx 20rpx;
  color: #344054;
  background-color: #f7f9fc;
  border-radius: 14rpx;

  &__k {
    color: $seller-text-secondary;
  }
}

.field {
  margin-bottom: 30rpx;

  &__label {
    display: block;
    margin-bottom: 12rpx;
    color: #344054;
    font-size: 26rpx;
    font-weight: 600;
  }

  &__hint {
    margin-top: 14rpx;
    color: #7b8494;
    font-size: 24rpx;
    line-height: 1.6;
  }
}

.input {
  box-sizing: border-box;
  width: 100%;
  height: 96rpx;
  padding: 0 24rpx;
  color: $seller-text;
  background-color: #f7f9fc;
  border: 2rpx solid #e7ebf2;
  border-radius: 18rpx;
  font-size: 29rpx;
}

.radios {
  display: flex;
  gap: 16rpx;
}

.radio {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-height: 88rpx;
  padding: 12rpx 18rpx;
  color: #566174;
  background-color: #f7f9fc;
  border: 2rpx solid #e7ebf2;
  border-radius: 16rpx;
  font-size: 25rpx;

  &--on {
    background-color: #eef4ff;
    border-color: #75aaf1;
    color: $seller-primary;
  }

  &__dot {
    box-sizing: border-box;
    width: 24rpx;
    height: 24rpx;
    border: 5rpx solid #ffffff;
    border-radius: 50%;
    background-color: #c9d2dc;
    box-shadow: 0 0 0 2rpx #c9d2dc;
  }

  &--on &__dot {
    background-color: $seller-primary;
    box-shadow: 0 0 0 2rpx $seller-primary;
  }
}

.btn {
  box-sizing: border-box;
  width: 100%;
  height: 96rpx;
  margin: 8rpx 0 0;
  border-radius: 18rpx;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 96rpx;

  &--primary {
    color: #ffffff;
    background: linear-gradient(100deg, $seller-primary 0%, #2d8bff 100%);
    box-shadow: 0 14rpx 28rpx rgba(22, 119, 255, 0.2);
  }
}

.scope-note {
  margin-top: 12rpx;
  color: $seller-text-secondary;
  font-size: 24rpx;
  line-height: 1.7;

  &--strong {
    color: $seller-text;
  }
}

.result {
  margin-top: 22rpx;
  padding: 22rpx;
  background-color: #edf8f2;
  border-radius: 16rpx;

  &__status {
    color: #16834b;
    font-weight: 700;
  }

  &__note {
    margin-top: 8rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
    line-height: 1.6;
  }
}

.error {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  color: #b42318;
  background-color: #fff3f1;
  border-radius: 14rpx;
  font-size: 25rpx;

  &__dot {
    flex-shrink: 0;
    width: 10rpx;
    height: 10rpx;
    background-color: #d92d20;
    border-radius: 50%;
  }
}
</style>
