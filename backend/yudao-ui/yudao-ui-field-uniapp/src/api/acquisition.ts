import { get, post } from '@/utils/request'

export interface AcquisitionCreateReq {
  clientRequestId?: string
  payeeId: number
  /** 场站编号：一次到场批次按「出售者 + 场站」聚合 */
  stationId?: number
  /** 交接批次编号（#50）：填了就按该批次的有效磅次计量，手填重量不采用 */
  handoverBatchId?: number
  /** 可选关联的采购订单编号（#51）：不填即「直接收购」；填了必须同时给订单明细 */
  purchaseOrderId?: number
  /** 可选关联的采购订单明细编号（#51）：明细品类须与本次收购品类一致 */
  purchaseOrderItemId?: number
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
  sellerSubjectType?: number
  sellerSubjectTypeName?: string
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
  /** 交接批次与有效磅次（#50）：计量结果引用的是有效磅次的值与版本 */
  handoverBatchId?: number
  weighingId?: number
  weighingSeqNo?: number
  /** 采购安排关联（#51）：0 表示未关联（直接收购） */
  purchaseOrderId?: number
  purchaseOrderItemId?: number
  /** 是否为「直接收购」：报表 / 列表口径，不是失败态 */
  directAcquisition?: boolean
  /** 采购安排口径文案：直接收购 / 采购订单 */
  purchaseArrangementText?: string
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
  /** 下一步是谁的事（后端按档位派生，ADR 0038） */
  statusNextStep?: string
  /** 进度是否异常：预开票 / 付款 / 开票 / 缴税 / 上传任一出问题；异常不替换档位 */
  abnormal?: boolean
  /** 异常标注，形如「付款：支付失败」 */
  abnormalReasons?: string[]
  invoicePartnerOrderId?: string
  source?: string
  remark?: string
  createTime?: number
}

/** 发票下载记录（现场端只用到「拿到 downloadId，再取 PDF」） */
export interface InvoiceDownloadVO {
  id?: number
  partnerOrderId?: string
  invoiceNumber?: string
  downloadStatus?: number
  downloadStatusName?: string
  errorMsg?: string
  files?: InvoiceDownloadFileVO[]
}

export interface InvoiceDownloadFileVO {
  downloadId?: number
  invoiceNumber?: string
  fileType?: string
  fileName?: string
  fileSize?: number
}

/**
 * 发起（或复用）发票原件下载，返回下载记录。
 *
 * <p>工行只回 PDF（工行答复 2026-09-18），所以这里不传 fileType，取回的文件里挑 PDF。
 */
export const downloadInvoice = (partnerOrderId: string) =>
  post<InvoiceDownloadVO>('/icbc/invoice-download/download', { partnerOrderId })

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

/** 可选采购安排（#51）：执行中且未过期的采购订单 + 品类明细，现场按品类选到具体明细 */
export interface PurchaseArrangementItemVO {
  itemId?: number
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  planQuantity?: number
  unitPrice?: number
}

export interface PurchaseArrangementVO {
  orderId?: number
  orderNo?: string
  contractNo?: string
  counterpartyName?: string
  stationId?: number
  stationName?: string
  startDate?: string
  endDate?: string
  items?: PurchaseArrangementItemVO[]
}

/** 有效采购安排（执行中且未过期的采购订单 + 品类明细）；没有就返回空列表，不阻断登记。 */
export const getUsablePurchaseArrangements = (payeeId: number) =>
  get<PurchaseArrangementVO[]>('/icbc/acquisition/purchase-arrangement/list', { payeeId })

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
