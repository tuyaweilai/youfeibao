import request from '@/config/axios'

// 场站与场站二维码（#34）：一码一场站，码内不带令牌，只编码场站码
export interface StationVO {
  id?: number
  stationCode?: string
  name?: string
  address?: string
  contactMobile?: string
  openStatus?: number // 1-在收货，0-暂停收货
  remark?: string
  entryUrl?: string
  createTime?: Date
}

export const StationApi = {
  getStationPage: async (params: any) => await request.get({ url: `/icbc/station/page`, params }),
  getStation: async (id: number) => await request.get({ url: `/icbc/station/get?id=` + id }),
  createStation: async (data: StationVO) => await request.post({ url: `/icbc/station/create`, data }),
  updateStation: async (data: StationVO) => await request.put({ url: `/icbc/station/update`, data }),
  deleteStation: async (id: number) => await request.delete({ url: `/icbc/station/delete?id=` + id })
}
