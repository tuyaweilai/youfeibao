<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb-10px"
      title="称量差异 = 实物量（接收量优先，无则净重）− 结算重量。结算重量只作计价基准，两者不要求相等，差额不静默抹平。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="收购单号" prop="acquisitionNo">
        <el-input v-model="queryParams.acquisitionNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="出售者" prop="sellerName">
        <el-input v-model="queryParams.sellerName" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-160px" />
      </el-form-item>
      <el-form-item label="只看有差异" prop="hasDifference">
        <el-switch v-model="queryParams.hasDifference" />
      </el-form-item>
      <el-form-item label="只看已验收" prop="onlyAccepted">
        <el-switch v-model="queryParams.onlyAccepted" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="收购单号" prop="acquisitionNo" min-width="200" />
      <el-table-column label="出售者" prop="sellerName" width="100" />
      <el-table-column label="品类" prop="categoryName" width="100" />
      <el-table-column label="净重" align="right" prop="netWeight" width="110" />
      <el-table-column label="扣杂" align="right" prop="deduction" width="100" />
      <el-table-column label="结算重量" align="right" prop="settlementWeight" width="110" />
      <el-table-column label="实物量" align="right" prop="physicalWeight" width="110" />
      <el-table-column label="接收量" align="right" prop="acceptedWeight" width="110" />
      <el-table-column label="退回量" align="right" prop="rejectedWeight" width="110" />
      <el-table-column label="余货出场" align="right" prop="residualWeight" width="110" />
      <el-table-column label="称量差异" align="right" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.weightDiff === null || row.weightDiff === undefined" type="info" size="small">—</el-tag>
          <el-tag v-else-if="row.weightDiff === 0" type="success" size="small">0</el-tag>
          <el-tag v-else :type="row.weightDiff > 0 ? 'warning' : 'danger'" size="small">{{ row.weightDiff }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="拒收原因" prop="rejectReason" min-width="160" show-overflow-tooltip />
      <el-table-column label="应付金额(元)" align="right" prop="amount" width="130" />
      <el-table-column label="交易时间" align="center" prop="tradeTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAcceptance(row.id!)" v-hasPermi="['icbc:acquisition:acceptance']">
            记录接收结论
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <AcceptanceForm ref="acceptanceRef" @success="getList" />
</template>

<script setup lang="ts">
import { AcquisitionApi, AcquisitionWeightDiffVO } from '@/api/icbc/acquisition'
import { dateFormatter } from '@/utils/formatTime'
import AcceptanceForm from './AcceptanceForm.vue'

defineOptions({ name: 'IcbcAcquisitionWeightDiff' })

const loading = ref(true)
const list = ref<AcquisitionWeightDiffVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  acquisitionNo: undefined,
  sellerName: undefined,
  hasDifference: true,
  onlyAccepted: undefined as boolean | undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await AcquisitionApi.getWeightDiffPage(queryParams)
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

const acceptanceRef = ref()
const openAcceptance = (id: number) => acceptanceRef.value.open(id)

onMounted(getList)
</script>
