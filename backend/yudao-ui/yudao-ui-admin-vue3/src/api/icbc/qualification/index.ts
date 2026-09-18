import request from '@/config/axios'

// 租户三层资质
export interface QualificationVO {
  id?: number
  type?: string // TAX / INDUSTRY / PUBLIC_SECURITY
  name?: string
  issuingAuthority?: string
  certNo?: string
  validFrom?: string
  validTo?: string
  fileUrl?: string
  status?: number // 0-待核实，1-有效，2-失效，3-吊销
  auditRemark?: string
  createTime?: Date
  expiringSoon?: boolean
}

export const QualificationApi = {
  getQualificationPage: async (params: any) =>
    await request.get({ url: `/icbc/qualification/page`, params }),
  getQualification: async (id: number) =>
    await request.get({ url: `/icbc/qualification/get?id=` + id }),
  createQualification: async (data: QualificationVO) =>
    await request.post({ url: `/icbc/qualification/create`, data }),
  updateQualification: async (data: QualificationVO) =>
    await request.put({ url: `/icbc/qualification/update`, data }),
  deleteQualification: async (id: number) =>
    await request.delete({ url: `/icbc/qualification/delete?id=` + id }),
  getExpiring: async (days = 30) =>
    await request.get({ url: `/icbc/qualification/expiring`, params: { days } }),
  isTenantReady: async () => await request.get({ url: `/icbc/qualification/tenant-ready` })
}
