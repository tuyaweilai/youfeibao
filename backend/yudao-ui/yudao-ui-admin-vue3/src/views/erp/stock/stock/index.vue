<!-- ERP 品类库存列表：按品类 / 仓库 / 库位 / 批次查询 -->
<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="品类" prop="goodsConfigId">
        <el-select
          v-model="queryParams.goodsConfigId"
          clearable
          filterable
          placeholder="请选择品类"
          class="!w-200px"
        >
          <el-option
            v-for="item in goodsConfigList"
            :key="item.id"
            :label="item.name"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseId">
        <el-select
          v-model="queryParams.warehouseId"
          clearable
          filterable
          placeholder="请选择仓库"
          class="!w-200px"
          @change="handleWarehouseChange"
        >
          <el-option
            v-for="item in warehouseList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="库位" prop="locationId">
        <el-select
          v-model="queryParams.locationId"
          clearable
          filterable
          placeholder="请选择库位"
          class="!w-200px"
        >
          <el-option
            v-for="item in locationList"
            :key="item.id"
            :label="item.name"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="批次" prop="batchId">
        <el-select
          v-model="queryParams.batchId"
          clearable
          filterable
          placeholder="请选择批次"
          class="!w-200px"
        >
          <el-option
            v-for="item in batchList"
            :key="item.id"
            :label="item.batchNo"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['erp:stock:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="品类" align="center">
        <template #default="scope">
          {{ goodsConfigName(scope.row.goodsConfigId) }}
        </template>
      </el-table-column>
      <el-table-column label="仓库" align="center" prop="warehouseName" />
      <el-table-column label="库位" align="center">
        <template #default="scope">
          {{ scope.row.locationName || '未指定' }}
        </template>
      </el-table-column>
      <el-table-column label="批次" align="center">
        <template #default="scope">
          {{ scope.row.batchNo || '未指定' }}
        </template>
      </el-table-column>
      <el-table-column
        label="库存量"
        align="center"
        prop="count"
        :formatter="erpCountTableColumnFormatter"
      />
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import download from '@/utils/download'
import { StockApi, StockVO } from '@/api/erp/stock/stock'
import { WarehouseApi, WarehouseVO } from '@/api/erp/stock/warehouse'
import { StockLocationApi, StockLocationVO } from '@/api/erp/stock/location'
import { StockBatchApi, StockBatchVO } from '@/api/erp/stock/batch'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'
import { erpCountTableColumnFormatter } from '@/utils'

/** ERP 品类库存列表 */
defineOptions({ name: 'ErpStock' })

const message = useMessage() // 消息弹窗

const loading = ref(true) // 列表的加载中
const list = ref<StockVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  goodsConfigId: undefined,
  warehouseId: undefined,
  locationId: undefined,
  batchId: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const goodsConfigList = ref<GoodsConfigVO[]>([]) // 品类列表
const warehouseList = ref<WarehouseVO[]>([]) // 仓库列表
const locationList = ref<StockLocationVO[]>([]) // 库位列表
const batchList = ref<StockBatchVO[]>([]) // 批次列表

/** 品类名称 */
const goodsConfigName = (id?: number) => {
  if (!id) return '-'
  return goodsConfigList.value.find((item) => item.id === id)?.name || String(id)
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await StockApi.getStockPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 切换仓库时，刷新其下的库位下拉 */
const handleWarehouseChange = async (warehouseId?: number) => {
  queryParams.locationId = undefined
  locationList.value = warehouseId
    ? await StockLocationApi.getStockLocationSimpleList(warehouseId)
    : []
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  locationList.value = []
  handleQuery()
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await StockApi.exportStock(queryParams)
    download.excel(data, '品类库存.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await getList()
  goodsConfigList.value = await GoodsConfigApi.getEnabledList()
  warehouseList.value = await WarehouseApi.getWarehouseSimpleList()
  batchList.value = await StockBatchApi.getStockBatchSimpleList()
})
</script>
