import request from '@/config/axios'

// 工作台待办与开票就绪徽标（#56 T18）
// 口径写在每一项的 definition 里，界面上与数字一起显示；来源明细最多 10 条。
export interface WorkbenchItemVO {
  id?: number
  no?: string
  title?: string
  subtitle?: string
  statusName?: string
  time?: number
  amount?: number
}

export interface WorkbenchTodoVO {
  code?: string
  name?: string
  total?: number
  // false 表示数据源尚未上线（见 unavailableReason），此时 total 恒为 0，不伪造数字
  available?: boolean
  unavailableReason?: string
  definition?: string
  items?: WorkbenchItemVO[]
}

export interface WorkbenchWarningVO {
  code?: string
  name?: string
  level?: string // OK / WARN / DANGER
  count?: number
  message?: string
  items?: WorkbenchItemVO[]
}

export interface WorkbenchReadinessItemVO {
  code?: string
  name?: string
  ready?: boolean
  message?: string
}

export interface WorkbenchReadinessVO {
  ready?: boolean
  items?: WorkbenchReadinessItemVO[]
}

export interface WorkbenchOverviewVO {
  todos?: WorkbenchTodoVO[]
  warnings?: WorkbenchWarningVO[]
  readiness?: WorkbenchReadinessVO
}

export const WorkbenchApi = {
  // 工作台一屏：八类待办 + 三条预警 + 开票就绪徽标
  getOverview: async () => {
    return await request.get<WorkbenchOverviewVO>({ url: `/icbc/workbench/overview` })
  }
}
