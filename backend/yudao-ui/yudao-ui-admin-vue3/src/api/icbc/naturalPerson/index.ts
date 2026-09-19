import request from '@/config/axios'

// 平台级自然人主体（跨租户身份档案，见 docs/adr/0017）
export interface NaturalPersonVO {
  id?: number
  outUserId?: string
  name?: string
  idCardNo?: string
  mobile?: string
  realNameStatus?: number // 0-未认证，1-认证中，2-认证通过，3-认证未通过
  realNameStatusName?: string
  realNameTime?: Date
  status?: number // 0-正常，1-已停用
  remark?: string
  memberUserIds?: number[]
  createTime?: Date
}

export const NaturalPersonApi = {
  getNaturalPersonPage: async (params: any) =>
    await request.get({ url: `/icbc/platform/natural-person/page`, params }),
  getNaturalPerson: async (id: number) =>
    await request.get({ url: `/icbc/platform/natural-person/get?id=` + id }),
  // 身份冲突清单（跨租户，只读）：同一身份证在不同租户姓名 / 手机号不一致者
  getIdentityConflicts: async () =>
    await request.get({ url: `/icbc/platform/natural-person/conflicts` }),
  // 身份认领：核实后把某个登录凭证绑到已有自然人主体上
  claim: async (data: { naturalPersonId: number; memberUserId: number; remark?: string }) =>
    await request.post({ url: `/icbc/platform/natural-person/claim`, data }),
  // 解绑登录凭证：只解绑凭证，主体与交易记录保留
  unbind: async (data: { naturalPersonId: number; memberUserId: number }) =>
    await request.post({ url: `/icbc/platform/natural-person/unbind`, data }),
  updateStatus: async (id: number, status: number, remark?: string) =>
    await request.put({
      url: `/icbc/platform/natural-person/update-status`,
      params: { id, status, remark }
    })
}

// 身份冲突清单：同一身份证跨租户姓名 / 手机号不一致，命中 ADR 0017「不自动合并」规则
export interface NaturalPersonConflictRecordVO {
  payeeId?: number
  tenantId?: number
  name?: string
  mobile?: string // 脱敏
  payeeNo?: string
  partnerPayeeId?: string
  createTime?: Date
}

export interface NaturalPersonConflictVO {
  idCardNo?: string // 脱敏
  naturalPersonId?: number
  outUserId?: string
  status?: number
  recordCount?: number
  records?: NaturalPersonConflictRecordVO[]
}
