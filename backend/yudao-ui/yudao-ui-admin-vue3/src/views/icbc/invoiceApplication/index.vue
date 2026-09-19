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
  <el-dialog v-model="statusVisible" title="开票状态（开票 / 缴税 / 上传各自独立）" width="680px">
    <el-descriptions v-if="statusResult" :column="2" border>
      <el-descriptions-item label="订单号">{{ statusResult.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="合作方订单号">{{ statusResult.partnerOrderId }}</el-descriptions-item>
      <el-descriptions-item label="自然人确认">{{ confirmStatusLabel(statusResult.confirmStatus) }}</el-descriptions-item>
      <el-descriptions-item label="预开票">{{ preInvoiceStatusLabel(statusResult.preInvoiceStatus) }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">{{ orderStatusLabel(statusResult.orderStatus) }}</el-descriptions-item>
      <el-descriptions-item label="开票">{{ statusResult.invoiceStatusName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="缴税">{{ statusResult.taxStatusName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="上传">{{ statusResult.uploadStatusName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发票号码">{{ statusResult.invoiceNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="实缴税额">{{ statusResult.taxRealAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="缴税时间">{{ formatTime(statusResult.taxTime) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="应征凭证序号">{{ statusResult.taxVoucherNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="红冲状态">{{ statusResult.redOffsetStatusName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="红冲流水号">{{ statusResult.redSerialNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="红票号码">{{ statusResult.redInvoiceNo || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-alert v-if="statusResult?.nextAction" class="mt-10px" type="warning" :closable="false"
      :title="statusResult.nextAction" />
    <template #footer>
      <el-button v-if="canCancelPreInvoice" type="warning" @click="handleCancelInvoice"
        v-hasPermi="['icbc:invoice-order:cancel']">
        取消预开票
      </el-button>
      <el-button v-if="canApplyRed" type="danger" @click="handleOpenRedApply"
        v-hasPermi="['icbc:red-invoice:apply']">
        发起红冲
      </el-button>
      <el-button v-if="canRevokeRed" @click="handleRevokeRed"
        v-hasPermi="['icbc:red-invoice:revoke']">
        撤销红字确认单
      </el-button>
      <el-button v-if="statusResult && ['缴税成功', '无需缴税'].includes(statusResult.taxStatusName || '')"
        type="primary" @click="handleTaxCertificate">
        查看缴税凭证
      </el-button>
    </template>
  </el-dialog>

  <!-- 发起红冲 -->
  <el-dialog v-model="redApplyVisible" title="发起红字冲销" width="520px">
    <el-form label-width="100px">
      <el-form-item label="红冲原因">
        <el-select v-model="redForm.reason" class="!w-full">
          <el-option label="开票有误（必须全额红冲）" value="01" />
          <el-option label="销货退回" value="02" />
          <el-option label="服务中止" value="03" />
          <el-option label="销售折让" value="04" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="redForm.reason !== '01'" label="红冲金额">
        <el-input-number v-model="redForm.amount" :precision="2" :min="0.01" class="!w-full" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="redForm.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="redApplyVisible = false">取消</el-button>
      <el-button type="primary" :loading="redSubmitting" @click="handleSubmitRedApply">发起红冲</el-button>
    </template>
  </el-dialog>

  <!-- 代办税费缴税凭证 -->
  <el-dialog v-model="certificateVisible" title="代办税费缴税凭证" width="720px">
    <el-descriptions v-if="certificate" :column="2" border>
      <el-descriptions-item label="凭证编号">{{ certificate.certificateNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ certificate.taxStatusName }}</el-descriptions-item>
      <el-descriptions-item label="扣缴义务人">{{ certificate.payerName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="纳税人识别号">{{ certificate.payerTaxNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="出售者">{{ certificate.sellerName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="身份证号">{{ certificate.sellerIdCardNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发票号码">{{ certificate.invoiceNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="开票日期">{{ certificate.invoiceDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发票金额">{{ certificate.invoiceAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="应缴税额">{{ certificate.taxAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="实缴税额">{{ certificate.taxRealAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="缴税时间">{{ certificate.taxTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="缴纳方式">{{ certificate.taxPaymentMethodName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="应征凭证序号">{{ certificate.taxVoucherNo || '-' }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="handleExportCertificate">导出凭证</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { AcquisitionApi, AcquisitionVO } from '@/api/icbc/acquisition'
import {
  InvoiceApi,
  InvoiceApplicationApi,
  InvoiceApplicationResultVO,
  InvoicePreCheckRespVO,
  InvoiceQueryRespVO,
  InvoiceTaxCertificateVO,
  RedInvoiceApi,
  RedInvoiceApplyResultVO
} from '@/api/icbc/invoice'
import { formatDate } from '@/utils/formatTime'
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

// ==================== 红冲与发票取消（#14） ====================
// 预开票成功且未支付时可以取消；已开票（真正出票）后只能红冲
const canCancelPreInvoice = computed(
  () => statusResult.value?.preInvoiceStatus === 2 && statusResult.value?.paymentStatus !== 2
)
const canApplyRed = computed(() => statusResult.value?.invoiceStatus === 2)
// 0/1/2/3/4/5/6/8/11 可撤销；7 红冲成功、9 撤销中、10 已撤销不可撤销
const RED_REVOCABLE = [0, 1, 2, 3, 4, 5, 6, 8, 11]
const canRevokeRed = computed(() => {
  const s = statusResult.value?.redOffsetStatus
  return s !== undefined && RED_REVOCABLE.includes(s)
})

const redApplyVisible = ref(false)
const redSubmitting = ref(false)
const redForm = reactive({ reason: '01', amount: undefined as number | undefined, remark: '' })

const handleOpenRedApply = () => {
  redForm.reason = '01'
  redForm.amount = statusResult.value?.invoiceAmount
  redForm.remark = ''
  redApplyVisible.value = true
}

const handleSubmitRedApply = async () => {
  const partnerOrderId = statusResult.value?.partnerOrderId
  if (!partnerOrderId) return
  redSubmitting.value = true
  try {
    const res: RedInvoiceApplyResultVO = await RedInvoiceApi.apply({
      partnerOrderId,
      reason: redForm.reason,
      amount: redForm.reason === '01' ? undefined : redForm.amount,
      remark: redForm.remark,
      jumpUrlBase: baseForm.jumpUrlBase
    })
    if (res.confirmPageHtml) {
      openIcbcForm(res.confirmPageHtml, '红字确认单页面')
    }
    message.success(res.duplicate ? '该蓝票已有红冲记录' : '已取得红字确认单页面')
    redApplyVisible.value = false
    await handleQueryStatus(partnerOrderId)
  } finally {
    redSubmitting.value = false
  }
}

const handleCancelInvoice = async () => {
  const partnerOrderId = statusResult.value?.partnerOrderId
  if (!partnerOrderId) return
  await message.confirm('确认取消这张尚未支付的预开票吗？')
  await RedInvoiceApi.cancel(partnerOrderId)
  message.success('已取消')
  await handleQueryStatus(partnerOrderId)
}

const handleRevokeRed = async () => {
  const redOffsetNo = statusResult.value?.redSerialNo
  const partnerOrderId = statusResult.value?.partnerOrderId
  if (!redOffsetNo || !partnerOrderId) return
  await message.confirm('确认撤销这张尚未生效的红字确认单吗？')
  await RedInvoiceApi.revoke(redOffsetNo)
  message.success('已撤销')
  await handleQueryStatus(partnerOrderId)
}

// ==================== 缴税凭证 ====================
const certificateVisible = ref(false)
const certificate = ref<InvoiceTaxCertificateVO>()
const handleTaxCertificate = async () => {
  const partnerOrderId = statusResult.value?.partnerOrderId
  if (!partnerOrderId) return
  certificate.value = await InvoiceApi.getTaxCertificate(partnerOrderId)
  certificateVisible.value = true
}
const handleExportCertificate = async () => {
  const partnerOrderId = certificate.value?.partnerOrderId
  if (!partnerOrderId) return
  await InvoiceApi.exportTaxCertificate(partnerOrderId)
}

const formatTime = (value?: Date) => (value ? formatDate(new Date(value)) : '')

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
