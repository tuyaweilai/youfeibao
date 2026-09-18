import request from '@/config/axios'

// 付方支付请求
export interface PaymentVO {
  appId?: string // 合作方编号
  outOrderId?: string // 合作方订单ID
  outVendorId?: string // 付方编号（回收企业 / 子商户）
  outUserId?: string // 收方编号（自然人）
  verifiedCode?: string // 机构编码（场景支付必输）
  ukeyId?: string // U盾ID（场景支付必输）
}

// 发起付款响应
export interface PaymentRespVO {
  returnCode?: string
  returnMsg?: string
  redirectUrl?: string // 企业支付页面表单 HTML
  msgId?: string
  outOrderId?: string
  icbcOrderNo?: string
  paymentStatus?: string
}

// 支付状态
export interface PaymentStatusVO {
  returnCode?: string
  returnMsg?: string
  outOrderId?: string
  icbcOrderNo?: string
  paymentStatus?: string
  paymentAmount?: number
  paymentTime?: Date
  outVendorId?: string
  outUserId?: string
  paymentSerialNo?: string
  errorCode?: string
  errorMsg?: string
}

// 付款 API
export const PaymentApi = {
  // 发起付方支付（返回企业支付页面表单）
  createPayment: async (data: PaymentVO) => {
    return await request.post({ url: `/icbc/payment/create`, data })
  },
  // 查询支付状态
  queryPayment: async (params: { outOrderId: string; icbcOrderNo?: string }) => {
    return await request.get({ url: `/icbc/payment/query`, params })
  }
}
