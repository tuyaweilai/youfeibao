<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="非销售出库：报损 / 退货出库 / 内部领用，不挂客户（一期不做销售出库）。只有过账才减库存，作废已过账的按相反方向冲销。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="出库单号" prop="stockOutNo">
        <el-input v-model="queryParams.stockOutNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="类型" prop="outType">
        <el-select v-model="queryParams.outType" placeholder="全部" clearable class="!w-160px">
          <el-option v-for="item in STOCK_OUT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-160px">
          <el-option v-for="item in STOCK_OPS_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm" v-hasPermi="['icbc:stock-out:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 登记出库
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="出库单号" prop="stockOutNo" min-width="180" />
      <el-table-column label="类型" prop="outTypeName" width="110" />
      <el-table-column label="出库合计" prop="totalQuantity" width="120" align="right" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="过账时间" :formatter="dateFormatter" prop="postedTime" width="170" />
      <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="190" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id!)">详情</el-button>
          <el-button
            v-if="row.status === STOCK_OPS_STATUS_ENUM.PENDING"
            link
            type="primary"
            @click="handlePost(row.id!)"
            v-hasPermi="['icbc:stock-out:manage']"
          >
            过账
          </el-button>
          <el-button
            v-if="row.status !== STOCK_OPS_STATUS_ENUM.CANCELLED"
            link
            type="danger"
            @click="handleCancel(row.id!)"
            v-hasPermi="['icbc:stock-out:manage']"
          >
            作废
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 登记出库 -->
  <Dialog v-model="formVisible" title="登记非销售出库" width="860px">
    <el-form ref="formRef" :model="formData" label-width="80px">
      <el-form-item label="出库类型" required>
        <el-radio-group v-model="formData.outType">
          <el-radio v-for="item in STOCK_OUT_TYPE_OPTIONS" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="明细" required>
        <div class="w-full">
          <el-table :data="formData.items" size="small">
            <el-table-column label="品类" min-width="150">
              <template #default="{ row }">
                <el-select v-model="row.goodsConfigId" placeholder="请选择" filterable class="!w-full">
                  <el-option v-for="item in goodsConfigList" :key="item.id" :label="item.name" :value="item.id!" />
                </el-select>
              </template>
            </el-table-column>
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
                  <el-option v-for="item in locationsOf(row.warehouseId)" :key="item.id" :label="item.name" :value="item.id!" />
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
            <el-table-column label="数量" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :precision="4" :controls="false" class="!w-full" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" :disabled="formData.items.length <= 1" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt-10px" plain @click="addItem"><Icon icon="ep:plus" class="mr-5px" /> 添加明细</el-button>
          <span class="ml-10px text-13px text-gray-500">合计：{{ totalQuantity }}</span>
        </div>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="出库事由，例如：雨天受潮" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button :loading="formLoading" @click="submit(false)">登记（待过账）</el-button>
      <el-button type="primary" :loading="formLoading" @click="submit(true)">登记并过账</el-button>
    </template>
  </Dialog>

  <!-- 详情 -->
  <Dialog v-model="detailVisible" title="非销售出库单详情" width="760px">
    <el-descriptions v-if="detail" :column="2" border size="small" class="mb-10px">
      <el-descriptions-item label="出库单号">{{ detail.stockOutNo }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ detail.outTypeName }}</el-descriptions-item>
      <el-descriptions-item label="出库合计">{{ detail.totalQuantity }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detail.statusName }}</el-descriptions-item>
      <el-descriptions-item label="过账时间">{{ formatTime(detail.postedTime) }}</el-descriptions-item>
      <el-descriptions-item label="作废原因">{{ detail.cancelReason || '—' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
    </el-descriptions>
    <el-table v-if="detail" :data="detail.items" size="small">
      <el-table-column label="品类" min-width="140">
        <template #default="{ row }">{{ goodsConfigName(row.goodsConfigId) }}</template>
      </el-table-column>
      <el-table-column label="仓库" min-width="140">
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
      <el-table-column label="库位" min-width="110">
        <template #default="{ row }">{{ locationName(row.locationId) }}</template>
      </el-table-column>
      <el-table-column label="批次" min-width="110">
        <template #default="{ row }">{{ batchNo(row.batchId) }}</template>
      </el-table-column>
      <el-table-column label="数量" prop="quantity" width="110" align="right" />
    </el-table>
  </Dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import {
  StockOutApi,
  StockOutVO,
  STOCK_OPS_STATUS_ENUM,
  STOCK_OPS_STATUS_OPTIONS,
  STOCK_OUT_TYPE_ENUM,
  STOCK_OUT_TYPE_OPTIONS
} from '@/api/icbc/stockOps'
import { WarehouseApi, WarehouseVO } from '@/api/erp/stock/warehouse'
import { StockLocationApi, StockLocationVO } from '@/api/erp/stock/location'
import { StockBatchApi, StockBatchVO } from '@/api/erp/stock/batch'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'

/** 非销售出库（#54 T16） */
defineOptions({ name: 'IcbcStockOut' })

const message = useMessage()

const loading = ref(false)
const list = ref<StockOutVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  stockOutNo: undefined,
  outType: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await StockOutApi.getPage(queryParams)
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
  queryParams.stockOutNo = undefined
  queryParams.outType = undefined
  queryParams.status = undefined
  queryParams.pageNo = 1
  getList()
}

// ==================== 登记 ====================
const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const formData = reactive({
  outType: STOCK_OUT_TYPE_ENUM.SCRAP,
  remark: '',
  items: [] as any[]
})
const totalQuantity = computed(() =>
  formData.items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0)
)
const openForm = () => {
  formData.outType = STOCK_OUT_TYPE_ENUM.SCRAP
  formData.remark = ''
  formData.items = [{ goodsConfigId: undefined, warehouseId: undefined, locationId: undefined, batchId: undefined, quantity: undefined }]
  formVisible.value = true
}
const addItem = () => {
  formData.items.push({ goodsConfigId: undefined, warehouseId: undefined, locationId: undefined, batchId: undefined, quantity: undefined })
}
const removeItem = (index: number) => {
  formData.items.splice(index, 1)
}
const submit = async (postNow: boolean) => {
  if (formData.items.some((item) => !item.goodsConfigId || !item.warehouseId)) {
    message.warning('每条明细都要选择品类与仓库')
    return
  }
  if (formData.items.some((item) => !item.quantity || item.quantity <= 0)) {
    message.warning('出库数量必须大于 0')
    return
  }
  formLoading.value = true
  try {
    const payload = { outType: formData.outType, items: formData.items, remark: formData.remark }
    if (postNow) {
      await StockOutApi.confirm(payload)
      message.success('已过账，库存已减少')
    } else {
      await StockOutApi.create(payload)
      message.success('已登记，待过账')
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handlePost = async (id: number) => {
  await message.confirm('过账后库存立即减少，确认过账？')
  await StockOutApi.post(id)
  message.success('已过账')
  await getList()
}
const handleCancel = async (id: number) => {
  const { value } = await message.prompt('作废原因（已过账的会同时冲销库存）', '作废出库单')
  await StockOutApi.cancel({ id, reason: value })
  message.success('已作废')
  await getList()
}

// ==================== 详情 ====================
const detailVisible = ref(false)
const detail = ref<StockOutVO>()
const openDetail = async (id: number) => {
  detail.value = await StockOutApi.get(id)
  detailVisible.value = true
}

// ==================== 维度名称（前端解析） ====================
const warehouseList = ref<WarehouseVO[]>([])
const locationList = ref<StockLocationVO[]>([])
const batchList = ref<StockBatchVO[]>([])
const goodsConfigList = ref<GoodsConfigVO[]>([])

const locationsOf = (warehouseId?: number) =>
  locationList.value.filter((item) => !warehouseId || item.warehouseId === warehouseId)
const warehouseName = (id?: number) => warehouseList.value.find((item) => item.id === id)?.name || '未指定'
const locationName = (id?: number) => locationList.value.find((item) => item.id === id)?.name || '未指定'
const batchNo = (id?: number) => batchList.value.find((item) => item.id === id)?.batchNo || '未指定'
const goodsConfigName = (id?: number) => goodsConfigList.value.find((item) => item.id === id)?.name || String(id ?? '—')
const formatTime = (time?: number) => (time ? dateFormatter(undefined as any, undefined as any, time) : '—')

const statusTagType = (status?: number) => {
  if (status === STOCK_OPS_STATUS_ENUM.POSTED) return 'success'
  if (status === STOCK_OPS_STATUS_ENUM.CANCELLED) return 'info'
  return 'warning'
}

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
  await getList()
})
</script>
