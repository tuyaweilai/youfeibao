import request from '@/config/axios'

// 企业门店 VO
export interface StoreVO {
  id: number // 主键ID
  enterpriseId: number // 企业ID
  enterpriseName?: string // 企业名称 (后端VO扩展或前端转换)
  parentId?: number // 上级门店ID
  parentName?: string // 上级门店名称 (后端VO扩展或前端转换)
  name: string // 门店名称
  storeCode?: string // 门店编码
  addressProvinceCode?: string // 门店地址-省编码
  addressCityCode?: string // 门店地址-市编码
  addressDistrictCode?: string // 门店地址-区编码
  addressDetail?: string // 门店地址-详细地址
  contactName?: string // 门店联系人姓名
  contactPhone?: string // 门店联系人电话
  status: number // 状态
  createTime: string // 创建时间
}

// 企业门店基础 VO (用于表单)
export interface EnterpriseStoreBaseVO {
  id?: number
  enterpriseId: number
  parentId?: number
  name: string
  storeCode?: string
  addressProvinceCode?: string
  addressCityCode?: string
  addressDistrictCode?: string
  addressDetail?: string
  contactName?: string
  contactPhone?: string
  status: number
}

// 更新门店状态 VO
export interface EnterpriseStoreUpdateStatusReqVO {
  id: number
  status: number
}

// 企业门店 API
export const StoreApi = {
  // 查询企业门店分页
  getStorePage: async (params: any) => {
    return await request.get({ url: `/enterprise/store/page`, params })
  },

  // 查询企业门店详情
  getStore: async (id: number) => {
    return await request.get({ url: `/enterprise/store/get?id=${id}` })
  },

  // 新增企业门店
  createStore: async (data: EnterpriseStoreBaseVO) => {
    return await request.post({ url: `/enterprise/store/create`, data })
  },

  // 修改企业门店
  updateStore: async (data: EnterpriseStoreBaseVO) => {
    return await request.put({ url: `/enterprise/store/update`, data })
  },

  // 删除企业门店
  deleteStore: async (id: number) => {
    return await request.delete({ url: `/enterprise/store/delete?id=${id}` })
  },

  // 更新门店状态
  updateStoreStatus: async (data: EnterpriseStoreUpdateStatusReqVO) => {
    return await request.put({ url: `/enterprise/store/update-status`, data })
  },

  // 获取企业简易列表 (用于下拉选择企业)
  getEnterpriseSimpleList: async () => {
    return await request.get({ url: '/enterprise/info/simple-list' })
  },

  // 获取门店树形列表 (用于选择上级门店)
  getStoreTree: async (enterpriseId?: number) => {
    const params = enterpriseId ? { enterpriseId } : {}
    return await request.get({ url: '/enterprise/store/list-tree', params })
  }
} 