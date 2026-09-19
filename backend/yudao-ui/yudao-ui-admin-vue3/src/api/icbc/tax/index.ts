import request from '@/config/axios'

/** 代办税费申报明细（#13） */
export interface TaxDeclarationItemVO {
  id?: number
  declarationId?: number
  periodMonth?: string
  payeeId?: number
  sellerName?: string
  idCardMasked?: string
  invoiceCount?: number
  salesAmount?: number
  amountAtOnePercent?: number
  amountAtThreePercent?: number
  otherAmount?: number
  crossTenantMonthAmount?: number
  vatExempt?: boolean
  overExempt?: boolean
  vatAmount?: number
  surchargeAmount?: number
  iitAmount?: number
  totalTaxAmount?: number
  paidAmount?: number
  status?: number
  statusName?: string
  paidAt?: number
  remark?: string
}

/** 代办税费申报单：本租户 × 本申报月（#13） */
export interface TaxDeclarationVO {
  id?: number
  declarationNo?: string
  periodMonth?: string
  declarationDeadline?: string
  daysLeft?: number
  overdue?: boolean
  status?: number
  statusName?: string
  nextAction?: string
  dataReady?: boolean
  missingDataCount?: number
  sellerCount?: number
  overExemptSellerCount?: number
  totalSalesAmount?: number
  amountAtOnePercent?: number
  amountAtThreePercent?: number
  otherAmount?: number
  vatAmount?: number
  surchargeAmount?: number
  iitAmount?: number
  totalTaxAmount?: number
  paidAmount?: number
  invoiceCount?: number
  declaredAt?: number
  declaredBy?: string
  declaredRemark?: string
  paidAt?: number
  paymentMethod?: string
  voucherNo?: string
  voucherFileUrl?: string
  message?: string
  items?: TaxDeclarationItemVO[]
}

/** 申报缺项 */
export interface TaxMissingDataVO {
  type?: string
  typeName?: string
  message?: string
  count?: number
  remedy?: string
  samples?: string[]
}

/** 申报数据齐备性 */
export interface TaxDeclarationPrecheckVO {
  periodMonth?: string
  ready?: boolean
  sellerCount?: number
  invoiceCount?: number
  missingCount?: number
  missing?: TaxMissingDataVO[]
  message?: string
}

/** 申报预警 */
export interface TaxDeclarationWarningVO {
  type?: string
  typeName?: string
  declarationId?: number
  periodMonth?: string
  declarationDeadline?: string
  daysLeft?: number
  overdue?: boolean
  totalTaxAmount?: number
  suspensionRisk?: boolean
  missingDataCount?: number
  message?: string
  nextAction?: string
}

/** 需补缴税费 */
export interface TaxSupplementVO {
  id?: number
  supplementNo?: string
  declarationId?: number
  periodMonth?: string
  payeeId?: number
  sellerName?: string
  reason?: string
  amountAtOnePercent?: number
  amountAtThreePercent?: number
  amount?: number
  status?: number
  statusName?: string
  nextAction?: string
  paidAmount?: number
  paidAt?: number
  voucherNo?: string
  voucherFileUrl?: string
  remark?: string
}

/** 待补缴累计（按 1% 与 3% 分列） */
export interface TaxSupplementSummaryVO {
  pendingCount?: number
  pendingAmount?: number
  pendingAmountAtOnePercent?: number
  pendingAmountAtThreePercent?: number
  message?: string
}

/** 汇算清缴提醒 */
export interface SettlementReminderVO {
  id?: number
  payeeId?: number
  sellerName?: string
  idCardMasked?: string
  taxYear?: number
  deadline?: string
  daysLeft?: number
  overdue?: boolean
  invoiceCount?: number
  invoicedAmount?: number
  paidTaxAmount?: number
  iitAmount?: number
  status?: number
  statusName?: string
  nextAction?: string
  remindedAt?: number
  handleRemark?: string
  remark?: string
}

/** 出售者汇算清缴对账单 */
export interface SellerSettlementStatementVO {
  payeeId?: number
  sellerName?: string
  idCardMasked?: string
  taxYear?: number
  deadline?: string
  daysLeft?: number
  overdue?: boolean
  invoiceCount?: number
  invoicedAmount?: number
  paidTaxAmount?: number
  iitAmount?: number
  months?: {
    month?: string
    invoiceCount?: number
    invoicedAmount?: number
    paidTaxAmount?: number
    iitAmount?: number
  }[]
  message?: string
  generatedAt?: number
}

/** 代办税费申报（#13） */
export const TaxDeclarationApi = {
  getDeclaration: async (periodMonth: string) => {
    return await request.get<TaxDeclarationVO>({ url: `/icbc/tax-declaration/get`, params: { periodMonth } })
  },
  getDeclarationPage: async (params: any) => {
    return await request.get({ url: `/icbc/tax-declaration/page`, params })
  },
  getItemPage: async (params: any) => {
    return await request.get({ url: `/icbc/tax-declaration/item/page`, params })
  },
  precheck: async (periodMonth: string) => {
    return await request.get<TaxDeclarationPrecheckVO>({
      url: `/icbc/tax-declaration/precheck`,
      params: { periodMonth }
    })
  },
  getWarnings: async () => {
    return await request.get<TaxDeclarationWarningVO[]>({ url: `/icbc/tax-declaration/warning/list` })
  },
  generate: async (periodMonth: string) => {
    return await request.post<TaxDeclarationVO>({
      url: `/icbc/tax-declaration/generate`,
      params: { periodMonth }
    })
  },
  declare: async (data: { periodMonth: string; declaredBy?: string; remark?: string }) => {
    return await request.post<TaxDeclarationVO>({ url: `/icbc/tax-declaration/declare`, data })
  },
  pay: async (data: {
    periodMonth: string
    paidAmount?: number
    paidAt?: number
    paymentMethod?: string
    voucherNo?: string
    voucherFileUrl?: string
    remark?: string
  }) => {
    return await request.post<TaxDeclarationVO>({ url: `/icbc/tax-declaration/pay`, data })
  }
}

/** 需补缴税费（#13） */
export const TaxSupplementApi = {
  create: async (data: any) => {
    return await request.post({ url: `/icbc/tax-supplement/create`, data })
  },
  getPage: async (params: any) => {
    return await request.get({ url: `/icbc/tax-supplement/page`, params })
  },
  getSummary: async () => {
    return await request.get<TaxSupplementSummaryVO>({ url: `/icbc/tax-supplement/summary` })
  },
  pay: async (data: any) => {
    return await request.post({ url: `/icbc/tax-supplement/pay`, data })
  }
}

/** 出售者汇算清缴（#13）。路径与 #33 的结算单（/icbc/settlement）分开，避免路由冲突。 */
export const SettlementApi = {
  getPage: async (params: any) => {
    return await request.get({ url: `/icbc/settlement-reminder/page`, params })
  },
  getStatement: async (payeeId: number, taxYear?: number) => {
    return await request.get<SellerSettlementStatementVO>({
      url: `/icbc/settlement-reminder/statement`,
      params: { payeeId, taxYear }
    })
  },
  remind: async (taxYear?: number) => {
    return await request.post({ url: `/icbc/settlement-reminder/remind`, params: { taxYear } })
  },
  handle: async (data: { id: number; status: number; handleRemark?: string }) => {
    return await request.post({ url: `/icbc/settlement-reminder/handle`, data })
  }
}
