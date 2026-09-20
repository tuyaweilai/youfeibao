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
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'LogisticsDriver' })

const SOURCE_NAME: Record<number, string> = { 1: '自有', 2: '承运商' }
const STATUS_NAME: Record<number, string> = { 0: '在职', 1: '离职', 2: '请假' }
const STATUS_TAG: Record<number, 'success' | 'warning' | 'info'> = { 0: 'success', 1: 'info', 2: 'warning' }

const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const list = ref<LogisticsDriverVO[]>([])
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
  return { userId: undefined, name: undefined, mobile: undefined, source: 1, status: 0, remark: undefined }
}

const formRules = reactive({
  userId: [{ required: true, message: '关联用户不能为空', trigger: 'change' }],
  name: [{ required: true, message: '司机姓名不能为空', trigger: 'blur' }],
  source: [{ required: true, message: '来源不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
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
.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}
</style>
