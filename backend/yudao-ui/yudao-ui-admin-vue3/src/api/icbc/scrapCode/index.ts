import request from '@/config/axios'

// 平台级报废产品税收分类编码表（跨租户共享，平台运营维护、租户只读）
export interface ScrapCodeVO {
  id?: number
  name?: string
  mergedCode?: string
  unit?: string
  taxRate?: number
  status?: number // 0-启用，1-停用
  remark?: string
  createTime?: Date
}

export const ScrapCodeApi = {
  getScrapCodePage: async (params: any) =>
    await request.get({ url: `/icbc/scrap-code/page`, params }),
  getScrapCode: async (id: number) =>
    await request.get({ url: `/icbc/scrap-code/get?id=` + id }),
  createScrapCode: async (data: ScrapCodeVO) =>
    await request.post({ url: `/icbc/scrap-code/create`, data }),
  updateScrapCode: async (data: ScrapCodeVO) =>
    await request.put({ url: `/icbc/scrap-code/update`, data }),
  deleteScrapCode: async (id: number) =>
    await request.delete({ url: `/icbc/scrap-code/delete?id=` + id }),
  getEnabledList: async () => await request.get({ url: `/icbc/scrap-code/enabled-list` })
}
