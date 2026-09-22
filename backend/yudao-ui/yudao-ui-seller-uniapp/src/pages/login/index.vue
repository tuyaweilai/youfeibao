<template>
  <view class="page">
    <view class="hero">
      <view class="brand-mark" aria-hidden="true">
        <view class="brand-mark__drop"></view>
        <view class="brand-mark__check"></view>
      </view>
      <view class="hero__eyebrow">自然人出售者服务</view>
      <view class="title">验证手机号</view>
      <view class="tip">登录后查看与你相关的结算、签署和开票进度</view>
    </view>

    <view class="card form-card">
      <view class="field">
        <text class="field__label">手机号</text>
        <view class="input-shell">
          <text class="input-prefix">+86</text>
          <view class="input-divider"></view>
          <input
            v-model="mobile"
            class="input"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            placeholder-class="input-placeholder"
          />
        </view>
      </view>
      <view class="field">
        <text class="field__label">验证码</text>
        <view class="code-row">
          <view class="input-shell input-shell--code">
            <input
              v-model="code"
              class="input"
              type="number"
              maxlength="6"
              placeholder="请输入 6 位验证码"
              placeholder-class="input-placeholder"
            />
          </view>
          <button class="code-btn" :disabled="counting > 0 || submitting" @click="onSendCode">
            {{ counting > 0 ? `${counting} 秒` : '获取验证码' }}
          </button>
        </view>
      </view>

      <view v-if="error" class="error" role="alert">
        <view class="error__dot"></view>
        <text>{{ error }}</text>
      </view>

      <button class="btn btn--primary" :loading="submitting" :disabled="submitting" @click="onLogin">
        验证并登录
      </button>
      <view class="form-hint">未注册的手机号验证后将自动创建账号</view>
    </view>

    <view class="trust-row">
      <view class="trust-item">
        <view class="trust-item__icon trust-item__icon--phone"></view>
        <text>仅用于身份验证</text>
      </view>
      <view class="trust-divider"></view>
      <view class="trust-item">
        <view class="trust-item__icon trust-item__icon--lock"></view>
        <text>信息安全保护</text>
      </view>
    </view>

    <view class="legal-note">同一手机号对应唯一登录凭证，不会因回收企业不同重复建号</view>
  </view>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { bindByLoginMobile, sendSmsCode, smsLogin, SellerSubject } from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'
import { rememberStationId, switchSellerTab } from '@/utils/nav'

defineOptions({ name: 'SellerLogin' })

const auth = useSellerAuthStore()
const mobile = ref('')
const code = ref('')
const stationCode = ref('')
const error = ref('')
const submitting = ref(false)
const counting = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

/** 登录后的落点固定在首页 tab：场站编号由上一页存进本地（底部导航换页带不了 query） */
function goHomeTab() {
  switchSellerTab('/pages/home/index')
}

onLoad((query) => {
  stationCode.value = (query?.station as string) || ''
  rememberStationId((query?.stationId as string) || '')
  // 已登录直接进首页
  if (auth.token && auth.subject) {
    goHomeTab()
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
      goHomeTab()
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
    goHomeTab()
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
  padding: 56rpx 32rpx 48rpx;
  color: #172033;
  background:
    radial-gradient(circle at 88% 4%, rgba(58, 149, 255, 0.12), transparent 34%),
    linear-gradient(180deg, #f7faff 0%, #f4f7fb 56%, #f7f8fa 100%);
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8rpx 20rpx 44rpx;
  text-align: center;
}

.brand-mark {
  position: relative;
  width: 104rpx;
  height: 104rpx;
  margin-bottom: 24rpx;
  background: linear-gradient(145deg, #2e8cff, #1264e7);
  border: 8rpx solid rgba(255, 255, 255, 0.9);
  border-radius: 32rpx;
  box-shadow: 0 18rpx 44rpx rgba(22, 119, 255, 0.22);

  &__drop {
    position: absolute;
    top: 23rpx;
    left: 33rpx;
    width: 34rpx;
    height: 46rpx;
    border: 5rpx solid #ffffff;
    border-radius: 60% 40% 58% 42% / 66% 46% 54% 34%;
    transform: rotate(45deg);
  }

  &__check {
    position: absolute;
    right: 24rpx;
    bottom: 26rpx;
    width: 28rpx;
    height: 13rpx;
    border-bottom: 5rpx solid #ffffff;
    border-left: 5rpx solid #ffffff;
    transform: rotate(-45deg);
  }
}

.hero__eyebrow {
  margin-bottom: 10rpx;
  color: #3077df;
  font-size: 24rpx;
  font-weight: 600;
  letter-spacing: 4rpx;
}

.title {
  font-size: 48rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
}

.tip {
  margin-top: 16rpx;
  color: #667085;
  font-size: 28rpx;
  line-height: 1.6;
}

.card {
  background-color: #ffffff;
  border: 1rpx solid rgba(22, 119, 255, 0.08);
  border-radius: 28rpx;
  box-shadow: 0 18rpx 54rpx rgba(31, 55, 88, 0.08);
}

.form-card {
  padding: 40rpx 32rpx 34rpx;
}

.field {
  margin-bottom: 28rpx;

  &__label {
    display: block;
    margin-bottom: 14rpx;
    color: #344054;
    font-size: 26rpx;
    font-weight: 600;
  }
}

.input-shell {
  display: flex;
  align-items: center;
  box-sizing: border-box;
  height: 96rpx;
  padding: 0 24rpx;
  background-color: #f7f9fc;
  border: 2rpx solid #e7ebf2;
  border-radius: 18rpx;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-within {
    background-color: #ffffff;
    border-color: #4b91f7;
    box-shadow: 0 0 0 6rpx rgba(22, 119, 255, 0.09);
  }

  &--code {
    flex: 1;
    min-width: 0;
  }
}

.input-prefix {
  flex-shrink: 0;
  color: #172033;
  font-size: 29rpx;
  font-weight: 600;
}

.input-divider {
  width: 1rpx;
  height: 34rpx;
  margin: 0 22rpx;
  background-color: #d7dce5;
}

.input {
  flex: 1;
  min-width: 0;
  height: 92rpx;
  color: #172033;
  font-size: 30rpx;
}

.input-placeholder {
  color: #a6adba;
}

.code-row {
  display: flex;
  gap: 18rpx;
}

.code-btn {
  flex-shrink: 0;
  width: 218rpx;
  height: 96rpx;
  margin: 0;
  padding: 0;
  color: #166fe5;
  background-color: #edf5ff;
  border: 2rpx solid #d4e7ff;
  border-radius: 18rpx;
  font-size: 26rpx;
  font-weight: 600;
  line-height: 94rpx;
  transition: background-color 0.2s ease, opacity 0.2s ease;

  &::after {
    border: 0;
  }

  &[disabled] {
    color: #98a2b3;
    background-color: #f2f4f7;
    border-color: #e4e7ec;
    opacity: 1;
  }
}

.btn {
  box-sizing: border-box;
  width: 100%;
  height: 96rpx;
  margin: 8rpx 0 0;
  color: #ffffff;
  border-radius: 18rpx;
  font-size: 31rpx;
  font-weight: 700;
  line-height: 96rpx;

  &--primary {
    background: linear-gradient(100deg, #1677ff 0%, #2d8bff 100%);
    box-shadow: 0 14rpx 28rpx rgba(22, 119, 255, 0.2);
  }

  &::after {
    border: 0;
  }

  &[disabled] {
    color: rgba(255, 255, 255, 0.78);
    opacity: 0.72;
  }
}

.form-hint {
  margin-top: 24rpx;
  color: #98a2b3;
  font-size: 24rpx;
  line-height: 1.5;
  text-align: center;
}

.trust-row {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 34rpx;
  color: #667085;
  font-size: 24rpx;
}

.trust-item {
  display: flex;
  align-items: center;
  gap: 10rpx;

  &__icon {
    position: relative;
    box-sizing: border-box;
    width: 26rpx;
    height: 30rpx;
    border: 3rpx solid #6096dd;

    &--phone {
      border-radius: 5rpx;

      &::after {
        position: absolute;
        bottom: 2rpx;
        left: 7rpx;
        width: 6rpx;
        height: 2rpx;
        background-color: #6096dd;
        border-radius: 2rpx;
        content: '';
      }
    }

    &--lock {
      height: 22rpx;
      margin-top: 8rpx;
      border-radius: 5rpx;

      &::before {
        position: absolute;
        top: -16rpx;
        left: 4rpx;
        box-sizing: border-box;
        width: 12rpx;
        height: 16rpx;
        border: 3rpx solid #6096dd;
        border-bottom: 0;
        border-radius: 8rpx 8rpx 0 0;
        content: '';
      }
    }
  }
}

.trust-divider {
  width: 1rpx;
  height: 24rpx;
  margin: 0 24rpx;
  background-color: #d0d5dd;
}

.legal-note {
  margin: 26rpx auto 0;
  color: #98a2b3;
  font-size: 22rpx;
  line-height: 1.6;
  text-align: center;
}

.error {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: -8rpx 0 20rpx;
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

@media (prefers-reduced-motion: reduce) {
  .input-shell,
  .code-btn {
    transition: none;
  }
}
</style>
