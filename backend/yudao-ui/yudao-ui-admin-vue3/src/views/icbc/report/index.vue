<template>
  <ContentWrap title="经营报表">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb-10px"
      title="四张表各按自己的口径取数，不相加、不互相代替：采购履约分五口径，收购台账按各重量口径分列，库存只给数量口径，结算付款按结算单汇总。实际收购量不含预约约量与采购计划量。"
    />
    <el-tabs v-model="activeTab">
      <!-- ==================== 采购履约 ==================== -->
      <el-tab-pane label="采购履约" name="performance">
        <TableDefinition :definition="definition('PURCHASE_PERFORMANCE')" />
        <el-form :inline="true" :model="performanceQuery" class="-mb-15px" label-width="90px">
          <el-form-item label="订单号">
            <el-input v-model="performanceQuery.orderNo" placeholder="请输入" clearable @keyup.enter="queryPerformance" class="!w-200px" />
          </el-form-item>
          <el-form-item label="交易对方">
            <el-input v-model="performanceQuery.counterpartyName" placeholder="请输入" clearable @keyup.enter="queryPerformance" class="!w-160px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="performanceQuery.status" placeholder="不限" clearable class="!w-140px">
              <el-option v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="queryPerformance"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="performanceList" :stripe="true" class="mt-10px">
          <el-table-column label="订单号" prop="orderNo" min-width="170" />
          <el-table-column label="交易对方" prop="counterpartyName" width="120" />
          <el-table-column label="状态" prop="statusName" width="100" />
          <el-table-column label="计划量" align="right" prop="planQuantity" width="110" />
          <el-table-column label="实际履约量" align="right" prop="performedQuantity" width="120" />
          <el-table-column label="余额" align="right" prop="balanceQuantity" width="110" />
          <el-table-column label="验收量" align="right" prop="acceptedQuantity" width="110" />
          <el-table-column label="入库量" align="right" prop="stockedQuantity" width="110" />
          <el-table-column label="结算量" align="right" prop="settledQuantity" width="110" />
          <el-table-column label="到期日" align="center" prop="endDate" width="120" />
          <el-table-column label="进度" width="160">
            <template #default="{ row }">
              <span v-if="row.completionRatio === null || row.completionRatio === undefined">—</span>
              <span v-else>{{ (row.completionRatio * 100).toFixed(1) }}%（{{ row.completionBasisName }}）</span>
            </template>
          </el-table-column>
          <el-table-column label="异常" width="160">
            <template #default="{ row }">
              <el-tag v-if="row.expired" type="danger" size="small" class="mr-5px">过期</el-tag>
              <el-tag v-if="row.overQuantity" type="warning" size="small">超量</el-tag>
              <span v-if="!row.expired && !row.overQuantity">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openProgress(row.orderId!)">五口径明细</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination :total="performanceTotal" v-model:page="performanceQuery.pageNo" v-model:limit="performanceQuery.pageSize" @pagination="getPerformance" />
      </el-tab-pane>

      <!-- ==================== 收购台账 ==================== -->
      <el-tab-pane label="收购台账" name="ledger">
        <TableDefinition :definition="definition('ACQUISITION_LEDGER')" />
        <el-form :inline="true" :model="ledgerQuery" class="-mb-15px" label-width="80px">
          <el-form-item label="收购单号">
            <el-input v-model="ledgerQuery.acquisitionNo" placeholder="请输入" clearable @keyup.enter="queryLedger" class="!w-200px" />
          </el-form-item>
          <el-form-item label="出售者">
            <el-input v-model="ledgerQuery.sellerName" placeholder="请输入" clearable @keyup.enter="queryLedger" class="!w-140px" />
          </el-form-item>
          <el-form-item label="品类">
            <el-input v-model="ledgerQuery.categoryName" placeholder="请输入" clearable @keyup.enter="queryLedger" class="!w-140px" />
          </el-form-item>
          <el-form-item label="回收方式">
            <el-select v-model="ledgerQuery.directAcquisition" placeholder="不限" clearable class="!w-140px">
              <el-option label="直接收购" :value="true" />
              <el-option label="关联采购订单" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="queryLedger"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="ledgerList" :stripe="true" class="mt-10px">
          <el-table-column label="收购单号" prop="acquisitionNo" min-width="170" />
          <el-table-column label="出售者" prop="sellerName" width="100" />
          <el-table-column label="场站" prop="stationName" width="120" />
          <el-table-column label="回收方式" prop="acquisitionModeText" width="150" show-overflow-tooltip />
          <el-table-column label="品类" prop="categoryName" width="90" />
          <el-table-column label="等级" prop="specification" width="80" />
          <el-table-column label="毛重" align="right" prop="grossWeight" width="100" />
          <el-table-column label="皮重" align="right" prop="tareWeight" width="100" />
          <el-table-column label="净重" align="right" prop="netWeight" width="100" />
          <el-table-column label="结算重量" align="right" prop="settlementWeight" width="110" />
          <el-table-column label="接收量" align="right" prop="acceptedWeight" width="100" />
          <el-table-column label="退回量" align="right" prop="rejectedWeight" width="100" />
          <el-table-column label="余货出场" align="right" prop="residualWeight" width="100" />
          <el-table-column label="成交金额" align="right" prop="amount" width="110" />
          <el-table-column label="对应单据" min-width="160">
            <template #default="{ row }">
              <span>{{ row.settlementNo || '未归结算' }}</span>
              <span v-if="row.invoicePartnerOrderId" class="ml-5px">/ 已开票</span>
            </template>
          </el-table-column>
          <el-table-column label="交易时间" align="center" prop="tradeTime" :formatter="dateFormatter" width="170" />
        </el-table>
        <Pagination :total="ledgerTotal" v-model:page="ledgerQuery.pageNo" v-model:limit="ledgerQuery.pageSize" @pagination="getLedger" />
      </el-tab-pane>

      <!-- ==================== 库存 ==================== -->
      <el-tab-pane label="库存" name="stock">
        <TableDefinition :definition="definition('STOCK')" />
        <el-radio-group v-model="stockView" class="mb-10px" @change="onStockViewChange">
          <el-radio-button label="balance">在库量</el-radio-button>
          <el-radio-button label="record">入出流水</el-radio-button>
        </el-radio-group>
        <el-form :inline="true" :model="stockQuery" class="-mb-15px" label-width="80px">
          <el-form-item label="品类">
            <el-input v-model.number="stockQuery.goodsConfigId" placeholder="品类编号" clearable @keyup.enter="queryStock" class="!w-140px" />
          </el-form-item>
          <el-form-item label="仓库">
            <el-input v-model.number="stockQuery.warehouseId" placeholder="仓库编号" clearable @keyup.enter="queryStock" class="!w-140px" />
          </el-form-item>
          <el-form-item label="批次">
            <el-input v-model.number="stockQuery.batchId" placeholder="批次编号" clearable @keyup.enter="queryStock" class="!w-140px" />
          </el-form-item>
          <el-form-item>
            <el-button @click="queryStock"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
          </el-form-item>
        </el-form>

        <el-table v-if="stockView === 'balance'" v-loading="loading" :data="stockBalanceList" :stripe="true" class="mt-10px">
          <el-table-column label="品类" prop="categoryName" min-width="100" />
          <el-table-column label="仓库" prop="warehouseName" width="120" />
          <el-table-column label="库位" prop="locationName" width="120" />
          <el-table-column label="批次" prop="batchNo" width="140" />
          <el-table-column label="入库时间" align="center" prop="batchInTime" :formatter="dateFormatter" width="170" />
          <el-table-column label="库龄(天)" align="right" prop="ageDays" width="100" />
          <el-table-column label="在库数量" align="right" prop="count" width="120" />
        </el-table>
        <el-table v-else v-loading="loading" :data="stockRecordList" :stripe="true" class="mt-10px">
          <el-table-column label="时间" align="center" prop="createTime" :formatter="dateFormatter" width="170" />
          <el-table-column label="业务类型" prop="bizTypeName" width="140" />
          <el-table-column label="业务单号" prop="bizNo" min-width="150" />
          <el-table-column label="品类" prop="categoryName" width="100" />
          <el-table-column label="仓库" prop="warehouseName" width="120" />
          <el-table-column label="库位" prop="locationName" width="120" />
          <el-table-column label="批次" prop="batchNo" width="140" />
          <el-table-column label="出入库" align="right" prop="count" width="110" />
          <el-table-column label="变动后库存" align="right" prop="totalCount" width="120" />
        </el-table>
        <Pagination :total="stockTotal" v-model:page="stockQuery.pageNo" v-model:limit="stockQuery.pageSize" @pagination="getStock" />
      </el-tab-pane>

      <!-- ==================== 结算付款 ==================== -->
      <el-tab-pane label="结算付款" name="settlement">
        <TableDefinition :definition="definition('SETTLEMENT_PAYMENT')" />
        <el-form :inline="true" :model="settlementQuery" class="-mb-15px" label-width="90px">
          <el-form-item label="结算单号">
            <el-input v-model="settlementQuery.settlementNo" placeholder="请输入" clearable @keyup.enter="querySettlement" class="!w-200px" />
          </el-form-item>
          <el-form-item label="出售者">
            <el-input v-model="settlementQuery.sellerName" placeholder="请输入" clearable @keyup.enter="querySettlement" class="!w-140px" />
          </el-form-item>
          <el-form-item label="办理进度">
            <el-select v-model="settlementQuery.paymentProgress" placeholder="不限" clearable class="!w-140px">
              <el-option label="未办理" value="UNPAID" />
              <el-option label="办理中" value="PROCESSING" />
              <el-option label="已支付" value="SUCCESS" />
              <el-option label="异常" value="FAILED" />
            </el-select>
          </el-form-item>
          <el-form-item label="回单">
            <el-select v-model="settlementQuery.receiptStatus" placeholder="不限" clearable class="!w-130px">
              <el-option label="已回单" value="RECEIVED" />
              <el-option label="未回单" value="PENDING" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="querySettlement"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="settlementList" :stripe="true" class="mt-10px">
          <el-table-column label="结算单号" prop="settlementNo" min-width="180" />
          <el-table-column label="出售者" prop="sellerName" width="100" />
          <el-table-column label="场站" prop="stationName" width="120" />
          <el-table-column label="结算金额" align="right" prop="settlementAmount" width="120" />
          <el-table-column label="收购单数" align="right" prop="acquisitionCount" width="100" />
          <el-table-column label="确认状态" prop="confirmStatusName" width="120" />
          <el-table-column label="办理进度" width="110">
            <template #default="{ row }">
              <el-tag :type="progressTagType(row.paymentProgress)" size="small">{{ row.paymentProgressName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="已支付/待办/异常" width="140">
            <template #default="{ row }">{{ row.paidCount }} / {{ row.pendingCount }} / {{ row.failedCount }}</template>
          </el-table-column>
          <el-table-column label="回单状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.receiptStatus === 'RECEIVED' ? 'success' : 'info'" size="small">
                {{ row.receiptStatus === 'RECEIVED' ? '已回单' : '未回单' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失败原因" prop="failReason" min-width="150" show-overflow-tooltip />
          <el-table-column label="未办理时长(小时)" align="right" prop="unhandledHours" width="140" />
        </el-table>
        <Pagination :total="settlementTotal" v-model:page="settlementQuery.pageNo" v-model:limit="settlementQuery.pageSize" @pagination="getSettlement" />
      </el-tab-pane>

      <!-- ==================== 异常表 ==================== -->
      <el-tab-pane label="异常表" name="anomaly">
        <TableDefinition :definition="definition('ANOMALY')" />
        <el-form :inline="true" :model="anomalyQuery" class="-mb-15px" label-width="80px">
          <el-form-item label="类型">
            <el-select v-model="anomalyQuery.type" placeholder="全部类型" clearable class="!w-180px">
              <el-option v-for="item in anomalyTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="出售者">
            <el-input v-model="anomalyQuery.sellerName" placeholder="请输入" clearable @keyup.enter="queryAnomaly" class="!w-140px" />
          </el-form-item>
          <el-form-item label="来源单号">
            <el-input v-model="anomalyQuery.bizNo" placeholder="请输入" clearable @keyup.enter="queryAnomaly" class="!w-180px" />
          </el-form-item>
          <el-form-item>
            <el-button @click="queryAnomaly"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="anomalyList" :stripe="true" class="mt-10px">
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag :type="row.severity === 'DANGER' ? 'danger' : 'warning'" size="small">{{ row.typeName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源单号" prop="bizNo" min-width="170" />
          <el-table-column label="主体" prop="subject" width="110" />
          <el-table-column label="品类" prop="categoryName" width="90" />
          <el-table-column label="异常说明" prop="detail" min-width="280" show-overflow-tooltip />
          <el-table-column label="涉及数量" align="right" prop="quantity" width="110" />
          <el-table-column label="涉及金额" align="right" prop="amount" width="110" />
          <el-table-column label="时间" align="center" prop="time" :formatter="dateFormatter" width="170" />
          <el-table-column label="口径" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tooltip :content="row.definition" placement="top">
                <span class="text-ellipsis">{{ row.definition }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="下钻" align="center" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="drillDown(row)">去处理</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination :total="anomalyTotal" v-model:page="anomalyQuery.pageNo" v-model:limit="anomalyQuery.pageSize" @pagination="getAnomaly" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <!-- 采购履约五口径明细（复用 #47 的进度接口，不在前端重算口径） -->
  <el-dialog v-model="progressVisible" title="采购履约五口径明细" width="900px">
    <el-descriptions v-if="progress" :column="3" border>
      <el-descriptions-item label="订单号">{{ progress.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ progress.statusName }}</el-descriptions-item>
      <el-descriptions-item label="完成比例口径">{{ progress.completionBasisName }}</el-descriptions-item>
    </el-descriptions>
    <el-table v-if="progress" :data="progress.items || []" :stripe="true" class="mt-10px">
      <el-table-column label="品类" prop="categoryName" min-width="120" />
      <el-table-column label="计划量" align="right" prop="quantity" width="110" />
      <el-table-column label="验收量" align="right" prop="acceptedQuantity" width="110" />
      <el-table-column label="入库量" align="right" prop="stockedQuantity" width="110" />
      <el-table-column label="结算量" align="right" prop="settledQuantity" width="110" />
      <el-table-column label="未履行" align="right" prop="unperformedQuantity" width="110" />
      <el-table-column label="超量" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.overQuantity" type="warning" size="small">超量</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { PurchaseOrderApi, PurchaseOrderProgressVO } from '@/api/icbc/purchaseOrder'
import { ReportApi, ReportTableVO } from '@/api/icbc/report'
import { dateFormatter } from '@/utils/formatTime'
import TableDefinition from './TableDefinition.vue'

defineOptions({ name: 'IcbcReport' })

const { push } = useRouter()

const activeTab = ref('performance')
const loading = ref(false)
const tableDefinitions = ref<Record<string, string>>({})
const definition = (code: string) => tableDefinitions.value[code] || ''

const orderStatusOptions = [
  { label: '草稿', value: 0 },
  { label: '执行中', value: 1 },
  { label: '暂停', value: 2 },
  { label: '完成', value: 3 },
  { label: '关闭', value: 4 }
]
const anomalyTypeOptions = [
  { label: '磅差', value: 'WEIGHT_DIFF' },
  { label: '超采购量', value: 'OVER_PURCHASE_QUANTITY' },
  { label: '超入库量', value: 'OVER_STOCK_IN' },
  { label: '重复关联', value: 'DUPLICATE_LINK' },
  { label: '长期未确认', value: 'LONG_UNCONFIRMED' },
  { label: '资料缺失', value: 'MISSING_EVIDENCE' }
]

// ==================== 采购履约 ====================
const performanceList = ref<any[]>([])
const performanceTotal = ref(0)
const performanceQuery = reactive({ pageNo: 1, pageSize: 10, orderNo: undefined, counterpartyName: undefined, status: undefined as number | undefined })
const getPerformance = async () => {
  loading.value = true
  try {
    const data = await ReportApi.getPurchasePerformancePage(performanceQuery)
    performanceList.value = data.list
    performanceTotal.value = data.total
  } finally {
    loading.value = false
  }
}
const queryPerformance = () => {
  performanceQuery.pageNo = 1
  getPerformance()
}

const progressVisible = ref(false)
const progress = ref<PurchaseOrderProgressVO>()
const openProgress = async (orderId: number) => {
  progress.value = await PurchaseOrderApi.getProgress(orderId)
  progressVisible.value = true
}

// ==================== 收购台账 ====================
const ledgerList = ref<any[]>([])
const ledgerTotal = ref(0)
const ledgerQuery = reactive({ pageNo: 1, pageSize: 10, acquisitionNo: undefined, sellerName: undefined, categoryName: undefined, directAcquisition: undefined as boolean | undefined })
const getLedger = async () => {
  loading.value = true
  try {
    const data = await ReportApi.getAcquisitionLedgerPage(ledgerQuery)
    ledgerList.value = data.list
    ledgerTotal.value = data.total
  } finally {
    loading.value = false
  }
}
const queryLedger = () => {
  ledgerQuery.pageNo = 1
  getLedger()
}

// ==================== 库存 ====================
const stockView = ref<'balance' | 'record'>('balance')
const stockBalanceList = ref<any[]>([])
const stockRecordList = ref<any[]>([])
const stockTotal = ref(0)
const stockQuery = reactive({ pageNo: 1, pageSize: 10, goodsConfigId: undefined, warehouseId: undefined, locationId: undefined, batchId: undefined, bizType: undefined, bizNo: undefined })
const getStock = async () => {
  loading.value = true
  try {
    if (stockView.value === 'balance') {
      const data = await ReportApi.getStockBalancePage(stockQuery)
      stockBalanceList.value = data.list
      stockTotal.value = data.total
    } else {
      const data = await ReportApi.getStockRecordPage(stockQuery)
      stockRecordList.value = data.list
      stockTotal.value = data.total
    }
  } finally {
    loading.value = false
  }
}
const queryStock = () => {
  stockQuery.pageNo = 1
  getStock()
}
const onStockViewChange = () => {
  stockQuery.pageNo = 1
  getStock()
}

// ==================== 结算付款 ====================
const settlementList = ref<any[]>([])
const settlementTotal = ref(0)
const settlementQuery = reactive({ pageNo: 1, pageSize: 10, settlementNo: undefined, sellerName: undefined, paymentProgress: undefined, receiptStatus: undefined })
const getSettlement = async () => {
  loading.value = true
  try {
    const data = await ReportApi.getSettlementPaymentPage(settlementQuery)
    settlementList.value = data.list
    settlementTotal.value = data.total
  } finally {
    loading.value = false
  }
}
const querySettlement = () => {
  settlementQuery.pageNo = 1
  getSettlement()
}
const progressTagType = (progress?: string) => {
  if (progress === 'SUCCESS') return 'success'
  if (progress === 'FAILED') return 'danger'
  if (progress === 'PROCESSING') return 'warning'
  return 'info'
}

// ==================== 异常表 ====================
const anomalyList = ref<any[]>([])
const anomalyTotal = ref(0)
const anomalyQuery = reactive({ pageNo: 1, pageSize: 10, type: undefined, sellerName: undefined, bizNo: undefined })
const getAnomaly = async () => {
  loading.value = true
  try {
    const data = await ReportApi.getAnomalyPage(anomalyQuery)
    anomalyList.value = data.list
    anomalyTotal.value = data.total
  } finally {
    loading.value = false
  }
}
const queryAnomaly = () => {
  anomalyQuery.pageNo = 1
  getAnomaly()
}
const drillDown = (row: any) => {
  const path = drillDownPath(row.drillDown)
  if (!path) return
  push({ path, query: row.bizNo ? { no: row.bizNo } : {} })
}
const drillDownPath = (code?: string) => {
  switch (code) {
    case 'acquisition':
      return '/recycling/acquisition'
    case 'acquisition-weight-diff':
      return '/recycling/weight-diff'
    case 'purchase-order-progress':
      return '/purchase/order'
    case 'stock-in':
      return '/warehouse/stock-in'
    case 'payment':
      return '/finance/payment'
    case 'settlement':
      return '/settlement/list'
    default:
      return undefined
  }
}

// ==================== 初始化 ====================
onMounted(async () => {
  const tables = await ReportApi.getTables()
  tableDefinitions.value = (tables || []).reduce((acc: Record<string, string>, item: ReportTableVO) => {
    acc[item.code!] = item.definition!
    return acc
  }, {})
  await getPerformance()
})
</script>
