<template>
  <!-- 发起授权 -->
  <ContentWrap title="发起企业授权">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="由法定代表人或财务负责人用税务 App 扫码完成实人认证并录入授权有效期。平台经适配层生成工行授权页面。"
    />
    <el-form :inline="true" label-width="100px">
      <el-form-item label="付方编号">
        <el-input v-model="initForm.outVendorId" placeholder="回收企业 / 子商户编号" clearable class="!w-260px" />
      </el-form-item>
      <el-form-item label="siteType">
        <el-input v-model="initForm.siteType" placeholder="选填" clearable class="!w-140px" />
      </el-form-item>
      <el-form-item label="userType">
        <el-input v-model="initForm.userType" placeholder="选填" clearable class="!w-140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="initLoading" @click="handleInit" v-hasPermi="['icbc:enterprise-auth:init']">
          发起授权
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 授权记录 -->
  <ContentWrap title="授权记录">
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="付方编号" prop="outVendorId" min-width="160" />
      <el-table-column label="siteType" align="center" prop="siteType" width="100" />
      <el-table-column label="userType" align="center" prop="userType" width="100" />
      <el-table-column label="授权状态" align="center" prop="authStatus" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.authStatus)">{{ statusLabel(row.authStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="markStatus(row.id, 1)" v-hasPermi="['icbc:enterprise-auth:update']">标记已授权</el-button>
          <el-button link type="danger" @click="markStatus(row.id, 2)" v-hasPermi="['icbc:enterprise-auth:update']">标记失效</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { EnterpriseAuthApi, EnterpriseAuthVO } from '@/api/icbc/enterpriseAuth'
import { openIcbcForm } from '../util'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'IcbcEnterpriseAuth' })

const message = useMessage()

const initForm = ref<{ outVendorId: string; siteType?: string; userType?: string }>({
  outVendorId: '',
  siteType: '01',
  userType: '01'
})
const initLoading = ref(false)
const handleInit = async () => {
  if (!initForm.value.outVendorId) {
    message.warning('请输入付方编号')
    return
  }
  initLoading.value = true
  try {
    const html = await EnterpriseAuthApi.init(initForm.value)
    if (html) {
      openIcbcForm(html, '企业授权')
      message.success('已生成企业授权页面')
      await getList()
    } else {
      message.alert('未返回授权页面')
    }
  } finally {
    initLoading.value = false
  }
}

const loading = ref(true)
const list = ref<EnterpriseAuthVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10 })

const STATUS: Record<number, { label: string; type: 'info' | 'success' | 'danger' }> = {
  0: { label: '未授权', type: 'info' },
  1: { label: '已授权', type: 'success' },
  2: { label: '已失效', type: 'danger' }
}
const statusLabel = (s?: number) => (s !== undefined ? STATUS[s]?.label ?? s : '-')
const statusType = (s?: number): 'info' | 'success' | 'danger' => (s !== undefined ? STATUS[s]?.type ?? 'info' : 'info')

const getList = async () => {
  loading.value = true
  try {
    const data = await EnterpriseAuthApi.getEnterpriseAuthPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const markStatus = async (id: number, authStatus: number) => {
  await EnterpriseAuthApi.updateStatus(id, authStatus)
  message.success('已更新')
  await getList()
}

onMounted(getList)
</script>
