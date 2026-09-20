import request from '@/config/axios'

/**
 * 交接批次与有效磅次（#50 T12）。
 *
 * <p>一个交易对方的一次物理交接记为一个交接批次；过磅保留每一次原始读数，只有被选定的那一次
 * 参与计量，其余留档不参与。同一车同一天两次送货是两个批次，不做去重。
 */
export interface HandoverWeighingVO {
  id?: number
  batchId?: number
  seqNo?: number
  grossWeight?: number
  tareWeight?: number
  netWeight?: number
  /** 毫秒时间戳（yudao 全局 Jackson 按毫秒序列化 LocalDateTime） */
  weighTime?: number
  weightTicketNo?: string
  weightTicketImageUrl?: string
  plateNo?: string
  effective?: boolean
  effectiveText?: string
  remark?: string
  createTime?: number
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
  /** 毫秒时间戳 */
  occurTime?: number
  sourceType?: string
  sourceTypeName?: string
  driverName?: string
  driverMobile?: string
  plateNo?: string
  appointmentId?: number
  purchaseOrderId?: number
  acquisitionCount?: number
  weighingChangeLocked?: boolean
  effectiveWeighingId?: number
  effectiveWeighingSeqNo?: number
  weighingList?: HandoverWeighingVO[]
  remark?: string
  createTime?: number
}

/** 来源方式：与后端 HandoverSourceTypeEnum 一一对应 */
export const HANDOVER_SOURCE_TYPES = [
  { label: '预约到站', value: 'APPOINTMENT' },
  { label: '直接到场', value: 'WALK_IN' },
  { label: '上门回收', value: 'ON_SITE' }
]

export const HandoverBatchApi = {
  getHandoverBatchPage: async (params: any) =>
    await request.get({ url: `/icbc/handover-batch/page`, params }),
  getHandoverBatch: async (id: number) =>
    await request.get({ url: `/icbc/handover-batch/get?id=` + id }),
  createHandoverBatch: async (data: HandoverBatchVO) =>
    await request.post({ url: `/icbc/handover-batch/create`, data }),
  updateHandoverBatch: async (data: HandoverBatchVO) =>
    await request.put({ url: `/icbc/handover-batch/update`, data }),
  getWeighings: async (batchId: number) =>
    await request.get({ url: `/icbc/handover-batch/weighings?batchId=` + batchId }),
  addWeighing: async (data: HandoverWeighingVO) =>
    await request.post({ url: `/icbc/handover-batch/weighing/add`, data }),
  selectEffectiveWeighing: async (data: { batchId: number; weighingId: number; reason?: string }) =>
    await request.post({ url: `/icbc/handover-batch/weighing/effective`, data })
}
