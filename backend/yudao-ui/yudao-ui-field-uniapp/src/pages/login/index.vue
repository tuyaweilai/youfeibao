<template>
  <view class="login">
    <view class="login__brand">收货现场</view>
    <view class="login__sub">回收企业 · 收货员现场端</view>

    <view class="login__form">
      <view class="field">
        <text class="field__label">租户编号</text>
        <input v-model="form.tenantId" class="field__input" placeholder="如 1" />
      </view>
      <view class="field">
        <text class="field__label">账号</text>
        <input v-model="form.username" class="field__input" placeholder="收货员账号" />
      </view>
      <view class="field">
        <text class="field__label">密码</text>
        <input v-model="form.password" class="field__input" password placeholder="密码" />
      </view>

      <button class="login__submit" :loading="loading" @click="onSubmit">登录</button>
      <view v-if="error" class="login__error">{{ error }}</view>
    </view>
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
  padding: 120rpx 48rpx 0;

  &__brand {
    font-size: 52rpx;
    font-weight: 700;
    text-align: center;
  }

  &__sub {
    margin-top: 16rpx;
    color: $field-text-secondary;
    text-align: center;
  }

  &__form {
    margin-top: 80rpx;
    padding: 40rpx 32rpx;
    background-color: #ffffff;
    border-radius: 16rpx;
  }

  &__submit {
    margin-top: 40rpx;
    color: #ffffff;
    background-color: $field-primary;
  }

  &__error {
    margin-top: 24rpx;
    color: #d03050;
    font-size: 26rpx;
  }
}

.field {
  margin-bottom: 32rpx;

  &__label {
    display: block;
    margin-bottom: 12rpx;
    color: $field-text-secondary;
    font-size: 26rpx;
  }

  &__input {
    height: 80rpx;
    padding: 0 20rpx;
    background-color: #f5f6f8;
    border-radius: 12rpx;
  }
}
</style>
