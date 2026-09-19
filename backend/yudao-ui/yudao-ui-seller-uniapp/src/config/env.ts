/** 后端 API 基地址（H5 开发用相对路径，由 vite 代理；生产可配成同源 /admin-api） */
export const API_BASE_URL: string = import.meta.env.VITE_APP_BASE_URL || '/admin-api'
