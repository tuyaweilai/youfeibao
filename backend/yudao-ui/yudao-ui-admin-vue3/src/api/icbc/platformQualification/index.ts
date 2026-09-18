import request from '@/config/axios'

// 平台运营：跨租户资质
export interface PlatformQualificationVO {
  id?: number
  tenantId?: number
  type?: string
  name?: string
  issuingAuthority?: string
  certNo?: string
  validFrom?: string
  validTo?: string
  status?: number
  auditRemark?: string
  createTime?: Date
}

export const PlatformQualificationApi = {
  getPage: async (params: any) =>
    await request.get({ url: `/icbc/platform/qualification/page`, params }),
  // 核实：回写状态与核实意见
  audit: async (id: number, status: number, auditRemark?: string) =>
    await request.put({
      url: `/icbc/platform/qualification/audit`,
      params: { id, status, auditRemark }
    })
}
