import { appGet, appPost } from '@/utils/request'

// ==================== 登录与身份绑定 ====================

export interface SellerLoginResp {
  accessToken: string
  refreshToken: string
  expiresTime: number
  subjects: SellerSubject[]
}

/**
 * 实人认证状态（与后端 `PayeeRealNameStatusEnum` 一致）：0 未认证 / 1 认证中 / 2 认证通过 / 3 认证未通过。
 * **1 是「认证中」，不是「已认证」**——判定只从这里取，别再写魔数（#90）。
 */
export const REAL_NAME_STATUS = {
  NOT_STARTED: 0,
  PENDING: 1,
  PASSED: 2,
  FAILED: 3
} as const

export interface SellerSubject {
  naturalPersonId: number
  name?: string
  mobile?: string
  idCardNo?: string
  /** 实名状态值（0 未认证 / 1 认证中 / 2 认证通过 / 3 认证未通过） */
  realNameStatus?: number
  realNameStatusName?: string
}

export const sendSmsCode = (mobile: string) =>
  appPost<boolean>('/icbc/seller/auth/sms-send', { mobile }, false)

export const smsLogin = (mobile: string, code: string) =>
  appPost<SellerLoginResp>('/icbc/seller/auth/sms-login', { mobile, code }, false)

export const bindByLoginMobile = () =>
  appPost<SellerSubject[]>('/icbc/seller/auth/subjects/bind-by-mobile')

export const logout = () => appPost<boolean>('/icbc/seller/auth/logout')

// ==================== 首页与记录 ====================

export interface PendingItem {
  type: 'SETTLEMENT' | 'AGREEMENT' | string
  typeName?: string
  tenantId?: number
  enterpriseName?: string
  settlementId?: number
  payeeId?: number
  title?: string
  statusName?: string
  deadlineTime?: string
  urgent?: boolean
}

export interface SellerHome {
  pendingCount?: number
  pendingSettlementCount?: number
  pendingAgreementCount?: number
  pendingItems?: PendingItem[]
  amountScopeNote?: string
  stationId?: number
  stationName?: string
}

export interface SellerRecord {
  acquisitionId: number
  acquisitionNo?: string
  categoryName?: string
  unit?: string
  settlementWeight?: number
  deduction?: number
  deductionMethod?: string
  unitPrice?: number
  amount?: number
  acquirerName?: string
  status?: number
  statusName?: string
  cancelReason?: string
  tradeTime?: string
  tradeAddress?: string
  printable?: boolean
}

export interface SellerRecordGroup {
  tenantId?: number
  enterpriseName?: string
  count?: number
  totalAmount?: number
  totalSettlementWeight?: number
  records?: SellerRecord[]
}

export interface SellerPayment {
  paymentOrderId: number
  orderNo?: string
  acquisitionNo?: string
  categoryName?: string
  acquirerName?: string
  paymentAmount?: number
  actuallyReceivedAmount?: number
  status?: number
  statusName?: string
  receiptNo?: string
  receiptTime?: string
  paymentTime?: string
  nextStep?: string
  sellerReceivedConfirmed?: boolean
  sellerReceivedConfirmedAt?: string
  canConfirmReceive?: boolean
}

export interface SellerInvoice {
  invoiceOrderId: number
  orderNo?: string
  acquisitionNo?: string
  acquirerName?: string
  invoiceNo?: string
  invoiceDate?: string
  invoiceAmount?: number
  taxAmount?: number
  invoiceStatus?: number
  invoiceStatusName?: string
  taxStatus?: number
  taxStatusName?: string
  uploadStatus?: number
  uploadStatusName?: string
  pdfAvailable?: boolean
}

export interface SellerInvoiceSummary {
  year?: number
  invoiceCount?: number
  totalInvoiceAmount?: number
  totalTaxAmount?: number
  taxScopeNote?: string
  invoices?: SellerInvoice[]
}

export interface SellerAuthorization {
  tenantId: number
  enterpriseName?: string
  payeeId?: number
  reverseInvoiceAuthorized?: boolean
  taxAgencyAuthorized?: boolean
  revoked?: boolean
  authorizedAt?: string
  revokedAt?: string
  revokeReason?: string
}

export interface SellerBankCard {
  payeeId?: number
  tenantId?: number
  enterpriseName?: string
  bankName?: string
  cardTail?: string
  /** 收款账户变更状态（#37）：0-银行审核中，1-已生效，2-已拒绝，9-已取消 */
  changeStatus?: number
  /** 变更状态名，例如「银行审核中」 */
  changeStatusName?: string
  /** 变更发起时间（毫秒时间戳；yudao 把 LocalDateTime 按毫秒序列化） */
  changeRequestedAt?: number
}

export interface SellerProfile {
  name?: string
  mobileMasked?: string
  idCardMasked?: string
  /** 实名状态值（0 未认证 / 1 认证中 / 2 认证通过 / 3 认证未通过） */
  realNameStatus?: number
  realNameStatusName?: string
  serviceMobile?: string
  logoutNote?: string
  bankCards?: SellerBankCard[]
}

export const getHome = (naturalPersonId: number, stationId?: number) =>
  appGet<SellerHome>('/icbc/seller/portal/home', { naturalPersonId, stationId })

export const getRecordGroups = (naturalPersonId: number) =>
  appGet<SellerRecordGroup[]>('/icbc/seller/portal/records', { naturalPersonId })

export const getPayments = (naturalPersonId: number) =>
  appGet<SellerPayment[]>('/icbc/seller/portal/payments', { naturalPersonId })

export const getInvoices = (naturalPersonId: number, year?: number) =>
  appGet<SellerInvoiceSummary>('/icbc/seller/portal/invoices', { naturalPersonId, year })

export const getAuthorizations = (naturalPersonId: number) =>
  appGet<SellerAuthorization[]>('/icbc/seller/portal/authorizations', { naturalPersonId })

export const revokeAuthorization = (naturalPersonId: number, tenantId: number, reason?: string) =>
  appPost<boolean>('/icbc/seller/portal/authorizations/revoke', { naturalPersonId, tenantId, reason })

export const getProfile = (naturalPersonId: number) =>
  appGet<SellerProfile>('/icbc/seller/portal/profile', { naturalPersonId })

// ==================== 变更收款账户（换银行卡，#37） ====================

/** 变更返回：工行收方修改数据接口已受理后的状态（#89 后不再返回一次性令牌） */
export interface SellerBankCardChange {
  changeNo?: string
  status?: number
  statusName?: string
  oldCardTail?: string
  newCardTail?: string
  message?: string
  scopeNote?: string
}

/**
 * 发起变更收款账户（换银行卡）。后端直接走工行收方修改数据接口提交：卡号由本人填，
 * 审核期间新交易的付款会挂起，原卡在审核通过前仍然有效。
 */
export const requestBankCardChange = (data: {
  naturalPersonId: number
  tenantId: number
  bankCardNo: string
  /** 是否本人我行卡：0-非我行用户，1-我行用户 */
  accountCode?: string
  bankName?: string
  bankBranch?: string
}) => appPost<SellerBankCardChange>('/icbc/seller/portal/bank-card/change', data)

// ==================== 实名认证入口（#89） ====================

export interface SellerRealNameLink {
  token?: string
  expiresTime?: string
  message?: string
}

/**
 * 取实名认证入口：签发一枚 ONBOARDING 一次性令牌，本人用它在自己微信里完成人脸。
 * 入驻由平台在实名通过后自动发起，本人不需要再点任何东西。
 */
export const mintRealNameLink = (naturalPersonId: number, payeeId: number) =>
  appPost<SellerRealNameLink>('/icbc/seller/portal/real-name/link', { naturalPersonId, payeeId })

export const confirmReceived = (naturalPersonId: number, paymentOrderId: number) =>
  appPost<boolean>('/icbc/seller/portal/payments/received', { naturalPersonId, paymentOrderId })

// ==================== 结算确认（#33 端点） ====================

export interface SettlementLine {
  acquisitionId: number
  acquisitionNo?: string
  categoryName?: string
  unit?: string
  settlementWeight?: number
  deduction?: number
  deductionMethod?: string
  unitPrice?: number
  adjustmentAmount?: number
  adjustmentReason?: string
  amount?: number
  status?: number
  statusName?: string
  cancelReason?: string
}

export interface Settlement {
  id: number
  settlementNo?: string
  payeeId?: number
  naturalPersonId?: number
  sellerName?: string
  generateTime?: string
  currentVersionId?: number
  currentVersionNo?: number
  confirmStatus?: number
  confirmStatusName?: string
  disputeReasonName?: string
  disputeNote?: string
  disputeCount?: number
  enterpriseReplyNote?: string
  enterpriseNotReplied?: boolean
  deadlineTime?: string
  acquisitionCount?: number
  totalSettlementWeight?: number
  totalAmount?: number
  lines?: SettlementLine[]
}

export const listSettlements = (naturalPersonId: number) =>
  appGet<Settlement[]>('/icbc/seller/settlement/list', { naturalPersonId })

// ==================== 到站预约（#35，ADR 0020：不是订单） ====================

export interface Appointment {
  id: number
  appointmentNo?: string
  enterpriseName?: string
  stationName?: string
  stationCode?: string
  categoryName?: string
  unit?: string
  expectedQuantity?: number
  expectedQuantityText?: string
  plateNo?: string
  expectedArrivalTime?: string
  status?: number
  statusName?: string
  arrivedAt?: string
  acquisitionId?: number
  cancelledAt?: string
  cancelReason?: string
  scopeNote?: string
  createTime?: string
}

export interface AppointmentGoods {
  id: number
  name?: string
  unit?: string
}

export const listAppointmentGoods = () =>
  appGet<AppointmentGoods[]>('/icbc/seller/appointment/goods')

export const createAppointment = (data: {
  naturalPersonId: number
  stationCode: string
  goodsConfigId: number
  expectedQuantity?: number
  plateNo?: string
  expectedArrivalTime: number
  remark?: string
}) => appPost<number>('/icbc/seller/appointment/create', data)

export const cancelAppointment = (naturalPersonId: number, id: number, reason?: string) =>
  appPost<boolean>('/icbc/seller/appointment/cancel', { naturalPersonId, id, reason })

export const listAppointments = (naturalPersonId: number) =>
  appGet<Appointment[]>('/icbc/seller/appointment/list', { naturalPersonId })

export const getSettlement = (naturalPersonId: number, id: number) =>
  appGet<Settlement>('/icbc/seller/settlement/get', { naturalPersonId, id })

export const confirmSettlement = (naturalPersonId: number, settlementId: number, versionId: number) =>
  appPost<boolean>('/icbc/seller/settlement/confirm', { naturalPersonId, settlementId, versionId })

export const disputeSettlement = (
  naturalPersonId: number,
  settlementId: number,
  reason: string,
  note?: string
) => appPost<boolean>('/icbc/seller/settlement/dispute', { naturalPersonId, settlementId, reason, note })
