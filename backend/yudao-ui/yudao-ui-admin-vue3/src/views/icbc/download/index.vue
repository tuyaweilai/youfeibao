<template>
  <ContentWrap title="发票下载与证据">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="工行只回 PDF。下载记录在发票上传通知到达后生成；下载成功后其文件即作为「发票流」证据归到该笔收购上。"
    />
    <el-form :inline="true" label-width="110px">
      <el-form-item label="合作方订单号">
        <el-input v-model="partnerOrderId" placeholder="请输入" clearable class="!w-260px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="queryByOrder" :loading="loading" v-hasPermi="['icbc:invoice-download:query']">按订单查</el-button>
      </el-form-item>
      <el-form-item label="发票号码">
        <el-input v-model="invoiceNumber" placeholder="请输入" clearable class="!w-260px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="queryByInvoice" :loading="loading" v-hasPermi="['icbc:invoice-download:query']">按发票查</el-button>
      </el-form-item>
    </el-form>

    <template v-if="record">
      <el-descriptions :column="2" border class="mt-10px">
        <el-descriptions-item label="合作方订单号">{{ record.partnerOrderId }}</el-descriptions-item>
        <el-descriptions-item label="工行订单号">{{ record.orderNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发票号码">{{ record.invoiceNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下载状态">
          <el-tag :type="record.downloadStatus === 2 ? 'success' : record.downloadStatus === 3 ? 'danger' : 'info'">
            {{ record.downloadStatusName || record.downloadStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件名称">{{ record.fileName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下载时间">{{ record.downloadTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ record.retryCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ record.errorMsg || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-10px">
        <el-button type="primary" :loading="loading" @click="handleDownload" v-hasPermi="['icbc:invoice-download:download']">执行下载</el-button>
        <el-button :loading="loading" @click="handleRetry" v-hasPermi="['icbc:invoice-download:retry']">重试</el-button>
      </div>

      <el-table :data="record.files || []" border class="mt-15px">
        <el-table-column label="发票号码" prop="invoiceNumber" min-width="180" />
        <el-table-column label="文件类型" prop="fileType" width="100" />
        <el-table-column label="文件名称" prop="fileName" min-width="200" />
        <el-table-column label="大小(字节)" prop="fileSize" width="120" />
        <el-table-column label="上传时间" prop="uploadTime" width="170" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDownloadFile(row)" v-hasPermi="['icbc:invoice-download:download-file']">下载文件</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </ContentWrap>
</template>

<script setup lang="ts">
import { InvoiceDownloadApi, InvoiceDownloadVO, InvoiceFileVO } from '@/api/icbc/download'
import download from '@/utils/download'

defineOptions({ name: 'IcbcDownload' })

const message = useMessage()

const partnerOrderId = ref('')
const invoiceNumber = ref('')
const loading = ref(false)
const record = ref<InvoiceDownloadVO>()

const queryByOrder = async () => {
  if (!partnerOrderId.value) {
    message.warning('请输入合作方订单号')
    return
  }
  loading.value = true
  try {
    record.value = await InvoiceDownloadApi.getRecord(partnerOrderId.value)
  } finally {
    loading.value = false
  }
}

const queryByInvoice = async () => {
  if (!invoiceNumber.value) {
    message.warning('请输入发票号码')
    return
  }
  loading.value = true
  try {
    record.value = await InvoiceDownloadApi.getByInvoiceNumber(invoiceNumber.value)
  } finally {
    loading.value = false
  }
}

const handleDownload = async () => {
  if (!record.value?.partnerOrderId) return
  loading.value = true
  try {
    record.value = await InvoiceDownloadApi.download({
      partnerOrderId: record.value.partnerOrderId,
      invoiceNumber: record.value.invoiceNumber,
      fileType: 'PDF'
    })
    message.success('下载完成')
  } finally {
    loading.value = false
  }
}

const handleRetry = async () => {
  if (!record.value?.id) return
  loading.value = true
  try {
    record.value = await InvoiceDownloadApi.retry(record.value.id)
    message.success('已重试')
  } finally {
    loading.value = false
  }
}

const handleDownloadFile = async (file: InvoiceFileVO) => {
  if (!file.downloadId || !file.fileType) return
  const data = await InvoiceDownloadApi.downloadFile({
    downloadId: file.downloadId,
    fileType: file.fileType
  })
  download.excel(data, file.fileName || 'invoice.pdf')
}
</script>
