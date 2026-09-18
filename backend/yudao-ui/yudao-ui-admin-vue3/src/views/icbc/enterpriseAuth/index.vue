<template>
  <!-- 发起授权 -->
  <ContentWrap title="发起企业授权">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="由法定代表人或财务负责人用税务 App 扫码完成实人认证。平台经适配层生成工行授权页面，授权结果与有效期由管理员回填。"
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
          <el-tag :type="effectiveStatus(row).type">{{ effectiveStatus(row).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="授权时间" align="center" prop="authTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="有效期止" align="center" prop="expireTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openUpdate(row)" v-hasPermi="['icbc:enterprise-auth:update']">
            回填结果
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 回填授权结果对话框 -->
  <Dialog v-model="updateVisible" title="回填授权结果" width="520">
    <el-form ref="updateFormRef" :model="updateForm" label-width="110px">
      <el-form-item label="付方编号">
        <el-input v-model="updateForm.outVendorId" disabled />
      </el-form-item>
      <el-form-item label="授权状态" prop="authStatus" :rules="[{ required: true, message: '请选择授权状态' }]">
        <el-select v-model="updateForm.authStatus" class="w-full">
          <el-option label="未授权" :value="0" />
          <el-option label="已授权" :value="1" />
          <el-option label="已失效" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="授权时间">
        <el-date-picker
          v-model="updateForm.authTime"
          type="datetime"
          value-format="x"
          placeholder="不填且已授权时默认当前时间"
          class="w-full"
        />
      </el-form-item>
      <el-form-item label="授权有效期止">
        <el-date-picker
          v-model="updateForm.expireTime"
          type="datetime"
          value-format="x"
          placeholder="请选择有效期止"
          class="w-full"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="updateForm.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="updateLoading" @click="submitUpdate">确 定</el-button>
      <el-button @click="updateVisible = false">取 消</el-button>
    </template>
  </Dialog>
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
// 已授权但有效期已过时，展示为「已过期」
const isExpired = (row: EnterpriseAuthVO) =>
  row.authStatus === 1 && !!row.expireTime && row.expireTime < Date.now()
const effectiveStatus = (row: EnterpriseAuthVO) => {
  if (isExpired(row)) return { label: '已过期', type: 'danger' as const }
  return STATUS[row.authStatus ?? -1] ?? { label: '-', type: 'info' as const }
}

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

// ===== 回填授权结果 =====
const updateVisible = ref(false)
const updateLoading = ref(false)
const updateFormRef = ref()
const updateForm = reactive({
  id: 0,
  outVendorId: '',
  authStatus: 1,
  authTime: undefined as number | undefined,
  expireTime: undefined as number | undefined,
  remark: undefined as string | undefined
})

const openUpdate = (row: EnterpriseAuthVO) => {
  updateForm.id = row.id!
  updateForm.outVendorId = row.outVendorId ?? ''
  updateForm.authStatus = row.authStatus ?? 1
  updateForm.authTime = row.authTime
  updateForm.expireTime = row.expireTime
  updateForm.remark = row.remark
  updateVisible.value = true
}

const submitUpdate = async () => {
  await updateFormRef.value.validate()
  updateLoading.value = true
  try {
    await EnterpriseAuthApi.updateResult({
      id: updateForm.id,
      authStatus: updateForm.authStatus,
      authTime: updateForm.authTime,
      expireTime: updateForm.expireTime,
      remark: updateForm.remark
    })
    message.success('已更新')
    updateVisible.value = false
    await getList()
  } finally {
    updateLoading.value = false
  }
}

onMounted(getList)
</script>
