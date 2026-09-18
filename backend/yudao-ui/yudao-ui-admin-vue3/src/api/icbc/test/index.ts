import request from '@/config/axios'

export interface IcbcConnectivity {
  reachable?: boolean
  returnCode?: number
  returnMsg?: string
}

// 工行适配层自检 API
export const IcbcTestApi = {
  // 适配层运行信息（不含密钥与网关地址）
  getConfig: async () => {
    return await request.get({ url: `/icbc/test/config` })
  },
  // 连通性校验
  checkConnectivity: async () => {
    return await request.get<IcbcConnectivity>({ url: `/icbc/test/connectivity` })
  }
}
