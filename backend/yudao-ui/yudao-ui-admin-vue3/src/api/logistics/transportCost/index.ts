import request from '@/config/axios'

// 运输费用（内部成本，#75 V8）：自有车的路桥 / 燃油等，按**实际承担方**记。
// 它不是承运商运费（那是付给承运商的应付，走运费单），也不是收购单里的调整项。
export interface LogisticsTransportCostVO {
  id?: number
  taskId?: number
  taskNo?: string
  costType?: number // 1-路桥费，2-燃油费，3-其他
  costTypeName?: string
  name?: string
  amount?: number
  bearer?: number // 1-承运商承担，2-本企业承担
  bearerName?: string
  occurDate?: Date
  remark?: string
  createTime?: Date
}

export const LogisticsTransportCostApi = {
  getTransportCostPage: async (params: any) =>
    await request.get({ url: `/logistics/transport-cost/page`, params }),
  getTransportCost: async (id: number) =>
    await request.get({ url: `/logistics/transport-cost/get?id=` + id }),
  getTransportCostListByTask: async (taskId: number) =>
    await request.get({ url: `/logistics/transport-cost/list-by-task?taskId=` + taskId }),
  createTransportCost: async (data: LogisticsTransportCostVO) =>
    await request.post({ url: `/logistics/transport-cost/create`, data }),
  updateTransportCost: async (data: LogisticsTransportCostVO) =>
    await request.put({ url: `/logistics/transport-cost/update`, data }),
  deleteTransportCost: async (id: number) =>
    await request.delete({ url: `/logistics/transport-cost/delete?id=` + id }),
  exportTransportCost: async (params: any) => {
    return await request.download({ url: `/logistics/transport-cost/export-excel`, params })
  }
}
