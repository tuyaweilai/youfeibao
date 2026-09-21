import request from '@/config/axios'

// 卡证识别（#103，ADR 0037）：平台级配置，与电子签章各立一处。
// 密钥只落后端、界面不回显明文。

/** 平台级参数（密钥只回「已配置」与否，不回明文） */
export interface CardRecognitionConfigVO {
  provider?: string // stub-未启用 / tencent-腾讯云 OCR
  providerFromConfigFile?: boolean
  secretId?: string
  secretKey?: string
  secretIdConfigured?: boolean
  secretKeyConfigured?: boolean
  region?: string
  endpoint?: string
  timeout?: number
  configFileFields?: string[] // 哪些生效值来自配置文件
  configured?: boolean // 识别能力是否真的可用（provider=tencent 且密钥齐备）
  missingFields?: string[]
  lastCheckResult?: string // OK / AUTH_FAILED / NETWORK / VENDOR_ERROR
  lastCheckResultName?: string
  lastCheckTime?: Date
  remark?: string
}

/** 连通性自检入参：密钥留空 = 用已存值（先验证、再保存） */
export interface CardRecognitionCheckVO {
  secretId?: string
  secretKey?: string
  region?: string
  endpoint?: string
  timeout?: number
}

/** 连通性自检结果 */
export interface CardRecognitionCheckRespVO {
  ok?: boolean
  result?: string
  resultName?: string
  checkTime?: Date
}

export const PlatformCardRecognitionApi = {
  getConfig: async () => await request.get({ url: `/icbc/platform/card-recognition/config` }),
  saveConfig: async (data: CardRecognitionConfigVO) =>
    await request.put({ url: `/icbc/platform/card-recognition/config`, data }),
  check: async (data: CardRecognitionCheckVO) =>
    await request.post({ url: `/icbc/platform/card-recognition/config/check`, data })
}
