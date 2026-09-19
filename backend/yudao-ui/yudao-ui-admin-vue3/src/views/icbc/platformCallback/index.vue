<template>
  <ContentWrap title="工行通知监控">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="九类银税協同通知的处理结果、时间与关联业务。失败的通知可查看原因并手动重放；已成功的通知不会被重复处理。"
    />

    <el-row :gutter="12" class="mb-15px">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-label">通知总数</div>
          <div class="stat-value">{{ summary.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-label">待处理</div>
          <div class="stat-value">{{ summary.pendingCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-label">处理成功</div>
          <div class="stat-value text-green-600">{{ summary.successCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-label">处理失败</div>
          <div class="stat-value text-red-600">{{ summary.failureCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="summary.types" :stripe="true" size="small" class="mb-15px">
      <el-table-column label="通知类型" prop="notifyTypeName" min-width="160" />
      <el-table-column label="关联业务" prop="businessName" width="140" />
      <el-table-column label="通知数" align="center" prop="total" width="100" />
      <el-table-column label="失败数" align="center" prop="failureCount" width="100">
        <template #default="{ row }">
          <span :class="{ 'text-red-600': row.failureCount > 0 }">{{ row.failureCount }}</span>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="通知类型" prop="notifyType">
        <el-select v-model="queryParams.notifyType" placeholder="请选择" clearable class="!w-190px">
          <el-option
            v-for="type in summary.types"
            :key="type.notifyType"
            :label="`${type.notifyType} ${type.notifyTypeName}`"
            :value="type.notifyType"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="processStatus">
        <el-select v-model="queryParams.processStatus" placeholder="请选择" clearable class="!w-150px">
          <el-option label="待处理" :value="0" />
          <el-option label="处理成功" :value="1" />
          <el-option label="处理失败" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务ID" prop="businessId">
        <el-input v-model="queryParams.businessId" placeholder="订单号 / 合作方订单号" clearable class="!w-220px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="通知ID" prop="notifyId" min-width="180" show-overflow-tooltip />
      <el-table-column label="通知类型" min-width="170">
        <template #default="{ row }">{{ row.notifyType }} {{ row.notifyTypeName }}</template>
      </el-table-column>
      <el-table-column label="关联业务" prop="businessName" width="110" />
      <el-table-column label="业务ID" prop="businessId" min-width="170" show-overflow-tooltip />
      <el-table-column label="处理状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.processStatus)">{{ row.processStatusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理结果" prop="processMsg" min-width="180" show-overflow-tooltip />
      <el-table-column label="处理时间" align="center" prop="processTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="重试次数" align="center" prop="retryCount" width="90" />
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.processStatus !== 1"
            link
            type="warning"
            v-hasPermi="['icbc:platform:callback:retry']"
            @click="replay(row)"
          >
            重放
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <el-dialog v-model="detailVisible" title="通知详情" width="760px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="通知ID">{{ detail.notifyId }}</el-descriptions-item>
      <el-descriptions-item label="通知类型">{{ detail.notifyType }} {{ detail.notifyTypeName }}</el-descriptions-item>
      <el-descriptions-item label="关联业务">{{ detail.businessName }}</el-descriptions-item>
      <el-descriptions-item label="业务ID">{{ detail.businessId }}</el-descriptions-item>
      <el-descriptions-item label="处理状态">
        <el-tag :type="statusType(detail.processStatus)">{{ detail.processStatusName }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="重试次数">{{ detail.retryCount }}</el-descriptions-item>
      <el-descriptions-item label="处理结果" :span="2">{{ detail.processMsg || '-' }}</el-descriptions-item>
      <el-descriptions-item label="下一步" :span="2">{{ detail.nextAction || '-' }}</el-descriptions-item>
    </el-descriptions>
    <div class="mt-15px mb-5px font-bold">通知报文</div>
    <el-input v-model="prettyData" type="textarea" :rows="12" readonly />
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import {
  PlatformCallbackApi,
  CallbackNotifySummaryVO,
  CallbackNotifyVO
} from '@/api/icbc/platformCallback'

defineOptions({ name: 'IcbcPlatformCallback' })

const message = useMessage()

const loading = ref(true)
const list = ref<CallbackNotifyVO[]>([])
const total = ref(0)
const summary = ref<CallbackNotifySummaryVO>({
  total: 0,
  pendingCount: 0,
  successCount: 0,
  failureCount: 0,
  types: []
})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  notifyType: undefined,
  processStatus: undefined,
  businessId: undefined
})
const queryFormRef = ref()

const detailVisible = ref(false)
const detail = ref<CallbackNotifyVO>({})
const prettyData = ref('')

const statusType = (status?: number): 'info' | 'success' | 'danger' => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

const getSummary = async () => {
  summary.value = await PlatformCallbackApi.getSummary()
}

const getList = async () => {
  loading.value = true
  try {
    const data = await PlatformCallbackApi.getPage(queryParams)
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

const openDetail = (row: CallbackNotifyVO) => {
  detail.value = row
  try {
    prettyData.value = row.notifyData ? JSON.stringify(JSON.parse(row.notifyData), null, 2) : ''
  } catch {
    prettyData.value = row.notifyData || ''
  }
  detailVisible.value = true
}

const replay = async (row: CallbackNotifyVO) => {
  await message.confirm('确认重放该通知？已成功的通知不会被重复处理。')
  await PlatformCallbackApi.replay(row.id!)
  message.success('已重放')
  await Promise.all([getList(), getSummary()])
}

const refresh = async () => {
  await getSummary()
  await getList()
}

onMounted(refresh)
</script>

<style scoped>
.stat-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin-top: 6px;
}
</style>
