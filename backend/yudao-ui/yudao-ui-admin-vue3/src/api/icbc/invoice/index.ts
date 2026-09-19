import request from '@/config/axios'

// 商品明细
export interface GoodsInfoVO {
  goodsConfigId?: number // 选择的品类配置（仅前端用，不发给工行）
  goodsSeqno?: string // 商品信息子序号
  projectName?: string // 项目名称
  goodsNum?: number // 商品总数
  goodsAmt?: number // 商品金额(元)
  price?: number // 含税单价(元)
  units?: string // 计量单位
  taxRate?: number // 增值税税率
  mergedCode?: string // 商品和服务税收分类合并编码
}

// 反向开票预下单请求（字段较多，此处列业务字段；工行技术字段由页面补默认值）
export interface InvoicePreOrderVO {
  outOrderId?: string
  outVendorId?: string
  outUserId?: string
  trxChannel?: string
  asynFlag?: string
  currency?: string
  payJumpUrl?: string
  invoiceNotifyUrl?: string
  invoiceJumpUrl?: string
  invoiceType?: string // 01-专票，02-普票
  orderAmount?: number
  naturalPersonName?: string
  cardType?: string
  cardNumber?: string
  sellerAddress?: string
  sellerTelephone?: string
  taxpayerNo?: string
  taxpayerName?: string
  drawerName?: string
  drawerCardType?: string
  drawerCardNumber?: string
  specificElements?: string // 16-农产品收购，24-报废产品收购
  buyerInvTypeCode?: string // 16→01，24→04
  areaCode?: string
  iitProject?: string
  mac?: string
  taxRate?: number
  goodsInfo?: GoodsInfoVO[]
}

export interface InvoicePreOrderRespVO {
  returnCode?: number
  returnMsg?: string
  redirectUrl?: string // 自然人确认页面表单 HTML
  orderNo?: string
  partnerOrderId?: string
}

export interface InvoiceQueryRespVO {
  returnCode?: number
  returnMsg?: string
  orderNo?: string
  partnerOrderId?: string
  acquisitionId?: number
  orderStatus?: number
  invoiceStatus?: number
  invoiceStatusName?: string
  paymentStatus?: number
  taxStatus?: number
  taxStatusName?: string
  uploadStatus?: number
  uploadStatusName?: string
  confirmStatus?: number
  preInvoiceStatus?: number
  invoiceNo?: string
  invoiceCode?: string
  invoiceDate?: Date
  invoiceAmount?: number
  taxAmount?: number
  taxRealAmount?: number
  taxTime?: Date
  taxPaymentMethod?: string
  taxPaymentMethodName?: string
  taxVoucherNo?: string
  nextAction?: string
  redSerialNo?: string
  redOffsetStatus?: number
  redOffsetStatusName?: string
  redInvoiceNo?: string
  redInvoiceDate?: Date
}

// 代办税费缴税凭证
export interface InvoiceTaxCertificateVO {
  certificateNo?: string
  partnerOrderId?: string
  orderNo?: string
  acquisitionNo?: string
  payerName?: string
  payerTaxNo?: string
  sellerName?: string
  sellerIdCardNo?: string
  invoiceNo?: string
  invoiceCode?: string
  invoiceDate?: string
  invoiceAmount?: number
  taxAmount?: number
  taxRealAmount?: number
  taxTime?: string
  taxPaymentMethodName?: string
  taxVoucherNo?: string
  taxStatusName?: string
  issuedTime?: string
}

// ==================== 开票申请（#8：按收购单发起，预下单与自然人确认） ====================

export interface InvoicePreCheckItemVO {
  code?: string
  name?: string
  passed?: boolean
  message?: string
  remedy?: string
}

export interface InvoicePreCheckRespVO {
  acquisitionId?: number
  acquisitionNo?: string
  allPassed?: boolean
  items?: InvoicePreCheckItemVO[]
}

export interface InvoiceApplicationResultVO {
  acquisitionId?: number
  acquisitionNo?: string
  success?: boolean
  duplicate?: boolean
  partnerOrderId?: string
  orderNo?: string
  confirmPageHtml?: string
  confirmStatus?: number
  preInvoiceStatus?: number
  orderStatus?: number
  message?: string
  failures?: InvoicePreCheckItemVO[]
}

export interface InvoiceApplicationBaseVO {
  invoiceType?: string // 01-专票，02-普票
  areaCode?: string
  drawerName?: string
  drawerCardType?: string
  drawerCardNumber?: string
  jumpUrlBase?: string
  mac?: string
  notes?: string
}

export interface InvoiceApplicationApplyVO extends InvoiceApplicationBaseVO {
  acquisitionId: number
}

export interface InvoiceApplicationBatchVO extends InvoiceApplicationBaseVO {
  acquisitionIds: number[]
}

// 开票申请 API
export const InvoiceApi = {
  // 预下单（返回自然人确认页面表单）
  preOrder: async (data: InvoicePreOrderVO) => {
    return await request.post({ url: `/icbc/invoice-order/pre-order`, data })
  },
  // 预查询
  query: async (params: { outOrderId: string }) => {
    return await request.get({ url: `/icbc/invoice-order/query`, params })
  },
  // 代办税费缴税凭证（缴税成功后）
  getTaxCertificate: async (partnerOrderId: string) => {
    return await request.get({ url: `/icbc/invoice-order/tax-certificate`, params: { partnerOrderId } })
  },
  // 导出缴税凭证（可打印）
  exportTaxCertificate: async (partnerOrderId: string) =>
    await request.download({ url: `/icbc/invoice-order/tax-certificate/export`, params: { partnerOrderId } })
}

// 开票申请 API（按已登记的收购单发起）
export const InvoiceApplicationApi = {
  // 发起前校验：逐项返回是否通过、哪里不满足、怎么补齐
  preCheck: async (params: { acquisitionId: number; invoiceType?: string }) => {
    return await request.get({ url: `/icbc/invoice-application/pre-check`, params })
  },
  // 单笔发起
  apply: async (data: InvoiceApplicationApplyVO) => {
    return await request.post({ url: `/icbc/invoice-application/apply`, data })
  },
  // 批量发起：逐笔独立成败
  applyBatch: async (data: InvoiceApplicationBatchVO) => {
    return await request.post({ url: `/icbc/invoice-application/apply-batch`, data })
  }
}

// ==================== 红冲与发票取消（#14） ====================

export interface RedInvoiceGoodsVO {
  goodsSeqno?: string
  blueGoodsSeqno?: string
  projectName?: string
  goodsNum?: string
  goodsAmt?: string
  weight?: string
  price?: string
  units?: string
}

export interface RedInvoiceApplyVO {
  partnerOrderId: string
  reason: string // 01 开票有误 / 02 销货退回 / 03 服务中止 / 04 销售折让
  amount?: number
  goods?: RedInvoiceGoodsVO[]
  jumpUrlBase?: string
  remark?: string
}

export interface RedInvoiceApplyResultVO {
  success?: boolean
  duplicate?: boolean
  redOffsetNo?: string
  partnerOrderId?: string
  reason?: string
  amount?: number
  redOffsetStatus?: number
  redOffsetStatusName?: string
  nextAction?: string
  confirmPageHtml?: string
  message?: string
}

export interface RedInvoiceQueryVO {
  redOffsetNo?: string
  partnerOrderId?: string
  invoiceOrderId?: number
  acquisitionId?: number
  reason?: string
  reasonName?: string
  amount?: number
  taxAmount?: number
  redOffsetStatus?: number
  redOffsetStatusName?: string
  redOffsetStatusCode?: string
  redInvoiceNo?: string
  redInvoiceDate?: Date
  revokeStatus?: string
  revokeTime?: Date
  nextAction?: string
  createTime?: Date
}

// 红冲与发票取消 API
export const RedInvoiceApi = {
  // 发起红字冲销，返回红字确认单页面表单 HTML
  apply: async (data: RedInvoiceApplyVO) => {
    return await request.post({ url: `/icbc/red-invoice/apply`, data })
  },
  // 撤销尚未生效的红字确认单
  revoke: async (redOffsetNo: string) => {
    return await request.post({ url: `/icbc/red-invoice/revoke`, data: { redOffsetNo } })
  },
  // 取消预开票成功但未支付的发票
  cancel: async (partnerOrderId: string) => {
    return await request.post({ url: `/icbc/red-invoice/cancel`, data: { partnerOrderId } })
  },
  // 按红冲流水号查询
  get: async (redOffsetNo: string) => {
    return await request.get({ url: `/icbc/red-invoice/get`, params: { redOffsetNo } })
  },
  // 按蓝票合作方订单号查询最近一次红冲
  getByPartner: async (partnerOrderId: string) => {
    return await request.get({ url: `/icbc/red-invoice/get-by-partner`, params: { partnerOrderId } })
  },
  // 主动向工行查询红冲最新状态
  refresh: async (redOffsetNo: string) => {
    return await request.get({ url: `/icbc/red-invoice/query`, params: { redOffsetNo } })
  }
}
