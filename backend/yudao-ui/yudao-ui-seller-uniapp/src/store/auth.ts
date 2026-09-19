import { reactive } from 'vue'
import * as sellerApi from '@/api/seller'
import {
  clearAuth,
  getSubject,
  getToken,
  setSubject,
  setToken,
  StoredSubject
} from '@/utils/auth'

interface SellerAuthState {
  token: string
  subject: StoredSubject | null
  signIn: (accessToken: string, target?: StoredSubject | null) => void
  setTokenAndSubject: (accessToken: string, target: StoredSubject) => void
  switchSubject: (target: StoredSubject) => void
  signOut: () => void
}

/**
 * 自然人端登录态：会员令牌 + 当前操作的自然人主体。
 *
 * <p>不用 pinia：本工程未安装 pinia（现场端才有），登录态只是一个单例即可。
 * 一个登录名下可挂多个主体（子女代老人操作），所以当前主体单独存。
 */
const state = reactive<SellerAuthState>({
  token: getToken(),
  subject: getSubject(),
  signIn(accessToken: string, target?: StoredSubject | null) {
    state.token = accessToken
    setToken(accessToken)
    if (target) {
      state.subject = target
      setSubject(target)
    } else {
      state.subject = null
      uni.removeStorageSync('seller_subject')
    }
  },
  setTokenAndSubject(accessToken: string, target: StoredSubject) {
    state.signIn(accessToken, target)
  },
  switchSubject(target: StoredSubject) {
    state.subject = target
    setSubject(target)
  },
  signOut() {
    sellerApi.logout().catch(() => undefined)
    state.token = ''
    state.subject = null
    clearAuth()
  }
})

export function useSellerAuthStore() {
  return state
}
