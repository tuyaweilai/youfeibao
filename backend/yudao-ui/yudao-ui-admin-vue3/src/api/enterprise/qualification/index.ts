import request from '@/config/axios'

// 企业资质 VO
export interface QualificationVO {
  id: number // 主键ID
  enterpriseId: number // 企业ID
  enterpriseName?: string // 企业名称 (后端VO扩展或前端转换)
  qualificationName: string // 资质名称
  qualificationType: number // 资质类型
  qualificationCode: string // 资质编号
  issueDate?: string // 发证日期
  expiryDate?: string // 到期日期
  issuingAuthority: string // 发证机关
  status: number // 状态
  qualificationFileId: number // 资质文件附件ID
  createTime: string // 创建时间
}

// 企业资质基础 VO (用于表单)
export interface EnterpriseQualificationBaseVO {
  id?: number
  enterpriseId: number
  qualificationType: number
  qualificationName: string
  qualificationCode?: string
  issueDate?: string
  expiryDate?: string
  issuingAuthority?: string
  qualificationFileId: number // 必填
}

// 企业资质审核请求 VO
export interface EnterpriseQualificationAuditReqVO {
  id: number // 资质ID
  auditDecision: boolean // 审核决定 (true: 通过, false: 拒绝)
  auditRemarks?: string // 审核备注
}

// 企业资质 API
export const QualificationApi = {
  // 查询企业资质分页
  getQualificationPage: async (params: any) => {
    return await request.get({ url: `/enterprise/qualification/page`, params })
  },

  // 查询企业资质详情
  getQualification: async (id: number) => {
    return await request.get({ url: `/enterprise/qualification/get?id=${id}` })
  },

  // 新增企业资质
  createQualification: async (data: EnterpriseQualificationBaseVO) => {
    return await request.post({ url: `/enterprise/qualification/create`, data })
  },

  // 修改企业资质
  updateQualification: async (data: EnterpriseQualificationBaseVO) => {
    return await request.put({ url: `/enterprise/qualification/update`, data })
  },

  // 删除企业资质
  deleteQualification: async (id: number) => {
    return await request.delete({ url: `/enterprise/qualification/delete?id=${id}` })
  },

  // 审核企业资质
  auditQualification: async (data: EnterpriseQualificationAuditReqVO) => {
    return await request.put({ url: `/enterprise/qualification/audit`, data })
  },

  // 获取企业简易列表 (假设存在，用于下拉选择企业)
  // 注意: 这个接口可能需要您在 @/api/enterprise/info/index.ts 或类似地方创建
  getEnterpriseSimpleList: async () => {
    // return await request.get({ url: '/enterprise/info/simple-list' }) // 示例接口路径
    console.warn('请实现 getEnterpriseSimpleList API 以加载企业列表')
    return [{ id: 1, name: '示例企业A' }, { id: 2, name: '示例企业B' }] // 返回示例数据
  }
} 