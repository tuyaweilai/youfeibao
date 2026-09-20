<template>
  <view class="login">
    <view class="login__brand">司机端</view>
    <view class="login__sub">回收企业 · 运输任务</view>

    <view class="login__form">
      <view class="field">
        <text class="field__label">租户编号</text>
        <input v-model="form.tenantId" class="field__input" placeholder="如 1" />
      </view>
      <view class="field">
        <text class="field__label">司机账号</text>
        <input v-model="form.username" class="field__input" placeholder="账号由企业管理员分配" />
      </view>
      <view class="field">
        <text class="field__label">密码</text>
        <input v-model="form.password" class="field__input" password placeholder="密码" />
      </view>

      <button class="login__submit" :loading="loading" @click="onSubmit">登录</button>
      <view v-if="error" class="login__error">{{ error }}</view>
      <view class="login__tip">
        账号是回收企业给你建的司机档案所关联的系统用户；还没建档请找管理员。
      </view>
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
const form = reactive({
  tenantId: '1',
  username: '',
  password: ''
})

async function onSubmit() {
  error.value = ''
  if (!form.tenantId || !form.username || !form.password) {
    error.value = '租户编号、账号与密码都要填'
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
.login {
  padding: 120rpx 48rpx 48rpx;

  &__brand {
    font-size: 56rpx;
    font-weight: 700;
  }

  &__sub {
    margin-top: 12rpx;
    color: #6b7a72;
    font-size: 26rpx;
  }

  &__form {
    margin-top: 80rpx;
  }

  &__submit {
    margin-top: 48rpx;
    background-color: #16a34a;
    color: #fff;
    border-radius: 12rpx;
  }

  &__error {
    margin-top: 24rpx;
    color: #dc2626;
    font-size: 26rpx;
  }

  &__tip {
    margin-top: 24rpx;
    color: #8a919f;
    font-size: 24rpx;
    line-height: 1.6;
  }
}

.field {
  margin-bottom: 32rpx;

  &__label {
    display: block;
    margin-bottom: 12rpx;
    color: #4b5563;
    font-size: 26rpx;
  }

  &__input {
    height: 80rpx;
    padding: 0 24rpx;
    background-color: #fff;
    border: 1rpx solid #e5e7eb;
    border-radius: 12rpx;
  }
}
</style>
