import request from '@/config/axios'

// 出售者触达（#36，见 docs/adr/0023-触达只靠短信与收货员转达.md）
export interface SellerNotifyVO {
  id?: number
  bizType?: string
  bizTypeName?: string
  bizKey?: string
  templateCode?: string
  payeeId?: number
  sellerName?: string
  mobileMasked?: string
  status?: number
  statusName?: string
  content?: string
  link?: string
  errorMsg?: string
  sendTime?: Date
  createTime?: Date
}

export interface SellerNotifySettingVO {
  platformEnabled?: boolean
  tenantEnabled?: boolean
  effectiveEnabled?: boolean
  sellerAppUrlConfigured?: boolean
  remark?: string
}

export interface SellerNotifyForwardLinkVO {
  settlementId?: number
  settlementNo?: string
  token?: string
  link?: string
  linkConfigured?: boolean
  expiresTime?: Date
  mobileMasked?: string
  notificationText?: string
  smsSent?: boolean
  message?: string
}

export const SellerNotifyApi = {
  getNotifyPage: async (params: any) =>
    await request.get({ url: `/icbc/notify/page`, params }),
  getSetting: async () => await request.get({ url: `/icbc/notify/setting` }),
  saveSetting: async (data: { smsEnabled: boolean; remark?: string }) =>
    await request.put({ url: `/icbc/notify/setting`, data }),
  // 收货员一键把结算确认链接转达给出售者
  forwardSettlementLink: async (data: { settlementId: number; sendSms?: boolean }) =>
    await request.post({ url: `/icbc/notify/settlement/forward-link`, data })
}

export const SELLER_NOTIFY_TYPES = [
  { label: '结算单待确认', value: 'SETTLEMENT_PENDING' },
  { label: '付款异常', value: 'PAYMENT_EXCEPTION' },
  { label: '发票已开出', value: 'INVOICE_ISSUED' }
]

export const SELLER_NOTIFY_STATUS = [
  { label: '未发送（开关关闭）', value: 0 },
  { label: '未发送（未留手机号）', value: 1 },
  { label: '未发送（未配置入口）', value: 2 },
  { label: '已发送', value: 3 },
  { label: '发送失败', value: 4 }
]
