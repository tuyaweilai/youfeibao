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

export interface PurchaseOrderProgressVO {
  orderId?: number
  orderNo?: string
  statusName?: string
  totalQuantity?: number
  receivedQuantity?: number
  remainingQuantity?: number
  totalAmount?: number
  scopeNote?: string
  items?: PurchaseOrderProgressItemVO[]
}

export interface PurchaseOrderProgressItemVO {
  itemId?: number
  categoryName?: string
  unit?: string
  quantity?: number
  receivedQuantity?: number
  remainingQuantity?: number
  dealCount?: number
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
