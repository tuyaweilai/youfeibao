<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="一个交易对方的一次物理交接记为一个交接批次。过磅保留每一次原始读数（磅次），只有被选定的那一次参与计量，其余留档不参与。"
    />
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="同一车同一天两次送货是两个批次（不做车牌 + 日期去重），磅单与收购单各归各。批次已产生收购单后，有效磅次不能再改（计量结果已引用当时那一版）。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="queryParams.plateNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="来源方式" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="请选择" clearable class="!w-160px">
          <el-option v-for="item in HANDOVER_SOURCE_TYPES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openCreate" v-hasPermi="['icbc:handover-batch:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 登记交接批次
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="批次号" prop="batchNo" min-width="180" />
      <el-table-column label="出售者" prop="sellerName" min-width="100" />
      <el-table-column label="场站 / 上门地址" min-width="180">
        <template #default="{ row }">
          <span>{{ row.stationName || row.visitAddress || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交接时间" prop="occurTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="来源方式" prop="sourceTypeName" min-width="110" />
      <el-table-column label="车牌" prop="plateNo" min-width="110" />
      <el-table-column label="司机" prop="driverName" min-width="100" />
      <el-table-column label="有效磅次" align="center" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.effectiveWeighingSeqNo" type="success">第 {{ row.effectiveWeighingSeqNo }} 次</el-tag>
          <el-tag v-else type="warning">未指定</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收购单" align="center" prop="acquisitionCount" width="90" />
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">磅次与有效磅次</el-button>
          <el-button link type="primary" @click="openUpdate(row)" v-hasPermi="['icbc:handover-batch:manage']">
            补录
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 登记批次 -->
  <Dialog v-model="createVisible" title="登记交接批次" width="620px">
    <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
      <el-form-item label="交易对方" prop="payeeId">
        <el-select
          v-model="createForm.payeeId"
          filterable
          remote
          :remote-method="searchPayee"
          placeholder="按姓名搜索出售者档案"
          class="!w-100%"
        >
          <el-option v-for="item in payeeOptions" :key="item.id" :label="`${item.name}（${item.mobile || item.idCardNo}）`" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="场站" prop="stationId">
        <el-select v-model="createForm.stationId" clearable placeholder="到场收货填场站" class="!w-100%">
          <el-option v-for="item in stationOptions" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="上门地址" prop="visitAddress">
        <el-input v-model="createForm.visitAddress" placeholder="上门回收填地址；与场站至少填一个" />
      </el-form-item>
      <el-form-item label="交接时间" prop="occurTime">
        <el-date-picker v-model="createForm.occurTime" type="datetime" value-format="x" placeholder="不填取登记时刻" class="!w-100%" />
      </el-form-item>
      <el-form-item label="来源方式" prop="sourceType">
        <el-select v-model="createForm.sourceType" class="!w-100%">
          <el-option v-for="item in HANDOVER_SOURCE_TYPES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="createForm.plateNo" placeholder="如 京A12345" />
      </el-form-item>
      <el-form-item label="司机姓名" prop="driverName">
        <el-input v-model="createForm.driverName" placeholder="选填" />
      </el-form-item>
      <el-form-item label="司机手机号" prop="driverMobile">
        <el-input v-model="createForm.driverMobile" placeholder="选填" />
      </el-form-item>
      <el-form-item label="预约 / 采购订单" prop="appointmentId">
        <el-input v-model="createForm.appointmentId" placeholder="都可空：临时上门的散户不被流程挡住" disabled />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitCreate" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="createVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 补录 -->
  <Dialog v-model="updateVisible" title="补录交接批次" width="560px">
    <el-form ref="updateFormRef" :model="updateForm" label-width="120px">
      <el-form-item label="批次号">
        <el-input v-model="updateForm.batchNo" disabled />
      </el-form-item>
      <el-form-item label="场站">
        <el-select v-model="updateForm.stationId" clearable placeholder="到场收货填场站" class="!w-100%">
          <el-option v-for="item in stationOptions" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="上门地址">
        <el-input v-model="updateForm.visitAddress" placeholder="与场站至少填一个" />
      </el-form-item>
      <el-form-item label="交接时间">
        <el-date-picker v-model="updateForm.occurTime" type="datetime" value-format="x" class="!w-100%" />
      </el-form-item>
      <el-form-item label="来源方式">
        <el-select v-model="updateForm.sourceType" class="!w-100%">
          <el-option v-for="item in HANDOVER_SOURCE_TYPES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="车牌号">
        <el-input v-model="updateForm.plateNo" />
      </el-form-item>
      <el-form-item label="司机姓名">
        <el-input v-model="updateForm.driverName" />
      </el-form-item>
      <el-form-item label="司机手机号">
        <el-input v-model="updateForm.driverMobile" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="updateForm.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitUpdate" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="updateVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 磅次与有效磅次 -->
  <Dialog v-model="detailVisible" :title="`磅次与有效磅次 · ${detail.batchNo || ''}`" width="900px">
    <el-alert
      v-if="detail.weighingChangeLocked"
      type="warning"
      :closable="false"
      class="mb-10px"
      title="该批次已产生收购单，计量结果已引用当时那一版磅次，不能再改有效磅次；要改请作废收购单或另建批次。"
    />
    <el-descriptions :column="3" border class="mb-10px">
      <el-descriptions-item label="出售者">{{ detail.sellerName || '—' }}</el-descriptions-item>
      <el-descriptions-item label="车牌">{{ detail.plateNo || '—' }}</el-descriptions-item>
      <el-descriptions-item label="来源方式">{{ detail.sourceTypeName || '—' }}</el-descriptions-item>
    </el-descriptions>

    <el-table :data="detail.weighingList || []" :stripe="true">
      <el-table-column label="第几次" align="center" prop="seqNo" width="80" />
      <el-table-column label="毛重" prop="grossWeight" width="110" />
      <el-table-column label="皮重" prop="tareWeight" width="110" />
      <el-table-column label="净重" prop="netWeight" width="110" />
      <el-table-column label="磅单号" prop="weightTicketNo" min-width="130" />
      <el-table-column label="磅单车牌" prop="plateNo" width="110" />
      <el-table-column label="过磅时间" prop="weighTime" :formatter="dateFormatter" width="170" />
      <el-table-column label="计量" align="center" width="110">
        <template #default="{ row }">
          <el-tag :type="row.effective ? 'success' : 'info'">{{ row.effectiveText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="row.effective || detail.weighingChangeLocked"
            @click="selectEffective(row)"
            v-hasPermi="['icbc:handover-batch:manage']"
          >
            指定有效
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-form :model="weighingForm" inline class="mt-15px" label-width="70px">
      <el-form-item label="毛重">
        <el-input v-model="weighingForm.grossWeight" class="!w-130px" placeholder="0" />
      </el-form-item>
      <el-form-item label="皮重">
        <el-input v-model="weighingForm.tareWeight" class="!w-130px" placeholder="0" />
      </el-form-item>
      <el-form-item label="磅单号">
        <el-input v-model="weighingForm.weightTicketNo" class="!w-160px" placeholder="选填" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="weighingForm.remark" class="!w-180px" placeholder="如复磅原因" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="formLoading" @click="submitWeighing" v-hasPermi="['icbc:handover-batch:manage']">
          新增一次磅次
        </el-button>
      </el-form-item>
    </el-form>
    <div class="text-12px text-gray-500">
      第一次磅次自动成为有效磅次；复磅后再点「指定有效」把参与计量的那一次指过去，其余留档不参与。
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { HandoverBatchApi, HandoverBatchVO, HandoverWeighingVO, HANDOVER_SOURCE_TYPES } from '@/api/icbc/handoverBatch'
import { PayeeApi, PayeeVO } from '@/api/icbc/payee'
import { StationApi, StationVO } from '@/api/icbc/station'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'IcbcHandoverBatch' })

const message = useMessage()

const loading = ref(true)
const formLoading = ref(false)
const list = ref<HandoverBatchVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  plateNo: undefined,
  sourceType: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HandoverBatchApi.getHandoverBatchPage(queryParams)
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

// ==================== 出售者 / 场站选项 ====================
const payeeOptions = ref<PayeeVO[]>([])
const stationOptions = ref<StationVO[]>([])

const searchPayee = async (keyword: string) => {
  const data = await PayeeApi.getPayeePage({ pageNo: 1, pageSize: 20, name: keyword })
  payeeOptions.value = data.list
}

const loadStations = async () => {
  const data = await StationApi.getStationPage({ pageNo: 1, pageSize: 100 })
  stationOptions.value = data.list
}

// ==================== 登记 ====================
const createVisible = ref(false)
const createFormRef = ref()
const createForm = reactive<HandoverBatchVO>({
  payeeId: undefined,
  stationId: undefined,
  visitAddress: '',
  occurTime: undefined,
  sourceType: 'WALK_IN',
  plateNo: '',
  driverName: '',
  driverMobile: '',
  remark: ''
})
const createRules = {
  payeeId: [{ required: true, message: '交易对方不能为空', trigger: 'change' }],
  plateNo: [{ required: true, message: '车牌号不能为空', trigger: 'blur' }],
  sourceType: [{ required: true, message: '来源方式不能为空', trigger: 'change' }]
}

const openCreate = async () => {
  await loadStations()
  createVisible.value = true
}

const submitCreate = async () => {
  await createFormRef.value.validate()
  if (!createForm.stationId && !createForm.visitAddress) {
    message.error('场站与上门地址至少填一个')
    return
  }
  formLoading.value = true
  try {
    await HandoverBatchApi.createHandoverBatch(createForm)
    message.success('已登记交接批次')
    createVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

// ==================== 补录 ====================
const updateVisible = ref(false)
const updateFormRef = ref()
const updateForm = reactive<HandoverBatchVO>({})

const openUpdate = async (row: HandoverBatchVO) => {
  await loadStations()
  Object.assign(updateForm, { ...row })
  updateVisible.value = true
}

const submitUpdate = async () => {
  formLoading.value = true
  try {
    await HandoverBatchApi.updateHandoverBatch(updateForm)
    message.success('已补录')
    updateVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

// ==================== 磅次与有效磅次 ====================
const detailVisible = ref(false)
const detail = ref<HandoverBatchVO>({})
const weighingForm = reactive<HandoverWeighingVO>({
  grossWeight: undefined,
  tareWeight: undefined,
  weightTicketNo: '',
  remark: ''
})

const openDetail = async (id?: number) => {
  detail.value = await HandoverBatchApi.getHandoverBatch(id!)
  detailVisible.value = true
}

const submitWeighing = async () => {
  formLoading.value = true
  try {
    await HandoverBatchApi.addWeighing({ ...weighingForm, batchId: detail.value.id })
    message.success('已新增磅次')
    Object.assign(weighingForm, { grossWeight: undefined, tareWeight: undefined, weightTicketNo: '', remark: '' })
    await openDetail(detail.value.id)
    await getList()
  } finally {
    formLoading.value = false
  }
}

const selectEffective = async (row: HandoverWeighingVO) => {
  try {
    const { value } = await message.prompt(`指定第 ${row.seqNo} 次为有效磅次（留档的那些不参与计量）`, '指定原因（选填）')
    await HandoverBatchApi.selectEffectiveWeighing({
      batchId: detail.value.id!,
      weighingId: row.id!,
      reason: value || undefined
    })
    message.success('已指定有效磅次')
    await openDetail(detail.value.id)
    await getList()
  } catch {}
}

onMounted(getList)
</script>
