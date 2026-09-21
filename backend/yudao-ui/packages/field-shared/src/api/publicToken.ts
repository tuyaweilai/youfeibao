import { post } from '@/utils/request'

export interface PublicTokenVO {
  token?: string
  purpose?: string
  purposeName?: string
  businessKey?: string
  expiresTime?: number
  maxUses?: number
}

/** 签发一次性公开令牌（给没有账号的自然人用） */
export const createPublicToken = (data: { purpose: string; payeeId?: number; partnerOrderId?: string }) =>
  post<PublicTokenVO>('/icbc/public-token/create', data)

/** 作废一枚公开令牌：链接立刻失效，收货员可重新生成（#94） */
export const revokePublicToken = (token: string) =>
  post<boolean>('/icbc/public-token/revoke', { token })
