import request from '@/config/axios'

// ERP 库位 VO
export interface StockLocationVO {
  id?: number // 库位编号
  warehouseId?: number // 仓库编号
  name?: string // 库位名称
  sort?: number // 排序
  remark?: string // 备注
  status?: number // 开启状态
  createTime?: Date // 创建时间
  warehouseName?: string // 仓库名称
}

// ERP 库位 API
export const StockLocationApi = {
  // 查询库位分页
  getStockLocationPage: async (params: any) => {
    return await request.get({ url: `/erp/stock-location/page`, params })
  },

  // 查询库位精简列表（可按仓库筛选）
  getStockLocationSimpleList: async (warehouseId?: number) => {
    return await request.get({ url: `/erp/stock-location/simple-list`, params: { warehouseId } })
  },

  // 查询库位详情
  getStockLocation: async (id: number) => {
    return await request.get({ url: `/erp/stock-location/get?id=` + id })
  },

  // 新增库位
  createStockLocation: async (data: StockLocationVO) => {
    return await request.post({ url: `/erp/stock-location/create`, data })
  },

  // 修改库位
  updateStockLocation: async (data: StockLocationVO) => {
    return await request.put({ url: `/erp/stock-location/update`, data })
  },

  // 删除库位
  deleteStockLocation: async (id: number) => {
    return await request.delete({ url: `/erp/stock-location/delete?id=` + id })
  },

  // 导出库位 Excel
  exportStockLocation: async (params) => {
    return await request.download({ url: `/erp/stock-location/export-excel`, params })
  }
}
