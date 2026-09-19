import request from '@/config/axios'

// 平台运营：计费计量（#16）
export interface BillingLedgerVO {
  id?: number
  tenantId?: number
  periodMonth?: string
  issuedCount?: number
  reversedCount?: number
  billableCount?: number
  unitPrice?: number
  amount?: number
  generatedTime?: Date
}

export const PlatformBillingApi = {
  getPage: async (params: any) =>
    await request.get({ url: `/icbc/platform/billing/page`, params }),
  // 重新计量某期间全部租户并落台账（幂等覆盖）
  generate: async (periodMonth: string) =>
    await request.post({ url: `/icbc/platform/billing/generate`, params: { periodMonth } }),
  // 重新计量某个租户某期间
  generateTenant: async (tenantId: number, periodMonth: string) =>
    await request.post({
      url: `/icbc/platform/billing/generate-tenant`,
      params: { tenantId, periodMonth }
    })
}
