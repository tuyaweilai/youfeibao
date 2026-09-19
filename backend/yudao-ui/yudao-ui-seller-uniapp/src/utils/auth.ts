const TOKEN_KEY = 'seller_member_token'
const SUBJECT_KEY = 'seller_subject'

export interface StoredSubject {
  naturalPersonId: number
  name: string
  mobile?: string
  idCardNo?: string
  realNameStatusName?: string
}

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getSubject(): StoredSubject | null {
  return uni.getStorageSync(SUBJECT_KEY) || null
}

export function setSubject(subject: StoredSubject) {
  uni.setStorageSync(SUBJECT_KEY, subject)
}

export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(SUBJECT_KEY)
}
