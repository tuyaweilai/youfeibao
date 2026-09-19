<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="场站是收货二维码的粒度：一码一场站，印在磅房 / 墙上。码内不带任何令牌，只编码场站码；自然人扫码先看公开信息，再手机号验证查看自己的待确认。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="场站名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="场站码" prop="stationCode">
        <el-input v-model="queryParams.stationCode" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="收货状态" prop="openStatus">
        <el-select v-model="queryParams.openStatus" placeholder="请选择" clearable class="!w-160px">
          <el-option label="在收货" :value="1" />
          <el-option label="暂停收货" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['icbc:station:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增场站
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="场站码" prop="stationCode" min-width="140" />
      <el-table-column label="场站名称" prop="name" min-width="140" />
      <el-table-column label="地址" prop="address" min-width="200" />
      <el-table-column label="联系电话" prop="contactMobile" min-width="130" />
      <el-table-column label="收货状态" align="center" prop="openStatus" width="110">
        <template #default="{ row }">
          <el-tag :type="row.openStatus === 1 ? 'success' : 'info'">
            {{ row.openStatus === 1 ? '在收货' : '暂停收货' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="二维码入口" prop="entryUrl" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showQrcode(row)">二维码</el-button>
          <el-button link type="primary" @click="openForm('update', row.id)" v-hasPermi="['icbc:station:manage']">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)" v-hasPermi="['icbc:station:manage']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-form-item label="场站码" prop="stationCode">
        <el-input v-model="formData.stationCode" placeholder="租户内唯一，建议英文 / 数字，如 STATION_A" />
      </el-form-item>
      <el-form-item label="场站名称" prop="name">
        <el-input v-model="formData.name" placeholder="如：城东收货点" />
      </el-form-item>
      <el-form-item label="场站地址" prop="address">
        <el-input v-model="formData.address" placeholder="如：某某路 1 号" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactMobile">
        <el-input v-model="formData.contactMobile" placeholder="公开，用于扫码后联系客服" />
      </el-form-item>
      <el-form-item label="收货状态" prop="openStatus">
        <el-switch v-model="formData.openStatus" :active-value="1" :inactive-value="0" active-text="在收货" inactive-text="暂停" />
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

  <Dialog title="场站二维码" v-model="qrcodeVisible" width="420px">
    <div class="qrcode">
      <Qrcode v-if="qrcodeUrl" :text="qrcodeUrl" :width="220" />
      <div class="qrcode__code">{{ currentStation?.stationCode }}</div>
      <el-input v-model="qrcodeUrl" readonly>
        <template #append>
          <el-button @click="copyUrl">复制</el-button>
        </template>
      </el-input>
      <div class="qrcode__tip">把二维码打印后贴在磅房 / 墙上；码内不含任何令牌，长期有效。</div>
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { StationApi, StationVO } from '@/api/icbc/station'
import { Qrcode } from '@/components/Qrcode'

defineOptions({ name: 'IcbcStation' })

const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const list = ref<StationVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  stationCode: undefined,
  openStatus: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await StationApi.getStationPage(queryParams)
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
const formData = ref<StationVO>(buildEmpty())

function buildEmpty(): StationVO {
  return { stationCode: undefined, name: undefined, address: undefined, contactMobile: undefined, openStatus: 1, remark: undefined }
}
const formRules = reactive({
  stationCode: [{ required: true, message: '场站码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '场站名称不能为空', trigger: 'blur' }]
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
      formData.value = await StationApi.getStation(id)
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
      await StationApi.createStation(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await StationApi.updateStation(formData.value)
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
    await StationApi.deleteStation(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

// ==================== 二维码 ====================
const qrcodeVisible = ref(false)
const qrcodeUrl = ref('')
const currentStation = ref<StationVO | null>(null)

const showQrcode = (row: StationVO) => {
  currentStation.value = row
  qrcodeUrl.value = row.entryUrl || `?station=${row.stationCode}`
  qrcodeVisible.value = true
}

const copyUrl = async () => {
  try {
    await navigator.clipboard.writeText(qrcodeUrl.value)
    message.success('已复制')
  } catch {
    message.warning('复制失败，请手动选择文本复制')
  }
}

onMounted(getList)
</script>

<style lang="scss" scoped>
.qrcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;

  &__code {
    font-size: 16px;
    font-weight: 600;
  }

  &__tip {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.6;
    text-align: center;
  }
}
</style>
