import request from '@/config/axios'

// 电子签章（#92，ADR 0036）：章是租户级的，回收企业才是发起方，平台不代盖。

/** 租户侧：本企业电子签章状态 */
export interface EsignTenantStatusVO {
  subCustomerNo?: string
  activationStatus?: number // 0-未开通，1-认证中，2-已激活
  activationStatusName?: string
  nextStep?: string
  operatorNo?: string
  sealNo?: string
  sealReady?: boolean
  contractQuota?: number
  contractUsed?: number
  remainingQuota?: number
  activatedTime?: Date
  platformConfigured?: boolean
}

/** 租户侧：一次性控制台链接 */
export interface EsignOpenConsoleVO {
  subCustomerNo?: string
  link?: string
  expiresTime?: Date
  activationStatus?: number
  activationStatusName?: string
  nextStep?: string
}

/** 平台级参数（密钥只回「已配置」与否，不回明文） */
export interface EsignConfigVO {
  environment?: string
  apiEndpoint?: string
  consoleEndpoint?: string
  appId?: string
  secretId?: string
  secretKey?: string
  secretIdConfigured?: boolean
  secretKeyConfigured?: boolean
  callbackUrl?: string
  callbackSignKey?: string
  callbackSignKeyConfigured?: boolean
  signLinkChannel?: string
  agreementTemplateId?: string
  noticeTemplateId?: string
  remark?: string
  configured?: boolean
  missingFields?: string[]
}

/** 平台运营：某个租户的电子签章状态与额度 */
export interface PlatformEsignTenantVO {
  tenantId?: number
  tenantName?: string
  subCustomerNo?: string
  activationStatus?: number
  activationStatusName?: string
  sealNo?: string
  sealReady?: boolean
  contractQuota?: number
  contractUsed?: number
  remainingQuota?: number
  activatedTime?: Date
}

export const EsignApi = {
  // 本企业电子签章状态与合同额度
  getStatus: async () => await request.get({ url: `/icbc/esign/status` }),
  // 开通：取一枚一次性控制台链接
  openConsole: async () => await request.post({ url: `/icbc/esign/open` }),
  // 确认已激活（企业认证通过 + 企业印章已创建）
  activate: async (data: { operatorNo?: string; sealNo: string; remark?: string }) =>
    await request.post({ url: `/icbc/esign/activate`, data })
}

export const PlatformEsignApi = {
  getConfig: async () => await request.get({ url: `/icbc/platform/esign/config` }),
  saveConfig: async (data: EsignConfigVO) =>
    await request.put({ url: `/icbc/platform/esign/config`, data }),
  listTenants: async () => await request.get({ url: `/icbc/platform/esign/tenant/list` }),
  updateQuota: async (data: { tenantId: number; contractQuota: number; remark?: string }) =>
    await request.put({ url: `/icbc/platform/esign/quota`, data })
}
