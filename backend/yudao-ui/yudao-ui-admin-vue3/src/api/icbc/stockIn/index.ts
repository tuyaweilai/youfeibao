import request from '@/config/axios'

/**
 * 待入库 → 入库单 → 库存流水（#52 T14，ADR 0027）。
 *
 * <p>入库是收购单派生的单向动作：验收后的货进待入库，仓管选仓库 / 库位 / 批次确认实际入库量。
 * 只有过账的入库才增加正式库存；重复确认幂等，累计入库不得超过可入库实物量。
 */
export interface StockInItemVO {
  id?: number
  stockInId?: number
  warehouseId?: number
  locationId?: number
  batchId?: number
  quantity?: number
  remark?: string
}

export interface StockInVO {
  id?: number
  stockInNo?: string
  acquisitionId?: number
  acquisitionNo?: string
  payeeId?: number
  sellerName?: string
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  availableQuantity?: number
  totalQuantity?: number
  status?: number
  statusName?: string
  /** 毫秒时间戳（yudao 全局 Jackson 按毫秒序列化 LocalDateTime） */
  postedTime?: number
  cancelReason?: string
  cancelledTime?: number
  remark?: string
  createTime?: number
  items?: StockInItemVO[]
}

export interface StockInPendingVO {
  acquisitionId?: number
  acquisitionNo?: string
  payeeId?: number
  sellerName?: string
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  /** 毫秒时间戳 */
  tradeTime?: number
  netWeight?: number
  availableQuantity?: number
  stockedQuantity?: number
  remainingQuantity?: number
}

/** 入库单状态：与后端 StockInStatusEnum 一一对应 */
export const STOCK_IN_STATUS_ENUM = {
  PENDING: 0,
  POSTED: 1,
  CANCELLED: 2
}

export const STOCK_IN_STATUS_OPTIONS = [
  { label: '待过账', value: STOCK_IN_STATUS_ENUM.PENDING },
  { label: '已过账', value: STOCK_IN_STATUS_ENUM.POSTED },
  { label: '已作废', value: STOCK_IN_STATUS_ENUM.CANCELLED }
]

export const StockInApi = {
  /** 待入库分页 */
  getPendingPage: async (params: any) =>
    await request.get({ url: `/icbc/stock-in/pending/page`, params }),
  /** 建入库单（待过账，不动库存） */
  createStockIn: async (data: any) => await request.post({ url: `/icbc/stock-in/create`, data }),
  /** 确认入库（建单并过账） */
  confirmStockIn: async (data: any) => await request.post({ url: `/icbc/stock-in/confirm`, data }),
  /** 过账入库单 */
  postStockIn: async (id: number) =>
    await request.post({ url: `/icbc/stock-in/post?id=` + id }),
  /** 作废入库单 */
  cancelStockIn: async (data: { id: number; reason: string }) =>
    await request.post({ url: `/icbc/stock-in/cancel`, data }),
  /** 入库单详情（含明细） */
  getStockIn: async (id: number) => await request.get({ url: `/icbc/stock-in/get?id=` + id }),
  /** 入库单分页 */
  getStockInPage: async (params: any) => await request.get({ url: `/icbc/stock-in/page`, params })
}
