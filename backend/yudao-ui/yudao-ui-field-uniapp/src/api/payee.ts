import { get } from '@/utils/request'

export interface PayeeVO {
  id?: number
  payeeNo?: string
  name?: string
  idCardNo?: string
  mobile?: string
  address?: string
  status?: number
  auditMsg?: string
}

/** 回头客带档：按身份证或手机号带出既有档案，查不到返回 null */
export const findReturningCustomer = (params: { idCardNo?: string; mobile?: string }) =>
  get<PayeeVO | null>('/icbc/seller-onboarding/returning-customer', params)
