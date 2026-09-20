import request from '@/config/axios'

// 收购登记（#7）
export interface AcquisitionVO {
  id?: number
  acquisitionNo?: string
  clientRequestId?: string
  payeeId?: number
  sellerSubjectType?: number
  sellerSubjectTypeName?: string
  stationId?: number
  /** 交接批次编号（#50）：填了就按该批次的有效磅次计量 */
  handoverBatchId?: number
  /** 有效磅次编号（计量结果引用的就是它） */
  weighingId?: number
  /** 有效磅次是第几次 */
  weighingSeqNo?: number
  /** 采购安排关联（#51）：0 表示未关联（直接收购） */
  purchaseOrderId?: number
  purchaseOrderItemId?: number
  /** 是否为「直接收购」：报表 / 列表口径，不是失败态 */
  directAcquisition?: boolean
  /** 采购安排口径文案：直接收购 / 采购订单 */
  purchaseArrangementText?: string
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
  /** 要件状态：COMPLETE-已齐，PENDING-待补档（缺身份证或银行卡，付款与开票被门禁拦住） */
  documentStatus?: string
  documentStatusName?: string
  documentGap?: string
  /** 物流侧交接登记编号（上门提货的现场交接来源） */
  logisticsHandoverId?: number
  driverId?: number
  vehicleId?: number
  /** 现场参考量 / 参考单价快照（不是计量事实：计量取有效磅次） */
  referenceQuantity?: number
  referenceUnitPrice?: number
  referenceFixReason?: string
  documentCompletedAt?: number
  documentCompleteRemark?: string
  invoicePartnerOrderId?: string
  source?: string
  remark?: string
  createTime?: Date
  updateTime?: Date
  // ==================== 接收结论与称量差异（#53 T15） ====================
  /** 扣杂原始值 */
  deduction?: number
  /** 扣杂录法：WEIGHT / RATIO */
  deductionMethod?: string
  /** 结算重量（唯一计价基准） */
  settlementWeight?: number
  /** 接收量（实际留下 / 进库的重量）；为空表示未做接收结论 */
  acceptedWeight?: number
  /** 退回量（拒收部分不进应付、不进库存） */
  rejectedWeight?: number
  /** 余货出场量（未接收、带离场站的余货） */
  residualWeight?: number
  /** 拒收原因 */
  rejectReason?: string
  /** 称量差异 = 实物量 − 结算重量（不静默抹平） */
  weightDiff?: number
}

/** 称量差异清单行（#53 T15，只读） */
export interface AcquisitionWeightDiffVO {
  id?: number
  acquisitionNo?: string
  payeeId?: number
  sellerName?: string
  categoryName?: string
  unit?: string
  netWeight?: number
  deduction?: number
  settlementWeight?: number
  physicalWeight?: number
  acceptedWeight?: number
  rejectedWeight?: number
  residualWeight?: number
  rejectReason?: string
  weightDiff?: number
  differenceNote?: string
  amount?: number
  status?: number
  tradeTime?: number
  createTime?: Date
}

/** 记录接收结论（#53 T15） */
export interface AcquisitionAcceptanceReqVO {
  id: number
  acceptedWeight: number
  rejectedWeight?: number
  residualWeight?: number
  rejectReason?: string
  remark?: string
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
  /** 记录接收结论：接收 / 部分接收 / 拒收（#53 T15） */
  recordAcceptance: async (data: AcquisitionAcceptanceReqVO) =>
    await request.post({ url: `/icbc/acquisition/acceptance`, data }),
  /** 称量差异清单（只读，供异常表消费） */
  getWeightDiffPage: async (params: any) =>
    await request.get({ url: `/icbc/acquisition/weight-diff/page`, params }),
  /** 补档放行（#73 V6）：待补档 → 已齐，留办理人与时间 */
  completeDocuments: async (data: { id: number; remark?: string }) =>
    await request.post({ url: `/icbc/acquisition/complete-documents`, data }),
  exportConfirmation: async (id: number) =>
    await request.download({ url: `/icbc/acquisition/confirmation/export`, params: { id } })
}
