import request from '@/config/axios'

/**
 * 库存作业（#54 T16，ADR 0025 / 0027）。
 *
 * 非销售出库 / 跨仓调拨 / 盘点调整 / 期初导入四类，库存只经 ERP 的 stock 域写
 * （后端走 StockApi），过账才动库存，作废按相反方向冲销。
 */

/** 库存作业单据状态：与后端 StockOpsStatusEnum 一一对应 */
export const STOCK_OPS_STATUS_ENUM = {
  PENDING: 0,
  POSTED: 1,
  CANCELLED: 2
}

export const STOCK_OPS_STATUS_OPTIONS = [
  { label: '待过账', value: STOCK_OPS_STATUS_ENUM.PENDING },
  { label: '已过账', value: STOCK_OPS_STATUS_ENUM.POSTED },
  { label: '已作废', value: STOCK_OPS_STATUS_ENUM.CANCELLED }
]

/** 非销售出库类型：与后端 StockOutTypeEnum 一一对应 */
export const STOCK_OUT_TYPE_ENUM = {
  SCRAP: 10,
  RETURN: 20,
  INTERNAL_USE: 30
}

export const STOCK_OUT_TYPE_OPTIONS = [
  { label: '报损', value: STOCK_OUT_TYPE_ENUM.SCRAP },
  { label: '退货出库', value: STOCK_OUT_TYPE_ENUM.RETURN },
  { label: '内部领用', value: STOCK_OUT_TYPE_ENUM.INTERNAL_USE }
]

/** 库存维度（仓库 / 库位 / 批次）明细：四类单据共用 */
export interface StockOpsItemVO {
  id?: number
  goodsConfigId?: number
  warehouseId?: number
  locationId?: number
  batchId?: number
  quantity?: number
  actualQuantity?: number
  bookQuantity?: number
  differenceQuantity?: number
  fromWarehouseId?: number
  fromLocationId?: number
  fromBatchId?: number
  toWarehouseId?: number
  toLocationId?: number
  toBatchId?: number
  remark?: string
}

/** 非销售出库单 */
export interface StockOutVO {
  id?: number
  stockOutNo?: string
  outType?: number
  outTypeName?: string
  totalQuantity?: number
  status?: number
  statusName?: string
  postedTime?: number
  cancelReason?: string
  cancelledTime?: number
  remark?: string
  createTime?: number
  items?: StockOpsItemVO[]
}

/** 跨仓调拨单 */
export interface StockMoveVO {
  id?: number
  moveNo?: string
  totalQuantity?: number
  status?: number
  statusName?: string
  postedTime?: number
  cancelReason?: string
  cancelledTime?: number
  remark?: string
  createTime?: number
  items?: StockOpsItemVO[]
}

/** 盘点单 */
export interface StockCheckVO {
  id?: number
  checkNo?: string
  status?: number
  statusName?: string
  postedTime?: number
  cancelReason?: string
  cancelledTime?: number
  remark?: string
  createTime?: number
  items?: StockOpsItemVO[]
}

/** 期初记录 */
export interface StockOpeningVO {
  id?: number
  openingNo?: string
  goodsConfigId?: number
  warehouseId?: number
  locationId?: number
  batchId?: number
  quantity?: number
  status?: number
  statusName?: string
  postedTime?: number
  cancelReason?: string
  cancelledTime?: number
  remark?: string
  createTime?: number
}

/** 「当前库存」口径就绪 */
export interface StockOpsReadinessVO {
  openingSupported?: boolean
  outboundSupported?: boolean
  moveSupported?: boolean
  checkSupported?: boolean
  openingImported?: boolean
  capabilitiesReady?: boolean
  currentStockReady?: boolean
  label?: string
  missingItems?: string[]
  notice?: string
}

export const StockOutApi = {
  create: async (data: any) => await request.post({ url: `/icbc/stock-out/create`, data }),
  confirm: async (data: any) => await request.post({ url: `/icbc/stock-out/confirm`, data }),
  post: async (id: number) => await request.post({ url: `/icbc/stock-out/post?id=` + id }),
  cancel: async (data: { id: number; reason: string }) =>
    await request.post({ url: `/icbc/stock-out/cancel`, data }),
  get: async (id: number) => await request.get({ url: `/icbc/stock-out/get?id=` + id }),
  getPage: async (params: any) => await request.get({ url: `/icbc/stock-out/page`, params })
}

export const StockMoveApi = {
  create: async (data: any) => await request.post({ url: `/icbc/stock-move/create`, data }),
  confirm: async (data: any) => await request.post({ url: `/icbc/stock-move/confirm`, data }),
  post: async (id: number) => await request.post({ url: `/icbc/stock-move/post?id=` + id }),
  cancel: async (data: { id: number; reason: string }) =>
    await request.post({ url: `/icbc/stock-move/cancel`, data }),
  get: async (id: number) => await request.get({ url: `/icbc/stock-move/get?id=` + id }),
  getPage: async (params: any) => await request.get({ url: `/icbc/stock-move/page`, params })
}

export const StockCheckApi = {
  create: async (data: any) => await request.post({ url: `/icbc/stock-check/create`, data }),
  confirm: async (data: any) => await request.post({ url: `/icbc/stock-check/confirm`, data }),
  post: async (id: number) => await request.post({ url: `/icbc/stock-check/post?id=` + id }),
  cancel: async (data: { id: number; reason: string }) =>
    await request.post({ url: `/icbc/stock-check/cancel`, data }),
  get: async (id: number) => await request.get({ url: `/icbc/stock-check/get?id=` + id }),
  getPage: async (params: any) => await request.get({ url: `/icbc/stock-check/page`, params })
}

export const StockOpeningApi = {
  importOpening: async (data: any) => await request.post({ url: `/icbc/stock-opening/import`, data }),
  cancel: async (data: { id: number; reason: string }) =>
    await request.post({ url: `/icbc/stock-opening/cancel`, data }),
  getPage: async (params: any) => await request.get({ url: `/icbc/stock-opening/page`, params })
}

export const StockOpsReadinessApi = {
  getReadiness: async () => await request.get({ url: `/icbc/stock-ops/readiness` })
}
