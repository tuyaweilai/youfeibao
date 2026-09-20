import request from '@/config/axios'

// 收购登记（#7）
export interface AcquisitionVO {
  id?: number
  acquisitionNo?: string
  clientRequestId?: string
  payeeId?: number
  stationId?: number
  /** 交接批次编号（#50）：填了就按该批次的有效磅次计量 */
  handoverBatchId?: number
  /** 有效磅次编号（计量结果引用的就是它） */
  weighingId?: number
  /** 有效磅次是第几次 */
  weighingSeqNo?: number
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

/** 收购登记响应：单据本身 + 该出售者的额度余量提示（#12） */
export interface AcquisitionCreateRespVO {
  id?: number
  acquisitionNo?: string
  quotaCapAmount?: number
  quotaUsedAmount?: number
  quotaRemainingAmount?: number
  quotaPassed?: boolean
  quotaMessage?: string
  monthlyOverExempt?: boolean
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
    await request.post<AcquisitionCreateRespVO>({ url: `/icbc/acquisition/create`, data }),
  syncOffline: async (items: AcquisitionVO[]) =>
    await request.post({ url: `/icbc/acquisition/sync-offline`, data: { items } }),
  correctRecognition: async (data: AcquisitionVO) =>
    await request.post({ url: `/icbc/acquisition/correct`, data }),
  exportConfirmation: async (id: number) =>
    await request.download({ url: `/icbc/acquisition/confirmation/export`, params: { id } })
}
