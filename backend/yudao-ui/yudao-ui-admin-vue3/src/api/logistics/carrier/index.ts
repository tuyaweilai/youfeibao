import request from '@/config/axios'

// 承运商档案（#70 V3）：运力吃紧时找的第三方公司。停用而不是删除——
// 历史任务上的司机与运费要留着。承运合同与运价归 V8。
export interface LogisticsCarrierVO {
  id?: number
  name?: string
  contactName?: string
  contactMobile?: string
  status?: number // 0-合作中，1-已停用
  statusName?: string
  remark?: string
  createTime?: Date
}

export const LogisticsCarrierApi = {
  getCarrierPage: async (params: any) =>
    await request.get({ url: `/logistics/carrier/page`, params }),
  getCarrier: async (id: number) => await request.get({ url: `/logistics/carrier/get?id=` + id }),
  createCarrier: async (data: LogisticsCarrierVO) =>
    await request.post({ url: `/logistics/carrier/create`, data }),
  updateCarrier: async (data: LogisticsCarrierVO) =>
    await request.put({ url: `/logistics/carrier/update`, data }),
  deleteCarrier: async (id: number) =>
    await request.delete({ url: `/logistics/carrier/delete?id=` + id }),
  /** 导出承运商 Excel（返回 Blob，页面交给 download.excel） */
  exportCarrier: async (params: any) => {
    return await request.download({ url: `/logistics/carrier/export-excel`, params })
  }
}
