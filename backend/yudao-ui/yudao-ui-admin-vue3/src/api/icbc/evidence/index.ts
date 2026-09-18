import request from '@/config/axios'

export interface EvidenceSourceVO {
  sourceType?: string
  title?: string
  ref?: string
  url?: string
  downloadId?: number
  fileType?: string
  occurredTime?: Date
}

export interface EvidenceFlowVO {
  flow?: string
  flowName?: string
  present?: boolean
  sources?: EvidenceSourceVO[]
}

export interface EvidenceAttachmentVO {
  id?: number
  flow?: string
  flowName?: string
  evidenceType?: string
  evidenceTypeName?: string
  title?: string
  fileUrl?: string
  fileName?: string
  occurredTime?: Date
  remark?: string
  createTime?: Date
}

export interface EvidenceChainVO {
  partnerOrderId?: string
  orderNo?: string
  invoiceNo?: string
  sellerName?: string
  totalAmount?: number
  presentCount?: number
  totalCount?: number
  completenessRate?: number
  complete?: boolean
  flows?: EvidenceFlowVO[]
  attachments?: EvidenceAttachmentVO[]
  updateTime?: Date
}

export interface EvidenceTypeVO {
  code?: string
  name?: string
  flow?: string
  flowName?: string
}

export interface EvidenceCompletenessItemVO {
  partnerOrderId?: string
  invoiceNo?: string
  presentCount?: number
  totalCount?: number
  completenessRate?: number
  missingFlows?: string[]
}

export interface EvidenceCompletenessSummaryVO {
  invoiceCount?: number
  completeCount?: number
  completenessRate?: number
  items?: EvidenceCompletenessItemVO[]
}

export interface AcquisitionLedgerVO {
  tradeTime?: Date
  tradeAddress?: string
  sellerName?: string
  sellerMobile?: string
  productName?: string
  specification?: string
  quantity?: number
  unit?: string
  unitPrice?: number
  amount?: number
  invoiceNo?: string
  partnerOrderId?: string
}

// 一票一档证据链 API
export const EvidenceApi = {
  get: async (partnerOrderId: string) => {
    return await request.get<EvidenceChainVO>({
      url: `/icbc/evidence/get`,
      params: { partnerOrderId }
    })
  },
  page: async (params: any) => {
    return await request.get({ url: `/icbc/evidence/page`, params })
  },
  completeness: async (params: any) => {
    return await request.get({ url: `/icbc/evidence/completeness`, params })
  },
  ledger: async (params: any) => {
    return await request.get<AcquisitionLedgerVO[]>({ url: `/icbc/evidence/ledger`, params })
  },
  types: async () => {
    return await request.get<EvidenceTypeVO[]>({ url: `/icbc/evidence/types` })
  },
  attach: async (data: any) => {
    return await request.post({ url: `/icbc/evidence/attach`, data })
  },
  remove: async (id: number) => {
    return await request.delete({ url: `/icbc/evidence/delete`, params: { id } })
  },
  exportPackage: async (partnerOrderId: string) => {
    return await request.download({
      url: `/icbc/evidence/export`,
      params: { partnerOrderId }
    })
  },
  exportBatch: async (data: { partnerOrderIds?: string[]; createTime?: number[] }) => {
    return await request.download({
      url: `/icbc/evidence/export-batch`,
      method: 'post',
      data
    })
  },
  exportLedger: async (params: { startTime?: string; endTime?: string }) => {
    return await request.download({ url: `/icbc/evidence/ledger/export`, params })
  }
}
