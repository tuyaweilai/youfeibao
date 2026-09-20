import request from '@/config/axios'

// 运输任务（#78 V2b；#71 V4 五类节点 + 异常 + 改派；#72 V5 多停靠点集货）：一车 + 一司机 + 一次执行。
// 一次集货不构成把几个出售者合并结算的依据：每个停靠点各自交接、各自复磅、各自结算（ADR 0031）。
export interface LogisticsTransportNodeVO {
  id?: number
  taskId?: number
  taskNo?: string
  stopId?: number // 提货相关节点非空；到达场站/卸货完成为整趟收尾，为空（V5 #72）
  nodeType?: number // 1-到达提货点，2-交接完成，3-起运，4-到达场站，5-卸货完成；空=异常事件
  nodeTypeName?: string
  nodeTime?: Date
  reportTime?: Date
  location?: string
  latitude?: number
  longitude?: number
  photos?: string[]
  operatorId?: number
  operatorName?: string
  // 异常是独立标记，不是节点类型也不是任务状态（V4 #71）
  abnormalType?: number // 1-车辆故障，2-交通事故，3-天气延误，4-道路封闭，5-货物损坏，6-对方不在，7-地址错误，8-其他
  abnormalTypeName?: string
  abnormalReason?: string
  abnormalResolved?: boolean
  abnormalResolvedAt?: Date
  abnormalResolvedName?: string
  abnormalResolvedRemark?: string
  remark?: string
}

export interface LogisticsTransportTaskReassignVO {
  id?: number
  taskId?: number
  prevVehicleId?: number
  prevPlateNo?: string
  prevDriverId?: number
  prevDriverName?: string
  prevDriverMobile?: string
  vehicleId?: number
  plateNo?: string
  driverId?: number
  driverName?: string
  driverMobile?: string
  reason?: string
  operatorId?: number
  operatorName?: string
  reassignTime?: Date
}

// 停靠点保存（建任务时一起带 / 事后追加）
export interface LogisticsTransportStopSaveVO {
  taskId?: number
  stopType?: number // 1-提货，2-送货
  payeeId?: number
  payeeName?: string
  payeeMobile?: string
  address?: string
  contactName?: string
  contactPhone?: string
  cargoName?: string
  estimatedQuantity?: number
  quantityUnit?: string
  expectedArrivalTime?: Date
  remark?: string
}

// 停靠点（带各自的节点、进度与断点）
export interface LogisticsTransportStopVO extends LogisticsTransportStopSaveVO {
  id?: number
  taskNo?: string
  stopNo?: number
  stopTypeName?: string
  status?: number // 0-待处理，1-进行中，2-已完成，3-已取消
  statusName?: string
  cancelReason?: string
  cancelTime?: Date
  nodes?: LogisticsTransportNodeVO[]
  missingNodeNames?: string[]
  missingEvidenceNames?: string[]
}

export interface LogisticsTransportTaskVO {
  id?: number
  taskNo?: string
  status?: number // 0-待分配，1-已分配，2-已接单，3-执行中，4-已完成，5-已取消
  statusName?: string
  vehicleId?: number
  plateNo?: string
  driverId?: number
  driverName?: string
  driverMobile?: string
  departureAddress?: string
  pickupAddress?: string
  pickupContactName?: string
  pickupContactPhone?: string
  expectedStartTime?: Date
  expectedEndTime?: Date
  purchaseOrderId?: number
  purchaseOrderNo?: string
  cargoName?: string
  estimatedQuantity?: number
  quantityUnit?: string
  assignTime?: Date
  acceptTime?: Date
  startTime?: Date
  completeTime?: Date
  cancelTime?: Date
  cancelReason?: string
  remark?: string
  overrideReason?: string // 授权放行原因（证件过期时管理员带着原因放行，正常派车为空）
  overrideBy?: number
  overrideTime?: Date
  createTime?: Date
  nodes?: LogisticsTransportNodeVO[]
  missingNodeNames?: string[]
  missingEvidenceNames?: string[]
  reassigns?: LogisticsTransportTaskReassignVO[]
  stops?: LogisticsTransportStopVO[]
  pendingStopCount?: number
  scopeNote?: string
}

// 建任务时可一次带多个停靠点（保存态，不是带进度的响应态）
export type LogisticsTransportTaskCreateVO = Omit<
  LogisticsTransportTaskVO,
  'stops' | 'nodes' | 'reassigns' | 'pendingStopCount' | 'scopeNote' | 'missingNodeNames' | 'missingEvidenceNames'
> & { stops?: LogisticsTransportStopSaveVO[] }

export const LogisticsTransportTaskApi = {
  getTaskPage: async (params: any) =>
    await request.get({ url: `/logistics/transport-task/page`, params }),
  getTask: async (id: number) =>
    await request.get({ url: `/logistics/transport-task/get?id=` + id }),
  createTask: async (data: LogisticsTransportTaskCreateVO) =>
    await request.post({ url: `/logistics/transport-task/create`, data }),
  updateTask: async (data: LogisticsTransportTaskVO) =>
    await request.put({ url: `/logistics/transport-task/update`, data }),
  assignTask: async (data: { id: number; vehicleId: number; driverId: number }) =>
    await request.put({ url: `/logistics/transport-task/assign`, data }),
  /**
   * 授权放行派车：证件过期时由管理员带着原因放行（留痕原因/授权人/时间）。
   * 车辆维修中、司机离职这类硬门禁不可绕过——后端会明确回「不能授权放行」。
   */
  assignTaskWithOverride: async (data: {
    id: number
    vehicleId: number
    driverId: number
    overrideReason: string
  }) => await request.put({ url: `/logistics/transport-task/assign-override`, data }),
  /**
   * 改派：换车换人并保留承接关系（原车原人 → 新车新人 + 原因）。
   * 只有已分配 / 已接单 / 执行中能改派；改派不改任务状态。
   */
  reassignTask: async (data: { id: number; vehicleId: number; driverId: number; reason: string }) =>
    await request.put({ url: `/logistics/transport-task/reassign`, data }),
  acceptTask: async (id: number) =>
    await request.put({ url: `/logistics/transport-task/accept?id=` + id }),
  completeTask: async (id: number) =>
    await request.put({ url: `/logistics/transport-task/complete?id=` + id }),
  cancelTask: async (data: { id: number; cancelReason: string }) =>
    await request.put({ url: `/logistics/transport-task/cancel`, data })
}

export const LogisticsTransportStopApi = {
  /** 给任务追加一个停靠点（停靠顺序接在最后） */
  createStop: async (data: LogisticsTransportStopSaveVO) =>
    await request.post({ url: `/logistics/transport-stop/create`, data }),
  /** 取消一个停靠点：只取消这一个，其它停靠点不受影响 */
  cancelStop: async (data: { id: number; cancelReason: string }) =>
    await request.put({ url: `/logistics/transport-stop/cancel`, data }),
  /** 按任务取停靠点及其各自的进度与断点 */
  getStopListByTask: async (taskId: number) =>
    await request.get({ url: `/logistics/transport-stop/list-by-task?taskId=` + taskId })
}

export const LogisticsTransportNodeApi = {
  reportNode: async (data: {
    taskId: number
    stopId?: number
    nodeType: number
    nodeTime: Date
    location?: string
    photos?: string[]
    clientRequestId: string
    remark?: string
  }) => await request.post({ url: `/logistics/transport-node/report`, data }),
  /** 上报异常：独立标记，不改任务状态（V4 #71） */
  reportAbnormal: async (data: {
    taskId: number
    stopId?: number
    abnormalType: number
    abnormalReason: string
    nodeTime: Date
    location?: string
    photos?: string[]
    clientRequestId: string
    remark?: string
  }) => await request.post({ url: `/logistics/transport-node/abnormal/report`, data }),
  /** 解决异常：记录谁 / 什么时候 / 怎么解决的 */
  resolveAbnormal: async (data: { id: number; resolveRemark?: string }) =>
    await request.put({ url: `/logistics/transport-node/abnormal/resolve`, data }),
  getListByTask: async (taskId: number) =>
    await request.get({ url: `/logistics/transport-node/list-by-task?taskId=` + taskId })
}
