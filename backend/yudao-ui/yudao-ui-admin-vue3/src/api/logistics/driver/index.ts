import request from '@/config/axios'

// 司机档案（#77 V2a）：司机是回收企业建档的租户内账号（自有与承运商同构），
// userId 指向租户内的系统用户，司机用它登录司机端。
export interface LogisticsDriverVO {
  id?: number
  userId?: number
  name?: string
  mobile?: string
  source?: number // 1-自有，2-承运商
  status?: number // 0-在职，1-离职，2-请假
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
    await request.delete({ url: `/logistics/driver/delete?id=` + id })
}
