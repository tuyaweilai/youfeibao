<template>
  <ContentWrap title="跨租户资质核实">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="平台运营跨租户查看并核实各回收企业的三层资质。任一层未核实为「有效」，该租户即不可开票。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="租户编号" prop="tenantId">
        <el-input v-model="queryParams.tenantId" placeholder="如 1" clearable class="!w-140px" />
      </el-form-item>
      <el-form-item label="资质层" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择" clearable class="!w-170px">
          <el-option label="税务侧" value="TAX" />
          <el-option label="行业侧" value="INDUSTRY" />
          <el-option label="公安侧" value="PUBLIC_SECURITY" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-150px">
          <el-option label="待核实" :value="0" />
          <el-option label="有效" :value="1" />
          <el-option label="失效" :value="2" />
          <el-option label="吊销" :value="3" />
        </el-select>
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
      <el-table-column label="租户编号" align="center" prop="tenantId" width="100" />
      <el-table-column label="资质层" align="center" prop="type" width="110">
        <template #default="{ row }">{{ layerLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column label="资质名称" prop="name" min-width="160" />
      <el-table-column label="发证机关" prop="issuingAuthority" min-width="150" />
      <el-table-column label="有效期止" align="center" prop="validTo" width="120" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="核实意见" prop="auditRemark" min-width="160" />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="audit(row.id, 1)" v-hasPermi="['icbc:platform:qualification:audit']">核实有效</el-button>
          <el-button link type="warning" @click="audit(row.id, 3)" v-hasPermi="['icbc:platform:qualification:audit']">吊销</el-button>
          <el-button link type="danger" @click="audit(row.id, 2)" v-hasPermi="['icbc:platform:qualification:audit']">标记失效</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { PlatformQualificationApi, PlatformQualificationVO } from '@/api/icbc/platformQualification'

defineOptions({ name: 'IcbcPlatformQualification' })

const message = useMessage()

const loading = ref(true)
const list = ref<PlatformQualificationVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10, tenantId: undefined, type: undefined, status: undefined })
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
    const data = await PlatformQualificationApi.getPage(queryParams)
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

const audit = async (id: number, status: number) => {
  const { value } = await message.prompt('请输入核实意见', '核实意见')
  await PlatformQualificationApi.audit(id, status, value || '')
  message.success('已核实')
  await getList()
}

onMounted(getList)
</script>
