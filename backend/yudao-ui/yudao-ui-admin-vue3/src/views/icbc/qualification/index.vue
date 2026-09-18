<template>
  <ContentWrap>
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="三层资质任一层失效或被吊销，该租户的开票能力即刻冻结。务必在到期前更新。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="资质层" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择" clearable class="!w-180px">
          <el-option label="税务侧" value="TAX" />
          <el-option label="行业侧" value="INDUSTRY" />
          <el-option label="公安侧" value="PUBLIC_SECURITY" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-160px">
          <el-option label="待核实" :value="0" />
          <el-option label="有效" :value="1" />
          <el-option label="失效" :value="2" />
          <el-option label="吊销" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['icbc:qualification:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="资质层" align="center" prop="type" width="110">
        <template #default="{ row }">{{ layerLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column label="资质名称" prop="name" min-width="180" />
      <el-table-column label="发证机关" prop="issuingAuthority" min-width="160" />
      <el-table-column label="证书编号" prop="certNo" min-width="140" />
      <el-table-column label="有效期止" align="center" prop="validTo" width="150">
        <template #default="{ row }">
          {{ row.validTo }}
          <el-tag v-if="row.expiringSoon" type="warning" size="small" class="ml-5px">临近到期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm('update', row.id)" v-hasPermi="['icbc:qualification:update']">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)" v-hasPermi="['icbc:qualification:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <QualificationForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { QualificationApi, QualificationVO } from '@/api/icbc/qualification'
import QualificationForm from './QualificationForm.vue'

defineOptions({ name: 'IcbcQualification' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<QualificationVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10, type: undefined, name: undefined, status: undefined })
const queryFormRef = ref()

const LAYER: Record<string, string> = { TAX: '税务侧', INDUSTRY: '行业侧', PUBLIC_SECURITY: '公安侧' }
const layerLabel = (type?: string) => (type ? LAYER[type] ?? type : '-')
const STATUS: Record<number, { label: string; type: 'info' | 'success' | 'danger' | 'warning' }> = {
  0: { label: '待核实', type: 'info' },
  1: { label: '有效', type: 'success' },
  2: { label: '失效', type: 'danger' },
  3: { label: '吊销', type: 'warning' }
}
const statusLabel = (s?: number) => (s !== undefined ? STATUS[s]?.label ?? s : '-')
const statusType = (s?: number): 'info' | 'success' | 'danger' | 'warning' =>
  s !== undefined ? STATUS[s]?.type ?? 'info' : 'info'

const getList = async () => {
  loading.value = true
  try {
    const data = await QualificationApi.getQualificationPage(queryParams)
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
    await QualificationApi.deleteQualification(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(getList)
</script>
