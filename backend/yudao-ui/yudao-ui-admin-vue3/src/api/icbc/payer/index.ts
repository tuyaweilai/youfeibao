import request from '@/config/axios'

// 付方档案（回收企业 / 工行付方）VO
export interface PayerVO {
  id?: number
  payerNo?: string // 工行付方编号（只读：工行分配，不接受前端在 create/update 里设置，#100）
  partnerPayerId?: string // 合作方付方编号（留空后端生成）
  name?: string // 企业名称
  creditCode?: string // 统一社会信用代码
  taxNo?: string // 纳税人识别号
  bankAccount?: string // 银行账户
  bankName?: string // 开户行名称
  address?: string // 企业地址
  telephone?: string // 企业电话
  contactName?: string // 联系人姓名
  contactMobile?: string // 联系人手机号
  taxpayerType?: string // 01-一般纳税人，02-小规模纳税人
  drawerName?: string // 开票人姓名（工行预下单必输，自动预下单要用）
  drawerCardNumber?: string // 开票人证件号码
  areaCode?: string // 应税行为发生地（省级税务机关代码，如 110000）
  businessType?: string // 业务类型
  status?: number // 0-待审核，1-审核通过，2-审核拒绝
  auditMsg?: string
  icbcPayerStatus?: string
  createTime?: Date
}

// 付方档案 API
export const PayerApi = {
  getPayerPage: async (params: any) => {
    return await request.get({ url: `/icbc/payer-info/page`, params })
  },
  getPayer: async (id: number) => {
    return await request.get({ url: `/icbc/payer-info/get?id=` + id })
  },
  createPayer: async (data: PayerVO) => {
    return await request.post({ url: `/icbc/payer-info/create`, data })
  },
  updatePayer: async (data: PayerVO) => {
    return await request.put({ url: `/icbc/payer-info/update`, data })
  },
  deletePayer: async (id: number) => {
    return await request.delete({ url: `/icbc/payer-info/delete?id=` + id })
  }
}
