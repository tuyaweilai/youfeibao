export interface StoredUser {
  nickname: string
  roles: string[]
}

// 存储键带 driver_ 前缀：同一台手机上司机端与收货员现场端是两个 H5，互不干扰登录态
const TOKEN_KEY = 'driver_token'
const REFRESH_TOKEN_KEY = 'driver_refresh_token'
const USER_KEY = 'driver_user'

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getRefreshToken(): string {
  return uni.getStorageSync(REFRESH_TOKEN_KEY) || ''
}

export function setRefreshToken(token: string) {
  uni.setStorageSync(REFRESH_TOKEN_KEY, token)
}

export function getStoredUser(): StoredUser | null {
  return uni.getStorageSync(USER_KEY) || null
}

export function setStoredUser(user: StoredUser) {
  uni.setStorageSync(USER_KEY, user)
}

export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(REFRESH_TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}
