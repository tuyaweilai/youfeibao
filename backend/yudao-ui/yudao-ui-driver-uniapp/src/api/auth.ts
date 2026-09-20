import { get, post } from '@/utils/request'

export interface LoginReq {
  username: string
  password: string
}

export interface LoginResp {
  userId: number
  accessToken: string
  refreshToken: string
  expiresTime: number
}

export interface PermissionInfo {
  user: {
    id: number
    nickname: string
    deptId?: number
  }
  roles: string[]
  permissions: string[]
}

/** 登录（不带 token） */
export const login = (data: LoginReq) => post<LoginResp>('/system/auth/login', data, false)

/** 登出 */
export const logout = () => post<boolean>('/system/auth/logout')

/** 获取当前用户的昵称、角色与权限 */
export const getPermissionInfo = () => get<PermissionInfo>('/system/auth/get-permission-info')
