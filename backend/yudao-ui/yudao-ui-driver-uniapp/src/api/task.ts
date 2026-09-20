import { get, post, put } from '@/utils/request'

/**
 * 司机端接口（#79 V2c）：只回答「派给我的活」。
 *
 * 服务端按登录账号对应的司机档案强制过滤与校验归属，所以这里不需要、也不应该传司机编号。
 * 司机端**没有任何金额入口**（现场不产生金额，ADR 0031）。
 */
export interface DriverTaskVO {
  id?: number
  taskNo?: string
  status?: number // 0-待分配，1-已分配，2-已接单，3-执行中，4-已完成，5-已取消
  statusName?: string
  plateNo?: string
  driverName?: string
  driverMobile?: string
  departureAddress?: string
  pickupAddress?: string
  pickupContactName?: string
  pickupContactPhone?: string
  expectedStartTime?: number
  expectedEndTime?: number
  cargoName?: string
  estimatedQuantity?: number
  quantityUnit?: string
  assignTime?: number
  acceptTime?: number
  startTime?: number
  completeTime?: number
  cancelTime?: number
  cancelReason?: string
  remark?: string
  nodes?: DriverNodeVO[]
  missingNodeNames?: string[]
  missingEvidenceNames?: string[]
  // 多停靠点集货（V5 #72）
  stops?: DriverStopVO[]
  pendingStopCount?: number
  scopeNote?: string
}

export interface DriverStopVO {
  id?: number
  taskId?: number
  stopNo?: number
  stopType?: number // 1-提货，2-送货
  stopTypeName?: string
  payeeId?: number
  payeeName?: string
  payeeMobile?: string
  address?: string
  contactName?: string
  contactPhone?: string
  cargoName?: string
  estimatedQuantity?: number
  quantityUnit?: string
  expectedArrivalTime?: number
  status?: number // 0-待处理，1-进行中，2-已完成，3-已取消
  statusName?: string
  cancelReason?: string
  cancelTime?: number
  nodes?: DriverNodeVO[]
  missingNodeNames?: string[]
  missingEvidenceNames?: string[]
}

export interface DriverNodeVO {
  id?: number
  stopId?: number
  nodeType?: number
  nodeTypeName?: string
  nodeTime?: number
  reportTime?: number
  location?: string
  photos?: string[]
  operatorName?: string
  // 异常是独立标记（V4 #71）
  abnormalType?: number
  abnormalTypeName?: string
  abnormalReason?: string
  abnormalResolved?: boolean
  abnormalResolvedAt?: number
  abnormalResolvedName?: string
  abnormalResolvedRemark?: string
  remark?: string
}

export interface DriverProfileVO {
  driverId: number
  name: string
  mobile: string
  source: number
  sourceName: string
}

export interface NodeReportReq {
  taskId: number
  stopId?: number
  nodeType: number
  nodeTime: number
  location?: string
  latitude?: number
  longitude?: number
  photos?: string[]
  clientRequestId: string
  remark?: string
}

export interface AbnormalReportReq {
  taskId: number
  stopId?: number
  abnormalType: number
  abnormalReason: string
  nodeTime: number
  location?: string
  latitude?: number
  longitude?: number
  photos?: string[]
  clientRequestId: string
  remark?: string
}

/** 我是谁（当前登录账号对应的司机档案） */
export const getProfile = () => get<DriverProfileVO>('/logistics/driver-app/profile')

/** 派给我的任务分页 */
export const getTaskPage = (params: Record<string, any>) =>
  get<{ list: DriverTaskVO[]; total: number }>('/logistics/driver-app/task/page', params)

/** 我的一趟任务（含时间线） */
export const getTask = (id: number) => get<DriverTaskVO>('/logistics/driver-app/task/get', { id })

/** 接单（接单接口取 query 参数，不是请求体） */
export const acceptTask = (id: number) =>
  put<boolean>('/logistics/driver-app/task/accept?id=' + id)

/** 上报运输节点（幂等：同一 clientRequestId 重复提交返回既有节点编号） */
export const reportNode = (data: NodeReportReq) =>
  post<number>('/logistics/driver-app/node/report', data as unknown as Record<string, any>)

/** 上报运输异常（独立标记，不改任务状态；幂等：同一 clientRequestId 重复提交返回既有记录编号） */
export const reportAbnormal = (data: AbnormalReportReq) =>
  post<number>('/logistics/driver-app/node/abnormal/report', data as unknown as Record<string, any>)
