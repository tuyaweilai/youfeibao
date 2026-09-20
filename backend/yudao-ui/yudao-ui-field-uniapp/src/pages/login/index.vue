<template>
  <view class="login">
    <view class="login__glow login__glow--top" />
    <view class="login__glow login__glow--bottom" />

    <view class="login__shell">
      <view class="hero">
        <view class="brand" aria-label="有肥宝收货现场">
          <view class="brand__mark" aria-hidden="true">
            <view class="brand__box brand__box--back" />
            <view class="brand__box brand__box--front" />
            <view class="brand__line" />
          </view>
          <view>
            <view class="brand__eyebrow">YOUFEIBAO FIELD</view>
            <view class="brand__name">有肥宝</view>
          </view>
        </view>

        <view class="hero__content">
          <view class="hero__badge">
            <text class="hero__badge-dot" />
            <text>现场作业系统</text>
          </view>
          <view class="hero__title">收得清楚，<br />每一笔都有据可查</view>
          <view class="hero__desc">面向回收企业收货员，完成现场收购登记、出售者建档与结算确认。</view>
        </view>

        <view class="hero__features">
          <view class="feature">
            <view class="feature__check">✓</view>
            <view>
              <view class="feature__title">过程留痕</view>
              <view class="feature__desc">关键节点完整记录</view>
            </view>
          </view>
          <view class="feature">
            <view class="feature__check">✓</view>
            <view>
              <view class="feature__title">数据可信</view>
              <view class="feature__desc">重量、金额清晰可核</view>
            </view>
          </view>
        </view>
      </view>

      <view class="login-card">
        <view class="login-card__header">
          <view class="login-card__eyebrow">工作人员入口</view>
          <view class="login-card__title">欢迎回来</view>
          <view class="login-card__desc">登录后继续今日的现场收货工作</view>
        </view>

        <view class="login-card__form">
          <view class="field">
            <view class="field__heading">
              <text class="field__label">租户编号</text>
              <text class="field__hint">由企业管理员提供</text>
            </view>
            <view class="field__control">
              <text class="field__prefix">T</text>
              <input
                v-model.trim="form.tenantId"
                class="field__input"
                aria-label="租户编号"
                placeholder="请输入租户编号"
                placeholder-class="field__placeholder"
              />
            </view>
          </view>

          <view class="field">
            <text class="field__label">收货员账号</text>
            <view class="field__control">
              <text class="field__prefix">U</text>
              <input
                v-model.trim="form.username"
                class="field__input"
                aria-label="收货员账号"
                autocomplete="username"
                placeholder="请输入账号"
                placeholder-class="field__placeholder"
              />
            </view>
          </view>

          <view class="field">
            <text class="field__label">登录密码</text>
            <view class="field__control">
              <text class="field__prefix">P</text>
              <input
                v-model="form.password"
                class="field__input"
                aria-label="登录密码"
                autocomplete="current-password"
                password
                placeholder="请输入密码"
                placeholder-class="field__placeholder"
                confirm-type="done"
                @confirm="onSubmit"
              />
            </view>
          </view>

          <view v-if="error" class="login-card__error" role="alert">
            <text class="login-card__error-mark">!</text>
            <text>{{ error }}</text>
          </view>

          <button
            class="login-card__submit"
            :class="{ 'login-card__submit--loading': loading }"
            :disabled="loading"
            :loading="loading"
            @click="onSubmit"
          >
            <text>{{ loading ? '正在登录' : '进入工作台' }}</text>
            <text v-if="!loading" class="login-card__arrow">→</text>
          </button>

          <view class="login-card__safe">
            <text class="login-card__safe-mark" />
            <text>仅限已授权的企业工作人员使用</text>
          </view>
        </view>
      </view>
    </view>

    <view class="login__footer">有肥宝 · 让每一笔收购更清楚</view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getTenantId, setTenantId } from '@/config/env'
import { useAuthStore } from '@/store/auth'

defineOptions({ name: 'FieldLogin' })

const auth = useAuthStore()
const form = reactive({ tenantId: getTenantId(), username: '', password: '' })
const loading = ref(false)
const error = ref('')

onShow(() => {
  if (auth.token) {
    uni.reLaunch({ url: '/pages/home/index' })
  }
})

async function onSubmit() {
  if (loading.value) return
  if (!form.tenantId || !form.username || !form.password) {
    error.value = '请填写租户编号、账号与密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    setTenantId(String(form.tenantId))
    await auth.login(form.username, form.password)
    uni.reLaunch({ url: '/pages/home/index' })
  } catch (e) {
    error.value = (e as Error).message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login {
  position: relative;
  box-sizing: border-box;
  min-height: 100vh;
  padding: calc(44rpx + env(safe-area-inset-top)) 32rpx calc(40rpx + env(safe-area-inset-bottom));
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 14%, rgba(31, 156, 105, 0.12), transparent 30%),
    linear-gradient(145deg, #f5faf7 0%, #f7f9f8 48%, #eef5f1 100%);

  &__glow {
    position: absolute;
    border: 1rpx solid rgba(28, 133, 91, 0.1);
    border-radius: 50%;
    pointer-events: none;

    &--top {
      top: -220rpx;
      right: -180rpx;
      width: 620rpx;
      height: 620rpx;
      box-shadow: 0 0 0 80rpx rgba(31, 156, 105, 0.025), 0 0 0 160rpx rgba(31, 156, 105, 0.018);
    }

    &--bottom {
      bottom: -360rpx;
      left: -300rpx;
      width: 720rpx;
      height: 720rpx;
      background: rgba(255, 255, 255, 0.36);
    }
  }

  &__shell {
    position: relative;
    z-index: 1;
    width: 100%;
    max-width: 980px;
    margin: 0 auto;
  }

  &__footer {
    position: relative;
    z-index: 1;
    margin-top: 48rpx;
    color: #76817b;
    font-size: 22rpx;
    letter-spacing: 2rpx;
    text-align: center;
  }
}

.hero {
  box-sizing: border-box;
  color: #f5fff9;
  background: linear-gradient(155deg, #123f31 0%, #175a42 58%, #1d7452 100%);
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 36rpx 44rpx;
  box-shadow: 0 24rpx 70rpx rgba(16, 64, 47, 0.16);

  &__content {
    margin-top: 64rpx;
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    gap: 12rpx;
    padding: 10rpx 18rpx;
    color: #ccebdd;
    font-size: 22rpx;
    letter-spacing: 2rpx;
    background: rgba(255, 255, 255, 0.09);
    border: 1rpx solid rgba(255, 255, 255, 0.12);
    border-radius: 999rpx;
  }

  &__badge-dot {
    width: 10rpx;
    height: 10rpx;
    background: #5ee0a2;
    border-radius: 50%;
    box-shadow: 0 0 0 6rpx rgba(94, 224, 162, 0.12);
  }

  &__title {
    margin-top: 28rpx;
    font-size: 52rpx;
    font-weight: 700;
    line-height: 1.28;
    letter-spacing: -1rpx;
  }

  &__desc {
    max-width: 560rpx;
    margin-top: 24rpx;
    color: rgba(235, 251, 243, 0.72);
    font-size: 26rpx;
    line-height: 1.75;
  }

  &__features {
    display: flex;
    gap: 40rpx;
    margin-top: 56rpx;
    padding-top: 32rpx;
    border-top: 1rpx solid rgba(255, 255, 255, 0.1);
  }
}

.brand {
  display: flex;
  align-items: center;
  gap: 20rpx;

  &__mark {
    position: relative;
    width: 64rpx;
    height: 64rpx;
    background: #ecfff4;
    border-radius: 18rpx;
  }

  &__box {
    position: absolute;
    width: 24rpx;
    height: 22rpx;
    border: 4rpx solid #176043;
    border-radius: 4rpx;

    &--back {
      top: 13rpx;
      left: 13rpx;
    }

    &--front {
      right: 11rpx;
      bottom: 11rpx;
      background: #ecfff4;
    }
  }

  &__line {
    position: absolute;
    top: 30rpx;
    left: 18rpx;
    width: 28rpx;
    height: 4rpx;
    background: #42b883;
    border-radius: 4rpx;
    transform: rotate(-35deg);
  }

  &__eyebrow {
    color: #90cbb0;
    font-size: 18rpx;
    font-weight: 600;
    letter-spacing: 3rpx;
  }

  &__name {
    margin-top: 3rpx;
    font-size: 30rpx;
    font-weight: 700;
    letter-spacing: 3rpx;
  }
}

.feature {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;

  &__check {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 32rpx;
    width: 32rpx;
    height: 32rpx;
    color: #114c35;
    font-size: 20rpx;
    font-weight: 700;
    background: #67dfa7;
    border-radius: 50%;
  }

  &__title {
    font-size: 24rpx;
    font-weight: 600;
  }

  &__desc {
    margin-top: 5rpx;
    color: rgba(235, 251, 243, 0.55);
    font-size: 20rpx;
  }
}

.login-card {
  box-sizing: border-box;
  padding: 44rpx 36rpx 40rpx;
  background: rgba(255, 255, 255, 0.96);
  border: 1rpx solid rgba(24, 78, 58, 0.08);
  border-top: 0;
  border-radius: 0 0 32rpx 32rpx;
  box-shadow: 0 28rpx 80rpx rgba(25, 63, 49, 0.1);

  &__eyebrow { color: #25855f; font-size: 22rpx; font-weight: 700; letter-spacing: 2rpx; }
  &__title { margin-top: 12rpx; color: #17221d; font-size: 44rpx; font-weight: 700; letter-spacing: -1rpx; }
  &__desc { margin-top: 12rpx; color: #728078; font-size: 25rpx; line-height: 1.6; }
  &__form { margin-top: 40rpx; }

  &__error {
    display: flex;
    align-items: center;
    gap: 12rpx;
    padding: 20rpx 22rpx;
    margin: 4rpx 0 24rpx;
    color: #a02b2b;
    font-size: 24rpx;
    line-height: 1.5;
    background: #fff3f2;
    border: 1rpx solid #f3ceca;
    border-radius: 14rpx;
  }

  &__error-mark {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 30rpx;
    width: 30rpx;
    height: 30rpx;
    color: #ffffff;
    font-size: 20rpx;
    font-weight: 700;
    background: #c94b45;
    border-radius: 50%;
  }

  &__submit {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 20rpx;
    height: 92rpx;
    padding: 0 32rpx;
    margin-top: 12rpx;
    color: #ffffff;
    font-size: 28rpx;
    font-weight: 600;
    line-height: 92rpx;
    background: #176b4c;
    border: 0;
    border-radius: 16rpx;
    box-shadow: 0 14rpx 30rpx rgba(23, 107, 76, 0.22);
    transition: background-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;

    &::after { display: none; }
    &:active { background: #125c40; box-shadow: 0 8rpx 18rpx rgba(23, 107, 76, 0.18); transform: translateY(1px); }
    &[disabled] { color: rgba(255, 255, 255, 0.82); background: #75a794; box-shadow: none; }
  }

  &__arrow { font-size: 34rpx; line-height: 1; }

  &__safe {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10rpx;
    margin-top: 28rpx;
    color: #89938e;
    font-size: 21rpx;
  }

  &__safe-mark { width: 10rpx; height: 10rpx; background: #43a477; border-radius: 50%; }
}

.field {
  margin-bottom: 28rpx;

  &__heading { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; }
  &__label { display: block; margin-bottom: 12rpx; color: #324039; font-size: 24rpx; font-weight: 600; }
  &__hint { margin-bottom: 12rpx; color: #9ba49f; font-size: 20rpx; }

  &__control {
    display: flex;
    align-items: center;
    height: 88rpx;
    padding: 0 22rpx;
    background: #f6f8f7;
    border: 2rpx solid #e5eae7;
    border-radius: 16rpx;
    transition: background-color 180ms ease, border-color 180ms ease, box-shadow 180ms ease;

    &:focus-within { background: #ffffff; border-color: #2d916b; box-shadow: 0 0 0 6rpx rgba(45, 145, 107, 0.1); }
  }

  &__prefix {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 44rpx;
    width: 44rpx;
    height: 44rpx;
    margin-right: 18rpx;
    color: #35785d;
    font-size: 20rpx;
    font-weight: 700;
    background: #e5f2eb;
    border-radius: 12rpx;
  }

  &__input { flex: 1; height: 88rpx; color: #1d2a24; font-size: 28rpx; line-height: 88rpx; }
}

:deep(.field__placeholder) { color: #a2aaa6; }

@media screen and (min-width: 768px) {
  .login {
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 48px;

    &__shell { display: grid; grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr); min-height: 640px; }
    &__footer { margin-top: 28px; font-size: 12px; }
  }

  .hero {
    display: flex;
    flex-direction: column;
    padding: 52px 56px;
    border-radius: 24px 0 0 24px;

    &__content { margin-top: auto; margin-bottom: auto; }
    &__badge { gap: 8px; padding: 7px 12px; font-size: 12px; letter-spacing: 1px; }
    &__badge-dot { width: 6px; height: 6px; box-shadow: 0 0 0 4px rgba(94, 224, 162, 0.12); }
    &__title { margin-top: 20px; font-size: 42px; line-height: 1.25; }
    &__desc { max-width: 420px; margin-top: 20px; font-size: 15px; }
    &__features { gap: 36px; margin-top: 0; padding-top: 24px; }
  }

  .brand {
    gap: 12px;

    &__mark { width: 44px; height: 44px; border-radius: 12px; }
    &__box { width: 16px; height: 15px; border-width: 2px; border-radius: 2px; }
    &__box--back { top: 9px; left: 9px; }
    &__box--front { right: 8px; bottom: 8px; }
    &__line { top: 21px; left: 13px; width: 19px; height: 2px; }
    &__eyebrow { font-size: 10px; letter-spacing: 2px; }
    &__name { font-size: 18px; letter-spacing: 2px; }
  }

  .feature {
    gap: 10px;
    &__check { flex-basis: 22px; width: 22px; height: 22px; font-size: 13px; }
    &__title { font-size: 13px; }
    &__desc { margin-top: 3px; font-size: 11px; }
  }

  .login-card {
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 54px 52px;
    border-top: 1px solid rgba(24, 78, 58, 0.08);
    border-left: 0;
    border-radius: 0 24px 24px 0;

    &__eyebrow { font-size: 12px; letter-spacing: 1px; }
    &__title { margin-top: 8px; font-size: 30px; }
    &__desc { margin-top: 8px; font-size: 14px; }
    &__form { margin-top: 32px; }
    &__error { gap: 8px; padding: 11px 12px; margin: 2px 0 18px; font-size: 13px; border-radius: 8px; }
    &__error-mark { flex-basis: 18px; width: 18px; height: 18px; font-size: 11px; }
    &__submit { gap: 12px; height: 52px; padding: 0 20px; margin-top: 6px; font-size: 15px; line-height: 52px; border-radius: 10px; box-shadow: 0 8px 18px rgba(23, 107, 76, 0.2); }
    &__arrow { font-size: 20px; }
    &__safe { gap: 6px; margin-top: 20px; font-size: 12px; }
    &__safe-mark { width: 6px; height: 6px; }
  }

  .field {
    margin-bottom: 20px;
    &__label { margin-bottom: 8px; font-size: 13px; }
    &__hint { margin-bottom: 8px; font-size: 11px; }
    &__control { height: 50px; padding: 0 13px; border-width: 1px; border-radius: 10px; }
    &__control:focus-within { box-shadow: 0 0 0 3px rgba(45, 145, 107, 0.1); }
    &__prefix { flex-basis: 28px; width: 28px; height: 28px; margin-right: 11px; font-size: 11px; border-radius: 7px; }
    &__input { height: 50px; font-size: 15px; line-height: 50px; }
  }
}

@media (prefers-reduced-motion: reduce) {
  .field__control,
  .login-card__submit { transition: none; }
}
</style>
