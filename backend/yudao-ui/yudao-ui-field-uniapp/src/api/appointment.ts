import { get, post } from '@/utils/request'

/**
 * 到站预约（#35，ADR 0020）：**不是订单**。
 *
 * <p>现场只在登记收购时用它带出品类、约多少与车牌；没有接单 / 拒单，只有到场与未到场。
 * 预计数量一律只是「约」，不能当准数用。
 */
export interface AppointmentVO {
  id: number
  appointmentNo?: string
  sellerName?: string
  stationId?: number
  stationName?: string
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  expectedQuantity?: number
  /** 一律以「约」标注，如「约 12.5 吨」 */
  expectedQuantityText?: string
  plateNo?: string
  expectedArrivalTime?: number
  status?: number
  statusName?: string
  scopeNote?: string
}

/** 某出售者「待到站」的预约，按预计到站时间升序（现场登记收购时带出） */
export const getPendingAppointments = (payeeId: number) =>
  get<AppointmentVO[]>('/icbc/appointment/pending', { payeeId })

/** 标记到场（登记成功后调用，可挂上收购单编号） */
export const markAppointmentArrived = (id: number, acquisitionId?: number) =>
  post<boolean>('/icbc/appointment/arrive', { id, acquisitionId })

/** 标记未到场 */
export const markAppointmentNoShow = (id: number, reason?: string) =>
  post<boolean>('/icbc/appointment/no-show', { id, reason })
