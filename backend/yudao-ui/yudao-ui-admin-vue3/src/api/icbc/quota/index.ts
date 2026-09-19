import request from '@/config/axios'

/** 「出售者 × 月」额度台账中的一个月（跨租户合并后的结果） */
export interface SellerQuotaMonthVO {
  month?: string
  issuedAmount?: number
  pendingAmount?: number
  redOffsetAmount?: number
  netAmount?: number
  amountAtOnePercent?: number
  amountAtThreePercent?: number
  otherAmount?: number
  overMonthlyExempt?: boolean
}

/** 出售者额度台账 */
export interface SellerQuotaVO {
  payeeId?: number
  name?: string
  idCardMasked?: string
  capAmount?: number
  issuedAmount?: number
  pendingAmount?: number
  redOffsetAmount?: number
  usedAmount?: number
  remainingAmount?: number
  windowStart?: number // 毫秒时间戳（yudao 全局把 LocalDateTime 按毫秒序列化）
  windowEnd?: number
  amountAtOnePercent?: number
  amountAtThreePercent?: number
  otherAmount?: number
  monthlyExemptAmount?: number
  currentMonthAmount?: number
  currentMonthOverExempt?: boolean
  quotaExceeded?: boolean
  message?: string
  months?: SellerQuotaMonthVO[]
}

/** 「这笔金额还能不能开」的额度结论 */
export interface SellerQuotaCheckVO {
  passed?: boolean
  capAmount?: number
  usedAmount?: number
  remainingAmount?: number
  remainingAfterAmount?: number
  quotaExceeded?: boolean
  message?: string
  remedy?: string
  currentMonthAmount?: number
  monthlyExemptAmount?: number
  monthlyOverExempt?: boolean
}

/** 额度超限后的经营主体登记引导记录 */
export interface SellerQuotaGuidanceVO {
  id?: number
  payeeId?: number
  sellerName?: string
  idCardMasked?: string
  triggerScene?: string
  triggerSceneName?: string
  triggerBizNo?: string
  usedAmount?: number
  capAmount?: number
  status?: number
  statusName?: string
  nextAction?: string
  triggeredAt?: number
  lastTriggeredAt?: number
  handledAt?: number
  handleRemark?: string
  remark?: string
}

/** 额度台账（#12：500 万滚动额度与经营主体登记引导） */
export const SellerQuotaApi = {
  /** 获得出售者额度台账（跨租户合并） */
  getSellerQuota: async (payeeId: number) => {
    return await request.get<SellerQuotaVO>({ url: `/icbc/quota/seller`, params: { payeeId } })
  },
  /** 判定这笔金额是否还在 500 万额度内 */
  checkQuota: async (payeeId: number, amount?: number) => {
    return await request.get<SellerQuotaCheckVO>({ url: `/icbc/quota/check`, params: { payeeId, amount } })
  },
  /** 分页获得额度超限的经营主体登记引导记录 */
  getGuidancePage: async (params: any) => {
    return await request.get({ url: `/icbc/quota/guidance/page`, params })
  },
  /** 处理一条引导记录（已引导 / 已办结） */
  handleGuidance: async (data: { id: number; status: number; handleRemark?: string }) => {
    return await request.put({ url: `/icbc/quota/guidance/handle`, data })
  }
}
