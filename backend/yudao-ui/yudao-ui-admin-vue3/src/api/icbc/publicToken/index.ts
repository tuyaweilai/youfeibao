import request from '@/config/axios'

export interface PublicTokenVO {
  token?: string
  purpose?: string
  purposeName?: string
  businessKey?: string
  expiresTime?: Date
  maxUses?: number
}

export interface PublicTokenCreateVO {
  purpose: string
  partnerOrderId?: string
  payeeId?: number
}

// 公开令牌 API（给没有账号的自然人签发短期令牌）
export const PublicTokenApi = {
  create: async (data: PublicTokenCreateVO) => {
    return await request.post<PublicTokenVO>({ url: `/icbc/public-token/create`, data })
  }
}

/** 拼出自然人可扫码/点开的公开端点地址（无需登录） */
export const buildPublicUrl = (path: string, token: string) =>
  `${window.location.origin}/admin-api/icbc/public/${path}?token=${encodeURIComponent(token)}`
