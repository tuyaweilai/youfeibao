import request from '@/config/axios'

/**
 * 进项收票登记与勾稽（#49 T11，ADR 0029）。
 *
 * 单位供货方（企业 / 个体工商户 / 个人独资企业 / 合伙企业 / 农民专业合作社）向回收企业开具
 * 增值税发票后，由财务登记票面事实，再勾稽到采购单据，让票、货、款三者对得上。
 * 自然人出售者不在本链路里（他们走反向开票）。
 */

// 票种：与后端 InputInvoiceTypeEnum 一一对应
export const INPUT_INVOICE_TYPE_ENUM = {
  SPECIAL: 1,
  GENERAL: 2
} as const

export const INPUT_INVOICE_TYPE_OPTIONS = [
  { value: INPUT_INVOICE_TYPE_ENUM.SPECIAL, label: '增值税专用发票' },
  { value: INPUT_INVOICE_TYPE_ENUM.GENERAL, label: '增值税普通发票' }
]

// 状态：与后端 InputInvoiceStatusEnum 一一对应
export const INPUT_INVOICE_STATUS_ENUM = {
  REGISTERED: 0,
  PARTIALLY_LINKED: 1,
  LINKED: 2
} as const

export const INPUT_INVOICE_STATUS_OPTIONS = [
  { value: INPUT_INVOICE_STATUS_ENUM.REGISTERED, label: '已登记' },
  { value: INPUT_INVOICE_STATUS_ENUM.PARTIALLY_LINKED, label: '部分勾稽' },
  { value: INPUT_INVOICE_STATUS_ENUM.LINKED, label: '已勾稽' }
]

// 可勾稽的单据类型：与后端 InputInvoiceBizTypeEnum 一一对应。
// PURCHASE_ORDER 由 #46（T08）落地、STOCK_IN 由 #52（T14）落地；编码先在这里列全，
// 单据号与单据金额由使用方传入（后端不反向依赖它们的实现）。
export const INPUT_INVOICE_BIZ_TYPE_OPTIONS = [
  { value: 'ACQUISITION', label: '收购单' },
  { value: 'PURCHASE_ORDER', label: '采购订单' },
  { value: 'STOCK_IN', label: '入库单' }
]

export interface InputInvoiceLinkVO {
  id?: number
  invoiceId?: number
  bizType?: string
  bizTypeName?: string
  bizId?: number
  bizNo?: string
  bizAmount?: number
  linkedAmount?: number
  remark?: string
  createTime?: Date
}

export interface InputInvoiceVO {
  id?: number
  invoiceNo?: string
  invoiceCode?: string
  invoiceType?: number
  invoiceTypeName?: string
  invoiceDate?: string
  sellerName?: string
  sellerTaxNo?: string
  amount?: number
  taxAmount?: number
  totalAmount?: number
  linkedAmount?: number
  remainingAmount?: number
  status?: number
  statusName?: string
  remark?: string
  links?: InputInvoiceLinkVO[]
  createTime?: Date
  updateTime?: Date
}

export const InputInvoiceApi = {
  getInputInvoicePage: async (params: any) =>
    await request.get({ url: `/icbc/input-invoice/page`, params }),
  getInputInvoice: async (id: number) =>
    await request.get({ url: `/icbc/input-invoice/get?id=` + id }),
  createInputInvoice: async (data: InputInvoiceVO) =>
    await request.post({ url: `/icbc/input-invoice/create`, data }),
  updateInputInvoice: async (data: InputInvoiceVO) =>
    await request.put({ url: `/icbc/input-invoice/update`, data }),
  deleteInputInvoice: async (id: number) =>
    await request.delete({ url: `/icbc/input-invoice/delete?id=` + id }),
  linkToBiz: async (data: InputInvoiceLinkVO) =>
    await request.post({ url: `/icbc/input-invoice/link`, data }),
  unlink: async (linkId: number) =>
    await request.post({ url: `/icbc/input-invoice/unlink?linkId=` + linkId })
}
