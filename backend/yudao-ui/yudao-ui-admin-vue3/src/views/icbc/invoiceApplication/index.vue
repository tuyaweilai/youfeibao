<template>
  <ContentWrap title="开票申请（按收购单发起）">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="预下单只生成『自然人确认页面』，不产生发票。出售者确认后预开票状态变为预开票成功；真正的票要等付款之后。"
    />

    <el-form :inline="true" :model="baseForm" label-width="120px" class="mb-10px">
      <el-form-item label="发票类型">
        <el-select v-model="baseForm.invoiceType" class="!w-200px">
          <el-option label="增值税普通发票" value="02" />
          <el-option label="增值税专用发票" value="01" />
        </el-select>
      </el-form-item>
      <el-form-item label="应税行为发生地">
        <el-input v-model="baseForm.areaCode" placeholder="省级税务机关代码，如 110000" class="!w-220px" />
      </el-form-item>
      <el-form-item label="开票人">
        <el-input v-model="baseForm.drawerName" placeholder="开票员姓名" class="!w-160px" />
      </el-form-item>
      <el-form-item label="开票人证件号">
        <el-input v-model="baseForm.drawerCardNumber" placeholder="开票员身份证号" class="!w-200px" />
      </el-form-item>
      <el-form-item label="回跳地址前缀">
        <el-input v-model="baseForm.jumpUrlBase" placeholder="工行页面跳回本平台的地址" class="!w-260px" />
      </el-form-item>
    </el-form>

    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="收购单号" prop="acquisitionNo">
        <el-input v-model="queryParams.acquisitionNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-220px" />
      </el-form-item>
      <el-form-item label="出售者" prop="sellerName">
        <el-input v-model="queryParams.sellerName" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button type="primary" :disabled="selectedIds.length === 0" :loading="applying"
          @click="handleApplySelected" v-hasPermi="['icbc:invoice-application:apply']">
          <Icon icon="ep:promotion" class="mr-5px" /> 批量发起开票申请（{{ selectedIds.length }}）
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" :selectable="(row: any) => !row.invoicePartnerOrderId" />
      <el-table-column label="收购单号" prop="acquisitionNo" min-width="200" />
      <el-table-column label="出售者" prop="sellerName" min-width="100" />
      <el-table-column label="品类" prop="categoryName" min-width="100" />
      <el-table-column label="计税方法" align="center" width="110">
        <template #default="{ row }">
          <el-tag :type="row.taxMethod === 'SIMPLE' ? 'warning' : 'success'" size="small">
            {{ row.taxMethod === 'SIMPLE' ? '简易计税' : '一般计税' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额(元)" align="right" prop="amount" width="120" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.statusName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="合作方订单号" prop="invoicePartnerOrderId" min-width="200" />
      <el-table-column label="操作" align="center" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handlePreCheck(row)" v-hasPermi="['icbc:invoice-application:query']">
            前置校验
          </el-button>
          <el-button link type="primary" :disabled="!!row.invoicePartnerOrderId"
            @click="handleApplyOne(row)" v-hasPermi="['icbc:invoice-application:apply']">
            发起开票申请
          </el-button>
          <el-button v-if="row.invoicePartnerOrderId" link type="primary" @click="handleQueryStatus(row.invoicePartnerOrderId)">
            查状态
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 前置校验结果 -->
  <el-dialog v-model="checkVisible" title="开票前置校验" width="720px">
    <el-result v-if="checkResult?.allPassed" icon="success" title="所有校验通过，可以发起开票申请" />
    <el-result v-else icon="warning" title="校验未通过，请按下表补齐" />
    <el-table :data="checkResult?.items || []" border size="small">
      <el-table-column label="校验项" prop="name" width="150" />
      <el-table-column label="结果" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.passed ? 'success' : 'danger'" size="small">{{ row.passed ? '通过' : '不通过' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" prop="message" min-width="200" />
      <el-table-column label="如何补齐" prop="remedy" min-width="240" />
    </el-table>
  </el-dialog>

  <!-- 申请结果 -->
  <el-dialog v-model="resultVisible" title="开票申请结果" width="820px">
    <el-table :data="results" border size="small">
      <el-table-column label="收购单号" prop="acquisitionNo" min-width="180" />
      <el-table-column label="结果" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.success ? 'success' : 'danger'" size="small">
            {{ row.success ? (row.duplicate ? '已发起' : '成功') : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" min-width="200">
        <template #default="{ row }">
          <span>{{ row.message }}</span>
          <div v-for="f in row.failures || []" :key="f.code" class="text-12px text-red-500">
            {{ f.name }}：{{ f.message }}（{{ f.remedy }}）
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button v-if="row.confirmPageHtml" link type="primary" @click="openConfirmPage(row)">
            打开自然人确认页面
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <!-- 状态查询 -->
  <el-dialog v-model="statusVisible" title="开票状态" width="560px">
    <el-descriptions v-if="statusResult" :column="2" border>
      <el-descriptions-item label="订单号">{{ statusResult.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="合作方订单号">{{ statusResult.partnerOrderId }}</el-descriptions-item>
      <el-descriptions-item label="自然人确认">{{ confirmStatusLabel(statusResult.confirmStatus) }}</el-descriptions-item>
      <el-descriptions-item label="预开票">{{ preInvoiceStatusLabel(statusResult.preInvoiceStatus) }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">{{ orderStatusLabel(statusResult.orderStatus) }}</el-descriptions-item>
      <el-descriptions-item label="发票号码">{{ statusResult.invoiceNo || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { AcquisitionApi, AcquisitionVO } from '@/api/icbc/acquisition'
import {
  InvoiceApi,
  InvoiceApplicationApi,
  InvoiceApplicationResultVO,
  InvoicePreCheckRespVO,
  InvoiceQueryRespVO
} from '@/api/icbc/invoice'
import { openIcbcForm } from '../util'

defineOptions({ name: 'IcbcInvoiceApplication' })

const message = useMessage()

const loading = ref(false)
const applying = ref(false)
const list = ref<AcquisitionVO[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const queryParams = reactive({ pageNo: 1, pageSize: 10, acquisitionNo: '', sellerName: '', status: 0 })

const baseForm = reactive({
  invoiceType: '02',
  areaCode: '110000',
  drawerName: '',
  drawerCardNumber: '',
  jumpUrlBase: window.location.origin
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AcquisitionApi.getAcquisitionPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const handleSelectionChange = (rows: AcquisitionVO[]) => {
  selectedIds.value = rows.map((r) => r.id!).filter(Boolean)
}

// ==================== 前置校验 ====================
const checkVisible = ref(false)
const checkResult = ref<InvoicePreCheckRespVO>()
const handlePreCheck = async (row: AcquisitionVO) => {
  checkResult.value = await InvoiceApplicationApi.preCheck({
    acquisitionId: row.id!,
    invoiceType: baseForm.invoiceType
  })
  checkVisible.value = true
}

// ==================== 发起申请 ====================
const resultVisible = ref(false)
const results = ref<InvoiceApplicationResultVO[]>([])

const buildBasePayload = () => ({
  invoiceType: baseForm.invoiceType,
  areaCode: baseForm.areaCode,
  drawerName: baseForm.drawerName,
  drawerCardType: '111',
  drawerCardNumber: baseForm.drawerCardNumber,
  jumpUrlBase: baseForm.jumpUrlBase
})

const validateBase = () => {
  if (!baseForm.areaCode || !baseForm.drawerName || !baseForm.drawerCardNumber || !baseForm.jumpUrlBase) {
    message.warning('请先填写应税行为发生地、开票人、开票人证件号与回跳地址前缀')
    return false
  }
  return true
}

const handleApplyOne = async (row: AcquisitionVO) => {
  if (!validateBase()) return
  applying.value = true
  try {
    const result = await InvoiceApplicationApi.apply({ acquisitionId: row.id!, ...buildBasePayload() })
    results.value = [result]
    resultVisible.value = true
    if (result.success) {
      message.success(result.duplicate ? '该收购单已发起过开票申请' : '已发起开票申请')
    }
    await getList()
  } finally {
    applying.value = false
  }
}

const handleApplySelected = async () => {
  if (!validateBase()) return
  applying.value = true
  try {
    results.value = await InvoiceApplicationApi.applyBatch({
      acquisitionIds: selectedIds.value,
      ...buildBasePayload()
    })
    resultVisible.value = true
    const ok = results.value.filter((r) => r.success).length
    message.success(`批量发起完成：成功 ${ok} / ${results.value.length}`)
    await getList()
  } finally {
    applying.value = false
  }
}

const openConfirmPage = (row: InvoiceApplicationResultVO) => {
  openIcbcForm(row.confirmPageHtml || '', '自然人确认页面')
}

// ==================== 状态查询 ====================
const statusVisible = ref(false)
const statusResult = ref<InvoiceQueryRespVO>()
const handleQueryStatus = async (partnerOrderId: string) => {
  statusResult.value = await InvoiceApi.query({ outOrderId: partnerOrderId })
  statusVisible.value = true
}

const CONFIRM_STATUS: Record<number, string> = { 0: '未确认', 1: '自然人确认完成', 2: '全部确认完成' }
const PRE_INVOICE_STATUS: Record<number, string> = {
  0: '初始', 1: '预开票中', 2: '预开票成功', 3: '预开票失败', 4: '预开票取消'
}
const ORDER_STATUS: Record<number, string> = {
  0: '待确认', 1: '已确认', 2: '已支付', 3: '已开票', 4: '已完成', 9: '已取消'
}
const confirmStatusLabel = (s?: number) => (s !== undefined ? CONFIRM_STATUS[s] ?? s : '-')
const preInvoiceStatusLabel = (s?: number) => (s !== undefined ? PRE_INVOICE_STATUS[s] ?? s : '-')
const orderStatusLabel = (s?: number) => (s !== undefined ? ORDER_STATUS[s] ?? s : '-')

const statusTagType = (status?: number) => {
  if (status === 3) return 'success'
  if (status === 4) return 'info'
  if (status === 1) return 'warning'
  return 'primary'
}

onMounted(getList)
</script>
