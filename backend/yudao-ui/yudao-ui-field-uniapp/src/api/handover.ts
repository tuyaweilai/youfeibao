import { get, post } from '@/utils/request'

/**
 * 交接批次与有效磅次（#50 T12）。
 *
 * <p>一个交易对方的一次物理交接记为一个交接批次；过磅保留每一次原始读数，只有被选定的那一次
 * 参与计量，其余留档不参与。同一车同一天两次送货是两个批次，磅单与收购单各归各。
 * 没有预约、没有采购订单也能建批次。
 */
export interface WeighingVO {
  id?: number
  batchId?: number
  seqNo?: number
  grossWeight?: number
  tareWeight?: number
  netWeight?: number
  weightTicketNo?: string
  weightTicketImageUrl?: string
  plateNo?: string
  effective?: boolean
  effectiveText?: string
  remark?: string
}

export interface HandoverBatchVO {
  id?: number
  batchNo?: string
  payeeId?: number
  sellerName?: string
  sellerMobile?: string
  stationId?: number
  stationName?: string
  visitAddress?: string
  occurTime?: number
  sourceType?: string
  sourceTypeName?: string
  driverName?: string
  driverMobile?: string
  plateNo?: string
  acquisitionCount?: number
  weighingChangeLocked?: boolean
  effectiveWeighingId?: number
  effectiveWeighingSeqNo?: number
  weighingList?: WeighingVO[]
}

export interface HandoverBatchCreateReq {
  payeeId: number
  stationId?: number
  visitAddress?: string
  occurTime?: number
  sourceType?: string
  driverName?: string
  driverMobile?: string
  plateNo: string
  remark?: string
}

/** 来源方式：与后端 HandoverSourceTypeEnum 一一对应 */
export const SOURCE_TYPES = [
  { label: '预约到站', value: 'APPOINTMENT' },
  { label: '直接到场', value: 'WALK_IN' },
  { label: '上门回收', value: 'ON_SITE' }
]

export const createHandoverBatch = (data: HandoverBatchCreateReq) =>
  post<number>('/icbc/handover-batch/create', data)

export const getHandoverBatch = (id: number) => get<HandoverBatchVO>('/icbc/handover-batch/get', { id })

export const getHandoverBatchPage = (params: { pageNo?: number; pageSize?: number; plateNo?: string }) =>
  get<{ list: HandoverBatchVO[]; total: number }>('/icbc/handover-batch/page', {
    pageNo: 1,
    pageSize: 20,
    ...params
  })

/** 本批次全部磅次（含留档不参与的那些） */
export const getWeighings = (batchId: number) =>
  get<WeighingVO[]>('/icbc/handover-batch/weighings', { batchId })

export const addWeighing = (data: {
  batchId: number
  grossWeight: number
  tareWeight: number
  weightTicketNo?: string
  plateNo?: string
  remark?: string
}) => post<number>('/icbc/handover-batch/weighing/add', data)

/** 指定哪一次磅次参与计量；该批次已产生收购单时后端会拒绝 */
export const selectEffectiveWeighing = (batchId: number, weighingId: number, reason?: string) =>
  post<boolean>('/icbc/handover-batch/weighing/effective', { batchId, weighingId, reason })
