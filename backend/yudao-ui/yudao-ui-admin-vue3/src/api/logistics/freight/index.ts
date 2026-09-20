import request from '@/config/axios'

// 承运商运费与对账（#75 V8）。
// 这里的「运费」是回收企业向**承运商**支付的运输服务费用，是另一笔账，不改变收购单金额与发票金额；
// 与收购单调整项里的「运费」（出售者货款上的加减项）不是同一个东西（CONTEXT.md「运费」）。
export interface LogisticsFreightVO {
  id?: number
  freightNo?: string
  taskId?: number
  taskNo?: string
  carrierId?: number
  carrierName?: string
  contractId?: number
  contractNo?: string
  billingMode?: number
  billingModeName?: string
  billQuantity?: number
  billUnitPrice?: number
  baseAmount?: number
  surchargeAmount?: number
  expectedAmount?: number
  actualAmount?: number
  varianceAmount?: number
  varianceReason?: string
  status?: number // 0-待确认应付，1-已确认应付，2-已登记付款凭证
  statusName?: string
  confirmByName?: string
  confirmTime?: Date
  confirmRemark?: string
  paymentVoucherNo?: string
  paymentVoucherUrl?: string
  paymentAmount?: number
  paidAt?: Date
  paymentRemark?: string
  remark?: string
  createTime?: Date
}

export interface LogisticsFreightReconciliationVO {
  carrierId?: number
  carrierName?: string
  contractId?: number
  contractNo?: string
  tripCount?: number
  expectedTotal?: number
  actualTotal?: number
  varianceTotal?: number
  pendingConfirmCount?: number
  voucherRegisteredCount?: number
}

export const LogisticsFreightApi = {
  getFreightPage: async (params: any) => await request.get({ url: `/logistics/freight/page`, params }),
  getFreight: async (id: number) => await request.get({ url: `/logistics/freight/get?id=` + id }),
  getFreightByTask: async (taskId: number) =>
    await request.get({ url: `/logistics/freight/get-by-task?taskId=` + taskId }),
  /** 按趟次汇集运费（这一趟必须是承运商的车，自有车建不出来） */
  createFreight: async (data: any) => await request.post({ url: `/logistics/freight/create`, data }),
  updateFreight: async (data: any) => await request.put({ url: `/logistics/freight/update`, data }),
  /** 确认应付（实际与应有有差异必须填原因，差异不抹平） */
  confirmFreight: async (data: any) => await request.put({ url: `/logistics/freight/confirm`, data }),
  /** 登记外部付款凭证（不接对公付款通道） */
  payFreight: async (data: any) => await request.put({ url: `/logistics/freight/pay`, data }),
  getReconciliation: async (params: any) =>
    await request.get({ url: `/logistics/freight/reconciliation`, params }),
  exportFreight: async (params: any) => {
    return await request.download({ url: `/logistics/freight/export-excel`, params })
  },
  exportReconciliation: async (params: any) => {
    return await request.download({ url: `/logistics/freight/reconciliation/export-excel`, params })
  }
}
