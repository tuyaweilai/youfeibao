<template>
  <ContentWrap title="一票一档证据链">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="一张票 = 一笔收购。五流证据由系统自动取（资金流=支付回单、发票流=发票原件、信息流=台账条目），合同流与货物流目前由人工补录。"
    />
    <el-form :inline="true" :model="queryParams" ref="queryFormRef" label-width="100px">
      <el-form-item label="合作方订单号" prop="partnerOrderId">
        <el-input
          v-model="queryParams.partnerOrderId"
          placeholder="请输入"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发票号码" prop="invoiceNo">
        <el-input
          v-model="queryParams.invoiceNo"
          placeholder="请输入"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="!w-360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          :loading="exporting"
          @click="handleExportBatch"
          v-hasPermi="['icbc:evidence:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 批量导出证据包
        </el-button>
        <el-button
          type="success"
          plain
          :loading="exporting"
          @click="handleExportLedger"
          v-hasPermi="['icbc:evidence:export']"
        >
          <Icon icon="ep:document" class="mr-5px" /> 导出收购台账
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      v-if="completeness"
      type="success"
      :closable="false"
      class="mb-10px"
      :title="`本页批量齐备率 ${completeness.completenessRate}%：共 ${completeness.invoiceCount} 票，五流齐备 ${completeness.completeCount} 票`"
    />
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      row-key="partnerOrderId"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column type="expand">
        <template #default="{ row }">
          <el-descriptions :column="2" border size="small" class="mb-10px">
            <el-descriptions-item label="齐备率">
              <el-tag :type="completenessType(row)">{{ row.presentCount }}/{{ row.totalCount }}（{{ row.completenessRate }}%）</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="缺失的流">{{ missingFlows(row).join('、') || '无' }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ row.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出售者">{{ row.sellerName || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="row.flows || []" border size="small">
            <el-table-column label="流" width="100">
              <template #default="{ row: flow }">{{ flow.flowName }}</template>
            </el-table-column>
            <el-table-column label="齐备" width="80" align="center">
              <template #default="{ row: flow }">
                <el-tag :type="flow.present ? 'success' : 'info'" size="small">
                  {{ flow.present ? '齐备' : '缺失' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="证据来源">
              <template #default="{ row: flow }">
                <div v-for="(source, index) in flow.sources || []" :key="index" class="source-line">
                  <span class="source-title">{{ source.title || source.sourceType }}</span>
                  <span class="source-ref">{{ source.ref || '-' }}</span>
                  <el-button
                    v-if="source.downloadId && source.fileType"
                    link
                    type="primary"
                    @click="handleDownloadInvoiceFile(source)"
                  >
                    下载原件
                  </el-button>
                  <el-link v-else-if="source.url" type="primary" :href="source.url" target="_blank">查看</el-link>
                </div>
                <span v-if="!flow.sources || !flow.sources.length" class="text-gray-400">-</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-10px">
            <el-button
              size="small"
              type="primary"
              plain
              @click="openAttach(row)"
              v-hasPermi="['icbc:evidence:attach']"
            >
              <Icon icon="ep:plus" class="mr-5px" /> 补录证据
            </el-button>
          </div>

          <el-table v-if="row.attachments && row.attachments.length" :data="row.attachments" border size="small" class="mt-10px">
            <el-table-column label="流" prop="flowName" width="100" />
            <el-table-column label="类型" prop="evidenceTypeName" width="140" />
            <el-table-column label="标题" prop="title" min-width="180" />
            <el-table-column label="文件" min-width="160">
              <template #default="{ row: item }">
                <el-link v-if="item.fileUrl" type="primary" :href="item.fileUrl" target="_blank">
                  {{ item.fileName || item.fileUrl }}
                </el-link>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="发生时间" align="center" width="170">
              <template #default="{ row: item }">{{ formatTs(item.occurredTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row: item }">
                <el-button
                  link
                  type="danger"
                  @click="handleDeleteEvidence(item)"
                  v-hasPermi="['icbc:evidence:delete']"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-table-column>

      <el-table-column label="合作方订单号" prop="partnerOrderId" min-width="200" />
      <el-table-column label="发票号码" prop="invoiceNo" min-width="180" />
      <el-table-column label="出售者" prop="sellerName" width="110" />
      <el-table-column label="金额" prop="totalAmount" width="110" align="right" />
      <el-table-column label="齐备率" width="150" align="center">
        <template #default="{ row }">
          <el-tag :type="completenessType(row)">{{ row.completenessRate }}%</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="缺失的流" min-width="220">
        <template #default="{ row }">{{ missingFlows(row).join('、') || '五流齐备' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="190" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="handleExportPackage(row)"
            v-hasPermi="['icbc:evidence:export']"
          >
            证据包
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleInvoiceQrcode(row)"
            v-hasPermi="['icbc:public-token:create']"
          >
            取票二维码
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 取票二维码 -->
  <el-dialog v-model="qrVisible" title="发票取件二维码" width="380px">
    <div class="qr-wrap">
      <Qrcode v-if="qrUrl" :text="qrUrl" :width="260" />
      <p class="qr-tip">扫码即可下载发票 PDF（短期令牌，单次有效）</p>
      <el-input v-model="qrUrl" readonly />
    </div>
  </el-dialog>

  <!-- 补录证据 -->
  <el-dialog v-model="attachVisible" title="补录证据" width="560px">
    <el-form ref="attachFormRef" :model="attachForm" :rules="attachRules" label-width="100px">
      <el-form-item label="合作方订单号">
        <el-input v-model="attachForm.partnerOrderId" disabled />
      </el-form-item>
      <el-form-item label="证据类型" prop="evidenceType">
        <el-select v-model="attachForm.evidenceType" placeholder="请选择" class="w-full">
          <el-option
            v-for="type in types"
            :key="type.code"
            :label="`${type.flowName} / ${type.name}`"
            :value="type.code!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="attachForm.title" placeholder="留空则取证据类型名称" />
      </el-form-item>
      <el-form-item label="文件地址" prop="fileUrl">
        <el-input v-model="attachForm.fileUrl" placeholder="外部文件 URL" />
      </el-form-item>
      <el-form-item label="文件名称" prop="fileName">
        <el-input v-model="attachForm.fileName" />
      </el-form-item>
      <el-form-item label="发生时间" prop="occurredTime">
        <el-date-picker
          v-model="attachForm.occurredTime"
          type="datetime"
          value-format="x"
          placeholder="选择时间"
          class="w-full"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="attachForm.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="attachVisible = false">取消</el-button>
      <el-button type="primary" :loading="attachLoading" @click="handleAttachSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { EvidenceApi, EvidenceChainVO, EvidenceAttachmentVO, EvidenceSourceVO, EvidenceTypeVO, EvidenceCompletenessSummaryVO } from '@/api/icbc/evidence'
import { InvoiceDownloadApi } from '@/api/icbc/download'
import { PublicTokenApi, buildPublicUrl } from '@/api/icbc/publicToken'
import { Qrcode } from '@/components/Qrcode'
import { formatDate } from '@/utils/formatTime'
import download from '@/utils/download'

/** 一票一档证据链 */
defineOptions({ name: 'IcbcEvidence' })

const message = useMessage()

const loading = ref(false)
const exporting = ref(false)
const list = ref<EvidenceChainVO[]>([])
const total = ref(0)
const completeness = ref<EvidenceCompletenessSummaryVO>()
const selection = ref<EvidenceChainVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  partnerOrderId: undefined,
  invoiceNo: undefined,
  createTime: [] as string[]
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await EvidenceApi.page(queryParams)
    list.value = data.list
    total.value = data.total
    await loadCompleteness()
  } finally {
    loading.value = false
  }
}

/** 本页批量齐备率 */
const loadCompleteness = async () => {
  const partnerOrderIds = list.value.map((row) => row.partnerOrderId!).filter(Boolean)
  completeness.value = partnerOrderIds.length
    ? await EvidenceApi.completeness({ partnerOrderIds })
    : undefined
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  queryParams.createTime = []
  handleQuery()
}

const handleSelectionChange = (rows: EvidenceChainVO[]) => {
  selection.value = rows
}

const missingFlows = (row: EvidenceChainVO) =>
  (row.flows || []).filter((flow) => !flow.present).map((flow) => flow.flowName)

const formatTs = (value?: number | string | Date) => {
  if (value === undefined || value === null || value === '') return '-'
  return formatDate(new Date(value), 'YYYY-MM-DD HH:mm:ss')
}

const completenessType = (row: EvidenceChainVO): 'success' | 'warning' | 'danger' => {
  if (row.complete) return 'success'
  return (row.presentCount || 0) >= 3 ? 'warning' : 'danger'
}

/** 导出单张票的证据包 */
const handleExportPackage = async (row: EvidenceChainVO) => {
  if (!row.partnerOrderId) return
  try {
    await message.exportConfirm()
    exporting.value = true
    const data = await EvidenceApi.exportPackage(row.partnerOrderId)
    download.zip(data, `证据包_${row.partnerOrderId}.zip`)
  } catch {
  } finally {
    exporting.value = false
  }
}

/** 生成发票取件二维码（短期单用途令牌，扫码即可下载 PDF） */
const qrVisible = ref(false)
const qrUrl = ref('')
const handleInvoiceQrcode = async (row: EvidenceChainVO) => {
  if (!row.partnerOrderId) return
  const token = await PublicTokenApi.create({
    purpose: 'INVOICE_DOWNLOAD',
    partnerOrderId: row.partnerOrderId
  })
  qrUrl.value = buildPublicUrl('invoice/download', token.token!)
  qrVisible.value = true
}

/** 批量导出：勾选了按勾选，否则按搜索的时间范围 */
const handleExportBatch = async () => {
  const partnerOrderIds = selection.value.map((row) => row.partnerOrderId!).filter(Boolean)
  const createTime = (queryParams.createTime || []).map((time: string) => new Date(time).getTime())
  if (!partnerOrderIds.length && createTime.length !== 2) {
    message.warning('请勾选要导出的票，或先选择创建时间范围')
    return
  }
  try {
    await message.exportConfirm()
    exporting.value = true
    const data = await EvidenceApi.exportBatch({ partnerOrderIds, createTime })
    download.zip(data, '证据包.zip')
  } catch {
  } finally {
    exporting.value = false
  }
}

/** 导出收购台账 */
const handleExportLedger = async () => {
  try {
    await message.exportConfirm()
    exporting.value = true
    const data = await EvidenceApi.exportLedger({
      startTime: queryParams.createTime?.[0],
      endTime: queryParams.createTime?.[1]
    })
    download.excel(data, '收购台账.xls')
  } catch {
  } finally {
    exporting.value = false
  }
}

/** 下载本地发票原件 */
const handleDownloadInvoiceFile = async (source: EvidenceSourceVO) => {
  if (!source.downloadId || !source.fileType) return
  const data = await InvoiceDownloadApi.downloadFile({
    downloadId: source.downloadId,
    fileType: source.fileType
  })
  download.pdf(data, source.title || 'invoice.pdf')
}

/** 补录证据 */
const attachVisible = ref(false)
const attachLoading = ref(false)
const attachFormRef = ref()
const types = ref<EvidenceTypeVO[]>([])
const attachForm = reactive<any>({
  partnerOrderId: undefined,
  evidenceType: undefined,
  title: undefined,
  fileUrl: undefined,
  fileName: undefined,
  occurredTime: undefined,
  remark: undefined
})
const attachRules = {
  evidenceType: [{ required: true, message: '证据类型不能为空', trigger: 'change' }]
}

const openAttach = async (row: EvidenceChainVO) => {
  if (!types.value.length) {
    types.value = (await EvidenceApi.types()) || []
  }
  attachForm.partnerOrderId = row.partnerOrderId
  attachForm.evidenceType = undefined
  attachForm.title = undefined
  attachForm.fileUrl = undefined
  attachForm.fileName = undefined
  attachForm.occurredTime = undefined
  attachForm.remark = undefined
  attachVisible.value = true
}

const handleAttachSubmit = async () => {
  await attachFormRef.value.validate()
  attachLoading.value = true
  try {
    await EvidenceApi.attach(attachForm)
    message.success('补录成功')
    attachVisible.value = false
    await getList()
  } finally {
    attachLoading.value = false
  }
}

const handleDeleteEvidence = async (item: EvidenceAttachmentVO) => {
  try {
    await message.delConfirm()
    await EvidenceApi.remove(item.id!)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.qr-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.qr-tip {
  color: #909399;
  font-size: 13px;
}
.source-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.source-title {
  font-weight: 500;
}
.source-ref {
  color: #909399;
}
</style>
