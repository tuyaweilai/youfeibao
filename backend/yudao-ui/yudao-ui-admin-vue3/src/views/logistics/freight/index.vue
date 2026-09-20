<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="运费是回收企业向承运商支付的运输服务费用，是另一笔账，不改变收购单金额与发票金额。它与收购单调整项里的「运费」（出售者货款上的加减项）不是同一个东西。自有车不产生承运商运费；路桥与燃油等内部费用走「运输费用」页，按实际承担方记。付款只登记外部付款凭证，不接对公付款通道。"
    />
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ==================== 运费单 ==================== -->
      <el-tab-pane label="运费单（按趟次）" name="order">
        <el-form class="-mb-15px" :model="orderQuery" ref="orderQueryRef" :inline="true" label-width="80px">
          <el-form-item label="运费单号" prop="freightNo">
            <el-input v-model="orderQuery.freightNo" placeholder="请输入" clearable @keyup.enter="getOrderList" class="!w-190px" />
          </el-form-item>
          <el-form-item label="任务单号" prop="taskNo">
            <el-input v-model="orderQuery.taskNo" placeholder="请输入" clearable @keyup.enter="getOrderList" class="!w-190px" />
          </el-form-item>
          <el-form-item label="承运商" prop="carrierId">
            <el-select v-model="orderQuery.carrierId" placeholder="请选择" clearable filterable class="!w-180px">
              <el-option v-for="c in carrierList" :key="c.id" :label="c.name" :value="c.id!" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="orderQuery.status" placeholder="请选择" clearable class="!w-150px">
              <el-option v-for="(label, value) in FREIGHT_STATUS_NAME" :key="value" :label="label" :value="Number(value)" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="getOrderList"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
            <el-button @click="resetOrderQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
            <el-button type="primary" plain @click="openOrderForm()" v-hasPermi="['logistics:freight:create']">
              <Icon icon="ep:plus" class="mr-5px" /> 汇集运费
            </el-button>
            <el-button type="success" plain @click="handleExportOrder" :loading="exportLoading" v-hasPermi="['logistics:freight:export']">
              <Icon icon="ep:download" class="mr-5px" /> 导出
            </el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="orderLoading" :data="orderList" :stripe="true" class="mt-15px">
          <el-table-column label="运费单号" prop="freightNo" min-width="180" />
          <el-table-column label="任务单号" prop="taskNo" min-width="180" />
          <el-table-column label="承运商" prop="carrierName" min-width="140" />
          <el-table-column label="合同" prop="contractNo" min-width="160" />
          <el-table-column label="计费" width="120">
            <template #default="{ row }">{{ row.billingModeName }} {{ row.billQuantity }} × {{ formatAmount(row.billUnitPrice) }}</template>
          </el-table-column>
          <el-table-column label="应有应付" align="right" width="110">
            <template #default="{ row }">{{ formatAmount(row.expectedAmount) }}</template>
          </el-table-column>
          <el-table-column label="实际应付" align="right" width="110">
            <template #default="{ row }">{{ formatAmount(row.actualAmount) }}</template>
          </el-table-column>
          <el-table-column label="差异" align="right" width="140">
            <template #default="{ row }">
              <span v-if="row.varianceAmount === null || row.varianceAmount === undefined">—</span>
              <el-tooltip v-else :content="row.varianceReason || '无差异'" placement="top">
                <span :class="row.varianceAmount === 0 ? '' : 'text-[var(--el-color-danger)]'">{{ formatAmount(row.varianceAmount) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="130">
            <template #default="{ row }">
              <el-tag :type="FREIGHT_STATUS_TAG[row.status] ?? 'info'">{{ row.statusName || '未知' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="付款凭证" prop="paymentVoucherNo" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" align="center" width="240" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" link type="primary" @click="openOrderForm(row)" v-hasPermi="['logistics:freight:update']">修改</el-button>
              <el-button v-if="row.status === 0" link type="success" @click="openConfirm(row)" v-hasPermi="['logistics:freight:confirm']">确认应付</el-button>
              <el-button v-if="row.status === 1" link type="warning" @click="openPay(row)" v-hasPermi="['logistics:freight:pay']">登记付款凭证</el-button>
              <el-button link type="info" @click="openOrderDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination :total="orderTotal" v-model:page="orderQuery.pageNo" v-model:limit="orderQuery.pageSize" @pagination="getOrderList" />
      </el-tab-pane>

      <!-- ==================== 对账汇总 ==================== -->
      <el-tab-pane label="对账汇总" name="reconciliation">
        <el-form class="-mb-15px" :model="reconQuery" ref="reconQueryRef" :inline="true" label-width="80px">
          <el-form-item label="承运商" prop="carrierName">
            <el-input v-model="reconQuery.carrierName" placeholder="请输入" clearable @keyup.enter="getReconciliation" class="!w-180px" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="reconQuery.status" placeholder="请选择" clearable class="!w-150px">
              <el-option v-for="(label, value) in FREIGHT_STATUS_NAME" :key="value" :label="label" :value="Number(value)" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="getReconciliation"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
            <el-button @click="resetReconQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
            <el-button type="success" plain @click="handleExportReconciliation" :loading="exportLoading" v-hasPermi="['logistics:freight:export']">
              <Icon icon="ep:download" class="mr-5px" /> 导出对账
            </el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="reconLoading" :data="reconList" :stripe="true" class="mt-15px" show-summary :summary-method="reconSummary">
          <el-table-column label="承运商" prop="carrierName" min-width="160" />
          <el-table-column label="合同" prop="contractNo" min-width="170" />
          <el-table-column label="趟次" align="right" prop="tripCount" width="80" />
          <el-table-column label="应有应付合计" align="right" width="140">
            <template #default="{ row }">{{ formatAmount(row.expectedTotal) }}</template>
          </el-table-column>
          <el-table-column label="实际应付合计" align="right" width="140">
            <template #default="{ row }">{{ formatAmount(row.actualTotal) }}</template>
          </el-table-column>
          <el-table-column label="差异合计" align="right" width="130">
            <template #default="{ row }">{{ formatAmount(row.varianceTotal) }}</template>
          </el-table-column>
          <el-table-column label="待确认" align="center" prop="pendingConfirmCount" width="90" />
          <el-table-column label="已登记付款凭证" align="center" prop="voucherRegisteredCount" width="130" />
        </el-table>
      </el-tab-pane>

      <!-- ==================== 运输费用（内部成本） ==================== -->
      <el-tab-pane label="运输费用（内部成本）" name="cost">
        <el-form class="-mb-15px" :model="costQuery" ref="costQueryRef" :inline="true" label-width="80px">
          <el-form-item label="任务单号" prop="taskNo">
            <el-input v-model="costQuery.taskNo" placeholder="请输入" clearable @keyup.enter="getCostList" class="!w-190px" />
          </el-form-item>
          <el-form-item label="费用类型" prop="costType">
            <el-select v-model="costQuery.costType" placeholder="请选择" clearable class="!w-150px">
              <el-option v-for="(label, value) in COST_TYPE_NAME" :key="value" :label="label" :value="Number(value)" />
            </el-select>
          </el-form-item>
          <el-form-item label="承担方" prop="bearer">
            <el-select v-model="costQuery.bearer" placeholder="请选择" clearable class="!w-160px">
              <el-option v-for="(label, value) in BEARER_NAME" :key="value" :label="label" :value="Number(value)" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="getCostList"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
            <el-button @click="resetCostQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
            <el-button type="primary" plain @click="openCostForm()" v-hasPermi="['logistics:transport-cost:create']">
              <Icon icon="ep:plus" class="mr-5px" /> 登记运输费用
            </el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="costLoading" :data="costList" :stripe="true" class="mt-15px">
          <el-table-column label="任务单号" prop="taskNo" min-width="180" />
          <el-table-column label="费用类型" align="center" prop="costTypeName" width="110" />
          <el-table-column label="费用名称" prop="name" min-width="150" />
          <el-table-column label="金额" align="right" width="120">
            <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="承担方" align="center" prop="bearerName" width="120" />
          <el-table-column label="发生日期" prop="occurDate" width="120" />
          <el-table-column label="备注" prop="remark" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" align="center" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCostForm(row)" v-hasPermi="['logistics:transport-cost:update']">编辑</el-button>
              <el-button link type="danger" @click="handleDeleteCost(row.id)" v-hasPermi="['logistics:transport-cost:delete']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination :total="costTotal" v-model:page="costQuery.pageNo" v-model:limit="costQuery.pageSize" @pagination="getCostList" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <!-- 汇集运费 -->
  <Dialog :title="orderFormTitle" v-model="orderFormVisible" width="520px">
    <el-form ref="orderFormRef" :model="orderForm" :rules="orderFormRules" label-width="100px" v-loading="orderFormLoading">
      <el-form-item label="运输任务" prop="taskId">
        <el-input-number v-model="orderForm.taskId" :min="1" :disabled="!!orderForm.id" class="!w-200px" />
        <span class="ml-10px text-[var(--el-text-color-secondary)]">这一趟必须是承运商的车</span>
      </el-form-item>
      <el-form-item label="承运商" prop="carrierId">
        <el-select v-model="orderForm.carrierId" placeholder="先选承运商，再选合同" filterable class="w-full" @change="onCarrierChange">
          <el-option v-for="c in carrierList" :key="c.id" :label="c.name" :value="c.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="承运合同" prop="contractId">
        <el-select v-model="orderForm.contractId" placeholder="运价来自合同" filterable class="w-full">
          <el-option
            v-for="ct in contractList"
            :key="ct.id"
            :label="`${ct.contractNo}（${ct.billingModeName} ${formatAmount(ct.unitPrice)}）`"
            :value="ct.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="计费量" prop="billQuantity">
        <el-input-number v-model="orderForm.billQuantity" :min="0.0001" :precision="4" class="!w-200px" />
        <span class="ml-10px text-[var(--el-text-color-secondary)]">按车填趟数、按吨填吨数、按公里填公里数</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitOrderForm" type="primary" :disabled="orderFormLoading">确 定</el-button>
      <el-button @click="orderFormVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 确认应付 -->
  <Dialog title="确认应付（差异必须留原因）" v-model="confirmVisible" width="520px">
    <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="110px">
      <el-form-item label="应有应付">
        <span>{{ formatAmount(currentOrder?.expectedAmount) }}</span>
      </el-form-item>
      <el-form-item label="实际应付" prop="actualAmount">
        <el-input-number v-model="confirmForm.actualAmount" :min="0" :precision="2" class="!w-200px" />
      </el-form-item>
      <el-form-item label="差异原因" prop="varianceReason">
        <el-input v-model="confirmForm.varianceReason" placeholder="与应有应付不同时必填，差异不抹平" />
      </el-form-item>
      <el-form-item label="确认备注" prop="confirmRemark">
        <el-input v-model="confirmForm.confirmRemark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitConfirm" type="primary">确 定</el-button>
      <el-button @click="confirmVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 登记外部付款凭证 -->
  <Dialog title="登记外部付款凭证（不接对公付款通道）" v-model="payVisible" width="520px">
    <el-form ref="payFormRef" :model="payForm" label-width="120px">
      <el-form-item label="付款凭证号" prop="paymentVoucherNo">
        <el-input v-model="payForm.paymentVoucherNo" placeholder="与附件至少填一个" />
      </el-form-item>
      <el-form-item label="凭证附件" prop="paymentVoucherUrl">
        <el-input v-model="payForm.paymentVoucherUrl" placeholder="附件 URL" />
      </el-form-item>
      <el-form-item label="实付金额" prop="paymentAmount">
        <el-input-number v-model="payForm.paymentAmount" :min="0" :precision="2" class="!w-200px" />
      </el-form-item>
      <el-form-item label="付款时间" prop="paidAt">
        <el-date-picker v-model="payForm.paidAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="!w-220px" />
      </el-form-item>
      <el-form-item label="付款备注" prop="paymentRemark">
        <el-input v-model="payForm.paymentRemark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitPay" type="primary">确 定</el-button>
      <el-button @click="payVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 运输费用（内部成本） -->
  <Dialog :title="costFormTitle" v-model="costFormVisible" width="520px">
    <el-form ref="costFormRef" :model="costForm" :rules="costRules" label-width="100px" v-loading="costFormLoading">
      <el-form-item label="运输任务" prop="taskId">
        <el-input-number v-model="costForm.taskId" :min="1" :disabled="!!costForm.id" class="!w-200px" />
      </el-form-item>
      <el-form-item label="费用类型" prop="costType">
        <el-select v-model="costForm.costType" class="w-full">
          <el-option v-for="(label, value) in COST_TYPE_NAME" :key="value" :label="label" :value="Number(value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="费用名称" prop="name">
        <el-input v-model="costForm.name" placeholder="如：绕行过路费" />
      </el-form-item>
      <el-form-item label="金额" prop="amount">
        <el-input-number v-model="costForm.amount" :min="0" :precision="2" class="!w-200px" />
      </el-form-item>
      <el-form-item label="承担方" prop="bearer">
        <el-radio-group v-model="costForm.bearer">
          <el-radio :value="2">本企业承担</el-radio>
          <el-radio :value="1">承运商承担</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="发生日期" prop="occurDate">
        <el-date-picker v-model="costForm.occurDate" type="date" value-format="YYYY-MM-DD" class="!w-200px" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="costForm.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitCostForm" type="primary" :disabled="costFormLoading">确 定</el-button>
      <el-button @click="costFormVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 运费单详情 -->
  <Dialog title="运费单详情" v-model="orderDetailVisible" width="600px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="运费单号">{{ currentOrder?.freightNo }}</el-descriptions-item>
      <el-descriptions-item label="任务单号">{{ currentOrder?.taskNo }}</el-descriptions-item>
      <el-descriptions-item label="承运商">{{ currentOrder?.carrierName }}</el-descriptions-item>
      <el-descriptions-item label="承运合同">{{ currentOrder?.contractNo }}</el-descriptions-item>
      <el-descriptions-item label="计费方式">{{ currentOrder?.billingModeName }}</el-descriptions-item>
      <el-descriptions-item label="计费量">{{ currentOrder?.billQuantity }}</el-descriptions-item>
      <el-descriptions-item label="运价">{{ formatAmount(currentOrder?.billUnitPrice) }}</el-descriptions-item>
      <el-descriptions-item label="基础运费">{{ formatAmount(currentOrder?.baseAmount) }}</el-descriptions-item>
      <el-descriptions-item label="附加费净额">{{ formatAmount(currentOrder?.surchargeAmount) }}</el-descriptions-item>
      <el-descriptions-item label="应有应付">{{ formatAmount(currentOrder?.expectedAmount) }}</el-descriptions-item>
      <el-descriptions-item label="实际应付">{{ formatAmount(currentOrder?.actualAmount) }}</el-descriptions-item>
      <el-descriptions-item label="差异">{{ formatAmount(currentOrder?.varianceAmount) }}</el-descriptions-item>
      <el-descriptions-item label="差异原因" :span="2">{{ currentOrder?.varianceReason || '—' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ currentOrder?.statusName }}</el-descriptions-item>
      <el-descriptions-item label="确认人">{{ currentOrder?.confirmByName || '—' }}</el-descriptions-item>
      <el-descriptions-item label="确认时间">{{ currentOrder?.confirmTime || '—' }}</el-descriptions-item>
      <el-descriptions-item label="确认备注">{{ currentOrder?.confirmRemark || '—' }}</el-descriptions-item>
      <el-descriptions-item label="付款凭证号">{{ currentOrder?.paymentVoucherNo || '—' }}</el-descriptions-item>
      <el-descriptions-item label="实付金额">{{ formatAmount(currentOrder?.paymentAmount) }}</el-descriptions-item>
      <el-descriptions-item label="付款时间">{{ currentOrder?.paidAt || '—' }}</el-descriptions-item>
      <el-descriptions-item label="付款备注">{{ currentOrder?.paymentRemark || '—' }}</el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script setup lang="ts">
import { LogisticsCarrierApi, LogisticsCarrierVO } from '@/api/logistics/carrier'
import {
  LogisticsCarrierContractApi,
  LogisticsCarrierContractVO
} from '@/api/logistics/carrierContract'
import {
  LogisticsFreightApi,
  LogisticsFreightVO,
  LogisticsFreightReconciliationVO
} from '@/api/logistics/freight'
import { LogisticsTransportCostApi, LogisticsTransportCostVO } from '@/api/logistics/transportCost'
import download from '@/utils/download'

defineOptions({ name: 'LogisticsFreight' })

const FREIGHT_STATUS_NAME: Record<number, string> = { 0: '待确认应付', 1: '已确认应付', 2: '已登记付款凭证' }
const FREIGHT_STATUS_TAG: Record<number, 'warning' | 'primary' | 'success'> = { 0: 'warning', 1: 'primary', 2: 'success' }
const COST_TYPE_NAME: Record<number, string> = { 1: '路桥费', 2: '燃油费', 3: '其他' }
const BEARER_NAME: Record<number, string> = { 1: '承运商承担', 2: '本企业承担' }

const { t } = useI18n()
const message = useMessage()

const activeTab = ref('order')
const exportLoading = ref(false)
const carrierList = ref<LogisticsCarrierVO[]>([])
const formatAmount = (value?: number) => (value === null || value === undefined ? '—' : Number(value).toFixed(2))

const onTabChange = () => {
  if (activeTab.value === 'order') getOrderList()
  else if (activeTab.value === 'reconciliation') getReconciliation()
  else getCostList()
}

// -------------------- 运费单 --------------------
const orderLoading = ref(false)
const orderList = ref<LogisticsFreightVO[]>([])
const orderTotal = ref(0)
const orderQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  freightNo: undefined,
  taskNo: undefined,
  carrierId: undefined,
  status: undefined
})
const orderQueryRef = ref()

const getOrderList = async () => {
  orderLoading.value = true
  try {
    const data = await LogisticsFreightApi.getFreightPage(orderQuery)
    orderList.value = data.list
    orderTotal.value = data.total
  } finally {
    orderLoading.value = false
  }
}
const resetOrderQuery = () => {
  orderQueryRef.value.resetFields()
  orderQuery.pageNo = 1
  getOrderList()
}
const handleExportOrder = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await LogisticsFreightApi.exportFreight(orderQuery)
    download.excel(data, '承运商运费.xls')
  } finally {
    exportLoading.value = false
  }
}

const orderFormVisible = ref(false)
const orderFormTitle = ref('汇集运费')
const orderFormLoading = ref(false)
const orderFormRef = ref()
const orderForm = ref<any>({ taskId: undefined, carrierId: undefined, contractId: undefined, billQuantity: 1 })
const contractList = ref<LogisticsCarrierContractVO[]>([])
const orderFormRules = reactive({
  taskId: [{ required: true, message: '运输任务不能为空', trigger: 'blur' }],
  contractId: [{ required: true, message: '承运合同不能为空', trigger: 'change' }],
  billQuantity: [{ required: true, message: '计费量不能为空', trigger: 'blur' }]
})
const onCarrierChange = async (carrierId?: number) => {
  orderForm.value.contractId = undefined
  contractList.value = carrierId ? await LogisticsCarrierContractApi.getContractListByCarrier(carrierId) : []
}
const openOrderForm = async (row?: LogisticsFreightVO) => {
  orderFormVisible.value = true
  orderFormLoading.value = false
  orderForm.value = row
    ? { id: row.id, taskId: row.taskId, carrierId: row.carrierId, contractId: row.contractId, billQuantity: row.billQuantity }
    : { taskId: undefined, carrierId: undefined, contractId: undefined, billQuantity: 1 }
  orderFormTitle.value = row ? '修改运费单' : '汇集运费'
  if (row?.carrierId) {
    contractList.value = await LogisticsCarrierContractApi.getContractListByCarrier(row.carrierId)
  } else {
    contractList.value = []
  }
}
const submitOrderForm = async () => {
  await orderFormRef.value.validate()
  orderFormLoading.value = true
  try {
    if (orderForm.value.id) {
      await LogisticsFreightApi.updateFreight({ id: orderForm.value.id, billQuantity: orderForm.value.billQuantity })
      message.success(t('common.updateSuccess'))
    } else {
      await LogisticsFreightApi.createFreight({
        taskId: orderForm.value.taskId,
        contractId: orderForm.value.contractId,
        billQuantity: orderForm.value.billQuantity
      })
      message.success(t('common.createSuccess'))
    }
    orderFormVisible.value = false
    await getOrderList()
  } finally {
    orderFormLoading.value = false
  }
}

// 确认应付
const confirmVisible = ref(false)
const confirmFormRef = ref()
const confirmForm = ref<any>({ id: undefined, actualAmount: undefined, varianceReason: undefined, confirmRemark: undefined })
const confirmRules = reactive({
  actualAmount: [{ required: true, message: '实际应付不能为空', trigger: 'blur' }]
})
const openConfirm = (row: LogisticsFreightVO) => {
  currentOrder.value = row
  confirmForm.value = { id: row.id, actualAmount: row.expectedAmount, varianceReason: undefined, confirmRemark: undefined }
  confirmVisible.value = true
}
const submitConfirm = async () => {
  await confirmFormRef.value.validate()
  await LogisticsFreightApi.confirmFreight(confirmForm.value)
  message.success('已确认应付')
  confirmVisible.value = false
  await getOrderList()
}

// 登记付款凭证
const payVisible = ref(false)
const payFormRef = ref()
const payForm = ref<any>({ id: undefined, paymentVoucherNo: undefined, paymentVoucherUrl: undefined, paymentAmount: undefined, paidAt: undefined, paymentRemark: undefined })
const openPay = (row: LogisticsFreightVO) => {
  currentOrder.value = row
  payForm.value = { id: row.id, paymentVoucherNo: undefined, paymentVoucherUrl: undefined, paymentAmount: row.actualAmount, paidAt: undefined, paymentRemark: undefined }
  payVisible.value = true
}
const submitPay = async () => {
  if (!payForm.value.paymentVoucherNo && !payForm.value.paymentVoucherUrl) {
    message.error('付款凭证号与附件至少填一个')
    return
  }
  await LogisticsFreightApi.payFreight(payForm.value)
  message.success('已登记付款凭证')
  payVisible.value = false
  await getOrderList()
}

// 详情
const orderDetailVisible = ref(false)
const currentOrder = ref<LogisticsFreightVO>()
const openOrderDetail = (row: LogisticsFreightVO) => {
  currentOrder.value = row
  orderDetailVisible.value = true
}

// -------------------- 对账汇总 --------------------
const reconLoading = ref(false)
const reconList = ref<LogisticsFreightReconciliationVO[]>([])
const reconQuery = reactive({ carrierName: undefined, status: undefined })
const reconQueryRef = ref()
const getReconciliation = async () => {
  reconLoading.value = true
  try {
    reconList.value = await LogisticsFreightApi.getReconciliation(reconQuery)
  } finally {
    reconLoading.value = false
  }
}
const resetReconQuery = () => {
  reconQueryRef.value.resetFields()
  getReconciliation()
}
const reconSummary = ({ columns, data }: any) => {
  const sums: string[] = []
  columns.forEach((column: any, index: number) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (['expectedTotal', 'actualTotal', 'varianceTotal'].includes(column.property)) {
      sums[index] = formatAmount(data.reduce((sum: number, row: any) => sum + Number(row[column.property] || 0), 0))
      return
    }
    sums[index] = ''
  })
  return sums
}
const handleExportReconciliation = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await LogisticsFreightApi.exportReconciliation(reconQuery)
    download.excel(data, '运费对账.xls')
  } finally {
    exportLoading.value = false
  }
}

// -------------------- 运输费用（内部成本） --------------------
const costLoading = ref(false)
const costList = ref<LogisticsTransportCostVO[]>([])
const costTotal = ref(0)
const costQuery = reactive({ pageNo: 1, pageSize: 10, taskNo: undefined, costType: undefined, bearer: undefined })
const costQueryRef = ref()
const getCostList = async () => {
  costLoading.value = true
  try {
    const data = await LogisticsTransportCostApi.getTransportCostPage(costQuery)
    costList.value = data.list
    costTotal.value = data.total
  } finally {
    costLoading.value = false
  }
}
const resetCostQuery = () => {
  costQueryRef.value.resetFields()
  costQuery.pageNo = 1
  getCostList()
}

const costFormVisible = ref(false)
const costFormTitle = ref('登记运输费用')
const costFormLoading = ref(false)
const costFormRef = ref()
const costForm = ref<any>({})
const costRules = reactive({
  taskId: [{ required: true, message: '运输任务不能为空', trigger: 'blur' }],
  costType: [{ required: true, message: '费用类型不能为空', trigger: 'change' }],
  amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }],
  bearer: [{ required: true, message: '承担方不能为空', trigger: 'change' }]
})
const openCostForm = (row?: LogisticsTransportCostVO) => {
  costFormVisible.value = true
  costForm.value = row
    ? { ...row }
    : { taskId: undefined, costType: 1, name: undefined, amount: undefined, bearer: 2, occurDate: undefined, remark: undefined }
  costFormTitle.value = row ? '修改运输费用' : '登记运输费用'
}
const submitCostForm = async () => {
  await costFormRef.value.validate()
  costFormLoading.value = true
  try {
    if (costForm.value.id) {
      await LogisticsTransportCostApi.updateTransportCost(costForm.value)
      message.success(t('common.updateSuccess'))
    } else {
      await LogisticsTransportCostApi.createTransportCost(costForm.value)
      message.success(t('common.createSuccess'))
    }
    costFormVisible.value = false
    await getCostList()
  } finally {
    costFormLoading.value = false
  }
}
const handleDeleteCost = async (id: number) => {
  try {
    await message.delConfirm()
    await LogisticsTransportCostApi.deleteTransportCost(id)
    message.success(t('common.delSuccess'))
    await getCostList()
  } catch {}
}

const init = async () => {
  const carriers = await LogisticsCarrierApi.getCarrierPage({ pageNo: 1, pageSize: 100 })
  carrierList.value = carriers.list
  await getOrderList()
}
init()
</script>
