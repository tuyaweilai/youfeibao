<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="期初导入：把启用平台之前就躺在仓库里的存量录进来。导入即过账（写库存流水）；同一「品类 + 仓库 + 库位 + 批次」只允许一条生效期初，录错了先作废再导，或用盘点调整修正。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="批次号" prop="openingNo">
        <el-input v-model="queryParams.openingNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="品类" prop="goodsConfigId">
        <el-select v-model="queryParams.goodsConfigId" placeholder="全部" clearable filterable class="!w-180px">
          <el-option v-for="item in goodsConfigList" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseId">
        <el-select v-model="queryParams.warehouseId" placeholder="全部" clearable filterable class="!w-180px">
          <el-option v-for="item in warehouseList" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-150px">
          <el-option label="已过账" :value="1" />
          <el-option label="已作废" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm" v-hasPermi="['icbc:stock-opening:manage']">
          <Icon icon="ep:upload" class="mr-5px" /> 导入期初
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="批次号" prop="openingNo" min-width="180" />
      <el-table-column label="品类" min-width="130">
        <template #default="{ row }">{{ goodsConfigName(row.goodsConfigId) }}</template>
      </el-table-column>
      <el-table-column label="仓库" min-width="130">
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
      <el-table-column label="库位" min-width="110">
        <template #default="{ row }">{{ locationName(row.locationId) }}</template>
      </el-table-column>
      <el-table-column label="批次" min-width="110">
        <template #default="{ row }">{{ batchNo(row.batchId) }}</template>
      </el-table-column>
      <el-table-column label="期初数量" prop="quantity" width="120" align="right" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 2 ? 'info' : 'success'">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="过账时间" :formatter="dateFormatter" prop="postedTime" width="170" />
      <el-table-column label="操作" align="center" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 2"
            link
            type="danger"
            @click="handleCancel(row.id!)"
            v-hasPermi="['icbc:stock-opening:manage']"
          >
            作废
          </el-button>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 导入期初 -->
  <Dialog v-model="formVisible" title="导入期初" width="900px">
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="导入即过账并写库存流水，请核对无误后再提交；同一维度重复导入会被整批拒绝。"
    />
    <el-form ref="formRef" :model="formData" label-width="90px">
      <el-form-item label="明细" required>
        <div class="w-full">
          <el-table :data="formData.items" size="small">
            <el-table-column label="品类" min-width="140">
              <template #default="{ row }">
                <el-select v-model="row.goodsConfigId" placeholder="请选择" filterable class="!w-full">
                  <el-option v-for="item in goodsConfigList" :key="item.id" :label="item.name" :value="item.id!" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="仓库" min-width="140">
              <template #default="{ row }">
                <el-select v-model="row.warehouseId" placeholder="请选择" filterable class="!w-full" @change="row.locationId = undefined">
                  <el-option v-for="item in warehouseList" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="库位" min-width="130">
              <template #default="{ row }">
                <el-select v-model="row.locationId" placeholder="不指定" clearable filterable class="!w-full">
                  <el-option v-for="item in locationsOf(row.warehouseId)" :key="item.id" :label="item.name" :value="item.id!" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="批次" min-width="130">
              <template #default="{ row }">
                <el-select v-model="row.batchId" placeholder="不指定" clearable filterable class="!w-full">
                  <el-option v-for="item in batchList" :key="item.id" :label="item.batchNo" :value="item.id!" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="期初数量" width="130">
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
          <el-button class="mt-10px" plain @click="addItem"><Icon icon="ep:plus" class="mr-5px" /> 添加行</el-button>
          <el-button class="mt-10px ml-10px" plain @click="pasteVisible = !pasteVisible">
            <Icon icon="ep:document-copy" class="mr-5px" /> 从 Excel 粘贴
          </el-button>
        </div>
      </el-form-item>
      <el-form-item v-if="pasteVisible" label="粘贴区">
        <el-input
          v-model="pasteText"
          type="textarea"
          :rows="4"
          placeholder="每行一条，逗号或制表符分隔：品类名称,仓库名称,库位名称（可空）,批次号（可空）,数量"
        />
        <el-button class="mt-10px" @click="applyPaste">解析并追加</el-button>
        <span class="ml-10px text-13px text-gray-500">名称需与系统中的品类 / 仓库 / 库位 / 批次一致；解析不到的行会被跳过并提示。</span>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="例如：启用平台前的存量" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submit">导入并过账</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { StockOpeningApi, StockOpeningVO } from '@/api/icbc/stockOps'
import { WarehouseApi, WarehouseVO } from '@/api/erp/stock/warehouse'
import { StockLocationApi, StockLocationVO } from '@/api/erp/stock/location'
import { StockBatchApi, StockBatchVO } from '@/api/erp/stock/batch'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'

/** 期初导入（#54 T16） */
defineOptions({ name: 'IcbcStockOpening' })

const message = useMessage()

const loading = ref(false)
const list = ref<StockOpeningVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  openingNo: undefined,
  goodsConfigId: undefined,
  warehouseId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await StockOpeningApi.getPage(queryParams)
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
  queryParams.openingNo = undefined
  queryParams.goodsConfigId = undefined
  queryParams.warehouseId = undefined
  queryParams.status = undefined
  queryParams.pageNo = 1
  getList()
}

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const pasteVisible = ref(false)
const pasteText = ref('')
const formData = reactive({ remark: '', items: [] as any[] })
const emptyItem = () => ({
  goodsConfigId: undefined,
  warehouseId: undefined,
  locationId: undefined,
  batchId: undefined,
  quantity: undefined
})
const openForm = () => {
  formData.remark = ''
  formData.items = [emptyItem()]
  pasteVisible.value = false
  pasteText.value = ''
  formVisible.value = true
}
const addItem = () => formData.items.push(emptyItem())
const removeItem = (index: number) => formData.items.splice(index, 1)

/** 从 Excel 粘贴：按名称解析成维度编号，解析不到的行跳过并提示 */
const applyPaste = () => {
  const lines = pasteText.value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
  let added = 0
  const failed: number[] = []
  lines.forEach((line, index) => {
    const cells = line.split(/[\t,，]/).map((cell) => cell.trim())
    const [categoryName, warehouseName, locationName, batchNo, quantityText] = cells
    const goodsConfig = goodsConfigList.value.find((item) => item.name === categoryName)
    const warehouse = warehouseList.value.find((item) => item.name === warehouseName)
    const quantity = Number(quantityText)
    if (!goodsConfig || !warehouse || !quantity || quantity <= 0) {
      failed.push(index + 1)
      return
    }
    const location = locationName
      ? locationList.value.find((item) => item.name === locationName && item.warehouseId === warehouse.id)
      : undefined
    const batch = batchNo ? batchList.value.find((item) => item.batchNo === batchNo) : undefined
    if ((locationName && !location) || (batchNo && !batch)) {
      failed.push(index + 1)
      return
    }
    formData.items.push({
      goodsConfigId: goodsConfig.id,
      warehouseId: warehouse.id,
      locationId: location?.id,
      batchId: batch?.id,
      quantity
    })
    added++
  })
  // 第一行常是表头，被跳过不计入失败提示
  if (failed.length > 0) {
    message.warning(`已追加 ${added} 行，第 ${failed.join('、')} 行解析失败（名称对不上或数量为空）`)
  } else {
    message.success(`已追加 ${added} 行`)
  }
}

const submit = async () => {
  const items = formData.items.filter((item) => item.goodsConfigId && item.warehouseId)
  if (items.length === 0) {
    message.warning('至少填写一行「品类 + 仓库 + 期初数量」')
    return
  }
  if (items.some((item) => !item.quantity || item.quantity <= 0)) {
    message.warning('期初数量必须大于 0')
    return
  }
  formLoading.value = true
  try {
    const openingNo = await StockOpeningApi.importOpening({ items, remark: formData.remark })
    message.success(`已导入并过账，批次号 ${openingNo}`)
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleCancel = async (id: number) => {
  const { value } = await message.prompt('作废原因（会同时冲销导入时加的库存）', '作废期初')
  await StockOpeningApi.cancel({ id, reason: value })
  message.success('已作废')
  await getList()
}

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
