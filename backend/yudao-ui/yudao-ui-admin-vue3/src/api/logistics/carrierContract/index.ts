import request from '@/config/axios'

// 承运合同（#75 V8）：运价与计费方式的约定，是运费对账的依据。
// 与「采购合同」是两份契约：采购合同是买货，承运合同是买运输服务。
export interface LogisticsCarrierContractSurchargeVO {
  name?: string
  amount?: number
  bearer?: number // 1-承运商承担，2-本企业承担
}

export interface LogisticsCarrierContractVO {
  id?: number
  contractNo?: string
  carrierId?: number
  carrierName?: string
  effectiveFrom?: Date
  effectiveTo?: Date
  route?: string
  goodsConfigId?: number
  categoryName?: string
  billingMode?: number // 1-按车，2-按吨，3-按公里
  billingModeName?: string
  unitPrice?: number
  surcharges?: LogisticsCarrierContractSurchargeVO[]
  status?: number // 0-生效，1-已停用
  statusName?: string
  remark?: string
  createTime?: Date
}

export const LogisticsCarrierContractApi = {
  getCarrierContractPage: async (params: any) =>
    await request.get({ url: `/logistics/carrier-contract/page`, params }),
  getCarrierContract: async (id: number) =>
    await request.get({ url: `/logistics/carrier-contract/get?id=` + id }),
  getContractListByCarrier: async (carrierId: number) =>
    await request.get({ url: `/logistics/carrier-contract/list-by-carrier?carrierId=` + carrierId }),
  createCarrierContract: async (data: LogisticsCarrierContractVO) =>
    await request.post({ url: `/logistics/carrier-contract/create`, data }),
  updateCarrierContract: async (data: LogisticsCarrierContractVO) =>
    await request.put({ url: `/logistics/carrier-contract/update`, data }),
  deleteCarrierContract: async (id: number) =>
    await request.delete({ url: `/logistics/carrier-contract/delete?id=` + id }),
  /** 导出承运合同 Excel（返回 Blob，页面交给 download.excel） */
  exportCarrierContract: async (params: any) => {
    return await request.download({ url: `/logistics/carrier-contract/export-excel`, params })
  }
}
