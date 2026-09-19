import request from '@/config/axios'

// 平台运营：工行通知监控与重放（#15）
export interface CallbackNotifyVO {
  id?: number
  notifyId?: string
  notifyType?: string
  notifyTypeName?: string
  businessName?: string
  businessId?: string
  notifyData?: string
  sign?: string
  processStatus?: number
  processStatusName?: string
  processMsg?: string
  processTime?: Date
  retryCount?: number
  createTime?: Date
  nextAction?: string
}

export interface CallbackNotifyTypeStatVO {
  notifyType: string
  notifyTypeName?: string
  businessName?: string
  total: number
  failureCount: number
}

export interface CallbackNotifySummaryVO {
  total: number
  pendingCount: number
  successCount: number
  failureCount: number
  types: CallbackNotifyTypeStatVO[]
}

export const PlatformCallbackApi = {
  getPage: async (params: any) =>
    await request.get({ url: `/icbc/platform/callback/page`, params }),
  get: async (id: number) =>
    await request.get({ url: `/icbc/platform/callback/get`, params: { id } }),
  getSummary: async () => await request.get({ url: `/icbc/platform/callback/summary` }),
  // 重放：只对处理失败 / 待处理的通知生效，已成功的不重复处理
  replay: async (id: number) =>
    await request.post({ url: `/icbc/platform/callback/replay`, params: { id } })
}
