import request from '@/config/axios'

// 出售者档案（工行收方）VO
export interface PayeeVO {
  id?: number // 主键
  payeeNo?: string // 收方编号（工行返回）
  partnerPayeeId?: string // 合作方收方编号（我方生成）
  name?: string // 收方姓名
  idCardNo?: string // 身份证号码
  mobile?: string // 手机号码
  bankCardNo?: string // 银行卡号
  bankName?: string // 开户银行
  bankBranch?: string // 开户支行
  address?: string // 地址
  status?: number // 审核状态：0-待审核，1-审核通过，2-审核拒绝
  auditMsg?: string // 审核信息
  businessType?: string // 业务类型
  icbcReceiverStatus?: string // 工行收方状态
  icbcMediumId?: string // 工行返回的账户标识
  icbcOpenacctStatus?: string // 工行侧开户状态
  occupation?: string // 职业
  companyName?: string // 关联企业名称
  createTime?: Date // 创建时间
}

// 出售者档案 API
export const PayeeApi = {
  // 查询出售者档案分页
  getPayeePage: async (params: any) => {
    return await request.get({ url: `/icbc/payee-info/page`, params })
  },

  // 查询出售者档案详情
  getPayee: async (id: number) => {
    return await request.get({ url: `/icbc/payee-info/get?id=` + id })
  },

  // 新增出售者档案
  createPayee: async (data: PayeeVO) => {
    return await request.post({ url: `/icbc/payee-info/create`, data })
  },

  // 修改出售者档案
  updatePayee: async (data: PayeeVO) => {
    return await request.put({ url: `/icbc/payee-info/update`, data })
  },

  // 删除出售者档案
  deletePayee: async (id: number) => {
    return await request.delete({ url: `/icbc/payee-info/delete?id=` + id })
  },

  // 导出出售者档案 Excel
  exportPayee: async (params: any) => {
    return await request.download({ url: `/icbc/payee-info/export-excel`, params })
  }
}
