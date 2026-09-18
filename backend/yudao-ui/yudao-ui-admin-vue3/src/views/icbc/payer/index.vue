<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="110px">
      <el-form-item label="企业名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="统一社会信用代码" prop="creditCode">
        <el-input v-model="queryParams.creditCode" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-220px" />
      </el-form-item>
      <el-form-item label="纳税人识别号" prop="taxNo">
        <el-input v-model="queryParams.taxNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-220px" />
      </el-form-item>
      <el-form-item label="审核状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-160px">
          <el-option label="待审核" :value="0" />
          <el-option label="审核通过" :value="1" />
          <el-option label="审核拒绝" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['icbc:payer-info:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="企业名称" align="center" prop="name" min-width="180" />
      <el-table-column label="统一社会信用代码" align="center" prop="creditCode" min-width="180" />
      <el-table-column label="纳税人识别号" align="center" prop="taxNo" min-width="180" />
      <el-table-column label="联系人" align="center" prop="contactName" width="110" />
      <el-table-column label="联系电话" align="center" prop="contactMobile" width="130" />
      <el-table-column label="审核状态" align="center" prop="status" width="110">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="操作" align="center" fixed="right" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['icbc:payer-info:update']">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['icbc:payer-info:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <PayerForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { PayerApi, PayerVO } from '@/api/icbc/payer'
import PayerForm from './PayerForm.vue'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'IcbcPayer' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<PayerVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  creditCode: undefined,
  taxNo: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await PayerApi.getPayerPage(queryParams)
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

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await PayerApi.deletePayer(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const statusMap: Record<number, { label: string; type: 'info' | 'success' | 'danger' }> = {
  0: { label: '待审核', type: 'info' },
  1: { label: '审核通过', type: 'success' },
  2: { label: '审核拒绝', type: 'danger' }
}
const statusLabel = (status?: number) =>
  status !== undefined && statusMap[status] ? statusMap[status].label : '未知'
const statusType = (status?: number): 'info' | 'success' | 'danger' =>
  status !== undefined && statusMap[status] ? statusMap[status].type : 'info'

onMounted(() => getList())
</script>
