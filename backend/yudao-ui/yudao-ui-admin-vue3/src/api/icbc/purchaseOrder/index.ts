import request from '@/config/axios'

// 采购订单（#46 T08，ADR 0027）：采购执行依据，一个合同 → 多个订单 → 多次收货。
// 与「到站预约」不是一回事：预约是交易对方声明的到场计划，订单是回收企业内部的采购计划。

// 状态：0-草稿，1-执行中，2-暂停，3-完成，4-关闭
export const PURCHASE_ORDER_STATUS_ENUM = {
  DRAFT: 0,
  EXECUTING: 1,
  SUSPENDED: 2,
  COMPLETED: 3,
  CLOSED: 4
} as const

export const PURCHASE_ORDER_STATUS_OPTIONS = [
  { value: PURCHASE_ORDER_STATUS_ENUM.DRAFT, label: '草稿' },
  { value: PURCHASE_ORDER_STATUS_ENUM.EXECUTING, label: '执行中' },
  { value: PURCHASE_ORDER_STATUS_ENUM.SUSPENDED, label: '暂停' },
  { value: PURCHASE_ORDER_STATUS_ENUM.COMPLETED, label: '完成' },
  { value: PURCHASE_ORDER_STATUS_ENUM.CLOSED, label: '关闭' }
]

// 定价方式：1-固定单价，2-按交货日价格表
export const PURCHASE_ORDER_PRICE_MODE_OPTIONS = [
  { value: 1, label: '固定单价' },
  { value: 2, label: '按交货日价格表' }
]

// 交易对方主体类型六态（与 ERP 的 SellerSubjectTypeEnum 同值）：判定规则是「是否属于自然人」。
export const PURCHASE_ORDER_COUNTERPARTY_TYPE_OPTIONS = [
  { value: 1, label: '自然人出售者' },
  { value: 2, label: '个体工商户' },
  { value: 3, label: '个人独资企业' },
  { value: 4, label: '合伙企业' },
  { value: 5, label: '企业法人' },
  { value: 6, label: '农民专业合作社' }
]

// 完成比例采用的履约口径（#47 T09）：只列能取到数的口径，入库口径等 #52 落地后才可选
export const PURCHASE_PERFORMANCE_BASIS_OPTIONS = [
  { value: 'ACCEPTED', label: '验收口径' },
  { value: 'SETTLED', label: '结算口径' }
]

// 履约异常的处理方式（#47 T09）
export const PURCHASE_DELIVERY_RULE_OPTIONS = [
  { value: 'BLOCK', label: '拦截' },
  { value: 'APPROVAL', label: '提交授权审核' }
]

// 履约异常类型与授权单状态（#47 T09）
export const PURCHASE_EXCEPTION_TYPE_OPTIONS = [
  { value: 'OVER_QUANTITY', label: '超量交货' },
  { value: 'EXPIRED', label: '过期交货' },
  { value: 'CROSS_STATION', label: '跨场站交货' }
]

export const PURCHASE_EXCEPTION_STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已拒绝' }
]

export const PURCHASE_ORDER_NATURAL_COUNTERPARTY_TYPE = 1

export interface PurchaseOrderPriceVO {
  id?: number
  itemId?: number
  deliveryDate?: string
  unitPrice?: number
  remark?: string
}

export interface PurchaseOrderItemVO {
  id?: number
  orderId?: number
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  quantity?: number
  priceMode?: number
  priceModeName?: string
  unitPrice?: number
  amount?: number
  receivedQuantity?: number
  remainingQuantity?: number
  dealCount?: number
  remark?: string
  prices?: PurchaseOrderPriceVO[]
}

export interface PurchaseOrderDealVO {
  id?: number
  orderId?: number
  itemId?: number
  categoryName?: string
  dealNo?: string
  dealTime?: number
  deliveryDate?: string
  quantity?: number
  unitPrice?: number
  referenceUnitPrice?: number
  priceAdjusted?: boolean
  adjustReason?: string
  sourceType?: string
  sourceId?: number
  sourceNo?: string
  remark?: string
}

export interface PurchaseOrderProgressMeasureVO {
  code?: string
  name?: string
  quantity?: number
  definition?: string
  source?: string
  available?: boolean
  unavailableReason?: string
}

export interface PurchaseOrderProgressAnomalyVO {
  code?: string
  name?: string
  message?: string
  severity?: string
}

export interface PurchaseOrderProgressVO {
  orderId?: number
  orderNo?: string
  statusName?: string
  totalQuantity?: number
  totalAmount?: number
  // 五口径（#47 T09）：各有各的来源，不相加
  planQuantity?: number
  acceptedQuantity?: number
  stockedQuantity?: number
  settledQuantity?: number
  unperformedQuantity?: number
  // 完成比例采用哪个口径，必须标明
  completionBasis?: string
  completionBasisName?: string
  completionBasisDefinition?: string
  completionBasisQuantity?: number
  completionRatio?: number
  measures?: PurchaseOrderProgressMeasureVO[]
  anomalies?: PurchaseOrderProgressAnomalyVO[]
  scopeNote?: string
  items?: PurchaseOrderProgressItemVO[]
}

export interface PurchaseOrderProgressItemVO {
  itemId?: number
  categoryName?: string
  unit?: string
  quantity?: number
  acceptedQuantity?: number
  stockedQuantity?: number
  settledQuantity?: number
  unperformedQuantity?: number
  overQuantity?: boolean
  dealCount?: number
}

export interface PurchaseOrderSettingVO {
  performanceBasis?: string
  performanceBasisName?: string
  performanceBasisDefinition?: string
  overQuantityRule?: string
  overQuantityRuleName?: string
  expiredRule?: string
  expiredRuleName?: string
  crossStationRule?: string
  crossStationRuleName?: string
  remark?: string
  scopeNote?: string
}

export interface PurchaseOrderExceptionVO {
  id?: number
  exceptionNo?: string
  orderId?: number
  orderNo?: string
  itemId?: number
  categoryName?: string
  exceptionType?: string
  exceptionTypeName?: string
  exceptionTypeDefinition?: string
  stationId?: number
  stationName?: string
  requestedQuantity?: number
  approvedQuantity?: number
  validUntil?: string
  reason?: string
  status?: number
  statusName?: string
  requestedBy?: number
  requestedTime?: number
  reviewedBy?: number
  reviewedTime?: number
  reviewRemark?: string
  effective?: boolean
  scopeNote?: string
}

export interface PurchaseOrderDeliveryCheckVO {
  orderId?: number
  orderNo?: string
  itemId?: number
  quantity?: number
  allowed?: boolean
  resolution?: string
  resolutionName?: string
  scopeNote?: string
  violations?: {
    exceptionType?: string
    exceptionTypeName?: string
    message?: string
    rule?: string
    ruleName?: string
    resolved?: boolean
    resolvedByExceptionId?: number
    resolvedByExceptionNo?: string
    pendingExceptionId?: number
    overageQuantity?: number
  }[]
}

export interface PurchaseOrderVO {
  id?: number
  orderNo?: string
  contractId?: number
  contractNo?: string
  counterpartyType?: number
  counterpartyTypeName?: string
  payeeId?: number
  supplierId?: number
  counterpartyName?: string
  stationId?: number
  stationName?: string
  startDate?: string
  endDate?: string
  status?: number
  statusName?: string
  expired?: boolean
  usableAsPurchaseBasis?: boolean
  totalQuantity?: number
  totalAmount?: number
  suspendReason?: string
  suspendedTime?: number
  completedTime?: number
  closeReason?: string
  remark?: string
  items?: PurchaseOrderItemVO[]
  createTime?: number
  updateTime?: number
}

export const PurchaseOrderApi = {
  getPurchaseOrderPage: async (params: any) =>
    await request.get({ url: `/icbc/purchase-order/page`, params }),
  getPurchaseOrder: async (id: number) =>
    await request.get({ url: `/icbc/purchase-order/get?id=` + id }),
  createPurchaseOrder: async (data: PurchaseOrderVO) =>
    await request.post({ url: `/icbc/purchase-order/create`, data }),
  updatePurchaseOrder: async (data: PurchaseOrderVO) =>
    await request.put({ url: `/icbc/purchase-order/update`, data }),
  updatePurchaseOrderStatus: async (data: { id: number; status: number; reason?: string }) =>
    await request.post({ url: `/icbc/purchase-order/update-status`, data }),
  deletePurchaseOrder: async (id: number) =>
    await request.delete({ url: `/icbc/purchase-order/delete?id=` + id }),
  getProgress: async (id: number) =>
    await request.get({ url: `/icbc/purchase-order/progress?id=` + id }),
  deliveryCheck: async (data: { orderId: number; itemId?: number; quantity?: number; stationId?: number }) =>
    await request.post({ url: `/icbc/purchase-order/delivery-check`, data }),
  getSetting: async () => await request.get({ url: `/icbc/purchase-order/setting` }),
  updateSetting: async (data: PurchaseOrderSettingVO) =>
    await request.put({ url: `/icbc/purchase-order/setting/update`, data }),
  getExceptionPage: async (params: any) =>
    await request.get({ url: `/icbc/purchase-order-exception/page`, params }),
  requestException: async (data: PurchaseOrderExceptionVO) =>
    await request.post({ url: `/icbc/purchase-order-exception/request`, data }),
  reviewException: async (data: {
    id: number
    approved: boolean
    approvedQuantity?: number
    validUntil?: string
    reviewRemark?: string
  }) => await request.post({ url: `/icbc/purchase-order-exception/review`, data }),
  getDealList: async (orderId: number) =>
    await request.get({ url: `/icbc/purchase-order/deal/list?orderId=` + orderId }),
  createDeal: async (data: PurchaseOrderDealVO) =>
    await request.post({ url: `/icbc/purchase-order/deal/create`, data }),
  resolvePrice: async (itemId: number, deliveryDate?: string) =>
    await request.get({
      url: `/icbc/purchase-order/resolve-price?itemId=` + itemId
        + (deliveryDate ? '&deliveryDate=' + deliveryDate : '')
    })
}
