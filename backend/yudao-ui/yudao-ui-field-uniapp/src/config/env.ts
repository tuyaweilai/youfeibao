/** 后端 API 基地址（H5 开发用相对路径，由 vite 代理；生产可配成同源 /admin-api） */
export const API_BASE_URL: string = import.meta.env.VITE_APP_BASE_URL || '/admin-api'

/** 自然人出售者端地址（用于生成「交给出售者自助办理」的链接；为空则只展示令牌） */
export const SELLER_APP_URL: string = import.meta.env.VITE_APP_SELLER_URL || ''

const TENANT_KEY = 'field_tenant_id'

/** 当前租户编号：优先本地存的，其次构建期默认值 */
export function getTenantId(): string {
  return uni.getStorageSync(TENANT_KEY) || import.meta.env.VITE_APP_TENANT_ID || '1'
}

export function setTenantId(tenantId: string) {
  uni.setStorageSync(TENANT_KEY, tenantId)
}
