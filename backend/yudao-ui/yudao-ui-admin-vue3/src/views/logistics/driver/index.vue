<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="司机是回收企业建档的租户内账号（自有与承运商同构，账号由企业创建，不自主注册）。关联用户是他登录司机端用的系统用户；一个用户只能建一份司机档案。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="司机姓名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="queryParams.mobile" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-select v-model="queryParams.source" placeholder="请选择" clearable class="!w-140px">
          <el-option label="自有" :value="1" />
          <el-option label="承运商" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-140px">
          <el-option label="在职" :value="0" />
          <el-option label="离职" :value="1" />
          <el-option label="请假" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['logistics:driver:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增司机
        </el-button>
        <el-button type="success" plain @click="handleExport" :loading="exportLoading" v-hasPermi="['logistics:driver:export']">
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="司机姓名" prop="name" min-width="110" />
      <el-table-column label="手机号" prop="mobile" min-width="130" />
      <el-table-column label="关联用户" align="center" prop="userId" width="110" />
      <el-table-column label="来源" align="center" prop="source" width="100">
        <template #default="{ row }">
          <el-tag :type="row.source === 2 ? 'warning' : 'info'">{{ SOURCE_NAME[row.source] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="驾驶证到期" align="center" prop="drivingLicenseExpiryDate" width="130">
        <template #default="{ row }">
          <span :class="{ 'expiry--warn': isExpired(row.drivingLicenseExpiryDate) }">
            {{ row.drivingLicenseExpiryDate || '未登记' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="资格证到期" align="center" prop="qualificationCertExpiryDate" width="130">
        <template #default="{ row }">
          <span :class="{ 'expiry--warn': isExpired(row.qualificationCertExpiryDate) }">
            {{ row.qualificationCertExpiryDate || '未登记' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] || 'info'">{{ STATUS_NAME[row.status] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm('update', row.id)" v-hasPermi="['logistics:driver:update']">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)" v-hasPermi="['logistics:driver:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px" v-loading="formLoading">
      <el-form-item label="关联用户" prop="userId">
        <el-input-number v-model="formData.userId" :min="1" :controls="false" class="!w-200px" />
        <div class="tip">该用户的系统用户编号（在「系统管理 → 用户」里查看）。司机用它登录司机端。</div>
      </el-form-item>
      <el-form-item label="司机姓名" prop="name">
        <el-input v-model="formData.name" placeholder="如：张三" />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="formData.mobile" placeholder="选填" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-select v-model="formData.source" class="!w-200px">
          <el-option label="自有" :value="1" />
          <el-option label="承运商" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="formData.source === 2" label="所属承运商" prop="carrierId">
        <el-select v-model="formData.carrierId" placeholder="必填：来源为承运商时必须选" filterable class="!w-200px">
          <el-option v-for="c in carrierOptions" :key="c.id" :label="c.name" :value="c.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="驾驶证号码" prop="drivingLicenseNo">
        <el-input v-model="formData.drivingLicenseNo" placeholder="选填" />
      </el-form-item>
      <el-form-item label="准驾车型" prop="drivingLicenseType">
        <el-input v-model="formData.drivingLicenseType" placeholder="如 A2 / B2" class="!w-200px" />
      </el-form-item>
      <el-form-item label="驾驶证到期" prop="drivingLicenseExpiryDate">
        <el-date-picker
          v-model="formData.drivingLicenseExpiryDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选填；过期将不能派车"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="资格证号码" prop="qualificationCertNo">
        <el-input v-model="formData.qualificationCertNo" placeholder="选填" />
      </el-form-item>
      <el-form-item label="资格证到期" prop="qualificationCertExpiryDate">
        <el-date-picker
          v-model="formData.qualificationCertExpiryDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选填；过期将不能派车"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="formData.status" class="!w-200px">
          <el-option label="在职" :value="0" />
          <el-option label="离职" :value="1" />
          <el-option label="请假" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { LogisticsDriverApi, LogisticsDriverVO } from '@/api/logistics/driver'
import { LogisticsCarrierApi, LogisticsCarrierVO } from '@/api/logistics/carrier'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'

defineOptions({ name: 'LogisticsDriver' })

const SOURCE_NAME: Record<number, string> = { 1: '自有', 2: '承运商' }
const STATUS_NAME: Record<number, string> = { 0: '在职', 1: '离职', 2: '请假' }
const STATUS_TAG: Record<number, 'success' | 'warning' | 'info'> = { 0: 'success', 1: 'info', 2: 'warning' }

const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const exportLoading = ref(false)
const list = ref<LogisticsDriverVO[]>([])
const carrierOptions = ref<LogisticsCarrierVO[]>([])

/** 到期日早于今天即过期——与后端门禁同一口径（当天到期仍有效） */
function isExpired(date?: string) {
  if (!date) return false
  return new Date(date).getTime() < new Date(new Date().toDateString()).getTime()
}

async function loadCarriers() {
  const page = await LogisticsCarrierApi.getCarrierPage({ pageNo: 1, pageSize: 100, status: 0 })
  carrierOptions.value = page.list
}
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  mobile: undefined,
  source: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await LogisticsDriverApi.getDriverPage(queryParams)
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

// ==================== 新增 / 编辑 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<LogisticsDriverVO>(buildEmpty())

function buildEmpty(): LogisticsDriverVO {
  return {
    userId: undefined,
    name: undefined,
    mobile: undefined,
    source: 1,
    carrierId: undefined,
    drivingLicenseNo: undefined,
    drivingLicenseType: undefined,
    drivingLicenseExpiryDate: undefined,
    qualificationCertNo: undefined,
    qualificationCertExpiryDate: undefined,
    status: 0,
    remark: undefined
  }
}

const formRules = reactive({
  userId: [{ required: true, message: '关联用户不能为空', trigger: 'change' }],
  name: [{ required: true, message: '司机姓名不能为空', trigger: 'blur' }],
  source: [{ required: true, message: '来源不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await LogisticsDriverApi.exportDriver(queryParams)
    download.excel(data, '司机.xls')
  } finally {
    exportLoading.value = false
  }
}

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  await loadCarriers()
  formType.value = type
  dialogTitle.value = t('action.' + type)
  formData.value = buildEmpty()
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await LogisticsDriverApi.getDriver(id)
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await LogisticsDriverApi.createDriver(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await LogisticsDriverApi.updateDriver(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await LogisticsDriverApi.deleteDriver(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 初始化 **/
getList()
</script>

<style scoped lang="scss">
.expiry--warn {
  color: var(--el-color-danger);
  font-weight: 600;
}

.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}
</style>
