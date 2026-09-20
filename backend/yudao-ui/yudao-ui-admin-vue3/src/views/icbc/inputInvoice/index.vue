<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="登记单位供货方开给回收企业的进项发票（专票 / 普票），再勾稽到采购单据，让票、货、款三者对得上。"
    />
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="同一销方 + 发票号码只能登记一次；勾稽金额不得超过单据金额，也不得超过发票价税合计。已勾稽的票不能改，请先取消勾稽。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="发票号码" prop="invoiceNo">
        <el-input v-model="queryParams.invoiceNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="销方名称" prop="sellerName">
        <el-input v-model="queryParams.sellerName" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="票种" prop="invoiceType">
        <el-select v-model="queryParams.invoiceType" placeholder="请选择" clearable class="!w-180px">
          <el-option v-for="item in INPUT_INVOICE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-150px">
          <el-option v-for="item in INPUT_INVOICE_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openCreate" v-hasPermi="['icbc:input-invoice:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 登记进项票
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="发票号码" prop="invoiceNo" min-width="150" />
      <el-table-column label="票种" prop="invoiceTypeName" min-width="150" />
      <el-table-column label="开票日期" prop="invoiceDate" width="120" />
      <el-table-column label="销方" prop="sellerName" min-width="180" />
      <el-table-column label="不含税金额" prop="amount" width="120" align="right" />
      <el-table-column label="税额" prop="taxAmount" width="110" align="right" />
      <el-table-column label="价税合计" prop="totalAmount" width="120" align="right" />
      <el-table-column label="已勾稽" prop="linkedAmount" width="110" align="right" />
      <el-table-column label="可勾稽余额" prop="remainingAmount" width="120" align="right" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">勾稽</el-button>
          <el-button
            link
            type="primary"
            :disabled="row.status !== INPUT_INVOICE_STATUS_ENUM.REGISTERED"
            @click="openUpdate(row)"
            v-hasPermi="['icbc:input-invoice:manage']"
          >
            修改
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.status !== INPUT_INVOICE_STATUS_ENUM.REGISTERED"
            @click="handleDelete(row.id!)"
            v-hasPermi="['icbc:input-invoice:manage']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 登记 / 修改 -->
  <Dialog v-model="formVisible" :title="formData.id ? '修改进项票' : '登记进项票'" width="640px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
      <el-form-item label="发票号码" prop="invoiceNo">
        <el-input v-model="formData.invoiceNo" placeholder="票面发票号码" />
      </el-form-item>
      <el-form-item label="发票代码" prop="invoiceCode">
        <el-input v-model="formData.invoiceCode" placeholder="数电票可空" />
      </el-form-item>
      <el-form-item label="票种" prop="invoiceType">
        <el-select v-model="formData.invoiceType" class="!w-100%">
          <el-option v-for="item in INPUT_INVOICE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="开票日期" prop="invoiceDate">
        <el-date-picker v-model="formData.invoiceDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="!w-100%" />
      </el-form-item>
      <el-form-item label="销方名称" prop="sellerName">
        <el-input v-model="formData.sellerName" placeholder="单位供货方名称" />
      </el-form-item>
      <el-form-item label="销方税号" prop="sellerTaxNo">
        <el-input v-model="formData.sellerTaxNo" placeholder="为空时按销方名称判重" />
      </el-form-item>
      <el-form-item label="不含税金额" prop="amount">
        <el-input v-model="formData.amount" placeholder="0.00" />
      </el-form-item>
      <el-form-item label="税额" prop="taxAmount">
        <el-input v-model="formData.taxAmount" placeholder="0.00" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="formVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 勾稽记录 -->
  <Dialog v-model="detailVisible" :title="`进项票勾稽 · ${detail.invoiceNo || ''}`" width="900px">
    <el-descriptions :column="4" border class="mb-10px">
      <el-descriptions-item label="销方">{{ detail.sellerName || '—' }}</el-descriptions-item>
      <el-descriptions-item label="票种">{{ detail.invoiceTypeName || '—' }}</el-descriptions-item>
      <el-descriptions-item label="价税合计">{{ detail.totalAmount ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="可勾稽余额">{{ detail.remainingAmount ?? '—' }}</el-descriptions-item>
    </el-descriptions>

    <el-table :data="detail.links || []" :stripe="true">
      <el-table-column label="单据类型" prop="bizTypeName" min-width="110" />
      <el-table-column label="单据号" prop="bizNo" min-width="150" />
      <el-table-column label="单据金额" prop="bizAmount" width="120" align="right" />
      <el-table-column label="勾稽金额" prop="linkedAmount" width="120" align="right" />
      <el-table-column label="备注" prop="remark" min-width="120" />
      <el-table-column label="操作" align="center" width="110">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleUnlink(row.id!)" v-hasPermi="['icbc:input-invoice:manage']">
            取消勾稽
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">新增勾稽</el-divider>
    <el-form :model="linkForm" inline label-width="80px">
      <el-form-item label="单据类型">
        <el-select v-model="linkForm.bizType" class="!w-150px">
          <el-option v-for="item in INPUT_INVOICE_BIZ_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="linkForm.bizId" class="!w-140px" placeholder="单据主键 id" />
      </el-form-item>
      <el-form-item label="单据号">
        <el-input v-model="linkForm.bizNo" class="!w-170px" placeholder="如 PO20260601" />
      </el-form-item>
      <el-form-item label="单据金额">
        <el-input v-model="linkForm.bizAmount" class="!w-130px" placeholder="0.00" />
      </el-form-item>
      <el-form-item label="勾稽金额">
        <el-input v-model="linkForm.linkedAmount" class="!w-130px" placeholder="0.00" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="formLoading" @click="submitLink" v-hasPermi="['icbc:input-invoice:manage']">
          勾 稽
        </el-button>
      </el-form-item>
    </el-form>
    <div class="text-12px text-gray-500">
      勾稽金额不得超过单据金额，也不得超过发票的可勾稽余额。采购订单与入库单的单据编号 / 金额由使用方填写（对应票 #46 / #52 落地后接入选择器）。
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import {
  InputInvoiceApi,
  InputInvoiceVO,
  InputInvoiceLinkVO,
  INPUT_INVOICE_TYPE_OPTIONS,
  INPUT_INVOICE_STATUS_OPTIONS,
  INPUT_INVOICE_STATUS_ENUM,
  INPUT_INVOICE_BIZ_TYPE_OPTIONS
} from '@/api/icbc/inputInvoice'

defineOptions({ name: 'IcbcInputInvoice' })

const message = useMessage()

const loading = ref(true)
const formLoading = ref(false)
const list = ref<InputInvoiceVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  invoiceNo: undefined,
  sellerName: undefined,
  invoiceType: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await InputInvoiceApi.getInputInvoicePage(queryParams)
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
const statusTagType = (status?: number) => {
  if (status === INPUT_INVOICE_STATUS_ENUM.LINKED) return 'success'
  if (status === INPUT_INVOICE_STATUS_ENUM.PARTIALLY_LINKED) return 'warning'
  return 'info'
}

// ==================== 登记 / 修改 ====================
const formVisible = ref(false)
const formRef = ref()
const formData = reactive<InputInvoiceVO>({
  invoiceNo: '',
  invoiceCode: '',
  invoiceType: 1,
  invoiceDate: undefined,
  sellerName: '',
  sellerTaxNo: '',
  amount: undefined,
  taxAmount: 0,
  remark: ''
})
const formRules = {
  invoiceNo: [{ required: true, message: '发票号码不能为空', trigger: 'blur' }],
  invoiceType: [{ required: true, message: '票种不能为空', trigger: 'change' }],
  invoiceDate: [{ required: true, message: '开票日期不能为空', trigger: 'change' }],
  sellerName: [{ required: true, message: '销方名称不能为空', trigger: 'blur' }],
  amount: [{ required: true, message: '不含税金额不能为空', trigger: 'blur' }]
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    invoiceNo: '',
    invoiceCode: '',
    invoiceType: 1,
    invoiceDate: undefined,
    sellerName: '',
    sellerTaxNo: '',
    amount: undefined,
    taxAmount: 0,
    remark: ''
  })
}

const openCreate = () => {
  resetForm()
  formVisible.value = true
}

const openUpdate = (row: InputInvoiceVO) => {
  resetForm()
  Object.assign(formData, {
    id: row.id,
    invoiceNo: row.invoiceNo,
    invoiceCode: row.invoiceCode,
    invoiceType: row.invoiceType,
    invoiceDate: row.invoiceDate,
    sellerName: row.sellerName,
    sellerTaxNo: row.sellerTaxNo,
    amount: row.amount,
    taxAmount: row.taxAmount,
    remark: row.remark
  })
  formVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formData.id) {
      await InputInvoiceApi.updateInputInvoice(formData)
      message.success('已修改')
    } else {
      await InputInvoiceApi.createInputInvoice(formData)
      message.success('已登记')
    }
    formVisible.value = false
    await getList()
  } catch {
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await InputInvoiceApi.deleteInputInvoice(id)
    message.success('已删除')
    await getList()
  } catch {}
}

// ==================== 勾稽 ====================
const detailVisible = ref(false)
const detail = ref<InputInvoiceVO>({})
const linkForm = reactive<InputInvoiceLinkVO>({
  bizType: 'PURCHASE_ORDER',
  bizId: undefined,
  bizNo: '',
  bizAmount: undefined,
  linkedAmount: undefined,
  remark: ''
})

const openDetail = async (id?: number) => {
  detail.value = await InputInvoiceApi.getInputInvoice(id!)
  detailVisible.value = true
}

const resetLinkForm = () => {
  Object.assign(linkForm, {
    bizType: 'PURCHASE_ORDER',
    bizId: undefined,
    bizNo: '',
    bizAmount: undefined,
    linkedAmount: undefined,
    remark: ''
  })
}

const submitLink = async () => {
  if (!linkForm.bizId || !linkForm.bizAmount || !linkForm.linkedAmount) {
    message.error('单据编号、单据金额与勾稽金额都不能为空')
    return
  }
  formLoading.value = true
  try {
    await InputInvoiceApi.linkToBiz({ ...linkForm, invoiceId: detail.value.id })
    message.success('已勾稽')
    resetLinkForm()
    await openDetail(detail.value.id)
    await getList()
  } catch {
  } finally {
    formLoading.value = false
  }
}

const handleUnlink = async (linkId: number) => {
  try {
    await message.delConfirm('取消勾稽后该金额会回到可勾稽余额，确定吗？')
    await InputInvoiceApi.unlink(linkId)
    message.success('已取消勾稽')
    await openDetail(detail.value.id)
    await getList()
  } catch {}
}

onMounted(getList)
</script>
