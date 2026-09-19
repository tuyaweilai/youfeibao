/** 后端公开 / 管理通道基地址（H5 开发用相对路径，由 vite 代理；生产可配成同源 /admin-api） */
export const API_BASE_URL: string = import.meta.env.VITE_APP_BASE_URL || '/admin-api'

/**
 * 自然人端登录态通道基地址（yudao 的 `/app-api`，令牌用户类型为会员）。
 * 登录、身份绑定与首页记录都走这里。
 */
export const APP_API_BASE_URL: string = import.meta.env.VITE_APP_APP_BASE_URL || '/app-api'

const TENANT_KEY = 'seller_tenant_id'

/** 当前租户编号：来自场站二维码解析结果（场站所属回收企业） */
export function getTenantId(): string {
  return uni.getStorageSync(TENANT_KEY) || import.meta.env.VITE_APP_TENANT_ID || ''
}

export function setTenantId(tenantId: string | number) {
  if (tenantId === null || tenantId === undefined || tenantId === '') {
    return
  }
  uni.setStorageSync(TENANT_KEY, String(tenantId))
}
