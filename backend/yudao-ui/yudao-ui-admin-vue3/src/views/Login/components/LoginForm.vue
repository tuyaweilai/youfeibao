<template>
  <el-form
    v-show="getShow"
    ref="formLogin"
    :model="loginData.loginForm"
    :rules="LoginRules"
    class="login-form"
    label-position="top"
    size="large"
    @submit.prevent="getCode"
  >
    <div class="form-heading">
      <LoginFormTitle />
      <p>欢迎回来，请登录您的企业账号</p>
    </div>
    <el-form-item v-if="loginData.tenantEnable === 'true'" label="企业名称" prop="tenantName">
      <el-input
        v-model="loginData.loginForm.tenantName"
        :placeholder="t('login.tenantNamePlaceholder')"
        :prefix-icon="iconHouse"
        autocomplete="organization"
      />
    </el-form-item>
    <el-form-item :label="t('login.username')" prop="username">
      <el-input
        v-model="loginData.loginForm.username"
        :placeholder="t('login.usernamePlaceholder')"
        :prefix-icon="iconAvatar"
        autocomplete="username"
      />
    </el-form-item>
    <el-form-item :label="t('login.password')" prop="password">
      <el-input
        v-model="loginData.loginForm.password"
        :placeholder="t('login.passwordPlaceholder')"
        :prefix-icon="iconLock"
        show-password
        type="password"
        autocomplete="current-password"
      />
    </el-form-item>
    <div class="form-options">
      <el-checkbox v-model="loginData.loginForm.rememberMe">{{ t('login.remember') }}</el-checkbox>
      <el-button
        link
        type="primary"
        class="forgot-password"
        @click="setLoginState(LoginStateEnum.RESET_PASSWORD)"
        >{{ t('login.forgetPassword') }}</el-button
      >
    </div>
    <el-button
      :loading="loginLoading"
      :disabled="loginLoading"
      class="login-submit"
      type="primary"
      native-type="submit"
      >{{ t('login.login') }}</el-button
    >
    <Verify
      v-if="loginData.captchaEnable === 'true'"
      ref="verify"
      :captchaType="captchaType"
      :imgSize="{ width: '400px', height: '200px' }"
      mode="pop"
      @success="handleLogin"
    />
  </el-form>
</template>
<script lang="ts" setup>
import { ElLoading } from 'element-plus'
import LoginFormTitle from './LoginFormTitle.vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

import { useIcon } from '@/hooks/web/useIcon'

import * as authUtil from '@/utils/auth'
import { usePermissionStore } from '@/store/modules/permission'
import * as LoginApi from '@/api/login'
import { LoginStateEnum, useFormValid, useLoginState } from './useLogin'

defineOptions({ name: 'LoginForm' })

const { t } = useI18n()
const iconHouse = useIcon({ icon: 'ep:house' })
const iconAvatar = useIcon({ icon: 'ep:avatar' })
const iconLock = useIcon({ icon: 'ep:lock' })
const formLogin = ref()
const { validForm } = useFormValid(formLogin)
const { setLoginState, getLoginState } = useLoginState()
const { currentRoute, push } = useRouter()
const permissionStore = usePermissionStore()
const redirect = ref<string>('')
const loginLoading = ref(false)
const verify = ref()
const captchaType = ref('blockPuzzle') // blockPuzzle 滑块 clickWord 点击文字

const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN)

const LoginRules = {
  tenantName: [{ required: true, message: t('login.tenantNamePlaceholder'), trigger: 'blur' }],
  username: [{ required: true, message: t('login.usernamePlaceholder'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordPlaceholder'), trigger: 'blur' }]
}
const loginData = reactive({
  isShowPassword: false,
  captchaEnable: import.meta.env.VITE_APP_CAPTCHA_ENABLE,
  tenantEnable: import.meta.env.VITE_APP_TENANT_ENABLE,
  loginForm: {
    tenantName:
      import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '有废宝', // 默认企业名；本租户就一个企业
    username: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
    password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
    captchaVerification: '',
    rememberMe: true // 默认记录我。如果不需要，可手动修改
  }
})

// 获取验证码
const getCode = async () => {
  if (loginLoading.value) return
  // 情况一，未开启：则直接登录
  if (loginData.captchaEnable === 'false') {
    await handleLogin({})
  } else {
    // 情况二，已开启：则展示验证码；只有完成验证码的情况，才进行登录
    // 弹出验证码
    verify.value.show()
  }
}
// 获取租户 ID
const getTenantId = async () => {
  if (loginData.tenantEnable === 'true') {
    const res = await LoginApi.getTenantIdByName(loginData.loginForm.tenantName)
    authUtil.setTenantId(res)
  }
}
// 记住我
const getLoginFormCache = () => {
  const loginForm = authUtil.getLoginForm()
  if (loginForm) {
    loginData.loginForm = {
      ...loginData.loginForm,
      username: loginForm.username ? loginForm.username : loginData.loginForm.username,
      password: loginForm.password ? loginForm.password : loginData.loginForm.password,
      rememberMe: loginForm.rememberMe,
      tenantName: loginForm.tenantName ? loginForm.tenantName : loginData.loginForm.tenantName
    }
  }
}
// 根据域名，获得租户信息
const getTenantByWebsite = async () => {
  const website = location.host
  const res = await LoginApi.getTenantByWebsite(website)
  if (res) {
    loginData.loginForm.tenantName = res.name
    authUtil.setTenantId(res.id)
  }
}
const loading = ref() // ElLoading.service 返回的实例
// 登录
const handleLogin = async (params: any) => {
  loginLoading.value = true
  try {
    const data = await validForm().catch(() => false)
    if (!data) {
      return
    }
    await getTenantId()
    const loginDataLoginForm = { ...loginData.loginForm }
    loginDataLoginForm.captchaVerification = params.captchaVerification
    const res = await LoginApi.login(loginDataLoginForm)
    if (!res) {
      return
    }
    loading.value = ElLoading.service({
      lock: true,
      text: '正在加载系统中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    if (loginDataLoginForm.rememberMe) {
      authUtil.setLoginForm(loginDataLoginForm)
    } else {
      authUtil.removeLoginForm()
    }
    authUtil.setToken(res)
    if (!redirect.value) {
      redirect.value = '/'
    }
    // 判断是否为SSO登录
    if (redirect.value.indexOf('sso') !== -1) {
      window.location.href = window.location.href.replace('/login?redirect=', '')
    } else {
      await push({ path: redirect.value || permissionStore.addRouters[0].path })
    }
  } finally {
    loginLoading.value = false
    loading.value?.close()
  }
}

watch(
  () => currentRoute.value,
  (route: RouteLocationNormalizedLoaded) => {
    redirect.value = route?.query?.redirect as string
  },
  {
    immediate: true
  }
)
onMounted(() => {
  getLoginFormCache()
  getTenantByWebsite()
})
</script>

<style lang="scss" scoped>
.form-heading {
  margin-bottom: 28px;
}
.form-heading p {
  margin: 10px 0 0;
  color: var(--login-muted, var(--el-text-color-secondary));
  font-size: 13px;
  line-height: 1.7;
}
.form-options {
  display: flex;
  margin: -8px 0 18px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.form-options .forgot-password {
  min-height: 44px;
  padding: 0;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0;
}
.login-submit {
  width: 100%;
}
</style>
