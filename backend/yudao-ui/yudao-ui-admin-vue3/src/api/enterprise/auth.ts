import request from '@/config/axios'

/**
 * 企业认证状态
 */
export interface EnterpriseAuthStatusVO {
  needAuthGuide: boolean
}

/**
 * 获取当前用户的企业认证状态
 */
export function getUserEnterpriseAuthStatus() {
  return request.get<EnterpriseAuthStatusVO>({ url: '/enterprise/auth/status' })
}