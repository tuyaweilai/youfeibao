const TOKEN_KEY = 'seller_public_token'
const PURPOSE_KEY = 'seller_public_purpose'

export interface EntryParams {
  token: string
  purpose: string
}

/**
 * 从启动参数 / URL 里取一次性令牌与用途。
 * 链接形如 `https://<seller-app>/#/?token=xxx&purpose=QUOTA_QUERY`。
 */
export function resolveEntryParams(): EntryParams {
  let token = ''
  let purpose = ''
  try {
    const launch = uni.getLaunchOptionsSync()
    token = (launch?.query?.token as string) || ''
    purpose = (launch?.query?.purpose as string) || ''
  } catch {
    // 忽略：非启动场景
  }
  // #ifdef H5
  if (!token) {
    const url = new URL(window.location.href)
    token = url.searchParams.get('token') || ''
    purpose = url.searchParams.get('purpose') || ''
    if (!token && url.hash.includes('?')) {
      const query = new URLSearchParams(url.hash.split('?')[1])
      token = query.get('token') || ''
      purpose = query.get('purpose') || ''
    }
  }
  // #endif
  return { token, purpose }
}

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getPurpose(): string {
  return uni.getStorageSync(PURPOSE_KEY) || ''
}

export function setPurpose(purpose: string) {
  uni.setStorageSync(PURPOSE_KEY, purpose)
}
