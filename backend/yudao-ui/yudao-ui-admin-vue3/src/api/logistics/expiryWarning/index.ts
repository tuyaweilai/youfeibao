import request from '@/config/axios'

// 物流证件到期提醒（#70 V3）：四类证件合成一个扁平列表，与派车门禁共用同一套「过期」判定，
// 所以不会出现「提醒说没事、派车却被拦」。
export interface LogisticsExpiryWarningItemVO {
  category?: string // 车辆-行驶证 / 车辆-保险 / 司机-驾驶证 / 司机-从业资格证
  subjectType?: number // 1-车辆，2-司机
  subjectId?: number
  subjectName?: string
  expiryDate?: string
  daysLeft?: number // 负数表示已过期
  expired?: boolean
}

export const LogisticsExpiryWarningApi = {
  getList: async (days = 30) =>
    await request.get({ url: `/logistics/expiry-warning/list`, params: { days } })
}
