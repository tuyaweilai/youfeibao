<template>
  <view class="login">
    <view class="login__shell">
      <view class="hero">
        <view class="brand" aria-label="反向发票合规平台回收企业工作台">
          <view class="brand__mark" aria-hidden="true">
            <view class="brand__box brand__box--back" />
            <view class="brand__box brand__box--front" />
            <view class="brand__line" />
          </view>
          <view>
            <view class="brand__eyebrow">INVOICE COMPLIANCE</view>
            <view class="brand__name">反向发票合规平台</view>
          </view>
        </view>

        <view class="hero__title">回收企业工作台</view>
        <view class="hero__desc">登录账号，开始今日收货工作</view>
      </view>

      <view class="login-card">
        <view class="login-card__form">
          <view class="field">
            <view class="field__heading">
              <text class="field__label">租户编号</text>
              <text class="field__hint">由企业管理员提供</text>
            </view>
            <view class="field__control">
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

    <view class="login__footer">反向发票合规平台 · 让每一笔收购更清楚</view>
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
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  min-height: 100dvh;
  margin: 0 auto;
  padding: calc(40px + env(safe-area-inset-top)) 24px calc(24px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, #edf7f1 0, #ffffff 260px);
  &__shell { width: 100%; }
  &__footer {
    margin-top: auto;
    padding-top: 40px;
    color: #66736c;
    font-size: 12px;
    text-align: center;
  }
}
.hero {
  margin-bottom: 36px;
  &__title { margin-top: 32px; color: #17221d; font-size: 30px; font-weight: 700; line-height: 1.4; }
  &__desc { margin-top: 8px; color: #66736c; font-size: 15px; line-height: 1.6; }
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  &__mark { position: relative; width: 44px; height: 44px; background: #176b4c; border-radius: 13px; }
  &__box { position: absolute; width: 15px; height: 14px; border: 2px solid #ffffff; border-radius: 3px; }
  &__box--back { top: 9px; left: 9px; }
  &__box--front { right: 8px; bottom: 8px; background: #176b4c; }
  &__line { position: absolute; top: 21px; left: 13px; width: 19px; height: 2px; background: #9be5bc; transform: rotate(-35deg); }
  &__eyebrow { color: #43745c; font-size: 10px; font-weight: 600; letter-spacing: 1.5px; }
  &__name { margin-top: 3px; color: #174e36; font-size: 19px; font-weight: 700; letter-spacing: 2px; }
}
.field {
  margin-bottom: 24px;
  &__heading { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
  &__label { display: block; margin-bottom: 10px; color: #324039; font-size: 15px; font-weight: 600; }
  &__hint { color: #66736c; font-size: 12px; }
  &__control {
    display: flex;
    align-items: center;
    min-height: 54px;
    padding: 0 16px;
    background: #f6f8f7;
    border: 1px solid #dce5df;
    border-radius: 12px;
    transition: border-color 180ms ease, box-shadow 180ms ease;
    &:focus-within { background: #ffffff; border-color: #287954; box-shadow: 0 0 0 3px rgba(40, 121, 84, 0.1); }
  }
  &__input { flex: 1; min-width: 0; height: 54px; color: #17221d; font-size: 16px; }
}
:deep(.field__placeholder) { color: #758078; }
.login-card {
  &__error { display: flex; align-items: center; gap: 8px; margin-bottom: 20px; padding: 12px; color: #a02b2b; background: #fff3f2; border-radius: 10px; font-size: 14px; line-height: 1.5; }
  &__error-mark { font-weight: 700; }
  &__submit {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    width: 100%;
    min-height: 54px;
    margin-top: 8px;
    color: #ffffff;
    background: #176b4c;
    border-radius: 12px;
    font-size: 17px;
    font-weight: 600;
    line-height: 54px;
    cursor: pointer;
    &::after { display: none; }
    &:active { background: #125c40; }
    &:focus-visible { outline: 3px solid #74ba97; outline-offset: 3px; }
    &[disabled] { color: #ffffff; background: #597c6b; }
  }
  &__arrow { font-size: 22px; }
  &__safe { display: flex; align-items: center; justify-content: center; gap: 7px; margin-top: 20px; color: #66736c; font-size: 12px; line-height: 1.5; }
  &__safe-mark { width: 5px; height: 5px; background: #287954; border-radius: 50%; }
}
@media (prefers-reduced-motion: reduce) {
  .field__control { transition: none; }
}
</style>
