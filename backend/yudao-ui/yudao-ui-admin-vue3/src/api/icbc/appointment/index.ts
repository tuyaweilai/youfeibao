import request from '@/config/axios'

// 到站预约（#35，ADR 0020）：不是订单，不占额度、不产生开票、不进五流；只有到场 / 未到场
export interface AppointmentVO {
  id?: number
  appointmentNo?: string
  naturalPersonId?: number
  payeeId?: number
  tenantId?: number
  enterpriseName?: string
  sellerName?: string
  stationId?: number
  stationCode?: string
  stationName?: string
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  expectedQuantity?: number
  expectedQuantityText?: string
  plateNo?: string
  expectedArrivalTime?: Date
  status?: number
  statusName?: string
  arrivedAt?: Date
  acquisitionId?: number
  cancelledAt?: Date
  cancelReason?: string
  noShowReason?: string
  remark?: string
  createTime?: Date
  scopeNote?: string
}

export const AppointmentApi = {
  getAppointmentPage: async (params: any) => await request.get({ url: `/icbc/appointment/page`, params }),
  getAppointment: async (id: number) => await request.get({ url: `/icbc/appointment/get?id=` + id }),
  getPendingByPayee: async (payeeId: number) =>
    await request.get({ url: `/icbc/appointment/pending?payeeId=` + payeeId }),
  arrive: async (data: { id: number; acquisitionId?: number }) =>
    await request.post({ url: `/icbc/appointment/arrive`, data }),
  noShow: async (data: { id: number; reason?: string }) =>
    await request.post({ url: `/icbc/appointment/no-show`, data })
}
