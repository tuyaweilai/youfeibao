<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="承运合同是回收企业与承运商之间关于运价与计费方式的约定（有效期、适用线路或品类、按车/按吨/按公里、附加费与其承担方），它是运费对账的依据。与「采购合同」是两份契约：采购合同是买货，承运合同是买运输服务。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="合同编号" prop="contractNo">
        <el-input v-model="queryParams.contractNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="承运商" prop="carrierId">
        <el-select v-model="queryParams.carrierId" placeholder="请选择" clearable filterable class="!w-200px">
          <el-option v-for="c in carrierList" :key="c.id" :label="c.name" :value="c.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-140px">
          <el-option label="生效" :value="0" />
          <el-option label="已停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['logistics:carrier-contract:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增承运合同
        </el-button>
        <el-button type="success" plain @click="handleExport" :loading="exportLoading" v-hasPermi="['logistics:carrier-contract:export']">
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="合同编号" prop="contractNo" min-width="180" />
      <el-table-column label="承运商" prop="carrierName" min-width="150" />
      <el-table-column label="有效期" min-width="200">
        <template #default="{ row }">
          {{ row.effectiveFrom }} ~ {{ row.effectiveTo || '长期' }}
        </template>
      </el-table-column>
      <el-table-column label="适用线路" prop="route" min-width="160" show-overflow-tooltip />
      <el-table-column label="适用品类" prop="categoryName" min-width="120" />
      <el-table-column label="计费方式" align="center" prop="billingModeName" width="100" />
      <el-table-column label="运价" align="right" width="120">
        <template #default="{ row }">{{ formatAmount(row.unitPrice) }}</template>
      </el-table-column>
      <el-table-column label="附加费" min-width="200">
        <template #default="{ row }">
          <span v-if="!row.surcharges || row.surcharges.length === 0">—</span>
          <el-tag
            v-for="(s, i) in row.surcharges"
            :key="i"
            class="mr-5px"
            :type="s.bearer === 2 ? 'warning' : 'info'"
          >{{ s.name }} {{ formatAmount(s.amount) }}（{{ BEARER_NAME[s.bearer] }}）</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ STATUS_NAME[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm('update', row.id)" v-hasPermi="['logistics:carrier-contract:update']">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)" v-hasPermi="['logistics:carrier-contract:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog :title="dialogTitle" v-model="dialogVisible" width="680px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <el-form-item label="承运商" prop="carrierId">
        <el-select v-model="formData.carrierId" placeholder="请选择承运商" filterable class="w-full">
          <el-option v-for="c in carrierList" :key="c.id" :label="c.name" :value="c.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="合同编号" prop="contractNo">
        <el-input v-model="formData.contractNo" placeholder="留空则由平台生成" />
      </el-form-item>
      <el-form-item label="有效期" prop="effectiveFrom">
        <el-date-picker v-model="formData.effectiveFrom" type="date" value-format="YYYY-MM-DD" placeholder="生效日期" class="!w-200px" />
        <span class="mx-10px">至</span>
        <el-date-picker v-model="formData.effectiveTo" type="date" value-format="YYYY-MM-DD" placeholder="失效日期（留空为长期）" class="!w-200px" />
      </el-form-item>
      <el-form-item label="适用线路" prop="route">
        <el-input v-model="formData.route" placeholder="如：城东场站—临平（与适用品类至少填一个）" />
      </el-form-item>
      <el-form-item label="适用品类" prop="goodsConfigId">
        <el-select v-model="formData.goodsConfigId" placeholder="不限定品类可不选" clearable filterable class="w-full" @change="onGoodsChange">
          <el-option v-for="g in goodsList" :key="g.id" :label="g.name" :value="g.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="计费方式" prop="billingMode">
        <el-radio-group v-model="formData.billingMode">
          <el-radio :value="1">按车</el-radio>
          <el-radio :value="2">按吨</el-radio>
          <el-radio :value="3">按公里</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="运价" prop="unitPrice">
        <el-input-number v-model="formData.unitPrice" :min="0" :precision="2" :step="10" class="!w-200px" />
        <span class="ml-10px text-[var(--el-text-color-secondary)]">按计费方式的单价（元）</span>
      </el-form-item>
      <el-form-item label="附加费">
        <div class="w-full">
          <div v-for="(s, index) in formData.surcharges" :key="index" class="flex items-center mb-8px">
            <el-input v-model="s.name" placeholder="名称" class="!w-180px" />
            <el-input-number v-model="s.amount" :min="0" :precision="2" class="ml-8px !w-140px" />
            <el-select v-model="s.bearer" placeholder="承担方" class="ml-8px !w-160px">
              <el-option label="本企业承担" :value="2" />
              <el-option label="承运商承担" :value="1" />
            </el-select>
            <el-button link type="danger" class="ml-8px" @click="removeSurcharge(index)">删除</el-button>
          </div>
          <el-button link type="primary" @click="addSurcharge"><Icon icon="ep:plus" /> 添加附加费</el-button>
        </div>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="0">生效</el-radio>
          <el-radio :value="1">已停用</el-radio>
        </el-radio-group>
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
import { LogisticsCarrierApi, LogisticsCarrierVO } from '@/api/logistics/carrier'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'
import {
  LogisticsCarrierContractApi,
  LogisticsCarrierContractVO,
  LogisticsCarrierContractSurchargeVO
} from '@/api/logistics/carrierContract'
import download from '@/utils/download'

defineOptions({ name: 'LogisticsCarrierContract' })

const STATUS_NAME: Record<number, string> = { 0: '生效', 1: '已停用' }
const BEARER_NAME: Record<number, string> = { 1: '承运商承担', 2: '本企业承担' }

const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const exportLoading = ref(false)
const list = ref<LogisticsCarrierContractVO[]>([])
const total = ref(0)
const carrierList = ref<LogisticsCarrierVO[]>([])
const goodsList = ref<GoodsConfigVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  contractNo: undefined,
  carrierId: undefined,
  status: undefined
})
const queryFormRef = ref()

const formatAmount = (value?: number) => (value === null || value === undefined ? '—' : Number(value).toFixed(2))

const getList = async () => {
  loading.value = true
  try {
    const data = await LogisticsCarrierContractApi.getCarrierContractPage(queryParams)
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
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await LogisticsCarrierContractApi.exportCarrierContract(queryParams)
    download.excel(data, '承运合同.xls')
  } finally {
    exportLoading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<LogisticsCarrierContractVO>(buildEmpty())

function buildEmpty(): LogisticsCarrierContractVO {
  return {
    carrierId: undefined,
    contractNo: undefined,
    effectiveFrom: undefined,
    effectiveTo: undefined,
    route: undefined,
    goodsConfigId: undefined,
    categoryName: undefined,
    billingMode: 1,
    unitPrice: undefined,
    surcharges: [],
    status: 0,
    remark: undefined
  }
}

const formRules = reactive({
  carrierId: [{ required: true, message: '承运商不能为空', trigger: 'change' }],
  effectiveFrom: [{ required: true, message: '生效日期不能为空', trigger: 'change' }],
  billingMode: [{ required: true, message: '计费方式不能为空', trigger: 'change' }],
  unitPrice: [{ required: true, message: '运价不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const addSurcharge = () => {
  formData.value.surcharges = [...(formData.value.surcharges || []), { name: '', amount: 0, bearer: 2 } as LogisticsCarrierContractSurchargeVO]
}
const removeSurcharge = (index: number) => {
  formData.value.surcharges = (formData.value.surcharges || []).filter((_, i) => i !== index)
}
const onGoodsChange = (id?: number) => {
  const goods = goodsList.value.find((g) => g.id === id)
  formData.value.categoryName = goods?.name
}

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  dialogTitle.value = t('action.' + type)
  formData.value = buildEmpty()
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      const data = await LogisticsCarrierContractApi.getCarrierContract(id)
      formData.value = { ...data, surcharges: data.surcharges || [] }
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  // 附加费每项都要名称 + 金额 + 承担方
  const surcharges = (formData.value.surcharges || []).filter((s) => s.name || s.amount || s.bearer)
  for (const s of surcharges) {
    if (!s.name) {
      message.error('附加费名称不能为空')
      return
    }
  }
  formData.value.surcharges = surcharges
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await LogisticsCarrierContractApi.createCarrierContract(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await LogisticsCarrierContractApi.updateCarrierContract(formData.value)
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
    await LogisticsCarrierContractApi.deleteCarrierContract(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const init = async () => {
  const [carriers, goods] = await Promise.all([
    LogisticsCarrierApi.getCarrierPage({ pageNo: 1, pageSize: 100 }),
    GoodsConfigApi.getEnabledList()
  ])
  carrierList.value = carriers.list
  goodsList.value = goods || []
  await getList()
}
init()
</script>
