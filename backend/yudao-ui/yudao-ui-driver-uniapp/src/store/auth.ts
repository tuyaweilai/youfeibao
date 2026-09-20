import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import {
  clearAuth,
  getStoredUser,
  getToken,
  setRefreshToken,
  setStoredUser,
  setToken
} from '@/utils/auth'

/** 司机端的登录态：token + 昵称 + 角色，持久化到本地存储 */
export const useAuthStore = defineStore('auth', () => {
  const stored = getStoredUser()
  const token = ref(getToken())
  const nickname = ref(stored?.nickname || '')
  const roles = ref<string[]>(stored?.roles || [])

  async function login(username: string, password: string) {
    const resp = await authApi.login({ username, password })
    token.value = resp.accessToken
    setToken(resp.accessToken)
    setRefreshToken(resp.refreshToken)
    const info = await authApi.getPermissionInfo()
    nickname.value = info.user?.nickname || username
    roles.value = info.roles || []
    setStoredUser({ nickname: nickname.value, roles: roles.value })
    return info
  }

  function logout() {
    // 尽力通知后端；网络失败也照样清本地，避免卡在登录态
    authApi.logout().catch(() => undefined)
    token.value = ''
    nickname.value = ''
    roles.value = []
    clearAuth()
  }

  function isLoggedIn() {
    return !!token.value
  }

  return { token, nickname, roles, login, logout, isLoggedIn }
})
