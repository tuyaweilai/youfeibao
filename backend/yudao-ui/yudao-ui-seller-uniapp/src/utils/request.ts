import { API_BASE_URL } from '@/config/env'

/** yudao 统一响应体 */
interface CommonResult<T> {
  code: number
  data: T
  msg: string
}

/**
 * 公开端点请求：不带 token 头、不带 tenant-id（端点在 yudao.tenant.ignore-urls 里），
 * 只把一次性令牌作为查询参数传给后端。
 */
export function request<T = any>(options: {
  url: string
  method?: 'GET' | 'POST'
  data?: Record<string, any>
}): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: API_BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      success: (res) => {
        const body = res.data as CommonResult<T>
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

export const get = <T = any>(url: string, data?: Record<string, any>) =>
  request<T>({ url, method: 'GET', data })

export const post = <T = any>(url: string, data?: Record<string, any>) =>
  request<T>({ url, method: 'POST', data })
