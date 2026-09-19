<template>
  <view class="page">
    <view class="card">
      <view class="title">手机号验证</view>
      <view class="tip">验证只用来确认你是这车货的出售者本人。注册就发生在「确认结算」这一步。</view>

      <view class="field">
        <text class="field__label">手机号</text>
        <input v-model="mobile" class="input" type="number" maxlength="11" placeholder="用于接收验证码的手机号" />
      </view>
      <view class="field">
        <text class="field__label">验证码</text>
        <view class="code-row">
          <input v-model="code" class="input input--code" type="number" maxlength="6" placeholder="6 位验证码" />
          <button class="code-btn" :disabled="counting > 0" @click="onSendCode">
            {{ counting > 0 ? `${counting}s` : '获取验证码' }}
          </button>
        </view>
      </view>

      <button class="btn btn--primary" :loading="submitting" @click="onLogin">登录并查看</button>
      <view v-if="error" class="error">{{ error }}</view>
    </view>

    <view class="card note">
      <view class="note__line">同一手机号只对应一个登录凭证，不会跟着哪一家回收企业重复建。</view>
      <view class="note__line">注销账号不等于删除交易记录。</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { bindByLoginMobile, sendSmsCode, smsLogin, SellerSubject } from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'

defineOptions({ name: 'SellerLogin' })

const auth = useSellerAuthStore()
const mobile = ref('')
const code = ref('')
const stationCode = ref('')
const stationId = ref('')
const error = ref('')
const submitting = ref(false)
const counting = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

onLoad((query) => {
  stationCode.value = (query?.station as string) || ''
  stationId.value = (query?.stationId as string) || ''
  // 已登录直接进首页
  if (auth.token && auth.subject) {
    uni.redirectTo({ url: `/pages/home/index?stationId=${stationId.value}` })
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

async function onSendCode() {
  error.value = ''
  if (!/^1\d{10}$/.test(mobile.value)) {
    error.value = '请输入 11 位手机号'
    return
  }
  try {
    await sendSmsCode(mobile.value)
    counting.value = 60
    timer = setInterval(() => {
      counting.value -= 1
      if (counting.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
    uni.showToast({ title: '验证码已发送', icon: 'none' })
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function onLogin() {
  error.value = ''
  if (!/^1\d{10}$/.test(mobile.value) || !code.value) {
    error.value = '请填写手机号与验证码'
    return
  }
  submitting.value = true
  try {
    const resp = await smsLogin(mobile.value, code.value)
    let subjects: SellerSubject[] = resp.subjects || []
    // 扫码进屋但还没绑过身份：用登录手机号在本租户里找收方档案
    if (subjects.length === 0) {
      try {
        subjects = await bindByLoginMobile()
      } catch (bindError) {
        subjects = []
        error.value = (bindError as Error).message
      }
    }
    if (subjects.length === 0) {
      // 没有匹配到任何身份：只留登录凭证，进首页给明确空态，不造假列表
      auth.signIn(resp.accessToken, null)
      uni.redirectTo({ url: `/pages/home/index?empty=1&stationId=${stationId.value}` })
      return
    }
    const first = subjects[0]
    auth.setTokenAndSubject(resp.accessToken, {
      naturalPersonId: first.naturalPersonId,
      name: first.name || '',
      mobile: first.mobile || mobile.value,
      idCardNo: first.idCardNo,
      realNameStatusName: first.realNameStatusName
    })
    uni.redirectTo({ url: `/pages/home/index?stationId=${stationId.value}` })
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    submitting.value = false
  }
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
}

.title {
  font-size: 36rpx;
  font-weight: 700;
}

.tip {
  margin: 12rpx 0 24rpx;
  color: $seller-text-secondary;
  line-height: 1.7;
}

.field {
  &__label {
    display: block;
    margin-bottom: 8rpx;
    color: $seller-text-secondary;
    font-size: 26rpx;
  }
}

.input {
  height: 84rpx;
  padding: 0 20rpx;
  background-color: #f5f6f8;
  border-radius: 12rpx;

  &--code {
    flex: 1;
  }
}

.code-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.code-btn {
  width: 220rpx;
  height: 84rpx;
  line-height: 84rpx;
  color: $seller-primary;
  background-color: #ffffff;
  border: 1rpx solid $seller-primary;
  border-radius: 12rpx;
  font-size: 26rpx;
}

.field + .field .input {
  margin-bottom: 20rpx;
}

.btn {
  width: 100%;
  color: #ffffff;
  background-color: $seller-primary;

  &--primary {
    margin-top: 8rpx;
  }
}

.note {
  background-color: #f7f8fa;

  &__line {
    color: $seller-text-secondary;
    font-size: 26rpx;
    line-height: 1.8;
  }
}

.error {
  margin-top: 16rpx;
  color: #cf1322;
  text-align: center;
}
</style>
