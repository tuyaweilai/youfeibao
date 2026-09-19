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

export const createAcquisition = (data: AcquisitionCreateReq) =>
  post<AcquisitionCreateResp>('/icbc/acquisition/create', data)

export const getAcquisition = (id: number) => get<AcquisitionVO>('/icbc/acquisition/get', { id })

export const getAcquisitionPage = (params: { pageNo: number; pageSize: number }) =>
  get<{ list: AcquisitionVO[]; total: number }>('/icbc/acquisition/page', params)
