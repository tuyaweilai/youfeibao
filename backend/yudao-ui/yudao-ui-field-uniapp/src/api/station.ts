import { get } from '@/utils/request'

/**
 * 场站（#33 / #34）。现场端登记收购、结束本次收货时都要选场站：
 * 一次到场批次按「同出售者 + 同场站」聚合，自然人端也按场站匹配待确认。
 */
export interface StationVO {
  id?: number
  stationCode?: string
  name?: string
  address?: string
  contactMobile?: string
  /** 1-在收货，0-暂停收货 */
  openStatus?: number
}

/** 本租户场站分页（收货员有查询权限） */
export const getStationPage = (params: { pageNo?: number; pageSize?: number } = {}) =>
  get<{ list: StationVO[]; total: number }>('/icbc/station/page', { pageNo: 1, pageSize: 100, ...params })
