import request from '@/config/axios'

// ERP 品类库存 VO
export interface StockVO {
  // 编号
  id: number
  // 品类编号（icbc_goods_config.id）
  goodsConfigId: number
  // 仓库编号
  warehouseId: number
  // 库位编号（0 表示未指定）
  locationId: number
  // 批次编号（0 表示未指定）
  batchId: number
  // 库存数量
  count: number
  // 仓库名称
  warehouseName: string
  // 库位名称
  locationName: string
  // 批次号
  batchNo: string
}

// ERP 品类库存 API
export const StockApi = {
  // 查询品类库存分页
  getStockPage: async (params: any) => {
    return await request.get({ url: `/erp/stock/page`, params })
  },

  // 查询品类库存详情
  getStock: async (id: number) => {
    return await request.get({ url: `/erp/stock/get?id=` + id })
  },

  // 查询品类库存详情（按品类 + 仓库 + 库位 + 批次）
  getStock2: async (goodsConfigId: number, warehouseId: number, locationId?: number, batchId?: number) => {
    return await request.get({
      url: `/erp/stock/get`,
      params: { goodsConfigId, warehouseId, locationId, batchId }
    })
  },

  // 获得品类库存数量
  getStockCount: async (goodsConfigId: number) => {
    return await request.get({ url: `/erp/stock/get-count`, params: { goodsConfigId } })
  },

  // 导出品类库存 Excel
  exportStock: async (params) => {
    return await request.download({ url: `/erp/stock/export-excel`, params })
  }
}
