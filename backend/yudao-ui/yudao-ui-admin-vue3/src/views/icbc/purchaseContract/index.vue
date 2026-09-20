<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="采购合同是采购条款（一个合同 → 多个采购订单 → 多次收货），审核通过前不得作为采购依据。它与自然人出售者的「框架收购协议」是两件事：后者是开票前置，前者是采购条款。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="合同编号" prop="contractNo">
        <el-input v-model="queryParams.contractNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="合同名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="交易对方" prop="counterpartyName">
        <el-input v-model="queryParams.counterpartyName" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="主体类型" prop="counterpartyType">
        <el-select v-model="queryParams.counterpartyType" placeholder="请选择" clearable class="!w-160px">
          <el-option v-for="item in CONTRACT_COUNTERPARTY_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-140px">
          <el-option v-for="item in PURCHASE_CONTRACT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['icbc:purchase-contract:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增合同
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="合同编号" prop="contractNo" min-width="200" />
      <el-table-column label="合同名称" prop="name" min-width="200" show-overflow-tooltip />
      <el-table-column label="交易对方" prop="counterpartyName" min-width="140" />
      <el-table-column label="主体类型" prop="counterpartyTypeName" width="130" />
      <el-table-column label="有效期" min-width="200">
        <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column label="适用品类" min-width="160">
        <template #default="{ row }">
          <span v-if="row.categories && row.categories.length">
            {{ row.categories.map((c: any) => c.categoryName).join('、') }}
          </span>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row)">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" prop="versionNo" width="70" align="center" />
      <el-table-column label="操作" align="center" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">明细</el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="primary"
            @click="openForm('update', row.id)"
            v-hasPermi="['icbc:purchase-contract:manage']"
          >
            编辑
          </el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="warning"
            @click="openSubmit(row)"
            v-hasPermi="['icbc:purchase-contract:manage']"
          >
            送审
          </el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="success"
            @click="openAudit(row)"
            v-hasPermi="['icbc:purchase-contract:audit']"
          >
            审核
          </el-button>
          <el-button
            v-if="row.status === 2"
            link
            type="danger"
            @click="openClose(row)"
            v-hasPermi="['icbc:purchase-contract:manage']"
          >
            关闭
          </el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['icbc:purchase-contract:manage']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 新增 / 编辑 -->
  <el-dialog v-model="formVisible" :title="formTitle" width="820px">
    <el-alert
      v-if="formMode === 'update' && current?.status === 2"
      type="warning"
      :closable="false"
      class="mb-10px"
      title="这是已生效合同：改动会新增一版快照并回到「待审核」，重新审核通过前整份合同不再是有效采购依据。"
    />
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="130px" v-loading="formLoading">
      <el-form-item label="合同名称" prop="name">
        <el-input v-model="formData.name" placeholder="如：2026 年度废钢采购合同" />
      </el-form-item>
      <el-form-item label="交易对方类型" prop="counterpartyType">
        <el-select v-model="formData.counterpartyType" class="!w-220px" @change="handleCounterpartyTypeChange">
          <el-option v-for="item in CONTRACT_COUNTERPARTY_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="formData.counterpartyType === CONTRACT_NATURAL_COUNTERPARTY_TYPE" label="自然人出售者" prop="payeeId">
        <el-select v-model="formData.payeeId" filterable placeholder="从出售者档案中选择" class="!w-360px">
          <el-option
            v-for="item in payeeOptions"
            :key="item.id"
            :label="item.name + (item.mobile ? '（' + item.mobile + '）' : '')"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-else label="单位供货方" prop="supplierId">
        <el-select v-model="formData.supplierId" filterable placeholder="从单位供货方中选择" class="!w-360px" @change="handleSupplierChange">
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="有效期" prop="startDate">
        <el-date-picker
          v-model="formData.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
          class="!w-200px"
        />
        <span class="mx-8px">至</span>
        <el-date-picker
          v-model="formData.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="适用品类" prop="categoryIds">
        <el-select v-model="formData.categoryIds" multiple filterable placeholder="至少选择一个品类" class="!w-100%">
          <el-option v-for="item in goodsOptions" :key="item.id" :label="item.name + '（' + (item.unit || '-') + '）'" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="数量约定" prop="quantityAgreement">
        <el-input v-model="formData.quantityAgreement" type="textarea" :rows="2" placeholder="如：全年不少于 1000 吨" />
      </el-form-item>
      <el-form-item label="计量标准" prop="measureStandard">
        <el-input v-model="formData.measureStandard" type="textarea" :rows="2" placeholder="如：以场站地磅毛重减皮重再减扣杂为准" />
      </el-form-item>
      <el-form-item label="质量标准" prop="qualityStandard">
        <el-input v-model="formData.qualityStandard" type="textarea" :rows="2" placeholder="如：废钢二级，杂质不超过 1%" />
      </el-form-item>
      <el-form-item label="价格规则" prop="priceRule">
        <el-input v-model="formData.priceRule" type="textarea" :rows="2" placeholder="如：按交货日挂牌价减 20 元/吨" />
      </el-form-item>
      <el-form-item label="运输责任" prop="transportResponsibility">
        <el-input v-model="formData.transportResponsibility" type="textarea" :rows="2" placeholder="如：供方送货到场站，运费自担" />
      </el-form-item>
      <el-form-item label="付款条款" prop="paymentTerms">
        <el-input v-model="formData.paymentTerms" type="textarea" :rows="2" placeholder="如：结算确认后 3 个工作日内付款" />
      </el-form-item>
      <el-form-item label="附件" prop="attachmentUrls">
        <UploadFile v-model="attachmentUrlsModel" :limit="5" />
      </el-form-item>
      <el-form-item v-if="formMode === 'update' && current?.status === 2" label="变更原因" prop="changeReason">
        <el-input v-model="formData.changeReason" type="textarea" :rows="2" placeholder="已生效合同的修改必填：说清改了哪一项" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">确 定</el-button>
    </template>
  </el-dialog>

  <!-- 送审 -->
  <el-dialog v-model="submitVisible" title="送审采购合同" width="480px">
    <el-alert type="info" :closable="false" class="mb-10px" title="送审落一版快照；审核通过前不得作为采购依据。" />
    <el-form label-width="100px">
      <el-form-item label="送审说明">
        <el-input v-model="submitDialog.changeReason" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitVisible = false">取 消</el-button>
      <el-button type="primary" @click="handleSubmit">送 审</el-button>
    </template>
  </el-dialog>

  <!-- 审核 -->
  <el-dialog v-model="auditVisible" title="审核采购合同" width="520px">
    <el-alert type="info" :closable="false" class="mb-10px" title="通过后合同生效，可作为采购依据；驳回退回草稿并必须写明审核意见。" />
    <el-radio-group v-model="auditForm.approved" class="mb-10px">
      <el-radio :label="true">通过并生效</el-radio>
      <el-radio :label="false">驳回退回草稿</el-radio>
    </el-radio-group>
    <el-input v-model="auditForm.remark" type="textarea" :rows="3" placeholder="审核意见（驳回必填）" />
    <template #footer>
      <el-button @click="auditVisible = false">取 消</el-button>
      <el-button type="primary" @click="handleAudit">提 交</el-button>
    </template>
  </el-dialog>

  <!-- 关闭 -->
  <el-dialog v-model="closeVisible" title="关闭采购合同" width="480px">
    <el-alert type="warning" :closable="false" class="mb-10px" title="关闭后不再作为采购依据；历史业务与版本仍可回查。" />
    <el-input v-model="closeForm.reason" type="textarea" :rows="2" placeholder="关闭原因（选填）" />
    <template #footer>
      <el-button @click="closeVisible = false">取 消</el-button>
      <el-button type="primary" @click="handleClose">关 闭</el-button>
    </template>
  </el-dialog>

  <!-- 明细 -->
  <el-dialog v-model="detailVisible" title="采购合同明细" width="860px">
    <template v-if="detail">
      <el-descriptions :column="3" border class="mb-10px">
        <el-descriptions-item label="合同编号">{{ detail.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="合同名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail)">{{ detail.statusName }}</el-tag>
          <el-tag v-if="detail.usableAsPurchaseBasis" type="success" class="ml-5px">可作为采购依据</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="交易对方">{{ detail.counterpartyName }}</el-descriptions-item>
        <el-descriptions-item label="主体类型">{{ detail.counterpartyTypeName }}</el-descriptions-item>
        <el-descriptions-item label="有效期">{{ detail.startDate }} ~ {{ detail.endDate }}</el-descriptions-item>
        <el-descriptions-item label="适用品类" :span="3">
          {{ (detail.categories || []).map((c) => c.categoryName).join('、') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="数量约定" :span="3">{{ detail.quantityAgreement || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计量标准" :span="3">{{ detail.measureStandard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="质量标准" :span="3">{{ detail.qualityStandard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="价格规则" :span="3">{{ detail.priceRule || '-' }}</el-descriptions-item>
        <el-descriptions-item label="运输责任" :span="3">{{ detail.transportResponsibility || '-' }}</el-descriptions-item>
        <el-descriptions-item label="付款条款" :span="3">{{ detail.paymentTerms || '-' }}</el-descriptions-item>
        <el-descriptions-item label="附件" :span="3">
          <template v-if="detail.attachmentUrls">
            <el-link
              v-for="(url, index) in detail.attachmentUrls.split(',')"
              :key="url"
              :href="url"
              target="_blank"
              type="primary"
              class="mr-10px"
            >
              附件{{ index + 1 }}
            </el-link>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="3">{{ detail.auditRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.closeReason" label="关闭原因" :span="3">{{ detail.closeReason }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">版本留痕（只追加，历史可回查）</el-divider>
      <el-table :data="detail.versions" size="small">
        <el-table-column label="版本" prop="versionNo" width="70" align="center" />
        <el-table-column label="变更原因" prop="changeReason" min-width="180" />
        <el-table-column label="审核结论" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.auditStatus === 1 ? 'success' : row.auditStatus === 2 ? 'danger' : 'info'">
              {{ row.auditStatusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" prop="auditRemark" min-width="160" show-overflow-tooltip />
        <el-table-column label="快照哈希" prop="snapshotHash" min-width="200" show-overflow-tooltip />
        <el-table-column label="送审时间" prop="createTime" width="170" :formatter="dateFormatter" />
      </el-table>
    </template>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  PurchaseContractApi,
  PurchaseContractVO,
  PURCHASE_CONTRACT_STATUS_OPTIONS,
  CONTRACT_COUNTERPARTY_TYPE_OPTIONS,
  CONTRACT_NATURAL_COUNTERPARTY_TYPE
} from '@/api/icbc/purchaseContract'
import { PayeeApi } from '@/api/icbc/payee'
import { GoodsConfigApi } from '@/api/icbc/goodsConfig'
import { SupplierApi } from '@/api/erp/purchase/supplier'
import { dateFormatter } from '@/utils/formatTime'
import UploadFile from '@/components/UploadFile/src/UploadFile.vue'

defineOptions({ name: 'IcbcPurchaseContract' })

const message = useMessage()

const loading = ref(true)
const list = ref<PurchaseContractVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  contractNo: undefined,
  name: undefined,
  counterpartyName: undefined,
  counterpartyType: undefined,
  status: undefined
})
const queryFormRef = ref()

const payeeOptions = ref<any[]>([])
const supplierOptions = ref<any[]>([])
const goodsOptions = ref<any[]>([])

const getList = async () => {
  loading.value = true
  try {
    const data = await PurchaseContractApi.getPurchaseContractPage(queryParams)
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

const statusTagType = (row: PurchaseContractVO) => {
  if (row.status === 2) return row.expired ? 'warning' : 'success'
  if (row.status === 1) return 'warning'
  if (row.status === 3) return 'info'
  return 'info'
}

// 交易对方与品类下拉：只在需要时加载一次
const loadOptions = async () => {
  if (!payeeOptions.value.length) {
    const page = await PayeeApi.getPayeePage({ pageNo: 1, pageSize: 100 })
    payeeOptions.value = page.list || []
  }
  if (!supplierOptions.value.length) {
    supplierOptions.value = (await SupplierApi.getSupplierSimpleList()) || []
  }
  if (!goodsOptions.value.length) {
    goodsOptions.value = (await GoodsConfigApi.getEnabledList()) || []
  }
}

// ==================== 新增 / 编辑 ====================
const formVisible = ref(false)
const formLoading = ref(false)
const formMode = ref<'create' | 'update'>('create')
const formTitle = computed(() => (formMode.value === 'create' ? '新增采购合同' : '编辑采购合同'))
const formRef = ref()
const current = ref<PurchaseContractVO>()
const formData = reactive<PurchaseContractVO & { changeReason?: string }>({})

// UploadFile 的 v-model 是「逗号分隔的 URL 字符串」，和合同上的 attachmentUrls 一致
const attachmentUrlsModel = computed({
  get: () => formData.attachmentUrls || '',
  set: (value: string) => {
    formData.attachmentUrls = value || undefined
  }
})

const formRules = reactive({
  name: [{ required: true, message: '合同名称不能为空', trigger: 'blur' }],
  counterpartyType: [{ required: true, message: '请选择交易对方类型', trigger: 'change' }],
  payeeId: [{ required: true, message: '请选择自然人出售者', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择单位供货方', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择有效期起', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择有效期止', trigger: 'change' }],
  categoryIds: [{ required: true, message: '至少选择一个适用品类', trigger: 'change' }]
})

const resetFormData = () => {
  Object.assign(formData, {
    id: undefined,
    name: undefined,
    counterpartyType: CONTRACT_NATURAL_COUNTERPARTY_TYPE,
    payeeId: undefined,
    supplierId: undefined,
    counterpartyName: undefined,
    startDate: undefined,
    endDate: undefined,
    quantityAgreement: undefined,
    measureStandard: undefined,
    qualityStandard: undefined,
    priceRule: undefined,
    transportResponsibility: undefined,
    paymentTerms: undefined,
    attachmentUrls: undefined,
    categoryIds: [],
    changeReason: undefined,
    remark: undefined
  })
}

const openForm = async (mode: 'create' | 'update', id?: number) => {
  formMode.value = mode
  resetFormData()
  current.value = undefined
  await loadOptions()
  if (mode === 'update' && id) {
    const detail = await PurchaseContractApi.getPurchaseContract(id)
    current.value = detail
    Object.assign(formData, {
      id: detail.id,
      name: detail.name,
      counterpartyType: detail.counterpartyType,
      payeeId: detail.payeeId,
      supplierId: detail.supplierId,
      counterpartyName: detail.counterpartyName,
      startDate: detail.startDate,
      endDate: detail.endDate,
      quantityAgreement: detail.quantityAgreement,
      measureStandard: detail.measureStandard,
      qualityStandard: detail.qualityStandard,
      priceRule: detail.priceRule,
      transportResponsibility: detail.transportResponsibility,
      paymentTerms: detail.paymentTerms,
      attachmentUrls: detail.attachmentUrls,
      categoryIds: (detail.categories || []).map((c) => c.goodsConfigId),
      remark: detail.remark
    })
  }
  formVisible.value = true
}

const handleCounterpartyTypeChange = () => {
  // 主体类型决定另一个 id 必须为空：切换时清掉，避免两个对手方同时挂着
  if (formData.counterpartyType === CONTRACT_NATURAL_COUNTERPARTY_TYPE) {
    formData.supplierId = undefined
    formData.counterpartyName = undefined
  } else {
    formData.payeeId = undefined
    formData.counterpartyName = undefined
  }
}

const handleSupplierChange = (id: number) => {
  const supplier = supplierOptions.value.find((item) => item.id === id)
  formData.counterpartyName = supplier?.name
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formMode.value === 'create') {
      await PurchaseContractApi.createPurchaseContract(formData)
      message.success('已创建草稿，送审并审核通过后才可作为采购依据')
    } else {
      await PurchaseContractApi.updatePurchaseContract(formData)
      message.success(current.value?.status === 2 ? '已提交新版，回到待审核' : '已保存')
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

// ==================== 送审 ====================
const submitVisible = ref(false)
const submitDialog = reactive<{ id?: number; changeReason?: string }>({})
const openSubmit = (row: PurchaseContractVO) => {
  submitDialog.id = row.id
  submitDialog.changeReason = undefined
  submitVisible.value = true
}
const handleSubmit = async () => {
  if (!submitDialog.id) return
  await PurchaseContractApi.submitPurchaseContract({ id: submitDialog.id, changeReason: submitDialog.changeReason })
  message.success('已送审')
  submitVisible.value = false
  await getList()
}

// ==================== 审核 ====================
const auditVisible = ref(false)
const auditForm = reactive<{ id?: number; approved: boolean; remark?: string }>({ approved: true })
const openAudit = (row: PurchaseContractVO) => {
  auditForm.id = row.id
  auditForm.approved = true
  auditForm.remark = undefined
  auditVisible.value = true
}
const handleAudit = async () => {
  if (!auditForm.id) return
  if (!auditForm.approved && !auditForm.remark?.trim()) {
    message.warning('驳回必须填写审核意见')
    return
  }
  await PurchaseContractApi.auditPurchaseContract({
    id: auditForm.id,
    approved: auditForm.approved,
    remark: auditForm.remark
  })
  message.success(auditForm.approved ? '已通过并生效' : '已驳回退回草稿')
  auditVisible.value = false
  await getList()
}

// ==================== 关闭 ====================
const closeVisible = ref(false)
const closeForm = reactive<{ id?: number; reason?: string }>({})
const openClose = (row: PurchaseContractVO) => {
  closeForm.id = row.id
  closeForm.reason = undefined
  closeVisible.value = true
}
const handleClose = async () => {
  if (!closeForm.id) return
  await PurchaseContractApi.closePurchaseContract({ id: closeForm.id, reason: closeForm.reason })
  message.success('已关闭')
  closeVisible.value = false
  await getList()
}

// ==================== 删除 ====================
const handleDelete = async (id?: number) => {
  if (!id) return
  await message.delConfirm()
  await PurchaseContractApi.deletePurchaseContract(id)
  message.success('已删除')
  await getList()
}

// ==================== 明细 ====================
const detailVisible = ref(false)
const detail = ref<PurchaseContractVO>()
const openDetail = async (row: PurchaseContractVO) => {
  detail.value = await PurchaseContractApi.getPurchaseContract(row.id!)
  detailVisible.value = true
}

getList()
</script>
