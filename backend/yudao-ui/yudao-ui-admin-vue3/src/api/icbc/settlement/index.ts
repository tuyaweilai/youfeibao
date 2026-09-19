import request from '@/config/axios'

// 结算单（#33，见 docs/adr/0018 / 0022 / 0024）
export interface SettlementLineVO {
  acquisitionId?: number
  acquisitionNo?: string
  categoryName?: string
  unit?: string
  quantity?: number
  grossWeight?: number
  tareWeight?: number
  deduction?: number
  deductionMethod?: string
  settlementWeight?: number
  unitPrice?: number
  adjustmentAmount?: number
  adjustmentReason?: string
  amount?: number
  status?: number
  statusName?: string
  cancelReason?: string
}

export interface SettlementVersionVO {
  id?: number
  versionNo?: number
  snapshotHash?: string
  changeReason?: string
  changedBy?: string
  source?: string
  totalSettlementWeight?: number
  totalAmount?: number
  acquisitionCount?: number
  createTime?: Date
}

export interface SettlementVO {
  id?: number
  settlementNo?: string
  payeeId?: number
  naturalPersonId?: number
  sellerName?: string
  sellerMobile?: string
  generateTime?: Date
  currentVersionId?: number
  currentVersionNo?: number
  confirmStatus?: number // 0待确认 1已确认 2有异议 3需线下签字确认 4已线下签字确认
  confirmStatusName?: string
  confirmTime?: Date
  confirmHash?: string
  disputeReason?: string
  disputeReasonName?: string
  disputeNote?: string
  disputeTime?: Date
  disputeCount?: number
  enterpriseReplyNote?: string
  enterpriseReplyTime?: Date
  enterpriseNotReplied?: boolean
  suggestOffline?: boolean
  deadlineTime?: Date
  offlineSignFileUrl?: string
  offlineSignHandler?: string
  offlineSignTime?: Date
  acquisitionCount?: number
  totalSettlementWeight?: number
  totalAmount?: number
  settled?: boolean
  remark?: string
  createTime?: Date
  versions?: SettlementVersionVO[]
  lines?: SettlementLineVO[]
}

export const SettlementApi = {
  getSettlementPage: async (params: any) =>
    await request.get({ url: `/icbc/settlement/page`, params }),
  getSettlement: async (id: number) =>
    await request.get({ url: `/icbc/settlement/get?id=` + id }),
  listByPayee: async (payeeId: number) =>
    await request.get({ url: `/icbc/settlement/list-by-payee?payeeId=` + payeeId }),
  // 结束本次收货，生成结算单
  generate: async (data: { payeeId: number; batchKey?: string; remark?: string }) =>
    await request.post({ url: `/icbc/settlement/generate`, data }),
  // 对异议的答复：改（新版本 + 原因）
  change: async (data: {
    settlementId: number
    changeReason: string
    lines: {
      acquisitionId: number
      deduction?: number
      deductionMethod?: string
      unitPrice?: number
      adjustmentAmount?: number
      adjustmentReason?: string
    }[]
  }) => await request.post({ url: `/icbc/settlement/change`, data }),
  // 对异议的答复：不改但附说明
  reply: async (data: { settlementId: number; note: string }) =>
    await request.post({ url: `/icbc/settlement/reply`, data }),
  // 线下签字确认
  offlineSign: async (data: { settlementId: number; fileUrl: string; handler: string; remark?: string }) =>
    await request.post({ url: `/icbc/settlement/offline-sign`, data }),
  // 作废未开票的收购单
  cancelAcquisition: async (data: { acquisitionId: number; reason: string }) =>
    await request.post({ url: `/icbc/settlement/cancel-acquisition`, data })
}

export const SETTLEMENT_CONFIRM_STATUS_OPTIONS = [
  { label: '待确认', value: 0 },
  { label: '已确认', value: 1 },
  { label: '有异议', value: 2 },
  { label: '需线下签字确认', value: 3 },
  { label: '已线下签字确认', value: 4 }
]

export const SETTLEMENT_DISPUTE_REASONS = [
  { label: '重量不符', value: '01' },
  { label: '扣杂不符', value: '02' },
  { label: '单价不符', value: '03' },
  { label: '品类或等级不符', value: '04' },
  { label: '货物不符', value: '05' },
  { label: '其他（须附说明）', value: '99' }
]
