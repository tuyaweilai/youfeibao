import { get, post } from '@/utils/request'

/** 结算单（#33 / #36）。现场端用它「结束本次收货」、看确认进度、把确认链接转达给出售者。 */

export interface SettlementLineVO {
  acquisitionId?: number
  acquisitionNo?: string
  categoryName?: string
  unit?: string
  settlementWeight?: number
  unitPrice?: number
  adjustmentAmount?: number
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
  createTime?: number
}

export interface SettlementVO {
  id?: number
  settlementNo?: string
  payeeId?: number
  naturalPersonId?: number
  sellerName?: string
  sellerMobile?: string
  generateTime?: number
  currentVersionId?: number
  currentVersionNo?: number
  /** 0-待确认，1-已确认，2-有异议，3-需线下签字确认，4-已线下签字确认 */
  confirmStatus?: number
  confirmStatusName?: string
  confirmTime?: number
  disputeReasonName?: string
  disputeCount?: number
  enterpriseNotReplied?: boolean
  deadlineTime?: number
  acquisitionCount?: number
  totalSettlementWeight?: number
  totalAmount?: number
  settled?: boolean
  versions?: SettlementVersionVO[]
  lines?: SettlementLineVO[]
}

export interface ForwardLinkVO {
  settlementId?: number
  settlementNo?: string
  token?: string
  link?: string
  linkConfigured?: boolean
  expiresTime?: number
  mobileMasked?: string
  notificationText?: string
  smsSent?: boolean
  message?: string
}

export const getSettlementPage = (params: {
  pageNo?: number
  pageSize?: number
  settlementNo?: string
  sellerName?: string
  confirmStatus?: number
}) => get<{ list: SettlementVO[]; total: number }>('/icbc/settlement/page', params)

export const getSettlement = (id: number) => get<SettlementVO>('/icbc/settlement/get', { id })

/** 结束本次收货：聚合同一出售者尚未归组的收购单，生成一张结算单 */
export const generateSettlement = (data: { payeeId: number; batchKey?: string; remark?: string }) =>
  post<number>('/icbc/settlement/generate', data)

/** 收货员一键把确认链接转达给出售者（可顺带发短信） */
export const forwardSettlementLink = (data: { settlementId: number; sendSms?: boolean }) =>
  post<ForwardLinkVO>('/icbc/notify/settlement/forward-link', data)
