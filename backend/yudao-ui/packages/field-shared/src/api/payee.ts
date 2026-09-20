// 请求封装由宿主工程通过 '@' 别名提供（收货员现场端与司机端各有一份，见 README）
import { get, post } from '@/utils/request'

export interface PayeeVO {
  id?: number
  payeeNo?: string
  partnerPayeeId?: string
  name?: string
  idCardNo?: string
  mobile?: string
  bankCardNo?: string
  bankName?: string
  bankBranch?: string
  address?: string
  status?: number
  auditMsg?: string
  businessType?: string
  icbcReceiverStatus?: string
  icbcMediumId?: string
  icbcOpenacctStatus?: string
  occupation?: string
  companyName?: string
}

/** 回头客带档：按身份证或手机号带出既有档案，查不到返回 null */
export const findReturningCustomer = (params: { idCardNo?: string; mobile?: string }) =>
  get<PayeeVO | null>('/icbc/seller-onboarding/returning-customer', params)

export const getPayee = (id: number) => get<PayeeVO>('/icbc/payee-info/get', { id })

export const createPayee = (data: PayeeVO) => post<number>('/icbc/payee-info/create', data)
