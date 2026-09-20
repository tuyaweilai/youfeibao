import request from '@/config/axios'

// 车辆档案（#77 V2a）：派车时从这里选车，车牌租户内唯一。
// 「运输中」由运输任务驱动，不接受在档案上手工设置（保存时会被后端拦下）。
export interface LogisticsVehicleVO {
  id?: number
  plateNo?: string
  vehicleType?: string
  capacityTon?: number
  drivingLicenseExpiryDate?: string // 行驶证到期日（过期不得派出；可授权放行）
  insuranceExpiryDate?: string // 保险到期日（同上）
  photos?: string[]
  gpsDeviceId?: string
  status?: number // 0-可用，1-运输中，2-维护中
  statusName?: string
  remark?: string
  createTime?: Date
}

export const LogisticsVehicleApi = {
  getVehiclePage: async (params: any) =>
    await request.get({ url: `/logistics/vehicle/page`, params }),
  getVehicle: async (id: number) => await request.get({ url: `/logistics/vehicle/get?id=` + id }),
  createVehicle: async (data: LogisticsVehicleVO) =>
    await request.post({ url: `/logistics/vehicle/create`, data }),
  updateVehicle: async (data: LogisticsVehicleVO) =>
    await request.put({ url: `/logistics/vehicle/update`, data }),
  deleteVehicle: async (id: number) =>
    await request.delete({ url: `/logistics/vehicle/delete?id=` + id }),
  /** 导出车辆 Excel（返回 Blob，页面交给 download.excel） */
  exportVehicle: async (params: any) => {
    return await request.download({ url: `/logistics/vehicle/export-excel`, params })
  }
}
