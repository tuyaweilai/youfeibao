import request from '@/config/axios'

// 采购合同（#45 / T07，ADR 0027）：采购条款，审核通过前不得作为采购依据。
// 与自然人出售者的「框架收购协议」（开票前置）是两件事，不合并。

// 状态：0-草稿，1-待审核，2-生效，3-关闭；4-过期（由「已生效 + 有效期止早于今天」推导，不落库）
export const PURCHASE_CONTRACT_STATUS_ENUM = {
  DRAFT: 0,
  PENDING_AUDIT: 1,
  EFFECTIVE: 2,
  CLOSED: 3,
  EXPIRED: 4
} as const

export const PURCHASE_CONTRACT_STATUS_OPTIONS = [
  { value: PURCHASE_CONTRACT_STATUS_ENUM.DRAFT, label: '草稿' },
  { value: PURCHASE_CONTRACT_STATUS_ENUM.PENDING_AUDIT, label: '待审核' },
  { value: PURCHASE_CONTRACT_STATUS_ENUM.EFFECTIVE, label: '生效' },
  { value: PURCHASE_CONTRACT_STATUS_ENUM.CLOSED, label: '关闭' }
]

// 交易对方主体类型六态（与 ERP 的 SellerSubjectTypeEnum 同值）：判定规则是「是否属于自然人」。
export const CONTRACT_COUNTERPARTY_TYPE_OPTIONS = [
  { value: 1, label: '自然人出售者' },
  { value: 2, label: '个体工商户' },
  { value: 3, label: '个人独资企业' },
  { value: 4, label: '合伙企业' },
  { value: 5, label: '企业法人' },
  { value: 6, label: '农民专业合作社' }
]

export const CONTRACT_NATURAL_COUNTERPARTY_TYPE = 1

export interface PurchaseContractCategoryVO {
  goodsConfigId?: number
  categoryName?: string
  unit?: string
}

export interface PurchaseContractVersionVO {
  versionNo?: number
  changeReason?: string
  changedBy?: string
  auditStatus?: number // 0-待审核，1-已通过，2-已驳回
  auditStatusName?: string
  auditedBy?: number
  auditedTime?: Date
  auditRemark?: string
  snapshotHash?: string
  createTime?: Date
}

export interface PurchaseContractVO {
  id?: number
  contractNo?: string
  name?: string
  counterpartyType?: number
  counterpartyTypeName?: string
  payeeId?: number
  supplierId?: number
  counterpartyName?: string
  startDate?: string
  endDate?: string
  quantityAgreement?: string
  measureStandard?: string
  qualityStandard?: string
  priceRule?: string
  transportResponsibility?: string
  paymentTerms?: string
  attachmentUrls?: string
  status?: number
  statusName?: string
  expired?: boolean
  usableAsPurchaseBasis?: boolean
  versionNo?: number
  submittedBy?: number
  submittedTime?: Date
  auditedBy?: number
  auditedTime?: Date
  auditRemark?: string
  closedBy?: number
  closedTime?: Date
  closeReason?: string
  remark?: string
  categoryIds?: number[]
  categories?: PurchaseContractCategoryVO[]
  versions?: PurchaseContractVersionVO[]
  createTime?: Date
  updateTime?: Date
}

export const PurchaseContractApi = {
  getPurchaseContractPage: async (params: any) =>
    await request.get({ url: `/icbc/purchase-contract/page`, params }),
  getPurchaseContract: async (id: number) =>
    await request.get({ url: `/icbc/purchase-contract/get?id=` + id }),
  createPurchaseContract: async (data: PurchaseContractVO) =>
    await request.post({ url: `/icbc/purchase-contract/create`, data }),
  updatePurchaseContract: async (data: PurchaseContractVO) =>
    await request.put({ url: `/icbc/purchase-contract/update`, data }),
  submitPurchaseContract: async (data: { id: number; changeReason?: string }) =>
    await request.post({ url: `/icbc/purchase-contract/submit`, data }),
  auditPurchaseContract: async (data: { id: number; approved: boolean; remark?: string }) =>
    await request.post({ url: `/icbc/purchase-contract/audit`, data }),
  closePurchaseContract: async (data: { id: number; reason?: string }) =>
    await request.post({ url: `/icbc/purchase-contract/close`, data }),
  deletePurchaseContract: async (id: number) =>
    await request.delete({ url: `/icbc/purchase-contract/delete?id=` + id })
}
