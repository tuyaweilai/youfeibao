import request from '@/config/axios'

// 收购登记（#7）
export interface AcquisitionVO {
  id?: number
  acquisitionNo?: string
  clientRequestId?: string
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
  plateMatched?: boolean
  vehicleFrontImageUrl?: string
  vehicleRearImageUrl?: string
  tradeAddress?: string
  tradeTime?: number // 时间戳毫秒
  settlementMethod?: string
  status?: number
  statusName?: string
  invoicePartnerOrderId?: string
  source?: string
  remark?: string
  createTime?: Date
  updateTime?: Date
}

export interface AcquisitionSyncResultVO {
  clientRequestId?: string
  id?: number
  acquisitionNo?: string
  success?: boolean
  duplicated?: boolean
  errorMsg?: string
}

// 收购登记 API
export const AcquisitionApi = {
  getAcquisitionPage: async (params: any) =>
    await request.get({ url: `/icbc/acquisition/page`, params }),
  getAcquisition: async (id: number) =>
    await request.get({ url: `/icbc/acquisition/get`, params: { id } }),
  getAcquisitionsByPayee: async (payeeId: number) =>
    await request.get({ url: `/icbc/acquisition/list-by-payee`, params: { payeeId } }),
  createAcquisition: async (data: AcquisitionVO) =>
    await request.post({ url: `/icbc/acquisition/create`, data }),
  syncOffline: async (items: AcquisitionVO[]) =>
    await request.post({ url: `/icbc/acquisition/sync-offline`, data: { items } }),
  correctRecognition: async (data: AcquisitionVO) =>
    await request.post({ url: `/icbc/acquisition/correct`, data }),
  exportConfirmation: async (id: number) =>
    await request.download({ url: `/icbc/acquisition/confirmation/export`, params: { id } })
}
