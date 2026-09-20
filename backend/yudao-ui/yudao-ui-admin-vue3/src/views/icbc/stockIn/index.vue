<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="入库是收购单派生的单向动作：验收后的货进待入库，仓管选仓库 / 库位 / 批次确认实际入库量。"
    />
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="现在只有入库流水（出库 / 调拨 / 盘点尚未落地），所以页面上只称「累计入库」，不称「当前库存」。只有过账的入库才增加正式库存，当前库存要等出库 / 调拨 / 盘点落地后才可称。"
    />
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="待入库" name="pending" />
      <el-tab-pane label="入库单" name="list" />
    </el-tabs>
  </ContentWrap>

  <!-- 待入库 -->
  <ContentWrap v-if="activeTab === 'pending'">
    <el-form class="-mb-15px" :model="pendingQuery" ref="pendingFormRef" :inline="true" label-width="80px">
      <el-form-item label="收购单号" prop="acquisitionNo">
        <el-input v-model="pendingQuery.acquisitionNo" placeholder="请输入" clearable @keyup.enter="getPendingList" class="!w-180px" />
      </el-form-item>
      <el-form-item label="出售者" prop="sellerName">
        <el-input v-model="pendingQuery.sellerName" placeholder="请输入" clearable @keyup.enter="getPendingList" class="!w-180px" />
      </el-form-item>
      <el-form-item label="品类" prop="goodsConfigId">
        <el-select v-model="pendingQuery.goodsConfigId" placeholder="请选择" clearable class="!w-200px">
          <el-option v-for="item in goodsConfigList" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="getPendingList"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetPendingQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="activeTab === 'pending'">
    <el-table v-loading="pendingLoading" :data="pendingList" :stripe="true">
      <el-table-column label="收购单号" prop="acquisitionNo" min-width="180" />
      <el-table-column label="出售者" prop="sellerName" min-width="120" />
      <el-table-column label="品类" prop="categoryName" min-width="120" />
      <el-table-column label="可入库实物量" prop="availableQuantity" width="130" align="right" />
      <el-table-column label="累计入库" prop="stockedQuantity" width="120" align="right" />
      <el-table-column label="剩余可入库" prop="remainingQuantity" width="120" align="right" />
      <el-table-column label="交易时间" prop="tradeTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openConfirm(row)" v-hasPermi="['icbc:stock-in:manage']">
            确认入库
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="pendingTotal"
      v-model:page="pendingQuery.pageNo"
      v-model:limit="pendingQuery.pageSize"
      @pagination="getPendingList"
    />
  </ContentWrap>

  <!-- 入库单 -->
  <ContentWrap v-if="activeTab === 'list'">
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="入库单号" prop="stockInNo">
        <el-input v-model="queryParams.stockInNo" placeholder="请输入" clearable @keyup.enter="getStockInList" class="!w-200px" />
      </el-form-item>
      <el-form-item label="收购单号" prop="acquisitionNo">
        <el-input v-model="queryParams.acquisitionNo" placeholder="请输入" clearable @keyup.enter="getStockInList" class="!w-180px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-150px">
          <el-option v-for="item in STOCK_IN_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="getStockInList"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="activeTab === 'list'">
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="入库单号" prop="stockInNo" min-width="200" />
      <el-table-column label="收购单号" prop="acquisitionNo" min-width="180" />
      <el-table-column label="出售者" prop="sellerName" min-width="120" />
      <el-table-column label="品类" prop="categoryName" min-width="120" />
      <el-table-column label="本次入库" prop="totalQuantity" width="110" align="right" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="过账时间" prop="postedTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="操作" align="center" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id!)">详情</el-button>
          <el-button
            v-if="row.status === STOCK_IN_STATUS_ENUM.PENDING"
            link
            type="primary"
            @click="handlePost(row.id!)"
            v-hasPermi="['icbc:stock-in:manage']"
          >
            过账
          </el-button>
          <el-button
            v-if="row.status !== STOCK_IN_STATUS_ENUM.CANCELLED"
            link
            type="danger"
            @click="handleCancel(row.id!)"
            v-hasPermi="['icbc:stock-in:manage']"
          >
            作废
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getStockInList" />
  </ContentWrap>

  <!-- 确认入库 -->
  <Dialog v-model="confirmVisible" title="确认入库" width="820px">
    <div v-if="confirmRow" class="mb-10px">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="收购单号">{{ confirmRow.acquisitionNo }}</el-descriptions-item>
        <el-descriptions-item label="出售者">{{ confirmRow.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="品类">{{ confirmRow.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="可入库实物量">{{ confirmRow.availableQuantity }}</el-descriptions-item>
        <el-descriptions-item label="累计入库">{{ confirmRow.stockedQuantity }}</el-descriptions-item>
        <el-descriptions-item label="剩余可入库">{{ confirmRow.remainingQuantity }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <el-form ref="confirmFormRef" :model="confirmForm" label-width="70px">
      <el-form-item label="明细" required>
        <div class="w-full">
          <el-table :data="confirmForm.items" size="small">
            <el-table-column label="仓库" min-width="150">
              <template #default="{ row }">
                <el-select v-model="row.warehouseId" placeholder="请选择" filterable class="!w-full" @change="row.locationId = undefined">
                  <el-option v-for="item in warehouseList" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="库位" min-width="140">
              <template #default="{ row }">
                <el-select v-model="row.locationId" placeholder="不指定" clearable filterable class="!w-full">
                  <el-option
                    v-for="item in locationsOf(row.warehouseId)"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id!"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="批次" min-width="140">
              <template #default="{ row }">
                <el-select v-model="row.batchId" placeholder="不指定" clearable filterable class="!w-full">
                  <el-option v-for="item in batchList" :key="item.id" :label="item.batchNo" :value="item.id!" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :precision="4" :controls="false" class="!w-full" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" :disabled="confirmForm.items.length <= 1" @click="removeItem($index)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt-10px" plain @click="addItem"><Icon icon="ep:plus" class="mr-5px" /> 添加库位</el-button>
          <span class="ml-10px text-13px text-gray-500">合计：{{ confirmTotal }}</span>
        </div>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="confirmForm.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="confirmVisible = false">取消</el-button>
      <el-button type="primary" :loading="confirmLoading" @click="submitConfirm">确认入库（过账）</el-button>
    </template>
  </Dialog>

  <!-- 入库单详情 -->
  <Dialog v-model="detailVisible" title="入库单详情" width="720px">
    <el-descriptions v-if="detail" :column="2" border size="small" class="mb-10px">
      <el-descriptions-item label="入库单号">{{ detail.stockInNo }}</el-descriptions-item>
      <el-descriptions-item label="收购单号">{{ detail.acquisitionNo }}</el-descriptions-item>
      <el-descriptions-item label="出售者">{{ detail.sellerName }}</el-descriptions-item>
      <el-descriptions-item label="品类">{{ detail.categoryName }}</el-descriptions-item>
      <el-descriptions-item label="可入库实物量">{{ detail.availableQuantity }}</el-descriptions-item>
      <el-descriptions-item label="本次入库">{{ detail.totalQuantity }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detail.statusName }}</el-descriptions-item>
      <el-descriptions-item label="过账时间">{{ formatTime(detail.postedTime) }}</el-descriptions-item>
      <el-descriptions-item label="作废原因" :span="2">{{ detail.cancelReason || '—' }}</el-descriptions-item>
    </el-descriptions>
    <el-table v-if="detail" :data="detail.items" size="small">
      <el-table-column label="仓库" min-width="150">
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
      <el-table-column label="库位" min-width="120">
        <template #default="{ row }">{{ locationName(row.locationId) }}</template>
      </el-table-column>
      <el-table-column label="批次" min-width="120">
        <template #default="{ row }">{{ batchNo(row.batchId) }}</template>
      </el-table-column>
      <el-table-column label="数量" prop="quantity" width="120" align="right" />
    </el-table>
  </Dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { StockInApi, StockInPendingVO, StockInVO, STOCK_IN_STATUS_ENUM, STOCK_IN_STATUS_OPTIONS } from '@/api/icbc/stockIn'
import { WarehouseApi, WarehouseVO } from '@/api/erp/stock/warehouse'
import { StockLocationApi, StockLocationVO } from '@/api/erp/stock/location'
import { StockBatchApi, StockBatchVO } from '@/api/erp/stock/batch'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'

/** 待入库与入库单（#52 T14） */
defineOptions({ name: 'IcbcStockIn' })

const message = useMessage()

const activeTab = ref('pending')

// ==================== 待入库 ====================
const pendingLoading = ref(false)
const pendingList = ref<StockInPendingVO[]>([])
const pendingTotal = ref(0)
const pendingQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  acquisitionNo: undefined,
  sellerName: undefined,
  goodsConfigId: undefined
})
const pendingFormRef = ref()

const getPendingList = async () => {
  pendingLoading.value = true
  try {
    const data = await StockInApi.getPendingPage(pendingQuery)
    pendingList.value = data.list
    pendingTotal.value = data.total
  } finally {
    pendingLoading.value = false
  }
}
const resetPendingQuery = () => {
  pendingQuery.acquisitionNo = undefined
  pendingQuery.sellerName = undefined
  pendingQuery.goodsConfigId = undefined
  pendingQuery.pageNo = 1
  getPendingList()
}

// ==================== 确认入库 ====================
const confirmVisible = ref(false)
const confirmLoading = ref(false)
const confirmRow = ref<StockInPendingVO>()
const confirmFormRef = ref()
const confirmForm = reactive({
  acquisitionId: undefined as number | undefined,
  remark: '',
  items: [] as any[]
})
const confirmTotal = computed(() =>
  confirmForm.items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0)
)

const openConfirm = (row: StockInPendingVO) => {
  confirmRow.value = row
  confirmForm.acquisitionId = row.acquisitionId
  confirmForm.remark = ''
  confirmForm.items = [
    {
      warehouseId: undefined,
      locationId: undefined,
      batchId: undefined,
      quantity: row.remainingQuantity
    }
  ]
  confirmVisible.value = true
}
const addItem = () => {
  confirmForm.items.push({ warehouseId: undefined, locationId: undefined, batchId: undefined, quantity: 0 })
}
const removeItem = (index: number) => {
  confirmForm.items.splice(index, 1)
}
const submitConfirm = async () => {
  if (confirmForm.items.some((item) => !item.warehouseId)) {
    message.warning('每条明细都要选择仓库')
    return
  }
  if (confirmForm.items.some((item) => !item.quantity || item.quantity <= 0)) {
    message.warning('入库数量必须大于 0')
    return
  }
  confirmLoading.value = true
  try {
    await StockInApi.confirmStockIn({
      acquisitionId: confirmForm.acquisitionId,
      items: confirmForm.items,
      remark: confirmForm.remark
    })
    message.success('已过账，库存已增加')
    confirmVisible.value = false
    await Promise.all([getPendingList(), getStockInList()])
  } finally {
    confirmLoading.value = false
  }
}

// ==================== 入库单 ====================
const loading = ref(false)
const list = ref<StockInVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  stockInNo: undefined,
  acquisitionNo: undefined,
  status: undefined
})
const queryFormRef = ref()

const getStockInList = async () => {
  loading.value = true
  try {
    const data = await StockInApi.getStockInPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const resetQuery = () => {
  queryParams.stockInNo = undefined
  queryParams.acquisitionNo = undefined
  queryParams.status = undefined
  queryParams.pageNo = 1
  getStockInList()
}
const handleTabChange = (name: string) => {
  if (name === 'pending') {
    getPendingList()
  } else {
    getStockInList()
  }
}

const handlePost = async (id: number) => {
  await message.confirm('过账后这批货才计入正式库存，确认过账？')
  await StockInApi.postStockIn(id)
  message.success('已过账')
  await getStockInList()
}
const handleCancel = async (id: number) => {
  const { value } = await message.prompt('作废原因（已过账的会同时冲销库存）', '作废入库单')
  await StockInApi.cancelStockIn({ id, reason: value })
  message.success('已作废')
  await Promise.all([getPendingList(), getStockInList()])
}

// ==================== 详情 ====================
const detailVisible = ref(false)
const detail = ref<StockInVO>()
const openDetail = async (id: number) => {
  detail.value = await StockInApi.getStockIn(id)
  detailVisible.value = true
}

// ==================== 维度名称（前端解析：icbc 只依赖 erp-api，拿不到仓库表） ====================
const warehouseList = ref<WarehouseVO[]>([])
const locationList = ref<StockLocationVO[]>([])
const batchList = ref<StockBatchVO[]>([])
const goodsConfigList = ref<GoodsConfigVO[]>([])

const locationsOf = (warehouseId?: number) =>
  locationList.value.filter((item) => !warehouseId || item.warehouseId === warehouseId)
const warehouseName = (id?: number) => warehouseList.value.find((item) => item.id === id)?.name || '未指定'
const locationName = (id?: number) => locationList.value.find((item) => item.id === id)?.name || '未指定'
const batchNo = (id?: number) => batchList.value.find((item) => item.id === id)?.batchNo || '未指定'
const formatTime = (time?: number) => (time ? dateFormatter(undefined as any, undefined as any, time) : '—')

const statusTagType = (status?: number) => {
  if (status === STOCK_IN_STATUS_ENUM.POSTED) return 'success'
  if (status === STOCK_IN_STATUS_ENUM.CANCELLED) return 'info'
  return 'warning'
}

/** 初始化 */
onMounted(async () => {
  const [warehouses, locations, batches, goodsConfigs] = await Promise.all([
    WarehouseApi.getWarehouseSimpleList(),
    StockLocationApi.getStockLocationSimpleList(),
    StockBatchApi.getStockBatchSimpleList(),
    GoodsConfigApi.getEnabledList()
  ])
  warehouseList.value = warehouses || []
  locationList.value = locations || []
  batchList.value = batches || []
  goodsConfigList.value = goodsConfigs || []
  await getPendingList()
})
</script>
