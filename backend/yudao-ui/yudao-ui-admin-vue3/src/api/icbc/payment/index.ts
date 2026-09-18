import request from '@/config/axios'

// 发起付款请求
export interface PaymentApplyVO {
  acquisitionId?: number // 收购单编号（与合作方订单号二选一）
  partnerOrderId?: string // 开票合作方订单号（等于收购单号）
  amount?: number // 本次付款金额，必须等于收购单金额
  verifiedCode?: string // 机构编码（场景支付必输）
  ukeyId?: string // U盾ID（场景支付必输）
}

// 发起付款响应
export interface PaymentApplyRespVO {
  success?: boolean
  duplicate?: boolean // 是否重复发起（未重复提交工行）
  message?: string
  partnerOrderId?: string
  orderNo?: string
  acquisitionId?: number
  payAmount?: number
  payPageHtml?: string // 企业支付页面表单 HTML
  paymentStatus?: number
  paymentStatusName?: string
  reInitiable?: boolean
  errorCode?: string
  errorMsg?: string
}

// 支付状态
export interface PaymentStatusVO {
  partnerOrderId?: string
  orderNo?: string
  acquisitionId?: number
  paymentAmount?: number
  actuallyReceivedAmount?: number
  paymentStatus?: number
  paymentStatusName?: string
  payStatus?: string // 工行原始状态码
  reInitiable?: boolean
  paymentTime?: number
  paymentSerialNo?: string
  icbcOrderNo?: string
  receiptNo?: string
  receiptTime?: number
  errorCode?: string
  errorMsg?: string
}

// 转账回单
export interface PaymentReceiptVO {
  partnerOrderId?: string
  orderNo?: string
  acquisitionId?: number
  receiptNo?: string
  receiptTime?: number
  receiptFileUrl?: string
  paymentSerialNo?: string
  paymentAmount?: number
  actuallyReceivedAmount?: number
  paymentStatusName?: string
}

// 付款 API
export const PaymentApi = {
  // 对预开票成功的收购发起付款（返回企业支付页面表单）
  applyPayment: async (data: PaymentApplyVO) => {
    return await request.post({ url: `/icbc/payment/apply`, data })
  },
  // 查询支付状态（后端会自动经工行收敛一次）
  queryPayment: async (params: { partnerOrderId: string }) => {
    return await request.get({ url: `/icbc/payment/query`, params })
  },
  // 查询转账回单归档信息
  getReceipt: async (params: { partnerOrderId: string }) => {
    return await request.get({ url: `/icbc/payment/receipt`, params })
  }
}
