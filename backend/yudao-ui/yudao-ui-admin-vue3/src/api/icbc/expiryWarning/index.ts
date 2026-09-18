import request from '@/config/axios'

// 资质到期预警（定时任务扫描后落库，供开票就绪页提醒）
export interface ExpiryWarningVO {
  id?: number
  qualificationId?: number
  type?: string
  name?: string
  validTo?: string
  status?: number // 0-待处理，1-已处理
  warnedAt?: Date
  createTime?: Date
}

export const ExpiryWarningApi = {
  getOpenList: async () => await request.get({ url: `/icbc/expiry-warning/list` }),
  acknowledge: async (id: number) =>
    await request.put({ url: `/icbc/expiry-warning/acknowledge?id=` + id })
}
