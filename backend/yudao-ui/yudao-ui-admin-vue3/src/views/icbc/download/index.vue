<!--
  发票下载与证据：列表优先的形态。

  一进来就列「已经下载下来的发票原件」——发票流证据的落点就是这些 PDF，所以这里第一件事是能直接
  把它们捞下来，而不是先让人背一串订单号。行内的「下载 PDF」直接用列表自带的 files（服务端批量带出），
  不在循环里逐条查。下载记录 / 执行下载 / 重试是失败排查用的，收进「查看详情」。
-->
<template>
  <ContentWrap title="发票下载与证据">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="工行只回 PDF。下载记录在发票上传通知到达后生成；下载成功后其文件即作为「发票流」证据归到该笔收购上。"
    />
    <el-form :inline="true" label-width="110px" class="-mb-15px">
      <el-form-item label="合作方订单号">
        <el-input
          v-model="queryParams.partnerOrderId"
          placeholder="如 ACQ..."
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发票号码">
        <el-input
          v-model="queryParams.invoiceNumber"
          placeholder="请输入"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="下载状态">
        <el-select
          v-model="queryParams.downloadStatus"
          placeholder="全部"
          clearable
          class="!w-160px"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />查询
        </el-button>
        <el-button @click="handleReset">
          <Icon icon="ep:refresh" class="mr-5px" />重置
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="发票号码" prop="invoiceNumber" min-width="180" />
      <el-table-column label="合作方订单号" prop="partnerOrderId" min-width="180" />
      <el-table-column label="文件名称" prop="fileName" min-width="220" show-overflow-tooltip />
      <el-table-column label="大小" width="100" align="right">
        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="下载状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.downloadStatus)">
            {{ row.downloadStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下载时间" prop="downloadTime" width="170" :formatter="dateFormatter" />
      <el-table-column label="操作" width="190" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="!pdfFile(row)"
            v-hasPermi="['icbc:invoice-download:download-file']"
            @click="handleDownloadPdf(row)"
          >
            下载 PDF
          </el-button>
          <el-button
            link
            type="primary"
            v-hasPermi="['icbc:invoice-download:query']"
            @click="openDetail(row)"
          >
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 详情：下载记录 + 文件列表；「执行下载 / 重试」是这张记录上的动作 -->
  <el-dialog v-model="detailVisible" title="发票下载详情" width="820px">
    <template v-if="record">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="合作方订单号">{{ record.partnerOrderId }}</el-descriptions-item>
        <el-descriptions-item label="工行订单号">{{ record.orderNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发票号码">{{ record.invoiceNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下载状态">
          <el-tag :type="statusTagType(record.downloadStatus)">
            {{ record.downloadStatusName || record.downloadStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件名称">{{ record.fileName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下载时间">
          {{ record.downloadTime ? formatDate(record.downloadTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ record.retryCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ record.errorMsg || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-10px">
        <el-button
          type="primary"
          :loading="loading"
          v-hasPermi="['icbc:invoice-download:download']"
          @click="handleDownload"
        >
          执行下载
        </el-button>
        <el-button
          :loading="loading"
          v-hasPermi="['icbc:invoice-download:retry']"
          @click="handleRetry"
        >
          重试
        </el-button>
      </div>

      <el-table :data="record.files || []" border class="mt-15px">
        <el-table-column label="发票号码" prop="invoiceNumber" min-width="180" />
        <el-table-column label="文件类型" prop="fileType" width="100" />
        <el-table-column label="文件名称" prop="fileName" min-width="200" show-overflow-tooltip />
        <el-table-column label="大小(字节)" prop="fileSize" width="120" />
        <el-table-column label="上传时间" prop="uploadTime" width="170" :formatter="dateFormatter" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              v-hasPermi="['icbc:invoice-download:download-file']"
              @click="handleDownloadFile(row)"
            >
              下载文件
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { InvoiceDownloadApi, InvoiceDownloadVO, InvoiceFileVO } from '@/api/icbc/download'
import download from '@/utils/download'
import { dateFormatter, formatDate } from '@/utils/formatTime'

defineOptions({ name: 'IcbcDownload' })

const message = useMessage()

const statusOptions = [
  { value: 0, label: '待下载' },
  { value: 1, label: '下载中' },
  { value: 2, label: '下载成功' },
  { value: 3, label: '下载失败' }
]

const loading = ref(false)
const list = ref<InvoiceDownloadVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  partnerOrderId: '',
  invoiceNumber: '',
  downloadStatus: undefined as number | undefined
})

const getList = async () => {
  loading.value = true
  try {
    const page = await InvoiceDownloadApi.getPage(queryParams)
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const handleReset = () => {
  queryParams.partnerOrderId = ''
  queryParams.invoiceNumber = ''
  queryParams.downloadStatus = undefined
  handleQuery()
}

// 失败 / 下载中 / 待下载都值得一眼看出来，成功后反而是常态
const statusTagType = (s?: number) => {
  if (s === 2) return 'success'
  if (s === 3) return 'danger'
  if (s === 1) return 'warning'
  return 'info'
}

const formatFileSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

// 工行只回 PDF；取第一个 PDF 文件，没有就是还没下下来
const pdfFile = (row: InvoiceDownloadVO) => (row.files || []).find((f) => f.fileType === 'PDF')

const doDownloadFile = async (file: InvoiceFileVO) => {
  if (!file.downloadId || !file.fileType) return
  const data = await InvoiceDownloadApi.downloadFile({
    downloadId: file.downloadId,
    fileType: file.fileType
  })
  download.excel(data, file.fileName || 'invoice.pdf')
}

const handleDownloadPdf = async (row: InvoiceDownloadVO) => {
  const file = pdfFile(row)
  if (!file) {
    message.warning('这笔还没有已下载的发票文件')
    return
  }
  await doDownloadFile(file)
}

const detailVisible = ref(false)
const record = ref<InvoiceDownloadVO>()

// 列表响应已经带了文件列表，详情不必再查一次
const openDetail = (row: InvoiceDownloadVO) => {
  record.value = row
  detailVisible.value = true
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
    await getList()
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
    await getList()
  } finally {
    loading.value = false
  }
}

const handleDownloadFile = async (file: InvoiceFileVO) => {
  await doDownloadFile(file)
}

onMounted(getList)
</script>
