import { API_BASE_URL, getTenantId } from '@/config/env'
import { clearAuth, getToken } from '@/utils/auth'

/** yudao 统一响应体 */
interface CommonResult<T> {
  code: number
  data: T
  msg: string
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface RequestOptions {
  url: string
  method?: Method
  data?: Record<string, any>
  header?: Record<string, string>
  /** 是否携带 token，默认携带；登录等公开接口传 false */
  auth?: boolean
}

/**
 * 统一请求封装：自动带 tenant-id 与 token；401 清登录态并回登录页。
 * 只用 uni.request，不引额外 HTTP 库，保证 H5 / 小程序 / App 一套代码。
 */
export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const header: Record<string, string> = {
      'tenant-id': getTenantId(),
      ...(options.header || {})
    }
    if (options.auth !== false) {
      const token = getToken()
      if (token) {
        header.Authorization = `Bearer ${token}`
      }
    }
    uni.request({
      url: API_BASE_URL + options.url,
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

export const get = <T = any>(url: string, data?: Record<string, any>, auth = true) =>
  request<T>({ url, method: 'GET', data, auth })

export const post = <T = any>(url: string, data?: Record<string, any>, auth = true) =>
  request<T>({ url, method: 'POST', data, auth })
