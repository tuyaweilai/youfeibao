import { API_BASE_URL, APP_API_BASE_URL, getTenantId } from '@/config/env'
import { clearAuth, getToken } from '@/utils/auth'

/** yudao 统一响应体 */
interface CommonResult<T> {
  code: number
  data: T
  msg: string
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

interface RequestOptions {
  baseUrl: string
  url: string
  method?: Method
  data?: Record<string, any>
  header?: Record<string, string>
  /** 是否携带登录令牌，默认携带（仅自然人端通道需要） */
  auth?: boolean
}

function doRequest<T>(options: RequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const header: Record<string, string> = { ...(options.header || {}) }
    if (options.auth !== false) {
      const token = getToken()
      if (token) {
        header.Authorization = `Bearer ${token}`
      }
    }
    uni.request({
      url: options.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        const body = res.data as CommonResult<T>
        if (res.statusCode === 401 || body?.code === 401) {
          clearAuth()
          uni.reLaunch({ url: '/pages/login/index' })
          reject(new Error('登录已过期，请重新登录'))
          return
        }
        if (res.statusCode !== 200 || !body) {
          reject(new Error(`请求失败（HTTP ${res.statusCode}）`))
          return
        }
        if (body.code !== 0) {
          reject(new Error(body.msg || '请求失败'))
          return
        }
        resolve(body.data)
      },
      fail: (err) => reject(new Error(err.errMsg || '网络异常，请稍后重试'))
    })
  })
}

/**
 * 公开端点请求：不带令牌头、不带 tenant-id（端点在 yudao 白名单里），
 * 只把一次性令牌作为查询参数传给后端。
 */
export function request<T = any>(options: {
  url: string
  method?: 'GET' | 'POST'
  data?: Record<string, any>
}): Promise<T> {
  return doRequest<T>({ baseUrl: API_BASE_URL, url: options.url, method: options.method, data: options.data, auth: false })
}

export const get = <T = any>(url: string, data?: Record<string, any>) =>
  request<T>({ url, method: 'GET', data })

export const post = <T = any>(url: string, data?: Record<string, any>) =>
  request<T>({ url, method: 'POST', data })

/**
 * 自然人端登录态请求：走 `/app-api`，带 `tenant-id`（场站所属回收企业）与会员令牌，401 清登录态。
 */
export function appRequest<T = any>(options: {
  url: string
  method?: Method
  data?: Record<string, any>
  auth?: boolean
}): Promise<T> {
  return doRequest<T>({
    baseUrl: APP_API_BASE_URL,
    url: options.url,
    method: options.method,
    data: options.data,
    auth: options.auth,
    header: getTenantId() ? { 'tenant-id': getTenantId() } : {}
  })
}

export const appGet = <T = any>(url: string, data?: Record<string, any>, auth = true) =>
  appRequest<T>({ url, method: 'GET', data, auth })

export const appPost = <T = any>(url: string, data?: Record<string, any>, auth = true) =>
  appRequest<T>({ url, method: 'POST', data, auth })
