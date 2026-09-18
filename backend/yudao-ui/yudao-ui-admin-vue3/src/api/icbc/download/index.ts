import request from '@/config/axios'

export interface InvoiceFileVO {
  id?: number
  downloadId?: number
  invoiceNumber?: string
  fileType?: string
  filePath?: string
  fileName?: string
  fileSize?: number
  fileMd5?: string
  uploadTime?: Date
  createTime?: Date
}

export interface InvoiceDownloadVO {
  id?: number
  invoiceOrderId?: number
  partnerOrderId?: string
  orderNumber?: string
  invoiceNumber?: string
  downloadUrl?: string
  filePath?: string
  fileName?: string
  fileSize?: number
  downloadStatus?: number
  downloadStatusName?: string
  downloadTime?: Date
  retryCount?: number
  errorMsg?: string
  createTime?: Date
  files?: InvoiceFileVO[]
}

// 发票下载与证据 API
export const InvoiceDownloadApi = {
  // 按合作方订单号取下载记录
  getRecord: async (partnerOrderId: string) => {
    return await request.get<InvoiceDownloadVO>({
      url: `/icbc/invoice-download/get`,
      params: { partnerOrderId }
    })
  },
  // 按发票号码取下载记录
  getByInvoiceNumber: async (invoiceNumber: string) => {
    return await request.get<InvoiceDownloadVO>({
      url: `/icbc/invoice-download/get-by-invoice`,
      params: { invoiceNumber }
    })
  },
  // 执行下载（已成功则直接返回）
  download: async (data: { partnerOrderId: string; invoiceNumber?: string; fileType?: string }) => {
    return await request.post({ url: `/icbc/invoice-download/download`, data })
  },
  // 重试下载
  retry: async (downloadId: number) => {
    return await request.post({ url: `/icbc/invoice-download/retry/${downloadId}` })
  },
  // 下载文件（流）
  downloadFile: async (params: { downloadId: number; fileType: string }) => {
    return await request.download({ url: `/icbc/invoice-download/download-file`, params })
  }
}
