<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="跨仓调拨：把同一品类的货从源维度搬到目标维度，源减目标加。过账一次写两条流水，余额与流水始终一致；作废已过账的按相反方向调回。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="调拨单号" prop="moveNo">
        <el-input v-model="queryParams.moveNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-160px">
          <el-option v-for="item in STOCK_OPS_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm" v-hasPermi="['icbc:stock-move:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 登记调拨
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="调拨单号" prop="moveNo" min-width="180" />
      <el-table-column label="调拨合计" prop="totalQuantity" width="120" align="right" />
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
            v-hasPermi="['icbc:stock-move:manage']"
          >
            过账
          </el-button>
          <el-button
            v-if="row.status !== STOCK_OPS_STATUS_ENUM.CANCELLED"
            link
            type="danger"
            @click="handleCancel(row.id!)"
            v-hasPermi="['icbc:stock-move:manage']"
          >
            作废
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 登记调拨 -->
  <Dialog v-model="formVisible" title="登记跨仓调拨" width="980px">
    <el-form ref="formRef" :model="formData" label-width="80px">
      <el-form-item label="明细" required>
        <div class="w-full">
          <el-table :data="formData.items" size="small">
            <el-table-column label="品类" min-width="130">
              <template #default="{ row }">
                <el-select v-model="row.goodsConfigId" placeholder="请选择" filterable class="!w-full">
                  <el-option v-for="item in goodsConfigList" :key="item.id" :label="item.name" :value="item.id!" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="源仓库" min-width="130">
              <template #default="{ row }">
                <el-select v-model="row.fromWarehouseId" placeholder="请选择" filterable class="!w-full" @change="row.fromLocationId = undefined">
                  <el-option v-for="item in warehouseList" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="源库位" min-width="120">
              <template #default="{ row }">
                <el-select v-model="row.fromLocationId" placeholder="不指定" clearable filterable class="!w-full">
                  <el-option v-for="item in locationsOf(row.fromWarehouseId)" :key="item.id" :label="item.name" :value="item.id!" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="目标仓库" min-width="130">
              <template #default="{ row }">
                <el-select v-model="row.toWarehouseId" placeholder="请选择" filterable class="!w-full" @change="row.toLocationId = undefined">
                  <el-option v-for="item in warehouseList" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="目标库位" min-width="120">
              <template #default="{ row }">
                <el-select v-model="row.toLocationId" placeholder="不指定" clearable filterable class="!w-full">
                  <el-option v-for="item in locationsOf(row.toWarehouseId)" :key="item.id" :label="item.name" :value="item.id!" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="120">
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
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="调拨事由，例如：1 号库腾位" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button :loading="formLoading" @click="submit(false)">登记（待过账）</el-button>
      <el-button type="primary" :loading="formLoading" @click="submit(true)">登记并过账</el-button>
    </template>
  </Dialog>

  <!-- 详情 -->
  <Dialog v-model="detailVisible" title="跨仓调拨单详情" width="860px">
    <el-descriptions v-if="detail" :column="2" border size="small" class="mb-10px">
      <el-descriptions-item label="调拨单号">{{ detail.moveNo }}</el-descriptions-item>
      <el-descriptions-item label="调拨合计">{{ detail.totalQuantity }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detail.statusName }}</el-descriptions-item>
      <el-descriptions-item label="过账时间">{{ formatTime(detail.postedTime) }}</el-descriptions-item>
      <el-descriptions-item label="作废原因">{{ detail.cancelReason || '—' }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
    </el-descriptions>
    <el-table v-if="detail" :data="detail.items" size="small">
      <el-table-column label="品类" min-width="130">
        <template #default="{ row }">{{ goodsConfigName(row.goodsConfigId) }}</template>
      </el-table-column>
      <el-table-column label="源" min-width="170">
        <template #default="{ row }">
          {{ warehouseName(row.fromWarehouseId) }} / {{ locationName(row.fromLocationId) }}
        </template>
      </el-table-column>
      <el-table-column label="目标" min-width="170">
        <template #default="{ row }">
          {{ warehouseName(row.toWarehouseId) }} / {{ locationName(row.toLocationId) }}
        </template>
      </el-table-column>
      <el-table-column label="数量" prop="quantity" width="110" align="right" />
    </el-table>
  </Dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { StockMoveApi, StockMoveVO, STOCK_OPS_STATUS_ENUM, STOCK_OPS_STATUS_OPTIONS } from '@/api/icbc/stockOps'
import { WarehouseApi, WarehouseVO } from '@/api/erp/stock/warehouse'
import { StockLocationApi, StockLocationVO } from '@/api/erp/stock/location'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'

/** 跨仓调拨（#54 T16） */
defineOptions({ name: 'IcbcStockMove' })

const message = useMessage()

const loading = ref(false)
const list = ref<StockMoveVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10, moveNo: undefined, status: undefined })
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await StockMoveApi.getPage(queryParams)
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
  queryParams.moveNo = undefined
  queryParams.status = undefined
  queryParams.pageNo = 1
  getList()
}

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const formData = reactive({ remark: '', items: [] as any[] })
const totalQuantity = computed(() =>
  formData.items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0)
)
const emptyItem = () => ({
  goodsConfigId: undefined,
  fromWarehouseId: undefined,
  fromLocationId: undefined,
  fromBatchId: undefined,
  toWarehouseId: undefined,
  toLocationId: undefined,
  toBatchId: undefined,
  quantity: undefined
})
const openForm = () => {
  formData.remark = ''
  formData.items = [emptyItem()]
  formVisible.value = true
}
const addItem = () => formData.items.push(emptyItem())
const removeItem = (index: number) => formData.items.splice(index, 1)

const submit = async (postNow: boolean) => {
  if (formData.items.some((item) => !item.goodsConfigId || !item.fromWarehouseId || !item.toWarehouseId)) {
    message.warning('每条明细都要选择品类、源仓库与目标仓库')
    return
  }
  if (formData.items.some((item) => !item.quantity || item.quantity <= 0)) {
    message.warning('调拨数量必须大于 0')
    return
  }
  if (formData.items.some((item) => item.fromWarehouseId === item.toWarehouseId && (item.fromLocationId || 0) === (item.toLocationId || 0))) {
    message.warning('源与目标是同一个仓库 / 库位，没有可移动的货')
    return
  }
  formLoading.value = true
  try {
    const payload = { items: formData.items, remark: formData.remark }
    if (postNow) {
      await StockMoveApi.confirm(payload)
      message.success('已过账，库存已调拨')
    } else {
      await StockMoveApi.create(payload)
      message.success('已登记，待过账')
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handlePost = async (id: number) => {
  await message.confirm('过账后源仓库库存立即减少、目标仓库增加，确认过账？')
  await StockMoveApi.post(id)
  message.success('已过账')
  await getList()
}
const handleCancel = async (id: number) => {
  const { value } = await message.prompt('作废原因（已过账的会按相反方向调回）', '作废调拨单')
  await StockMoveApi.cancel({ id, reason: value })
  message.success('已作废')
  await getList()
}

const detailVisible = ref(false)
const detail = ref<StockMoveVO>()
const openDetail = async (id: number) => {
  detail.value = await StockMoveApi.get(id)
  detailVisible.value = true
}

const warehouseList = ref<WarehouseVO[]>([])
const locationList = ref<StockLocationVO[]>([])
const goodsConfigList = ref<GoodsConfigVO[]>([])

const locationsOf = (warehouseId?: number) =>
  locationList.value.filter((item) => !warehouseId || item.warehouseId === warehouseId)
const warehouseName = (id?: number) => warehouseList.value.find((item) => item.id === id)?.name || '未指定'
const locationName = (id?: number) => locationList.value.find((item) => item.id === id)?.name || '未指定'
const goodsConfigName = (id?: number) => goodsConfigList.value.find((item) => item.id === id)?.name || String(id ?? '—')
const formatTime = (time?: number) => (time ? dateFormatter(undefined as any, undefined as any, time) : '—')

const statusTagType = (status?: number) => {
  if (status === STOCK_OPS_STATUS_ENUM.POSTED) return 'success'
  if (status === STOCK_OPS_STATUS_ENUM.CANCELLED) return 'info'
  return 'warning'
}

onMounted(async () => {
  const [warehouses, locations, goodsConfigs] = await Promise.all([
    WarehouseApi.getWarehouseSimpleList(),
    StockLocationApi.getStockLocationSimpleList(),
    GoodsConfigApi.getEnabledList()
  ])
  warehouseList.value = warehouses || []
  locationList.value = locations || []
  goodsConfigList.value = goodsConfigs || []
  await getList()
})
</script>
