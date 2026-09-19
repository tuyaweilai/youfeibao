<template>
  <ContentWrap title="计费计量">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="按成功开具的报废产品收购发票张数计量，蓝票被成功红冲后不再计入，红票本身不计。计量台账是平台自己的账，回收企业租户不可读写。"
    />

    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="期间" prop="periodMonth">
        <el-date-picker
          v-model="queryParams.periodMonth"
          type="month"
          value-format="YYYY-MM"
          placeholder="如 2026-09"
          clearable
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item label="租户编号" prop="tenantId">
        <el-input v-model="queryParams.tenantId" placeholder="如 1" clearable class="!w-140px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          v-hasPermi="['icbc:platform:billing:manage']"
          :disabled="!generatePeriod"
          @click="generateAll"
        >
          重新计量
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="租户编号" align="center" prop="tenantId" width="100" />
      <el-table-column label="期间" align="center" prop="periodMonth" width="100" />
      <el-table-column label="成功开具" align="center" prop="issuedCount" width="100" />
      <el-table-column label="已红冲" align="center" prop="reversedCount" width="100" />
      <el-table-column label="计费张数" align="center" prop="billableCount" width="100">
        <template #default="{ row }">
          <span class="font-bold">{{ row.billableCount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单价（元/张）" align="center" prop="unitPrice" width="120" />
      <el-table-column label="应计费用（元）" align="center" width="130">
        <template #default="{ row }">
          <span class="text-green-600 font-bold">{{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="计量时间" align="center" prop="generatedTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="warning"
            v-hasPermi="['icbc:platform:billing:manage']"
            @click="generateOne(row)"
          >
            重新计量
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { PlatformBillingApi, BillingLedgerVO } from '@/api/icbc/platformBilling'

defineOptions({ name: 'IcbcPlatformBilling' })

const message = useMessage()

const loading = ref(true)
const list = ref<BillingLedgerVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  tenantId: undefined,
  periodMonth: undefined
})
const queryFormRef = ref()

const generatePeriod = computed(() => queryParams.periodMonth)

const getList = async () => {
  loading.value = true
  try {
    const data = await PlatformBillingApi.getPage(queryParams)
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
  queryFormRef.value.resetFields()
  handleQuery()
}

const generateAll = async () => {
  await message.confirm(`确认按 ${generatePeriod.value} 重新计量全部租户？同一租户同一期间会覆盖，不产生重复记录。`)
  const result = await PlatformBillingApi.generate(generatePeriod.value!)
  message.success(`已计量 ${result.length} 个租户`)
  await getList()
}

const generateOne = async (row: BillingLedgerVO) => {
  await message.confirm(`确认重新计量租户 ${row.tenantId} 的 ${row.periodMonth}？`)
  await PlatformBillingApi.generateTenant(row.tenantId!, row.periodMonth!)
  message.success('已重新计量')
  await getList()
}

onMounted(getList)
</script>
