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
  orderStatus?: number
  invoiceStatus?: number
  paymentStatus?: number
  taxStatus?: number
  invoiceNo?: string
  invoiceCode?: string
  invoiceDate?: Date
  invoiceAmount?: number
  taxAmount?: number
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
  }
}
