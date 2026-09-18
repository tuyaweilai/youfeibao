import request from '@/config/axios'

// 企业用户关系 VO
export interface UserRelationVO {
  id: number // 主键ID
  userId: number // 用户ID
  username?: string // 用户名称 (后端VO扩展或前端转换)
  nickname?: string // 用户昵称 (后端VO扩展或前端转换)
  enterpriseId: number // 企业ID
  enterpriseName?: string // 企业名称 (后端VO扩展或前端转换)
  storeId?: number // 门店ID
  storeName?: string // 门店名称 (后端VO扩展或前端转换)
  relationType: number // 关系类型（1: 管理员, 2: 员工）
  isPrimaryContact: boolean // 是否主联系人
  isDefaultEnterprise: boolean // 是否默认企业
  createTime: string // 创建时间
}

// 企业用户关系基础 VO (用于表单)
export interface EnterpriseUserRelationBaseVO {
  id?: number
  userId: number
  enterpriseId: number
  storeId?: number
  relationType: number
  isPrimaryContact: boolean
  isDefaultEnterprise: boolean
}

// 设置默认企业 VO
export interface EnterpriseUserRelationSetDefaultReqVO {
  userId: number
  relationId: number
}

// 企业用户关系 API
export const UserRelationApi = {
  // 查询企业用户关系分页
  getUserRelationPage: async (params: any) => {
    return await request.get({ url: `/enterprise/user-relation/page`, params })
  },

  // 查询企业用户关系详情
  getUserRelation: async (id: number) => {
    return await request.get({ url: `/enterprise/user-relation/get?id=${id}` })
  },

  // 新增企业用户关系
  createUserRelation: async (data: EnterpriseUserRelationBaseVO) => {
    return await request.post({ url: `/enterprise/user-relation/create`, data })
  },

  // 修改企业用户关系
  updateUserRelation: async (data: EnterpriseUserRelationBaseVO) => {
    return await request.put({ url: `/enterprise/user-relation/update`, data })
  },

  // 删除企业用户关系
  deleteUserRelation: async (id: number) => {
    return await request.delete({ url: `/enterprise/user-relation/delete?id=${id}` })
  },

  // 设置默认企业
  setDefaultEnterprise: async (data: EnterpriseUserRelationSetDefaultReqVO) => {
    return await request.put({ url: `/enterprise/user-relation/set-default`, data })
  },

  // 获取用户简易列表 (用于下拉选择用户)
  getUserSimpleList: async (keyword?: string) => {
    const params = keyword ? { keyword } : {}
    return await request.get({ url: '/system/user/simple-list', params })
  },

  // 获取企业简易列表 (用于下拉选择企业)
  getEnterpriseSimpleList: async () => {
    return await request.get({ url: '/enterprise/info/simple-list' })
  },

  // 获取门店简易列表 (用于下拉选择门店，根据企业ID筛选)
  getStoreSimpleList: async (enterpriseId?: number) => {
    const params = enterpriseId ? { enterpriseId } : {}
    return await request.get({ url: '/enterprise/store/simple-list', params })
  }
} 