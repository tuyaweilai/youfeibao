import request from '@/config/axios'

// 车辆档案（#77 V2a）：派车时从这里选车，车牌租户内唯一。
// 「运输中」由运输任务驱动，不接受在档案上手工设置（保存时会被后端拦下）。
export interface LogisticsVehicleVO {
  id?: number
  plateNo?: string
  vehicleType?: string
  capacityTon?: number
  status?: number // 0-可用，1-运输中，2-维护中
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
    await request.delete({ url: `/logistics/vehicle/delete?id=` + id })
}
