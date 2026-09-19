import { get } from '@/utils/request'

export interface GoodsConfigVO {
  id?: number
  name?: string
  unit?: string
  taxRate?: number
  /** SIMPLE-简易计税，GENERAL-一般计税 */
  taxMethod?: string
  mergedCode?: string
  status?: number
}

/** 启用中的品类（现场端选品类后带出单位 / 税率 / 计税方法 / 税收分类编码） */
export const getEnabledGoodsList = () => get<GoodsConfigVO[]>('/icbc/goods-config/enabled-list')
