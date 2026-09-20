import request from '@/config/axios'

// ERP 批次 VO
export interface StockBatchVO {
  id?: number // 批次编号
  batchNo?: string // 批次号
  goodsConfigId?: number // 品类编号
  inTime?: number // 入库时间（毫秒时间戳）
  remark?: string // 备注
  status?: number // 开启状态
  createTime?: Date // 创建时间
}

// ERP 批次 API
export const StockBatchApi = {
  // 查询批次分页
  getStockBatchPage: async (params: any) => {
    return await request.get({ url: `/erp/stock-batch/page`, params })
  },

  // 查询批次精简列表
  getStockBatchSimpleList: async () => {
    return await request.get({ url: `/erp/stock-batch/simple-list` })
  },

  // 查询批次详情
  getStockBatch: async (id: number) => {
    return await request.get({ url: `/erp/stock-batch/get?id=` + id })
  },

  // 新增批次
  createStockBatch: async (data: StockBatchVO) => {
    return await request.post({ url: `/erp/stock-batch/create`, data })
  },

  // 修改批次
  updateStockBatch: async (data: StockBatchVO) => {
    return await request.put({ url: `/erp/stock-batch/update`, data })
  },

  // 删除批次
  deleteStockBatch: async (id: number) => {
    return await request.delete({ url: `/erp/stock-batch/delete?id=` + id })
  },

  // 导出批次 Excel
  exportStockBatch: async (params) => {
    return await request.download({ url: `/erp/stock-batch/export-excel`, params })
  }
}
