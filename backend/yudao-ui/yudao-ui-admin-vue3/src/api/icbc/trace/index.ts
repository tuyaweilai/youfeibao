import request from '@/config/axios'

/**
 * 关联单据查询（#55 T17）。
 *
 * <p>只读聚合：采购订单—现场收货—仓储入库—结算确认四栏 + 付款 / 发票，按单号 / 车牌 / 主体反查。
 * 敏感字段（税号 / 身份证 / 手机号 / 银行卡）默认脱敏；未脱敏查看与导出各需独立岗位权限。
 */

/** 查号方式：与后端 TraceKeywordTypeEnum 一一对应 */
export const TRACE_KEYWORD_TYPE_OPTIONS = [
  { label: '自动识别', value: 'AUTO' },
  { label: '收购单号', value: 'ACQUISITION_NO' },
  { label: '交接批次号', value: 'HANDOVER_BATCH_NO' },
  { label: '采购订单号', value: 'PURCHASE_ORDER_NO' },
  { label: '入库单号', value: 'STOCK_IN_NO' },
  { label: '结算单号', value: 'SETTLEMENT_NO' },
  { label: '发票号 / 合作方订单号', value: 'INVOICE_NO' },
  { label: '支付单号', value: 'PAYMENT_ORDER_NO' },
  { label: '车牌号', value: 'PLATE_NO' },
  { label: '出售者主体', value: 'SELLER' }
]

/** 阶段状态：与后端 TraceStageStatusEnum 一一对应 */
export const TRACE_STAGE_STATUS_ENUM = {
  NOT_STARTED: 0,
  IN_PROGRESS: 1,
  COMPLETED: 2,
  EXCEPTION: 3,
  NOT_APPLICABLE: 4
}

export const traceStageStatusTag = (status?: number) => {
  switch (status) {
    case TRACE_STAGE_STATUS_ENUM.NOT_STARTED:
      return 'info'
    case TRACE_STAGE_STATUS_ENUM.IN_PROGRESS:
      return 'warning'
    case TRACE_STAGE_STATUS_ENUM.COMPLETED:
      return 'success'
    case TRACE_STAGE_STATUS_ENUM.EXCEPTION:
      return 'danger'
    default:
      return 'info'
  }
}

export interface TraceNodeVO {
  bizType?: string
  bizId?: number
  bizNo?: string
  title?: string
  status?: number
  statusName?: string
  quantity?: number
  weight?: number
  amount?: number
  unit?: string
  time?: number
  detailPath?: string
  children?: TraceNodeVO[]
}

export interface TraceStageVO {
  code?: string
  name?: string
  definition?: string
  status?: number
  statusName?: string
  notApplicable?: boolean
  missingLink?: boolean
  quantity?: number
  weight?: number
  amount?: number
  unit?: string
  note?: string
  nodes?: TraceNodeVO[]
}

export interface TraceDifferenceVO {
  code?: string
  name?: string
  definition?: string
  expectedWeight?: number
  actualWeight?: number
  difference?: number
  note?: string
}

export interface TraceHistoryVO {
  time?: number
  stageCode?: string
  stageName?: string
  action?: string
  detail?: string
}

export interface TraceAttachmentVO {
  name?: string
  url?: string
  sourceType?: string
  sourceNo?: string
}

export interface TraceSensitiveVO {
  buyerTaxNo?: string
  sellerIdCard?: string
  sellerMobile?: string
  sellerBankCard?: string
}

export interface TraceRowVO {
  anchorType?: string
  handoverBatchId?: number
  handoverBatchNo?: string
  acquisitionId?: number
  acquisitionNo?: string
  payeeId?: number
  sellerName?: string
  categoryName?: string
  specification?: string
  unit?: string
  plateNo?: string
  stationName?: string
  tradeAddress?: string
  tradeTime?: number
  directAcquisition?: boolean
  physicalWeight?: number
  settlementWeight?: number
  stockedWeight?: number
  amount?: number
  acquisitionStatus?: number
  acquisitionStatusName?: string
  sensitiveUnmasked?: boolean
  sensitive?: TraceSensitiveVO
  stages?: TraceStageVO[]
  differences?: TraceDifferenceVO[]
  histories?: TraceHistoryVO[]
  attachments?: TraceAttachmentVO[]
  hiddenNodeCount?: number
}

export interface TraceSummaryVO {
  acquisitionCount?: number
  totalSettlementWeight?: number
  totalStockedWeight?: number
  totalAmount?: number
  differenceCount?: number
  countNote?: string
}

export interface TraceSearchRespVO {
  list: TraceRowVO[]
  total: number
  summary?: TraceSummaryVO
  scopeNote?: string
  hiddenDetailCount?: number
}

export const TraceApi = {
  search: async (params: any) => {
    return await request.get<TraceSearchRespVO>({ url: `/icbc/trace/search`, params })
  },
  get: async (acquisitionId: number) => {
    return await request.get<TraceRowVO>({ url: `/icbc/trace/get`, params: { acquisitionId } })
  },
  /** 未脱敏详情（需要 icbc:trace:sensitive:view 权限） */
  getSensitive: async (acquisitionId: number) => {
    return await request.get<TraceRowVO>({ url: `/icbc/trace/sensitive`, params: { acquisitionId } })
  },
  export: async (params: any) => {
    return await request.download({ url: `/icbc/trace/export`, params })
  }
}
