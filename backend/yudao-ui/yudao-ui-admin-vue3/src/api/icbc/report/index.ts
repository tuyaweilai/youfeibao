import request from '@/config/axios'

// 经营报表与异常表（#57 T19）
// 四张表 + 一张派生异常表，全部只读；每张表的口径在 definition 里，界面上与数字一起显示。
// 所有指标都带来源单据编号 / 单号，可下钻到明细；实际收购量不含预约约量与采购计划量。

export interface ReportTableVO {
  code?: string
  name?: string
  definition?: string
}

export interface ReportPurchasePerformanceVO {
  orderId?: number
  orderNo?: string
  counterpartyName?: string
  stationName?: string
  statusName?: string
  endDate?: string
  expired?: boolean
  planQuantity?: number
  performedQuantity?: number
  balanceQuantity?: number
  acceptedQuantity?: number
  stockedQuantity?: number
  settledQuantity?: number
  completionBasis?: string
  completionBasisName?: string
  completionRatio?: number
  overQuantity?: boolean
  anomalyNames?: string[]
}

export interface ReportAcquisitionLedgerVO {
  acquisitionId?: number
  acquisitionNo?: string
  sellerName?: string
  stationId?: number
  stationName?: string
  directAcquisition?: boolean
  acquisitionModeText?: string
  purchaseOrderId?: number
  purchaseOrderNo?: string
  categoryName?: string
  specification?: string
  unit?: string
  grossWeight?: number
  tareWeight?: number
  netWeight?: number
  settlementWeight?: number
  acceptedWeight?: number
  rejectedWeight?: number
  residualWeight?: number
  weightDiff?: number
  quantity?: number
  unitPrice?: number
  amount?: number
  settlementId?: number
  settlementNo?: string
  invoicePartnerOrderId?: string
  tradeTime?: number
  statusName?: string
}

export interface ReportStockBalanceVO {
  goodsConfigId?: number
  categoryName?: string
  warehouseId?: number
  warehouseName?: string
  locationId?: number
  locationName?: string
  batchId?: number
  batchNo?: string
  batchInTime?: number
  ageDays?: number
  count?: number
}

export interface ReportStockRecordVO {
  id?: number
  goodsConfigId?: number
  categoryName?: string
  warehouseId?: number
  warehouseName?: string
  locationId?: number
  locationName?: string
  batchId?: number
  batchNo?: string
  count?: number
  totalCount?: number
  bizTypeName?: string
  bizId?: number
  bizNo?: string
  createTime?: number
}

export interface ReportSettlementPaymentVO {
  settlementId?: number
  settlementNo?: string
  sellerName?: string
  stationName?: string
  settlementAmount?: number
  acquisitionCount?: number
  confirmStatusName?: string
  paymentProgress?: string
  paymentProgressName?: string
  paidCount?: number
  pendingCount?: number
  failedCount?: number
  receiptStatus?: string
  failReason?: string
  unhandledHours?: number
  generateTime?: number
}

export interface ReportAnomalyVO {
  type?: string
  typeName?: string
  definition?: string
  severity?: string
  drillDown?: string
  bizType?: string
  bizId?: number
  bizNo?: string
  subject?: string
  categoryName?: string
  detail?: string
  quantity?: number
  amount?: number
  time?: number
}

export interface ReportPurchasePerformancePageReqVO extends PageParam {
  orderNo?: string
  counterpartyName?: string
  status?: number
}

export interface ReportAcquisitionLedgerPageReqVO extends PageParam {
  acquisitionNo?: string
  payeeId?: number
  sellerName?: string
  stationId?: number
  categoryName?: string
  directAcquisition?: boolean
}

export interface ReportStockBalancePageReqVO extends PageParam {
  goodsConfigId?: number
  warehouseId?: number
  locationId?: number
  batchId?: number
}

export interface ReportStockRecordPageReqVO extends ReportStockBalancePageReqVO {
  bizType?: number
  bizNo?: string
}

export interface ReportSettlementPaymentPageReqVO extends PageParam {
  settlementNo?: string
  payeeId?: number
  sellerName?: string
  confirmStatus?: number
  paymentProgress?: string
  receiptStatus?: string
}

export interface ReportAnomalyPageReqVO extends PageParam {
  type?: string
  payeeId?: number
  sellerName?: string
  bizNo?: string
}

export const ReportApi = {
  // 五张表的清单与口径说明
  getTables: async () => {
    return await request.get({ url: `/icbc/report/tables` })
  },
  getPurchasePerformancePage: async (params: ReportPurchasePerformancePageReqVO) => {
    return await request.get({ url: `/icbc/report/purchase-performance/page`, params })
  },
  getAcquisitionLedgerPage: async (params: ReportAcquisitionLedgerPageReqVO) => {
    return await request.get({ url: `/icbc/report/acquisition-ledger/page`, params })
  },
  getStockBalancePage: async (params: ReportStockBalancePageReqVO) => {
    return await request.get({ url: `/icbc/report/stock/balance/page`, params })
  },
  getStockRecordPage: async (params: ReportStockRecordPageReqVO) => {
    return await request.get({ url: `/icbc/report/stock/record/page`, params })
  },
  getSettlementPaymentPage: async (params: ReportSettlementPaymentPageReqVO) => {
    return await request.get({ url: `/icbc/report/settlement-payment/page`, params })
  },
  getAnomalyPage: async (params: ReportAnomalyPageReqVO) => {
    return await request.get({ url: `/icbc/report/anomaly/page`, params })
  }
}
