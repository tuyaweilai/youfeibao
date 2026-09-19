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
  /** 扣杂原始值：按重量时是重量，按比例时是比例（0~1） */
  deduction?: number
  /** 扣杂录法：WEIGHT-按重量，RATIO-按比例 */
  deductionMethod?: string
  /** 调整项（元，可正可负） */
  adjustmentAmount?: number
  /** 调整原因；调整项非零时必填 */
  adjustmentReason?: string
  /** 数量口径说明 */
  quantityNote?: string
  /** 司机姓名（运输信息） */
  driverName?: string
  /** 司机手机号（运输信息） */
  driverMobile?: string
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
  deduction?: number
  deductionMethod?: string
  /** 结算重量 = 毛重 − 皮重 − 扣杂（唯一计价基准） */
  settlementWeight?: number
  adjustmentAmount?: number
  adjustmentReason?: string
  quantityNote?: string
  driverName?: string
  driverMobile?: string
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
  deduction?: number
  deductionMethod?: string
  unitPrice?: number
  adjustmentAmount?: number
  adjustmentReason?: string
  quantityNote?: string
  driverName?: string
  driverMobile?: string
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
