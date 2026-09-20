import request from '@/config/axios'

// 卖方主体类型（六态）。判定规则是「是否属于自然人」：自然人出售者走反向开票，其余走单位供货方的进项收票。
export const SELLER_SUBJECT_TYPE_ENUM = {
  NATURAL: 1,
  INDIVIDUAL_BUSINESS: 2,
  SOLE_PROPRIETORSHIP: 3,
  PARTNERSHIP: 4,
  ENTERPRISE_LEGAL: 5,
  FARMER_COOPERATIVE: 6
} as const

// 单位供货方档案只收「自然人以外」的五类；自然人出售者由「出售者档案」维护。
export const SUPPLIER_SUBJECT_TYPE_OPTIONS = [
  { value: SELLER_SUBJECT_TYPE_ENUM.INDIVIDUAL_BUSINESS, label: '个体工商户' },
  { value: SELLER_SUBJECT_TYPE_ENUM.SOLE_PROPRIETORSHIP, label: '个人独资企业' },
  { value: SELLER_SUBJECT_TYPE_ENUM.PARTNERSHIP, label: '合伙企业' },
  { value: SELLER_SUBJECT_TYPE_ENUM.ENTERPRISE_LEGAL, label: '企业法人' },
  { value: SELLER_SUBJECT_TYPE_ENUM.FARMER_COOPERATIVE, label: '农民专业合作社' }
]

// 纳税人资格
export const TAXPAYER_QUALIFICATION_OPTIONS = [
  { value: 1, label: '一般纳税人' },
  { value: 2, label: '小规模纳税人' }
]

// ERP 供应商 VO
export interface SupplierVO {
  id: number // 供应商编号
  name: string // 供应商名称
  subjectType: number // 主体类型
  subjectTypeName?: string // 主体类型名
  taxpayerQualification: number // 纳税人资格
  taxpayerQualificationName?: string // 纳税人资格名
  address: string // 地址
  contact: string // 联系人
  mobile: string // 手机号码
  telephone: string // 联系电话
  email: string // 电子邮箱
  fax: string // 传真
  remark: string // 备注
  status: number // 开启状态
  sort: number // 排序
  taxNo: string // 纳税人识别号
  taxPercent: number // 税率
  bankName: string // 开户行
  bankAccount: string // 开户账号
  bankAddress: string // 开户地址
}

// ERP 供应商 API
export const SupplierApi = {
  // 查询供应商分页
  getSupplierPage: async (params: any) => {
    return await request.get({ url: `/erp/supplier/page`, params })
  },

  // 获得供应商精简列表
  getSupplierSimpleList: async () => {
    return await request.get({ url: `/erp/supplier/simple-list` })
  },

  // 查询供应商详情
  getSupplier: async (id: number) => {
    return await request.get({ url: `/erp/supplier/get?id=` + id })
  },

  // 新增供应商
  createSupplier: async (data: SupplierVO) => {
    return await request.post({ url: `/erp/supplier/create`, data })
  },

  // 修改供应商
  updateSupplier: async (data: SupplierVO) => {
    return await request.put({ url: `/erp/supplier/update`, data })
  },

  // 删除供应商
  deleteSupplier: async (id: number) => {
    return await request.delete({ url: `/erp/supplier/delete?id=` + id })
  },

  // 导出供应商 Excel
  exportSupplier: async (params) => {
    return await request.download({ url: `/erp/supplier/export-excel`, params })
  }
}
