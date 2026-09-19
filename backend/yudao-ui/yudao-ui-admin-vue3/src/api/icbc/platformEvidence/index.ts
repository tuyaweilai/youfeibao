import request from '@/config/axios'

// 平台运营：全平台五流齐备率与异常票（#15）
export interface PlatformCompletenessItemVO {
  tenantId?: number
  partnerOrderId?: string
  invoiceNo?: string
  presentCount?: number
  totalCount?: number
  completenessRate?: number
  missingFlows?: string[]
}

export interface PlatformCompletenessVO {
  invoiceCount: number
  completeCount: number
  completenessRate: number
  items: PlatformCompletenessItemVO[]
}

export interface PlatformExceptionInvoiceVO {
  tenantId?: number
  partnerOrderId?: string
  orderNo?: string
  invoiceNo?: string
  invoiceStatusName?: string
  taxStatusName?: string
  uploadStatusName?: string
  paymentStatusName?: string
  completenessRate?: number
  missingFlows?: string[]
  reasons: string[]
  createTime?: Date
}

export const PlatformEvidenceApi = {
  getCompleteness: async () => await request.get({ url: `/icbc/platform/evidence/completeness` }),
  getExceptionList: async () =>
    await request.get({ url: `/icbc/platform/evidence/exception-invoice/list` })
}
