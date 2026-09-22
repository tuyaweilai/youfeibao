<template>
  <view class="login">
    <view class="login__hero">
      <view class="brand"><view class="brand__symbol">优</view><text>某某环保公司</text><text class="brand__tag">司机端</text></view>
      <view class="hero-copy"><view class="eyebrow">每一程，都有着落</view><view class="hero-title">安心出发，<br />顺利交接。</view><view class="hero-desc">运输任务随时看，现场交接轻松办。</view></view>
      <image class="hero-art" src="/static/driver-route.svg" mode="aspectFit" aria-label="货车沿路线驶向回收场站的插画" />
      <view class="hero-steps"><text>接收任务</text><text class="step-line" /><text>现场交接</text><text class="step-line" /><text>到站上报</text></view>
    </view>

    <view class="login__body">
      <view class="login__form">
        <view class="form-heading"><view class="form-title">司机登录</view><view class="form-sub">登录企业账号，开启今天的运输任务</view></view>
        <view class="field">
          <label for="driver-tenant" class="field__label">企业编号 <text class="field__hint">租户编号</text></label>
          <input id="driver-tenant" v-model="form.tenantId" class="field__input" aria-label="企业编号" placeholder="请输入企业编号" />
        </view>
        <view class="field">
          <label for="driver-account" class="field__label">司机账号</label>
          <input id="driver-account" v-model="form.username" class="field__input" aria-label="司机账号" placeholder="请输入管理员分配的账号" />
        </view>
        <view class="field">
          <label for="driver-password" class="field__label">登录密码</label>
          <view class="password-field">
            <input id="driver-password" v-model="form.password" class="field__input" aria-label="登录密码" :password="!showPassword" placeholder="请输入密码" confirm-type="go" @confirm="onSubmit" />
            <button class="password-toggle" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button>
          </view>
        </view>
        <view v-if="error" class="login__error" role="alert">{{ error }}</view>
        <button class="login__submit" :loading="loading" :disabled="loading" @click="onSubmit">{{ loading ? '正在登录' : '登录并查看任务' }}<text v-if="!loading" class="submit-arrow">→</text></button>
        <view class="login__tip"><view class="tip-icon">i</view><view><view class="tip-title">还没有账号或忘记密码？</view><view>请联系企业管理员，为你开通司机账号或重置密码。</view></view></view>
      </view>
      <view class="login__footer">优废宝 · 让回收运输更有序</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { setTenantId } from '@/config/env'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const form = reactive({
  tenantId: '1',
  username: '',
  password: ''
})

async function onSubmit() {
  if (loading.value) return
  error.value = ''
  if (!form.tenantId || !form.username || !form.password) {
    error.value = '请填写企业编号、司机账号和密码'
    return
  }
  loading.value = true
  try {
    setTenantId(form.tenantId)
    await auth.login(form.username, form.password)
    // 登录成功先确认身份：不是司机就当场说清，别让他进去看一堆空列表
    try {
      const { getProfile } = await import('@/api/task')
      await getProfile()
    } catch (e) {
      error.value = (e as Error).message
      auth.logout()
      return
    }
    uni.reLaunch({ url: '/pages/task/list' })
  } catch (e) {
    error.value = (e as Error).message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login { width: 100%; max-width: 480px; min-height: 100vh; margin: 0 auto; background: #f4f7f7; }
.login__hero { position: relative; overflow: hidden; padding: calc(28px + env(safe-area-inset-top)) 28px 42px; background: #103f43; color: #fff; }
.brand { display: flex; align-items: center; gap: 10px; font-size: 21px; font-weight: 650; }
.brand__symbol { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: 10px; background: #d2eee5; color: #103f43; font-size: 21px; }
.brand__tag { margin-left: 2px; padding: 3px 9px; border: 1px solid #507477; border-radius: 5px; font-size: 11px; font-weight: 400; color: #d8e9e8; }
.hero-copy { position: relative; z-index: 1; margin-top: 32px; }
.eyebrow { color: #b5d8d4; font-size: 12px; letter-spacing: 3px; }
.hero-title { margin-top: 10px; font-size: 34px; font-weight: 650; line-height: 1.35; letter-spacing: 1px; }
.hero-desc { margin-top: 13px; font-size: 13px; color: #d0e2e0; }
.hero-art { position: absolute; right: -20px; top: 68px; width: 210px; height: 180px; opacity: .7; }
.hero-steps { display: flex; align-items: center; gap: 12px; margin-top: 28px; font-size: 12px; color: #d0e2e0; }
.step-line { width: 24px; height: 1px; background: #648b89; }
.login__body { position: relative; padding: 0 18px calc(24px + env(safe-area-inset-bottom)); }
.login__form { position: relative; margin: -20px auto 0; padding: 27px 24px 24px; max-width: 420px; box-sizing: border-box; border: 1px solid #e2eaea; border-radius: 22px; background: #fff; box-shadow: 0 10px 32px #183f4310; }
.form-heading { margin-bottom: 25px; }
.form-title { font-size: 24px; font-weight: 650; color: #18383c; }
.form-sub { margin-top: 7px; color: #657579; font-size: 13px; line-height: 1.6; }
.field { margin-bottom: 19px; }
.field__label { display: block; margin-bottom: 9px; font-size: 14px; font-weight: 550; color: #304c50; }
.field__hint { margin-left: 6px; font-size: 11px; font-weight: 400; color: #6a7b7e; }
.field__input { box-sizing: border-box; width: 100%; height: 50px; padding: 0 14px; border: 1px solid #dce5e5; border-radius: 10px; background: #f7f9f9; color: #18383c; font-size: 16px; }
.field__input:focus-within { border-color: #0f766e; box-shadow: 0 0 0 3px #0f766e15; }
.password-field { position: relative; }
.password-field .field__input { padding-right: 65px; }
.password-toggle { position: absolute; right: 3px; top: 3px; min-width: 58px; height: 44px; margin: 0; padding: 0 10px; background: transparent; color: #376867; font-size: 13px; line-height: 44px; }
.password-toggle::after { border: 0; }
.login__submit { display: flex; align-items: center; justify-content: center; gap: 18px; width: 100%; height: 52px; margin-top: 25px; border-radius: 11px; background: #0f766e; color: #fff; font-size: 16px; font-weight: 600; line-height: 52px; box-shadow: 0 5px 12px #0f766e20; }
.login__submit[disabled] { background: #507a77; color: #fff; }
.submit-arrow { font-size: 22px; font-weight: 400; }
.login__error { padding: 10px 12px; border-radius: 8px; background: #fff1f0; color: #b42318; font-size: 13px; line-height: 1.5; }
.login__tip { display: flex; gap: 10px; margin-top: 25px; padding-top: 20px; border-top: 1px solid #edf1f1; font-size: 12px; line-height: 1.8; color: #687b7e; }
.tip-icon { flex-shrink: 0; width: 16px; height: 16px; margin-top: 3px; border: 1px solid #718689; border-radius: 50%; text-align: center; font-size: 11px; line-height: 16px; }
.tip-title { margin-bottom: 3px; color: #39595c; font-weight: 550; }
.login__footer { margin-top: 23px; text-align: center; font-size: 11px; letter-spacing: 1px; color: #6c8083; }
@media (max-width: 374px) { .login__hero { padding-left: 22px; } .hero-art { right: -65px; opacity: .4; } .login__form { padding: 24px 18px; } }
</style>
