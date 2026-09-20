<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="超量 / 过期 / 跨场站交货被企业配置成「提交授权审核」时，先在这里提交一张授权单；审核通过后按授权范围放行。授权只放宽被授权的那一件事（追加量 / 有效期 / 场站），不改订单状态、不改已发生的业务。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="异常类型" prop="exceptionType">
        <el-select v-model="queryParams.exceptionType" placeholder="请选择" clearable class="!w-160px">
          <el-option v-for="item in PURCHASE_EXCEPTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-140px">
          <el-option v-for="item in PURCHASE_EXCEPTION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openRequest" v-hasPermi="['icbc:purchase-exception:request']">
          <Icon icon="ep:plus" class="mr-5px" /> 提交授权审核
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="授权单号" prop="exceptionNo" min-width="180" />
      <el-table-column label="订单号" prop="orderNo" min-width="180" />
      <el-table-column label="异常类型" prop="exceptionTypeName" width="120" />
      <el-table-column label="明细 / 场站" min-width="150">
        <template #default="{ row }">{{ row.categoryName || row.stationName || '-' }}</template>
      </el-table-column>
      <el-table-column label="申请量" prop="requestedQuantity" width="100" align="right" />
      <el-table-column label="授权追加量" width="110" align="right">
        <template #default="{ row }">{{ row.approvedQuantity ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="有效期止" width="120">
        <template #default="{ row }">{{ row.validUntil || '不设' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row)">{{ row.statusName }}</el-tag>
          <el-tag v-if="row.status === 1 && row.effective" type="success" size="small" class="ml-5px">生效中</el-tag>
          <el-tag v-else-if="row.status === 1" type="info" size="small" class="ml-5px">已过期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交原因" prop="reason" min-width="150" show-overflow-tooltip />
      <el-table-column label="提交 / 审核" min-width="200">
        <template #default="{ row }">
          <div>{{ row.requestedTime ? formatDate(new Date(row.requestedTime), 'YYYY-MM-DD HH:mm:ss') : '-' }}</div>
          <div class="text-gray-500">
            {{ row.reviewedTime ? formatDate(new Date(row.reviewedTime), 'YYYY-MM-DD HH:mm:ss') : '待审核' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">口径说明</el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="success"
            @click="openReview(row)"
            v-hasPermi="['icbc:purchase-exception:audit']"
          >
            审核
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 提交授权审核 -->
  <el-dialog v-model="requestVisible" title="提交履约异常授权审核" width="640px">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="超量必须指明明细，跨场站必须指明场站；同一个异常已有待审核的申请时不重复提交。"
    />
    <el-form :model="requestForm" label-width="140px" ref="requestFormRef">
      <el-form-item label="采购订单" prop="orderId" :rules="[{ required: true, message: '请选择采购订单' }]">
        <el-select
          v-model="requestForm.orderId"
          filterable
          remote
          :remote-method="searchOrders"
          placeholder="输入订单号搜索"
          class="!w-350px"
          @change="onOrderChange"
        >
          <el-option v-for="item in orderOptions" :key="item.id" :label="item.orderNo" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="异常类型" prop="exceptionType" :rules="[{ required: true, message: '请选择异常类型' }]">
        <el-select v-model="requestForm.exceptionType" class="!w-350px" @change="onTypeChange">
          <el-option v-for="item in PURCHASE_EXCEPTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="requestForm.exceptionType === 'OVER_QUANTITY'" label="订单明细">
        <el-select v-model="requestForm.itemId" placeholder="请选择品类明细" class="!w-350px">
          <el-option v-for="item in itemOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="requestForm.exceptionType === 'CROSS_STATION'" label="本次交货场站">
        <el-select v-model="requestForm.stationId" placeholder="请选择场站" class="!w-350px">
          <el-option v-for="item in stationOptions" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="本次交货量" prop="requestedQuantity" :rules="[{ required: true, message: '请填写本次交货量' }]">
        <el-input-number v-model="requestForm.requestedQuantity" :min="0.0001" :precision="4" class="!w-200px" />
      </el-form-item>
      <el-form-item label="期望有效期止">
        <el-date-picker v-model="requestForm.validUntil" type="date" value-format="YYYY-MM-DD" placeholder="可空，空表示不设有效期" class="!w-250px" />
      </el-form-item>
      <el-form-item label="原因" prop="reason" :rules="[{ required: true, message: '请填写原因' }]">
        <el-input v-model="requestForm.reason" type="textarea" :rows="3" class="!w-450px" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="requestVisible = false">取 消</el-button>
      <el-button type="primary" @click="submitRequest" :loading="submitLoading">提 交</el-button>
    </template>
  </el-dialog>

  <!-- 审核 -->
  <el-dialog v-model="reviewVisible" title="审核履约异常授权单" width="560px">
    <el-alert type="info" :closable="false" class="mb-10px" :title="current?.scopeNote" />
    <el-form :model="reviewForm" label-width="140px">
      <el-form-item label="审核结论">
        <el-radio-group v-model="reviewForm.approved">
          <el-radio :label="true">通过</el-radio>
          <el-radio :label="false">拒绝</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="reviewForm.approved && current?.exceptionType === 'OVER_QUANTITY'" label="授权追加量">
        <el-input-number v-model="reviewForm.approvedQuantity" :min="0" :precision="4" class="!w-200px" />
        <div class="text-gray-500 text-12px">不填按提交的交货量（{{ current?.requestedQuantity }}）生效</div>
      </el-form-item>
      <el-form-item v-if="reviewForm.approved" label="授权有效期止">
        <el-date-picker v-model="reviewForm.validUntil" type="date" value-format="YYYY-MM-DD" placeholder="可空，空表示不设有效期" class="!w-250px" />
      </el-form-item>
      <el-form-item label="审核意见">
        <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="3" class="!w-450px" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reviewVisible = false">取 消</el-button>
      <el-button type="primary" @click="submitReview" :loading="submitLoading">提 交</el-button>
    </template>
  </el-dialog>

  <!-- 口径说明 -->
  <el-dialog v-model="detailVisible" title="授权口径说明" width="560px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="授权单号">{{ current?.exceptionNo }}</el-descriptions-item>
      <el-descriptions-item label="异常类型">{{ current?.exceptionTypeName }}（{{ current?.exceptionType }}）</el-descriptions-item>
      <el-descriptions-item label="类型口径">{{ current?.exceptionTypeDefinition }}</el-descriptions-item>
      <el-descriptions-item label="本次授权放宽了什么">{{ current?.scopeNote }}</el-descriptions-item>
      <el-descriptions-item label="审核意见">{{ current?.reviewRemark || '-' }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  PurchaseOrderApi,
  PurchaseOrderVO,
  PurchaseOrderExceptionVO,
  PurchaseOrderItemVO,
  PURCHASE_EXCEPTION_TYPE_OPTIONS,
  PURCHASE_EXCEPTION_STATUS_OPTIONS
} from '@/api/icbc/purchaseOrder'
import { StationApi } from '@/api/icbc/station'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'IcbcPurchaseOrderException' })

const message = useMessage()

const loading = ref(true)
const submitLoading = ref(false)
const list = ref<PurchaseOrderExceptionVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: undefined,
  exceptionType: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await PurchaseOrderApi.getExceptionPage(queryParams)
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
const resetQuery = () => {
  queryParams.orderNo = undefined
  queryParams.exceptionType = undefined
  queryParams.status = undefined
  handleQuery()
}

const statusTagType = (row: PurchaseOrderExceptionVO) => {
  if (row.status === 1) return row.effective ? 'success' : 'info'
  if (row.status === 2) return 'danger'
  return 'warning'
}

// ==================== 提交授权审核 ====================
const requestVisible = ref(false)
const requestFormRef = ref()
const orderOptions = ref<PurchaseOrderVO[]>([])
const itemOptions = ref<PurchaseOrderItemVO[]>([])
const stationOptions = ref<any[]>([])
const requestForm = reactive<PurchaseOrderExceptionVO>({})

const searchOrders = async (keyword?: string) => {
  const data = await PurchaseOrderApi.getPurchaseOrderPage({ pageNo: 1, pageSize: 20, orderNo: keyword })
  orderOptions.value = data.list
}
const onOrderChange = async (orderId: number) => {
  requestForm.itemId = undefined
  const order = await PurchaseOrderApi.getPurchaseOrder(orderId)
  itemOptions.value = order.items || []
}
const onTypeChange = () => {
  requestForm.itemId = undefined
  requestForm.stationId = undefined
}
const openRequest = async () => {
  Object.assign(requestForm, {
    orderId: undefined,
    itemId: undefined,
    exceptionType: undefined,
    stationId: undefined,
    requestedQuantity: undefined,
    validUntil: undefined,
    reason: undefined
  })
  itemOptions.value = []
  await searchOrders()
  const stationPage = await StationApi.getStationPage({ pageNo: 1, pageSize: 100 })
  stationOptions.value = stationPage.list
  requestVisible.value = true
}
const submitRequest = async () => {
  await requestFormRef.value?.validate()
  if (requestForm.exceptionType === 'OVER_QUANTITY' && !requestForm.itemId) {
    message.warning('超量必须指明采购订单明细')
    return
  }
  if (requestForm.exceptionType === 'CROSS_STATION' && !requestForm.stationId) {
    message.warning('跨场站必须指明本次交货场站')
    return
  }
  submitLoading.value = true
  try {
    await PurchaseOrderApi.requestException(requestForm)
    message.success('已提交，等审核通过后按授权范围放行')
    requestVisible.value = false
    await getList()
  } finally {
    submitLoading.value = false
  }
}

// ==================== 审核 ====================
const reviewVisible = ref(false)
const current = ref<PurchaseOrderExceptionVO>()
const reviewForm = reactive<{
  id?: number
  approved: boolean
  approvedQuantity?: number
  validUntil?: string
  reviewRemark?: string
}>({ approved: true })

const openReview = (row: PurchaseOrderExceptionVO) => {
  current.value = row
  Object.assign(reviewForm, {
    id: row.id,
    approved: true,
    approvedQuantity: row.requestedQuantity,
    validUntil: row.validUntil,
    reviewRemark: undefined
  })
  reviewVisible.value = true
}
const submitReview = async () => {
  submitLoading.value = true
  try {
    await PurchaseOrderApi.reviewException({ ...reviewForm, id: reviewForm.id! })
    message.success(reviewForm.approved ? '已通过' : '已拒绝')
    reviewVisible.value = false
    await getList()
  } finally {
    submitLoading.value = false
  }
}

const detailVisible = ref(false)
const openDetail = (row: PurchaseOrderExceptionVO) => {
  current.value = row
  detailVisible.value = true
}

getList()
</script>
