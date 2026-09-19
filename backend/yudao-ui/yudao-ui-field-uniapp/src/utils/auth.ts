export interface StoredUser {
  nickname: string
  roles: string[]
}

const TOKEN_KEY = 'field_token'
const REFRESH_TOKEN_KEY = 'field_refresh_token'
const USER_KEY = 'field_user'

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
