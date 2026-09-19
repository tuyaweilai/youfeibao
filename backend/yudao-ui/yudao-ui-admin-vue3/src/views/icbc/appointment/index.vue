<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="预约到站不是订单：不占额度、不产生开票、不进五流。它只在收货员登记收购时带出品类、约多少与车牌；没有接单 / 拒单，只有到场与未到场。"
    />
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="预计数量一律只是「约」，任何统计与额度口径都不得引用预约数据。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="110px">
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="场站编号" prop="stationId">
        <el-input v-model="queryParams.stationId" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-160px" />
      </el-form-item>
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="queryParams.plateNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="预约编号" prop="appointmentNo" min-width="180" />
      <el-table-column label="出售者" prop="sellerName" min-width="100" />
      <el-table-column label="场站" prop="stationName" min-width="140" />
      <el-table-column label="品类" prop="categoryName" min-width="110" />
      <el-table-column label="预计数量" prop="expectedQuantityText" min-width="120">
        <template #default="{ row }">
          <span>{{ row.expectedQuantityText || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="车牌" prop="plateNo" min-width="110" />
      <el-table-column label="预计到站" prop="expectedArrivalTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="row.status !== 0"
            @click="handleArrive(row)"
            v-hasPermi="['icbc:appointment:manage']"
          >
            到场
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.status !== 0"
            @click="handleNoShow(row)"
            v-hasPermi="['icbc:appointment:manage']"
          >
            未到场
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { AppointmentApi, AppointmentVO } from '@/api/icbc/appointment'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'IcbcAppointment' })

const message = useMessage()

const statusOptions = [
  { label: '待到站', value: 0 },
  { label: '已到场', value: 1 },
  { label: '未到场', value: 2 },
  { label: '已取消', value: 9 }
]

const loading = ref(true)
const list = ref<AppointmentVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  status: undefined,
  stationId: undefined,
  plateNo: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await AppointmentApi.getAppointmentPage(queryParams)
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

const statusTag = (status?: number) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 9) return 'info'
  return 'warning'
}

const handleArrive = async (row: AppointmentVO) => {
  try {
    await message.confirm(`确认「${row.sellerName || row.appointmentNo}」已到场？`)
    await AppointmentApi.arrive({ id: row.id! })
    message.success('已标记到场')
    await getList()
  } catch {}
}

const handleNoShow = async (row: AppointmentVO) => {
  try {
    const { value } = await message.prompt('填写未到场说明（选填）', '未到场')
    await AppointmentApi.noShow({ id: row.id!, reason: value || undefined })
    message.success('已标记未到场')
    await getList()
  } catch {}
}

onMounted(getList)
</script>
