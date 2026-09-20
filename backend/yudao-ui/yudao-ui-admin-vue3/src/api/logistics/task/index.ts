import request from '@/config/axios'

// 运输任务（#78 V2b）：一车 + 一司机 + 一次执行；节点是过程事实与货物流凭证。
// 本票只开放「起运」一类节点的上报，其余四类与异常归 #71。
export interface LogisticsTransportNodeVO {
  id?: number
  taskId?: number
  taskNo?: string
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
}

export const LogisticsTransportTaskApi = {
  getTaskPage: async (params: any) =>
    await request.get({ url: `/logistics/transport-task/page`, params }),
  getTask: async (id: number) =>
    await request.get({ url: `/logistics/transport-task/get?id=` + id }),
  createTask: async (data: LogisticsTransportTaskVO) =>
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

export const LogisticsTransportNodeApi = {
  reportNode: async (data: {
    taskId: number
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
