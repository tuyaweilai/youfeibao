import { get, post } from '@/utils/request'

export interface AcquisitionCreateReq {
  clientRequestId?: string
  payeeId: number
  goodsConfigId: number
  specification?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  grossWeight?: number
  tareWeight?: number
  netWeight?: number
  weightTicketNo?: string
  weightTicketImageUrl?: string
  weightTicketPlateNo?: string
  vehiclePlateNo?: string
  vehicleFrontImageUrl?: string
  vehicleRearImageUrl?: string
  tradeAddress?: string
  tradeTime?: number
  settlementMethod?: string
  source?: string
  remark?: string
}

export interface AcquisitionCreateResp {
  id?: number
  acquisitionNo?: string
  quotaCapAmount?: number
  quotaUsedAmount?: number
  quotaRemainingAmount?: number
  quotaPassed?: boolean
  quotaMessage?: string
  monthlyOverExempt?: boolean
}

export interface AcquisitionVO {
  id?: number
  acquisitionNo?: string
  payeeId?: number
  sellerName?: string
  sellerMobile?: string
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  taxRate?: number
  taxMethod?: string
  mergedCode?: string
  specification?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  grossWeight?: number
  tareWeight?: number
  netWeight?: number
  weightTicketNo?: string
  weightTicketImageUrl?: string
  weightTicketPlateNo?: string
  vehiclePlateNo?: string
  /** 车牌比对结果：true-一致，false-不一致，null-无法比对 */
  plateMatched?: boolean | null
  vehicleFrontImageUrl?: string
  vehicleRearImageUrl?: string
  tradeAddress?: string
  tradeTime?: number
  settlementMethod?: string
  status?: number
  statusName?: string
  invoicePartnerOrderId?: string
  source?: string
  remark?: string
  createTime?: number
}

export interface AcquisitionCorrectionReq {
  id: number
  grossWeight?: number
  tareWeight?: number
  netWeight?: number
  weightTicketNo?: string
  weightTicketPlateNo?: string
  vehiclePlateNo?: string
  remark?: string
}

export const createAcquisition = (data: AcquisitionCreateReq) =>
  post<AcquisitionCreateResp>('/icbc/acquisition/create', data)

/** 人工修正磅单 / 车牌识别结果，修正后后端重新做车牌比对 */
export const correctAcquisition = (data: AcquisitionCorrectionReq) =>
  post<boolean>('/icbc/acquisition/correct', data)

export const getAcquisition = (id: number) => get<AcquisitionVO>('/icbc/acquisition/get', { id })

export const getAcquisitionPage = (params: { pageNo: number; pageSize: number }) =>
  get<{ list: AcquisitionVO[]; total: number }>('/icbc/acquisition/page', params)

export interface AcquisitionSyncResultVO {
  clientRequestId?: string
  id?: number
  acquisitionNo?: string
  success?: boolean
  duplicated?: boolean
  errorMsg?: string
}

/** 弱网补传：按 clientRequestId 幂等，重复补传不产生重复单据，逐条返回成败 */
export const syncOfflineAcquisitions = (items: AcquisitionCreateReq[]) =>
  post<AcquisitionSyncResultVO[]>('/icbc/acquisition/sync-offline', { items })
