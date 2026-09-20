import { get, post } from '@/utils/request'

/**
 * 司机端交接登记（V6 #73）。
 *
 * 司机在提货点就**一个停靠点**登记的现场交接事实：品类、参考量、参考单价与凭证照片。
 *
 * **这里没有金额**：现场不产生金额（ADR 0031）——结算重量回场复磅才定稿，
 * 收购单那时才由 icbc 侧按这份登记生成。参考量 / 参考单价是现场约定值，会被复磅结果与
 * 收货员的修正覆盖（修正必须留原因）。
 *
 * 缺身份证或银行卡时记「待补档」：事实照记，付款与开票被门禁拦住，补档后放行。
 */
export interface DriverHandoverVO {
  id?: number
  handoverNo?: string
  taskId?: number
  taskNo?: string
  stopId?: number
  address?: string
  payeeId?: number
  payeeName?: string
  payeeMobile?: string
  goodsConfigId?: number
  categoryName?: string
  unit?: string
  referenceQuantity?: number
  referenceUnitPrice?: number
  photos?: string[]
  driverId?: number
  driverName?: string
  vehicleId?: number
  plateNo?: string
  occurTime?: number
  /** COMPLETE-已齐，PENDING-待补档 */
  documentStatus?: string
  documentStatusName?: string
  documentGap?: string
  remark?: string
}

export interface DriverHandoverCreateReq {
  taskId: number
  stopId?: number
  payeeId: number
  payeeName?: string
  payeeMobile?: string
  goodsConfigId: number
  categoryName?: string
  unit?: string
  referenceQuantity: number
  referenceUnitPrice?: number
  photos: string[]
  documentStatus?: string
  documentGap?: string
  occurTime?: number
  clientRequestId?: string
  remark?: string
}

/** 登记交接（带 clientRequestId 时幂等，弱网重复提交返回同一条） */
export const createHandover = (data: DriverHandoverCreateReq) =>
  post<number>('/logistics/driver-app/handover/create', data)

/** 我这趟任务上的交接登记（集货时一家一条） */
export const getMyHandoverList = (taskId: number) =>
  get<DriverHandoverVO[]>('/logistics/driver-app/handover/list', { taskId })

/** 品类配置（只读）：交接登记的品类是权威品类（ADR 0028），不能用自由文本 */
export interface GoodsConfigVO {
  id: number
  name: string
  unit?: string
  taxRate?: number
  taxMethod?: string
}

export const getEnabledGoodsConfigs = () =>
  get<GoodsConfigVO[]>('/icbc/goods-config/enabled-list')
