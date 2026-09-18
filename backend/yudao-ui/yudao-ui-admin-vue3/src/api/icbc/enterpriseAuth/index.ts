import request from '@/config/axios'

export interface EnterpriseAuthVO {
  id?: number
  outVendorId?: string
  siteType?: string
  userType?: string
  authStatus?: number // 0-未授权，1-已授权，2-已失效
  authTime?: Date
  expireTime?: Date
  remark?: string
  createTime?: Date
}

export interface EnterpriseAuthUpdateVO {
  id: number
  authStatus: number
  authTime?: string
  expireTime?: string
  remark?: string
}

export const EnterpriseAuthApi = {
  // 发起授权，返回工行授权页面表单 HTML
  init: async (data: { outVendorId: string; siteType?: string; userType?: string }) =>
    await request.post({ url: `/icbc/enterprise-auth/init`, data }),
  getEnterpriseAuthPage: async (params: any) =>
    await request.get({ url: `/icbc/enterprise-auth/page`, params }),
  // 人工回填授权结果与授权有效期（工行无查询接口）
  updateResult: async (data: EnterpriseAuthUpdateVO) =>
    await request.put({ url: `/icbc/enterprise-auth/update-result`, data })
}
