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
  /** 物流侧交接登记编号（上门提货时非空；回场复磅时按现场交接登记建批次） */
  logisticsHandoverId?: number
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
  driverId?: number
  vehicleId?: number
  /** COMPLETE-已齐，PENDING-待补档（缺身份证或银行卡，付款与开票被门禁拦住） */
  documentStatus?: string
  documentStatusName?: string
  documentGap?: string
  /** 现场参考量（不是计量事实：计量取有效磅次） */
  referenceQuantity?: number
  /** 现场参考单价（生成收购单时的单价默认值，修正要留原因） */
  referenceUnitPrice?: number
  /** 现场凭证照片（仅详情返回） */
  referencePhotos?: string[]
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

/** 待回场复磅的现场交接登记（磅房按它建批次；现场参考量与照片凭证都在这里） */
export interface HandoverIntakeCandidateVO {
  logisticsHandoverId?: number
  handoverNo?: string
  taskId?: number
  taskNo?: string
  stopId?: number
  payeeId?: number
  payeeName?: string
  payeeMobile?: string
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  referenceQuantity?: number
  referenceUnitPrice?: number
  photos?: string[]
  address?: string
  plateNo?: string
  driverName?: string
  occurTime?: number
  documentStatus?: string
  documentStatusName?: string
  documentGap?: string
}

export interface HandoverIntakeReqVO {
  logisticsHandoverId: number
  stationId: number
  grossWeight: number
  tareWeight: number
  weighTime?: number
  weightTicketNo?: string
  weightTicketImageUrl?: string
  plateNo?: string
  remark?: string
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
    await request.post({ url: `/icbc/handover-batch/weighing/effective`, data }),
  /** 待回场复磅的现场交接登记（物流读取面；已建过批次的会被过滤掉） */
  getPendingIntakeList: async () =>
    await request.get({ url: `/icbc/handover-batch/pending-intake-list` }),
  /** 按现场交接登记回场复磅：建批次 + 落第一次磅次（同一现场交接登记幂等） */
  intakeFromHandover: async (data: HandoverIntakeReqVO) =>
    await request.post({ url: `/icbc/handover-batch/intake`, data })
}
