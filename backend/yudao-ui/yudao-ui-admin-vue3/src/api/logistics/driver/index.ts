import request from '@/config/axios'

// 司机档案（#77 V2a）：司机是回收企业建档的租户内账号（自有与承运商同构），
// userId 指向租户内的系统用户，司机用它登录司机端。
export interface LogisticsDriverVO {
  id?: number
  userId?: number
  name?: string
  mobile?: string
  source?: number // 1-自有，2-承运商
  carrierId?: number // 来源为承运商时必填
  drivingLicenseNo?: string
  drivingLicenseType?: string // 准驾车型
  drivingLicenseExpiryDate?: string // 过期不得派出；可授权放行
  qualificationCertNo?: string
  qualificationCertExpiryDate?: string // 同上
  status?: number // 0-在职，1-离职，2-请假
  sourceName?: string
  statusName?: string
  remark?: string
  createTime?: Date
}

export const LogisticsDriverApi = {
  getDriverPage: async (params: any) =>
    await request.get({ url: `/logistics/driver/page`, params }),
  getDriver: async (id: number) => await request.get({ url: `/logistics/driver/get?id=` + id }),
  createDriver: async (data: LogisticsDriverVO) =>
    await request.post({ url: `/logistics/driver/create`, data }),
  updateDriver: async (data: LogisticsDriverVO) =>
    await request.put({ url: `/logistics/driver/update`, data }),
  deleteDriver: async (id: number) =>
    await request.delete({ url: `/logistics/driver/delete?id=` + id }),
  /** 导出司机 Excel（返回 Blob，页面交给 download.excel） */
  exportDriver: async (params: any) => {
    return await request.download({ url: `/logistics/driver/export-excel`, params })
  }
}
