import request from '@/config/axios'

// 品类与税收分类编码配置
export interface GoodsConfigVO {
  id?: number
  name?: string
  unit?: string
  taxRate?: number
  taxMethod?: string // SIMPLE-简易计税，GENERAL-一般计税
  mergedCode?: string
  status?: number // 0-启用，1-停用
  remark?: string
  createTime?: Date
}

export const GoodsConfigApi = {
  getGoodsConfigPage: async (params: any) =>
    await request.get({ url: `/icbc/goods-config/page`, params }),
  getGoodsConfig: async (id: number) =>
    await request.get({ url: `/icbc/goods-config/get?id=` + id }),
  createGoodsConfig: async (data: GoodsConfigVO) =>
    await request.post({ url: `/icbc/goods-config/create`, data }),
  updateGoodsConfig: async (data: GoodsConfigVO) =>
    await request.put({ url: `/icbc/goods-config/update`, data }),
  deleteGoodsConfig: async (id: number) =>
    await request.delete({ url: `/icbc/goods-config/delete?id=` + id }),
  getEnabledList: async () => await request.get({ url: `/icbc/goods-config/enabled-list` })
}
