<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="收购单号" prop="acquisitionNo">
        <el-input v-model="queryParams.acquisitionNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-220px" />
      </el-form-item>
      <el-form-item label="出售者" prop="sellerName">
        <el-input v-model="queryParams.sellerName" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="车牌号" prop="vehiclePlateNo">
        <el-input v-model="queryParams.vehiclePlateNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-160px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-140px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="采购安排" prop="directAcquisition">
        <el-select v-model="queryParams.directAcquisition" placeholder="请选择" clearable class="!w-160px">
          <el-option label="直接收购" :value="true" />
          <el-option label="关联采购订单" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['icbc:acquisition:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 登记收购
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="收购单号" prop="acquisitionNo" min-width="200" />
      <el-table-column label="出售者" prop="sellerName" min-width="100" />
      <el-table-column label="主体类型" prop="sellerSubjectTypeName" width="130" />
      <el-table-column label="联系方式" prop="sellerMobile" width="130" />
      <el-table-column label="品类" prop="categoryName" min-width="100" />
      <el-table-column label="数量" align="right" width="140">
        <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
      </el-table-column>
      <el-table-column label="金额(元)" align="right" prop="amount" width="120" />
      <el-table-column label="采购安排" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="row.directAcquisition ? 'info' : 'primary'" size="small">
            {{ row.purchaseArrangementText || (row.directAcquisition ? '直接收购' : '采购订单') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="车牌" align="center" width="130">
        <template #default="{ row }">
          <span>{{ row.vehiclePlateNo || '-' }}</span>
          <el-tag v-if="row.plateMatched === true" type="success" size="small" class="ml-1">一致</el-tag>
          <el-tag v-else-if="row.plateMatched === false" type="danger" size="small" class="ml-1">不一致</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交易时间" align="center" prop="tradeTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="要件" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="row.documentStatus === 'PENDING' ? 'warning' : 'success'" size="small">
            {{ row.documentStatusName || '已齐' }}
          </el-tag>
          <div v-if="row.documentGap" class="text-12px text-red-500">{{ row.documentGap }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.statusName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAcceptance(row.id!)" v-hasPermi="['icbc:acquisition:acceptance']">
            接收结论
          </el-button>
          <el-button link type="primary" @click="openForm('correct', row.id)" v-hasPermi="['icbc:acquisition:update']">
            修正识别结果
          </el-button>
          <el-button link type="primary" @click="handleExport(row.id)" v-hasPermi="['icbc:acquisition:export']">
            确认书
          </el-button>
          <el-button
            v-if="row.documentStatus === 'PENDING'"
            link
            type="warning"
            @click="handleCompleteDocuments(row)"
            v-hasPermi="['icbc:acquisition:update']"
          >
            补档放行
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <AcquisitionForm ref="formRef" @success="getList" />
  <AcceptanceForm ref="acceptanceRef" @success="getList" />
</template>

<script setup lang="ts">
import { AcquisitionApi, AcquisitionVO } from '@/api/icbc/acquisition'
import AcquisitionForm from './AcquisitionForm.vue'
import AcceptanceForm from './AcceptanceForm.vue'
import download from '@/utils/download'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'IcbcAcquisition' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<AcquisitionVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  acquisitionNo: undefined,
  sellerName: undefined,
  vehiclePlateNo: undefined,
  status: undefined,
  directAcquisition: undefined as boolean | undefined
})
const queryFormRef = ref()

const statusOptions = [
  { label: '已登记', value: 0 },
  { label: '待付款', value: 1 },
  { label: '已付款', value: 2 },
  { label: '已开票', value: 3 },
  { label: '已取消', value: 9 }
]
const statusTagType = (status?: number) => {
  if (status === 3) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 9) return 'info'
  return ''
}

const getList = async () => {
  loading.value = true
  try {
    const data = await AcquisitionApi.getAcquisitionPage(queryParams)
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

const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)

const acceptanceRef = ref()
const openAcceptance = (id: number) => acceptanceRef.value.open(id)

/**
 * 补档放行（V6 #73）：上门提货缺身份证 / 银行卡时先记为待补档，付款与开票被门禁拦住。
 * 证件补齐后在这里放行——留办理人与时间，不以「改一个状态」了事。
 */
const handleCompleteDocuments = async (row: AcquisitionVO) => {
  try {
    const { value } = await message.prompt(
      `补档放行 ${row.acquisitionNo}（当前缺：${row.documentGap || '证件'}）`,
      '补档说明'
    )
    await AcquisitionApi.completeDocuments({ id: row.id!, remark: value || undefined })
    message.success('已放行：这一笔可以正常付款与开票了')
    await getList()
  } catch {}
}

const handleExport = async (id: number) => {
  try {
    await message.exportConfirm()
    const data = await AcquisitionApi.exportConfirmation(id)
    download.excel(data, `收购确认书_${id}.xls`)
  } catch {}
}

onMounted(getList)
</script>
