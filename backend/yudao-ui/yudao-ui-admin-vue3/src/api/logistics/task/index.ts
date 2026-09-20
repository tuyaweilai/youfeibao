import request from '@/config/axios'

// 运输任务（#78 V2b）：一车 + 一司机 + 一次执行；节点是过程事实与货物流凭证。
// 本票只开放「起运」一类节点的上报，其余四类与异常归 #71。
export interface LogisticsTransportNodeVO {
  id?: number
  taskId?: number
  taskNo?: string
  nodeType?: number // 1-到达提货点，2-交接完成，3-起运，4-到达场站，5-卸货完成
  nodeTypeName?: string
  nodeTime?: Date
  reportTime?: Date
  location?: string
  latitude?: number
  longitude?: number
  photos?: string[]
  operatorId?: number
  operatorName?: string
  remark?: string
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
  createTime?: Date
  nodes?: LogisticsTransportNodeVO[]
  missingNodeNames?: string[]
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
    clientRequestId: string
    remark?: string
  }) => await request.post({ url: `/logistics/transport-node/report`, data }),
  getListByTask: async (taskId: number) =>
    await request.get({ url: `/logistics/transport-node/list-by-task?taskId=` + taskId })
}
