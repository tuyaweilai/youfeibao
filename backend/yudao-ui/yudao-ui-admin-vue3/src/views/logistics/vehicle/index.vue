<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="车辆是派车的可选对象，车牌租户内唯一。「运输中」由运输任务驱动，不能在这里手工选——否则派车会看到一辆在跑却被标成可用的车。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="queryParams.plateNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="车辆类型" prop="vehicleType">
        <el-input v-model="queryParams.vehicleType" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-160px">
          <el-option label="可用" :value="0" />
          <el-option label="运输中" :value="1" />
          <el-option label="维护中" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['logistics:vehicle:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增车辆
        </el-button>
        <el-button type="success" plain @click="handleExport" :loading="exportLoading" v-hasPermi="['logistics:vehicle:export']">
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="车牌号" prop="plateNo" min-width="120" />
      <el-table-column label="车辆类型" prop="vehicleType" min-width="120" />
      <el-table-column label="载重（吨）" align="right" prop="capacityTon" min-width="110" />
      <el-table-column label="行驶证到期" align="center" prop="drivingLicenseExpiryDate" width="130">
        <template #default="{ row }">
          <span :class="{ 'expiry--warn': isExpired(row.drivingLicenseExpiryDate) }">
            {{ row.drivingLicenseExpiryDate || '未登记' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="保险到期" align="center" prop="insuranceExpiryDate" width="130">
        <template #default="{ row }">
          <span :class="{ 'expiry--warn': isExpired(row.insuranceExpiryDate) }">
            {{ row.insuranceExpiryDate || '未登记' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] || 'info'">{{ STATUS_NAME[row.status] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm('update', row.id)" v-hasPermi="['logistics:vehicle:update']">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)" v-hasPermi="['logistics:vehicle:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px" v-loading="formLoading">
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="formData.plateNo" placeholder="如：浙A12345（租户内唯一）" />
      </el-form-item>
      <el-form-item label="车辆类型" prop="vehicleType">
        <el-input v-model="formData.vehicleType" placeholder="如：厢式货车 / 平板 / 自卸" />
      </el-form-item>
      <el-form-item label="载重（吨）" prop="capacityTon">
        <el-input-number v-model="formData.capacityTon" :precision="3" :min="0" :controls="false" class="!w-200px" />
      </el-form-item>
      <el-form-item label="行驶证到期" prop="drivingLicenseExpiryDate">
        <el-date-picker
          v-model="formData.drivingLicenseExpiryDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选填；过期将不能派车"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="保险到期" prop="insuranceExpiryDate">
        <el-date-picker
          v-model="formData.insuranceExpiryDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选填；过期将不能派车"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="车辆照片" prop="photos">
        <UploadFile v-model="vehiclePhotos" :limit="5" />
      </el-form-item>
      <el-form-item label="GPS 设备号" prop="gpsDeviceId">
        <el-input v-model="formData.gpsDeviceId" placeholder="选填；一期只登记，轨迹接入另行立项" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="formData.status" class="!w-200px">
          <el-option label="可用" :value="0" />
          <el-option label="维护中" :value="2" />
        </el-select>
        <div class="tip">「运输中」由运输任务驱动，不在这里选。</div>
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
import { LogisticsVehicleApi, LogisticsVehicleVO } from '@/api/logistics/vehicle'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'

defineOptions({ name: 'LogisticsVehicle' })

const STATUS_NAME: Record<number, string> = { 0: '可用', 1: '运输中', 2: '维护中' }
const STATUS_TAG: Record<number, 'success' | 'warning' | 'info'> = { 0: 'success', 1: 'warning', 2: 'info' }

const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const exportLoading = ref(false)
const list = ref<LogisticsVehicleVO[]>([])

/** 到期日早于今天即过期——与后端门禁同一口径（当天到期仍有效） */
function isExpired(date?: string) {
  if (!date) return false
  return new Date(date).getTime() < new Date(new Date().toDateString()).getTime()
}
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  plateNo: undefined,
  vehicleType: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await LogisticsVehicleApi.getVehiclePage(queryParams)
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
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await LogisticsVehicleApi.exportVehicle(queryParams)
    download.excel(data, '车辆.xls')
  } finally {
    exportLoading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<LogisticsVehicleVO>(buildEmpty())
/** 上传组件单独绑一个 ref（仓库既有做法），提交时再落到表单上 */
const vehiclePhotos = ref<string[]>([])

function buildEmpty(): LogisticsVehicleVO {
  return {
    plateNo: undefined,
    vehicleType: undefined,
    capacityTon: undefined,
    drivingLicenseExpiryDate: undefined,
    insuranceExpiryDate: undefined,
    photos: [],
    gpsDeviceId: undefined,
    status: 0,
    remark: undefined
  }
}

const formRules = reactive({
  plateNo: [{ required: true, message: '车牌号不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  dialogTitle.value = t('action.' + type)
  formData.value = buildEmpty()
  vehiclePhotos.value = []
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await LogisticsVehicleApi.getVehicle(id)
      vehiclePhotos.value = formData.value.photos || []
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formData.value.photos = vehiclePhotos.value
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await LogisticsVehicleApi.createVehicle(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await LogisticsVehicleApi.updateVehicle(formData.value)
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
    await LogisticsVehicleApi.deleteVehicle(id)
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
