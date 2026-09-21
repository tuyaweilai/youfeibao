<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="88px"
    >
      <el-form-item label="姓名" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入姓名"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="身份证号" prop="idCardNo">
        <el-input
          v-model="queryParams.idCardNo"
          placeholder="请输入身份证号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input
          v-model="queryParams.mobile"
          placeholder="请输入手机号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="审核状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-160px">
          <el-option label="待审核" :value="0" />
          <el-option label="审核通过" :value="1" />
          <el-option label="审核拒绝" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['icbc:payee-info:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['icbc:payee-info:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="姓名" align="center" prop="name" min-width="100" />
      <el-table-column label="身份证号" align="center" prop="idCardNo" min-width="180" />
      <el-table-column label="手机号" align="center" prop="mobile" width="130" />
      <el-table-column label="银行卡号" align="center" prop="bankCardNo" min-width="170" />
      <el-table-column label="收款账户" align="center" width="150">
        <template #default="scope">
          <el-tag v-if="scope.row.bankCardChangeStatus === 0" type="warning">
            变更中（新卡尾号 {{ scope.row.bankCardChangeNewCardTail || '—' }}）
          </el-tag>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column label="开户银行" align="center" prop="bankName" min-width="140" />
      <el-table-column label="审核状态" align="center" prop="status" width="110">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="170"
      />
      <el-table-column label="操作" align="center" fixed="right" width="200">
        <template #default="scope">
          <el-button link type="primary" @click="openChangeDialog(scope.row)"> 换卡记录 </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['icbc:payee-info:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['icbc:payee-info:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 换卡记录弹窗：企业侧要能看清「钱会打到哪张卡、审核到哪一步」 -->
  <el-dialog v-model="changeDialogVisible" title="收款账户变更记录" width="760px">
    <el-table v-loading="changeLoading" :data="changeList" :stripe="true">
      <el-table-column label="变更单号" prop="changeNo" min-width="180" />
      <el-table-column label="原卡尾号" prop="oldCardTail" width="100" />
      <el-table-column label="新卡尾号" prop="newCardTail" width="100" />
      <el-table-column label="状态" align="center" width="120">
        <template #default="scope">
          <el-tag :type="changeTagType(scope.row.status)">{{ scope.row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发起时间" prop="requestedAt" :formatter="dateFormatter" width="170" />
      <el-table-column label="原因" prop="rejectReason" min-width="160" />
      <el-table-column label="操作" align="center" width="110">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 0"
            link
            type="danger"
            v-hasPermi="['icbc:payee-info:update']"
            @click="cancelChange(scope.row)"
          >
            取消变更
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="changeDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 表单弹窗：添加/修改 -->
  <PayeeForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { PayeeApi, PayeeVO, PayeeBankCardChangeVO } from '@/api/icbc/payee'
import PayeeForm from './PayeeForm.vue'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'

/** 出售者档案 列表 */
defineOptions({ name: 'IcbcPayee' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<PayeeVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  idCardNo: undefined,
  mobile: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await PayeeApi.getPayeePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await PayeeApi.deletePayee(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await PayeeApi.exportPayee(queryParams)
    download.excel(data, '出售者档案.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const statusMap: Record<number, { label: string; type: 'info' | 'success' | 'danger' }> = {
  0: { label: '待审核', type: 'info' },
  1: { label: '审核通过', type: 'success' },
  2: { label: '审核拒绝', type: 'danger' }
}
const statusLabel = (status?: number) =>
  status !== undefined && statusMap[status] ? statusMap[status].label : '未知'
const statusType = (status?: number): 'info' | 'success' | 'danger' =>
  status !== undefined && statusMap[status] ? statusMap[status].type : 'info'

// ==================== 换卡记录（#37） ====================

const changeDialogVisible = ref(false)
const changeLoading = ref(false)
const changeList = ref<PayeeBankCardChangeVO[]>([])

const openChangeDialog = async (row: PayeeVO) => {
  changeDialogVisible.value = true
  changeLoading.value = true
  try {
    changeList.value = await PayeeApi.getBankCardChangeList(row.id!)
  } finally {
    changeLoading.value = false
  }
}

const changeTagType = (status?: number): 'warning' | 'success' | 'danger' | 'info' => {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

const cancelChange = async (row: PayeeBankCardChangeVO) => {
  try {
    await message.confirm('取消后原卡继续有效，该出售者的付款随即恢复。确认取消这笔变更？')
  } catch {
    return
  }
  await PayeeApi.cancelBankCardChange(row.id!, '企业侧人工取消')
  message.success('已取消，原卡继续有效')
  changeList.value = await PayeeApi.getBankCardChangeList(row.payeeId!)
  await getList()
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>
